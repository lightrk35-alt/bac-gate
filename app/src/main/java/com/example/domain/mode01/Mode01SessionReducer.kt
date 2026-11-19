package com.example.domain.mode01

import com.example.data.model.BacGateCatalog
import com.example.data.model.DifficultyLevel
import com.example.data.model.StudyUnit
import com.example.data.model.UnitStatusSummary

sealed interface Mode01SessionState {
    data class Setup(
        val unitSummary: UnitStatusSummary,
        val selectedDuration: RoundDurationOption = RoundDurationOption.SECONDS_30
    ) : Mode01SessionState

    data class ActiveQuestion(
        val unit: StudyUnit,
        val durationSeconds: Int,
        val remainingMillis: Long,
        val score: Int,
        val correctCount: Int,
        val wrongCount: Int,
        val questionsShownCount: Int,
        val difficultyTracker: DifficultyTrackerState,
        val question: DateQuizQuestion,
        val selectedOptionIndex: Int? = null,
        val isAnswerLocked: Boolean = false,
        val isTimerPaused: Boolean = false,
        val startedNewCycleNotice: Boolean = false
    ) : Mode01SessionState

    data class Finished(
        val summary: Mode01RoundSummary
    ) : Mode01SessionState
}

class Mode01SessionReducer {
    companion object {
        const val POINTS_PER_CORRECT = 10
        const val POINTS_PER_WRONG = 0
    }

    fun selectAnswer(
        state: Mode01SessionState.ActiveQuestion,
        optionIndex: Int
    ): Mode01SessionState.ActiveQuestion {
        if (state.isAnswerLocked || state.remainingMillis <= 0L) return state
        val option = state.question.options.getOrNull(optionIndex) ?: return state
        val isCorrect = option.isCorrect
        val newScore = state.score + if (isCorrect) POINTS_PER_CORRECT else POINTS_PER_WRONG
        val newCorrect = state.correctCount + if (isCorrect) 1 else 0
        val newWrong = state.wrongCount + if (isCorrect) 0 else 1
        val newDifficulty = DifficultyProgression.recordAnswer(state.difficultyTracker, isCorrect)

        return state.copy(
            score = newScore,
            correctCount = newCorrect,
            wrongCount = newWrong,
            difficultyTracker = newDifficulty,
            selectedOptionIndex = optionIndex,
            isAnswerLocked = true,
            isTimerPaused = true
        )
    }

    fun tickTimer(
        state: Mode01SessionState.ActiveQuestion,
        elapsedMillis: Long
    ): Mode01SessionState {
        if (state.isTimerPaused || state.isAnswerLocked) return state
        val nextRemaining = (state.remainingMillis - elapsedMillis).coerceAtLeast(0L)
        return if (nextRemaining == 0L) {
            Mode01SessionState.Finished(
                buildSummary(
                    unit = state.unit,
                    durationSeconds = state.durationSeconds,
                    score = state.score,
                    correctCount = state.correctCount,
                    wrongCount = state.wrongCount,
                    questionsShownCount = state.questionsShownCount,
                    highestDifficulty = state.difficultyTracker.highestDifficultyReached
                )
            )
        } else {
            state.copy(remainingMillis = nextRemaining)
        }
    }

    fun buildSummary(
        unit: StudyUnit,
        durationSeconds: Int,
        score: Int,
        correctCount: Int,
        wrongCount: Int,
        questionsShownCount: Int,
        highestDifficulty: DifficultyLevel
    ): Mode01RoundSummary {
        val totalAnswered = correctCount + wrongCount
        val accuracy = if (totalAnswered > 0) {
            ((correctCount * 100.0) / totalAnswered).toInt()
        } else {
            0
        }
        return Mode01RoundSummary(
            subjectTitle = BacGateCatalog.SUBJECT_HISTORY_TITLE,
            categoryTitle = BacGateCatalog.CATEGORY_DATES_TITLE,
            unit = unit,
            durationSeconds = durationSeconds,
            totalScore = score,
            correctCount = correctCount,
            wrongCount = wrongCount,
            questionsShownCount = questionsShownCount,
            accuracyPercent = accuracy,
            highestDifficulty = highestDifficulty
        )
    }
}
