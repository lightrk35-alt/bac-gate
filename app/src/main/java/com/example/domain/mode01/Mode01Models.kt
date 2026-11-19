package com.example.domain.mode01

import com.example.data.model.DatePrecision
import com.example.data.model.DifficultyLevel
import com.example.data.model.StudyUnit

enum class RoundDurationOption(val seconds: Int, val arabicLabel: String) {
    SECONDS_30(30, "30 ثانية"),
    SECONDS_60(60, "60 ثانية")
}

data class DateQuizOption(
    val optionKey: String,
    val dateDisplay: String,
    val sourceEventId: String,
    val isCorrect: Boolean
)

data class DateQuizQuestion(
    val eventId: String,
    val unit: StudyUnit,
    val modeId: String,
    val promptQuestion: String,
    val eventTitle: String,
    val datePrecision: DatePrecision,
    val difficulty: DifficultyLevel,
    val options: List<DateQuizOption>,
    val explanation: String,
    val correctDateDisplay: String,
    val cycleNumber: Int
)

data class DifficultyTrackerState(
    val currentDifficulty: DifficultyLevel = DifficultyLevel.EASY,
    val highestDifficultyReached: DifficultyLevel = DifficultyLevel.EASY,
    val answeredInWindow: Int = 0,
    val correctInWindow: Int = 0
)

data class Mode01RoundSummary(
    val subjectTitle: String,
    val categoryTitle: String,
    val unit: StudyUnit,
    val durationSeconds: Int,
    val totalScore: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val questionsShownCount: Int,
    val accuracyPercent: Int,
    val highestDifficulty: DifficultyLevel
)
