package com.example

import com.example.data.model.BranchEligibility
import com.example.data.model.DateBankEnvelope
import com.example.data.model.DateEventRecord
import com.example.data.model.DatePrecision
import com.example.data.model.DifficultyLevel
import com.example.data.model.StudyUnit
import com.example.data.model.VerificationStatus
import com.example.data.repository.DateBankRepository
import com.example.data.repository.InMemoryDateBankDataSource
import com.example.data.repository.InMemoryShuffleBagStore
import com.example.domain.mode01.DifficultyProgression
import com.example.domain.mode01.DifficultyTrackerState
import com.example.domain.mode01.Mode01SessionReducer
import com.example.domain.mode01.Mode01SessionState
import com.example.domain.mode01.QuestionSelector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Mode01EngineUnitTest {

    private fun makeRecord(
        id: String,
        unitId: String = "unit_1",
        precision: DatePrecision = DatePrecision.DAY,
        dateDisplay: String,
        startDate: String? = dateDisplay,
        difficulty: DifficultyLevel = DifficultyLevel.EASY,
        verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
        branchEligibility: BranchEligibility = BranchEligibility.EXPERIMENTAL_SCIENCES_VERIFIED
    ) = DateEventRecord(
        id = id,
        subjectId = "history",
        categoryId = "dates",
        modeId = "mode_01",
        unitId = unitId,
        eventTitle = "حدث $id",
        rawEventText = "حدث $id",
        rawDate = dateDisplay,
        startDate = startDate,
        endDate = null,
        datePrecision = precision,
        dateDisplay = dateDisplay,
        difficulty = difficulty,
        explanation = "شرح موثق للحدث $id",
        sourceName = "الكتاب المدرسي",
        sourceUrl = null,
        sourcePage = null,
        verificationStatus = verificationStatus,
        branchEligibility = branchEligibility,
        reviewNotes = "",
        sourceGroup = "${unitId}_primary"
    )

    private fun buildFixtureRepository(): DateBankRepository {
        val records = listOf(
            // Unit 1: 5 verified day records + 2 unverified/conflicting + 4 verified year records
            makeRecord("u1_d1", "unit_1", DatePrecision.DAY, "24 أكتوبر 1945", "1945-10-24", DifficultyLevel.EASY),
            makeRecord("u1_d2", "unit_1", DatePrecision.DAY, "12 مارس 1947", "1947-03-12", DifficultyLevel.EASY),
            makeRecord("u1_d3", "unit_1", DatePrecision.DAY, "05 جوان 1947", "1947-06-05", DifficultyLevel.EASY),
            makeRecord("u1_d4", "unit_1", DatePrecision.DAY, "04 أفريل 1949", "1949-04-04", DifficultyLevel.MEDIUM),
            makeRecord("u1_d5", "unit_1", DatePrecision.DAY, "14 ماي 1955", "1955-05-14", DifficultyLevel.HARD),
            // Needs review / conflicting / wrong branch in Unit 1 — MUST NEVER APPEAR
            makeRecord(
                "u1_needs_rev", "unit_1", DatePrecision.DAY, "06 أكتوبر 1947", "1947-10-06",
                verificationStatus = VerificationStatus.NEEDS_REVIEW
            ),
            makeRecord(
                "u1_conflict", "unit_1", DatePrecision.DAY, "08 ماي 1949", "1949-05-08",
                verificationStatus = VerificationStatus.CONFLICTING
            ),
            makeRecord(
                "u1_rejected", "unit_1", DatePrecision.DAY, "01 جويلية 1991", "1991-07-01",
                verificationStatus = VerificationStatus.REJECTED
            ),
            makeRecord(
                "u1_wrong_branch", "unit_1", DatePrecision.DAY, "25 ديسمبر 1991", "1991-12-25",
                branchEligibility = BranchEligibility.NOT_RELEVANT
            ),
            // Unit 2: 4 verified year records + 2 verified month records (< 4 month records, so month is unplayable!)
            makeRecord("u2_y1", "unit_2", DatePrecision.YEAR, "1953", "1953", DifficultyLevel.EASY),
            makeRecord("u2_y2", "unit_2", DatePrecision.YEAR, "1967", "1967", DifficultyLevel.EASY),
            makeRecord("u2_y3", "unit_2", DatePrecision.YEAR, "1969", "1969", DifficultyLevel.EASY),
            makeRecord("u2_y4", "unit_2", DatePrecision.YEAR, "1973", "1973", DifficultyLevel.EASY),
            makeRecord("u2_m1", "unit_2", DatePrecision.MONTH, "جويلية 1958", "1958-07", DifficultyLevel.EASY),
            makeRecord("u2_m2", "unit_2", DatePrecision.MONTH, "مارس 1962", "1962-03", DifficultyLevel.EASY)
        )

        return DateBankRepository(
            InMemoryDateBankDataSource(
                DateBankEnvelope(
                    schemaVersion = 1,
                    project = "BAC;GATE",
                    studio = "ZARQYNE",
                    subjectId = "history",
                    categoryId = "dates",
                    modeId = "mode_01",
                    totalRawRowsImported = records.size,
                    totalMergedDuplicates = 0,
                    totalConflictingRecords = 1,
                    records = records
                )
            )
        )
    }

    @Test
    fun `strictly filters out needs_review conflicting rejected and non_experimental_sciences`() {
        val repo = buildFixtureRepository()
        val playableU1 = repo.getPlayableRecordsForUnit(StudyUnit.UNIT_1)
        val ids = playableU1.map { it.id }.toSet()

        assertEquals(setOf("u1_d1", "u1_d2", "u1_d3", "u1_d4", "u1_d5"), ids)
        assertFalse("u1_needs_rev" in ids)
        assertFalse("u1_conflict" in ids)
        assertFalse("u1_rejected" in ids)
        assertFalse("u1_wrong_branch" in ids)
    }

    @Test
    fun `never shows questions without at least 3 same-unit same-precision distractors`() {
        val repo = buildFixtureRepository()
        val playableU2 = repo.getPlayableRecordsForUnit(StudyUnit.UNIT_2)
        // Month records (only 2 in unit_2) must NOT be included; only the 4 year records are playable
        assertEquals(setOf("u2_y1", "u2_y2", "u2_y3", "u2_y4"), playableU2.map { it.id }.toSet())
    }

    @Test
    fun `no cross-unit leakage and precision matches options strictly`() = runTest {
        val repo = buildFixtureRepository()
        val store = InMemoryShuffleBagStore()
        val selector = QuestionSelector(repo, store, Random(42))

        val u1Question = selector.selectNextQuestion(StudyUnit.UNIT_1, DifficultyLevel.EASY).question!!
        assertEquals(StudyUnit.UNIT_1, u1Question.unit)
        assertEquals(DatePrecision.DAY, u1Question.datePrecision)
        assertEquals("متى وقع الحدث؟", u1Question.promptQuestion)
        assertTrue(u1Question.options.all { it.sourceEventId.startsWith("u1_d") })
        assertEquals(4, u1Question.options.map { it.dateDisplay }.distinct().size)
        assertEquals(1, u1Question.options.count { it.isCorrect })

        val u2Question = selector.selectNextQuestion(StudyUnit.UNIT_2, DifficultyLevel.EASY).question!!
        assertEquals(StudyUnit.UNIT_2, u2Question.unit)
        assertEquals(DatePrecision.YEAR, u2Question.datePrecision)
        assertEquals("في أي سنة وقع الحدث؟", u2Question.promptQuestion)
        assertTrue(u2Question.options.all { it.sourceEventId.startsWith("u2_y") })
    }

    @Test
    fun `shuffle bag exhausts all unit questions before starting cycle 2`() = runTest {
        val repo = buildFixtureRepository()
        val store = InMemoryShuffleBagStore()
        val selector = QuestionSelector(repo, store, Random(7))

        val cycle1Ids = mutableListOf<String>()
        var lastId: String? = null
        repeat(5) {
            val res = selector.selectNextQuestion(
                unit = StudyUnit.UNIT_1,
                targetDifficulty = DifficultyLevel.EASY,
                lastDisplayedEventId = lastId
            )
            assertFalse(res.startedNewCycle)
            val q = res.question!!
            cycle1Ids.add(q.eventId)
            lastId = q.eventId
        }
        assertEquals(5, cycle1Ids.distinct().size)

        // 6th call triggers cycle 2 and does not repeat lastId immediately
        val cycle2Res = selector.selectNextQuestion(
            unit = StudyUnit.UNIT_1,
            targetDifficulty = DifficultyLevel.EASY,
            lastDisplayedEventId = lastId
        )
        assertTrue(cycle2Res.startedNewCycle)
        assertEquals(2, cycle2Res.question!!.cycleNumber)
        assertTrue(cycle2Res.question.eventId != lastId)
    }

    @Test
    fun `option positions shuffle and avoid previous cycle order`() = runTest {
        val repo = buildFixtureRepository()
        val store = InMemoryShuffleBagStore()
        val selector = QuestionSelector(repo, store, Random(99))

        val seenCorrectIndices = mutableSetOf<Int>()
        val firstOrderByEvent = mutableMapOf<String, List<String>>()
        var lastId: String? = null

        repeat(5) {
            val q = selector.selectNextQuestion(StudyUnit.UNIT_1, DifficultyLevel.EASY, lastId).question!!
            seenCorrectIndices.add(q.options.indexOfFirst { it.isCorrect })
            firstOrderByEvent[q.eventId] = q.options.map { it.sourceEventId }
            lastId = q.eventId
        }
        assertTrue("Expected correct option position to vary", seenCorrectIndices.size > 1)

        // Run second full cycle and confirm option order avoids repeating previous cycle's order
        repeat(5) {
            val q = selector.selectNextQuestion(StudyUnit.UNIT_1, DifficultyLevel.EASY, lastId).question!!
            val newOrder = q.options.map { it.sourceEventId }
            assertTrue(
                "Option order should differ from cycle 1 for ${q.eventId}",
                newOrder != firstOrderByEvent[q.eventId]
            )
            lastId = q.eventId
        }
    }

    @Test
    fun `scoring adds +10 for correct, 0 for wrong, locks answer, and timer ends round at zero`() = runTest {
        val repo = buildFixtureRepository()
        val store = InMemoryShuffleBagStore()
        val selector = QuestionSelector(repo, store, Random(1))
        val reducer = Mode01SessionReducer()

        val q = selector.selectNextQuestion(StudyUnit.UNIT_1, DifficultyLevel.EASY).question!!
        val correctIdx = q.options.indexOfFirst { it.isCorrect }
        val wrongIdx = q.options.indexOfFirst { !it.isCorrect }

        val active = Mode01SessionState.ActiveQuestion(
            unit = StudyUnit.UNIT_1,
            durationSeconds = 30,
            remainingMillis = 30_000L,
            score = 0,
            correctCount = 0,
            wrongCount = 0,
            questionsShownCount = 1,
            difficultyTracker = DifficultyTrackerState(),
            question = q
        )

        val afterCorrect = reducer.selectAnswer(active, correctIdx)
        assertEquals(10, afterCorrect.score)
        assertEquals(1, afterCorrect.correctCount)
        assertTrue(afterCorrect.isAnswerLocked)
        assertTrue(afterCorrect.isTimerPaused)

        // Second tap while locked must not change score
        val afterSecondTap = reducer.selectAnswer(afterCorrect, wrongIdx)
        assertEquals(10, afterSecondTap.score)
        assertEquals(1, afterSecondTap.correctCount)

        // Wrong answer on unlocked question yields +0
        val afterWrong = reducer.selectAnswer(active, wrongIdx)
        assertEquals(0, afterWrong.score)
        assertEquals(1, afterWrong.wrongCount)

        // Ticking while paused keeps remaining time frozen
        val afterPausedTick = reducer.tickTimer(afterCorrect, 5_000L) as Mode01SessionState.ActiveQuestion
        assertEquals(30_000L, afterPausedTick.remainingMillis)

        // Ticking down to zero finishes round immediately
        val finished = reducer.tickTimer(active, 30_000L)
        assertTrue(finished is Mode01SessionState.Finished)
        val summary = (finished as Mode01SessionState.Finished).summary
        assertEquals(30, summary.durationSeconds)
        assertEquals(StudyUnit.UNIT_1, summary.unit)
    }

    @Test
    fun `difficulty advances after 5 answers with at least 4 correct`() {
        var tracker = DifficultyTrackerState()
        assertEquals(DifficultyLevel.EASY, tracker.currentDifficulty)

        // 3 correct + 2 wrong -> stays EASY
        repeat(3) { tracker = DifficultyProgression.recordAnswer(tracker, true) }
        repeat(2) { tracker = DifficultyProgression.recordAnswer(tracker, false) }
        assertEquals(DifficultyLevel.EASY, tracker.currentDifficulty)

        // 4 correct + 1 wrong -> promotes to MEDIUM
        repeat(4) { tracker = DifficultyProgression.recordAnswer(tracker, true) }
        tracker = DifficultyProgression.recordAnswer(tracker, false)
        assertEquals(DifficultyLevel.MEDIUM, tracker.currentDifficulty)
        assertEquals(DifficultyLevel.MEDIUM, tracker.highestDifficultyReached)

        // 5 correct -> promotes to HARD
        repeat(5) { tracker = DifficultyProgression.recordAnswer(tracker, true) }
        assertEquals(DifficultyLevel.HARD, tracker.currentDifficulty)
        assertEquals(DifficultyLevel.HARD, tracker.highestDifficultyReached)
    }

    @Test
    fun `unit with zero verified records returns null question cleanly`() = runTest {
        val repo = buildFixtureRepository()
        val store = InMemoryShuffleBagStore()
        val selector = QuestionSelector(repo, store)
        val res = selector.selectNextQuestion(StudyUnit.UNIT_3, DifficultyLevel.EASY)
        assertNull(res.question)
        assertEquals(0, res.totalPlayableInUnit)
        assertNotNull(repo.getUnitStatusSummary(StudyUnit.UNIT_3))
    }
}
