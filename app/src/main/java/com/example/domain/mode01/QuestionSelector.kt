package com.example.domain.mode01

import com.example.data.model.BacGateCatalog
import com.example.data.model.DateEventRecord
import com.example.data.model.DatePrecision
import com.example.data.model.DifficultyLevel
import com.example.data.model.StudyUnit
import com.example.data.repository.DateBankRepository
import com.example.data.repository.ShuffleBagStore
import kotlin.math.abs
import kotlin.random.Random

data class QuestionSelectionResult(
    val question: DateQuizQuestion?,
    val startedNewCycle: Boolean,
    val totalPlayableInUnit: Int
)

class QuestionSelector(
    private val repository: DateBankRepository,
    private val shuffleBagStore: ShuffleBagStore,
    private val random: Random = Random.Default,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    fun buildUnitKey(
        unit: StudyUnit,
        subjectId: String = BacGateCatalog.SUBJECT_HISTORY_ID,
        categoryId: String = BacGateCatalog.CATEGORY_DATES_ID,
        modeId: String = BacGateCatalog.MODE_01_ID
    ): String = "$subjectId:$categoryId:$modeId:${unit.unitId}"

    suspend fun selectNextQuestion(
        unit: StudyUnit,
        targetDifficulty: DifficultyLevel,
        lastDisplayedEventId: String? = null,
        modeId: String = BacGateCatalog.MODE_01_ID
    ): QuestionSelectionResult {
        val playablePool = repository.getPlayableRecordsForUnit(unit)
        if (playablePool.isEmpty()) {
            return QuestionSelectionResult(
                question = null,
                startedNewCycle = false,
                totalPlayableInUnit = 0
            )
        }

        val unitKey = buildUnitKey(unit = unit, modeId = modeId)
        var snapshot = shuffleBagStore.getCycleSnapshot(unitKey)
        var startedNewCycle = false

        var unseenPool = playablePool.filter { it.id !in snapshot.shownEventIds }
        if (unseenPool.isEmpty()) {
            val nextCycle = snapshot.cycleNumber + 1
            shuffleBagStore.startNewCycle(
                unitKey = unitKey,
                newCycleNumber = nextCycle,
                startedAtEpochMs = clockMs()
            )
            snapshot = shuffleBagStore.getCycleSnapshot(unitKey)
            unseenPool = playablePool
            startedNewCycle = true
        }

        // Avoid immediate repeat across cycle boundary whenever more than 1 playable record exists.
        val candidatePool = if (unseenPool.size > 1 && lastDisplayedEventId != null) {
            unseenPool.filter { it.id != lastDisplayedEventId }.ifEmpty { unseenPool }
        } else {
            unseenPool
        }

        val chosenEvent = pickByNearestDifficulty(candidatePool, targetDifficulty)
        val distractorPool = repository.getEligibleDistractorPool(unit, chosenEvent.datePrecision)
        val options = buildShuffledOptions(
            unitKey = unitKey,
            target = chosenEvent,
            samePrecisionPool = distractorPool
        )

        // If for any reason 4 distinct options cannot be formed, return null rather than invalid choices.
        if (options.size < 4) {
            return QuestionSelectionResult(
                question = null,
                startedNewCycle = startedNewCycle,
                totalPlayableInUnit = playablePool.size
            )
        }

        // Mark question as USED immediately upon selection/display!
        shuffleBagStore.markQuestionShown(
            unitKey = unitKey,
            cycleNumber = snapshot.cycleNumber,
            eventId = chosenEvent.id,
            shownAtEpochMs = clockMs()
        )

        val effectiveDifficulty = when (chosenEvent.difficulty) {
            DifficultyLevel.UNASSIGNED -> targetDifficulty
            else -> chosenEvent.difficulty
        }

        val question = DateQuizQuestion(
            eventId = chosenEvent.id,
            unit = unit,
            modeId = modeId,
            promptQuestion = formatPromptForPrecision(chosenEvent.datePrecision),
            eventTitle = chosenEvent.eventTitle,
            datePrecision = chosenEvent.datePrecision,
            difficulty = effectiveDifficulty,
            options = options,
            explanation = chosenEvent.explanation.ifBlank {
                "التاريخ المعتمد لهذا الحدث هو: ${chosenEvent.dateDisplay}"
            },
            correctDateDisplay = chosenEvent.dateDisplay,
            cycleNumber = snapshot.cycleNumber
        )

        return QuestionSelectionResult(
            question = question,
            startedNewCycle = startedNewCycle,
            totalPlayableInUnit = playablePool.size
        )
    }

    private fun pickByNearestDifficulty(
        candidates: List<DateEventRecord>,
        target: DifficultyLevel
    ): DateEventRecord {
        val exactMatch = candidates.filter { it.difficulty == target }
        if (exactMatch.isNotEmpty()) {
            return exactMatch.shuffled(random).first()
        }

        val sortedTiers = candidates
            .groupBy { it.difficulty }
            .entries
            .sortedBy { (diff, _) ->
                if (diff == DifficultyLevel.UNASSIGNED) Int.MAX_VALUE
                else abs(diff.tierOrder - target.tierOrder)
            }

        val nearestBucket = sortedTiers.first().value
        return nearestBucket.shuffled(random).first()
    }

    private suspend fun buildShuffledOptions(
        unitKey: String,
        target: DateEventRecord,
        samePrecisionPool: List<DateEventRecord>
    ): List<DateQuizOption> {
        val targetDateKey = target.normalizedDateKey()
        val seenDateKeys = mutableSetOf(targetDateKey)
        val seenDisplays = mutableSetOf(target.dateDisplay.trim())

        val distinctDistractors = samePrecisionPool
            .asSequence()
            .filter { it.id != target.id }
            .shuffled(random)
            .filter { candidate ->
                val dKey = candidate.normalizedDateKey()
                val dDisp = candidate.dateDisplay.trim()
                if (dKey in seenDateKeys || dDisp in seenDisplays) {
                    false
                } else {
                    seenDateKeys.add(dKey)
                    seenDisplays.add(dDisp)
                    true
                }
            }
            .take(3)
            .toList()

        if (distinctDistractors.size < 3) {
            return emptyList()
        }

        val baseOptions = buildList {
            add(
                DateQuizOption(
                    optionKey = target.normalizedDateKey(),
                    dateDisplay = target.dateDisplay,
                    sourceEventId = target.id,
                    isCorrect = true
                )
            )
            distinctDistractors.forEach { d ->
                add(
                    DateQuizOption(
                        optionKey = d.normalizedDateKey(),
                        dateDisplay = d.dateDisplay,
                        sourceEventId = d.id,
                        isCorrect = false
                    )
                )
            }
        }

        val previousOrder = shuffleBagStore.getLastOptionOrder(unitKey, target.id)
        var shuffled = baseOptions.shuffled(random)
        if (previousOrder != null && previousOrder.size == 4) {
            var attempts = 0
            while (attempts < 10 && shuffled.map { it.sourceEventId } == previousOrder) {
                shuffled = baseOptions.shuffled(random)
                attempts++
            }
            if (shuffled.map { it.sourceEventId } == previousOrder) {
                shuffled = listOf(shuffled[1], shuffled[2], shuffled[3], shuffled[0])
            }
        }

        shuffleBagStore.saveLastOptionOrder(
            unitKey = unitKey,
            eventId = target.id,
            optionKeys = shuffled.map { it.sourceEventId }
        )

        return shuffled
    }

    companion object {
        fun formatPromptForPrecision(precision: DatePrecision): String = when (precision) {
            DatePrecision.DAY -> "متى وقع الحدث؟"
            DatePrecision.YEAR -> "في أي سنة وقع الحدث؟"
            DatePrecision.MONTH -> "في أي شهر وسنة وقع الحدث؟"
            DatePrecision.RANGE -> "ما الفترة التي امتد خلالها الحدث؟"
            DatePrecision.UNKNOWN -> "متى وقع الحدث؟"
        }
    }
}
