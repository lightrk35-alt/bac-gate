package com.example.domain.mode01

import com.example.data.model.DifficultyLevel

object DifficultyProgression {
    const val WINDOW_SIZE = 5
    const val PROMOTION_THRESHOLD = 4

    fun recordAnswer(
        state: DifficultyTrackerState,
        isCorrect: Boolean
    ): DifficultyTrackerState {
        val newAnswered = state.answeredInWindow + 1
        val newCorrect = state.correctInWindow + if (isCorrect) 1 else 0

        return if (newAnswered >= WINDOW_SIZE) {
            val promoted = if (newCorrect >= PROMOTION_THRESHOLD) {
                state.currentDifficulty.nextTier()
            } else {
                state.currentDifficulty
            }
            val highest = if (promoted.tierOrder > state.highestDifficultyReached.tierOrder) {
                promoted
            } else {
                state.highestDifficultyReached
            }
            DifficultyTrackerState(
                currentDifficulty = promoted,
                highestDifficultyReached = highest,
                answeredInWindow = 0,
                correctInWindow = 0
            )
        } else {
            state.copy(
                answeredInWindow = newAnswered,
                correctInWindow = newCorrect
            )
        }
    }
}
