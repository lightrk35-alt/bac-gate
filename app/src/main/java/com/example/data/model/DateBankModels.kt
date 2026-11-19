package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class DatePrecision {
    @Json(name = "day") DAY,
    @Json(name = "month") MONTH,
    @Json(name = "year") YEAR,
    @Json(name = "range") RANGE,
    @Json(name = "unknown") UNKNOWN
}

@JsonClass(generateAdapter = false)
enum class DifficultyLevel(val arabicLabel: String, val tierOrder: Int) {
    @Json(name = "easy") EASY("سهل", 1),
    @Json(name = "medium") MEDIUM("متوسط", 2),
    @Json(name = "hard") HARD("صعب", 3),
    @Json(name = "unassigned") UNASSIGNED("غير مصنف", 0);

    fun nextTier(): DifficultyLevel = when (this) {
        EASY -> MEDIUM
        MEDIUM -> HARD
        HARD -> HARD
        UNASSIGNED -> EASY
    }
}

@JsonClass(generateAdapter = false)
enum class VerificationStatus(val arabicLabel: String) {
    @Json(name = "verified") VERIFIED("معتمد"),
    @Json(name = "needs_review") NEEDS_REVIEW("يحتاج مراجعة"),
    @Json(name = "conflicting") CONFLICTING("متعارض"),
    @Json(name = "rejected") REJECTED("مرفوض")
}

@JsonClass(generateAdapter = false)
enum class BranchEligibility(val arabicLabel: String) {
    @Json(name = "experimental_sciences_verified")
    EXPERIMENTAL_SCIENCES_VERIFIED("معتمد لشعبة العلوم التجريبية"),
    @Json(name = "needs_review")
    NEEDS_REVIEW("بانتظار تدقيق الشعبة"),
    @Json(name = "not_relevant")
    NOT_RELEVANT("غير معني بالشعبة")
}

@JsonClass(generateAdapter = true)
data class SourceReference(
    val sourceGroup: String,
    val sourceName: String,
    val rawDate: String,
    val rawEventText: String,
    val dateDisplay: String
)

@JsonClass(generateAdapter = true)
data class DateEventRecord(
    val id: String,
    val subjectId: String,
    val categoryId: String,
    val modeId: String,
    val unitId: String,
    val eventTitle: String,
    val rawEventText: String,
    val rawDate: String,
    val startDate: String?,
    val endDate: String?,
    val datePrecision: DatePrecision,
    val dateDisplay: String,
    val difficulty: DifficultyLevel,
    val explanation: String,
    val sourceName: String,
    val sourceReferences: List<SourceReference> = emptyList(),
    val sourceUrl: String?,
    val sourcePage: Int?,
    val verificationStatus: VerificationStatus,
    val branchEligibility: BranchEligibility,
    val reviewNotes: String,
    val sourceGroup: String
) {
    fun normalizedDateKey(): String = listOfNotNull(
        datePrecision.name,
        startDate,
        endDate,
        dateDisplay.trim()
    ).joinToString("|")
}

@JsonClass(generateAdapter = true)
data class DateBankEnvelope(
    val schemaVersion: Int,
    val project: String,
    val studio: String,
    val subjectId: String,
    val categoryId: String,
    val modeId: String,
    val totalRawRowsImported: Int,
    val totalMergedDuplicates: Int,
    val totalConflictingRecords: Int,
    val records: List<DateEventRecord>
)

enum class StudyUnit(val unitId: String, val arabicTitle: String, val unitNumber: Int) {
    UNIT_1("unit_1", "الوحدة الأولى", 1),
    UNIT_2("unit_2", "الوحدة الثانية", 2),
    UNIT_3("unit_3", "الوحدة الثالثة", 3);

    companion object {
        fun fromId(id: String?): StudyUnit? = entries.firstOrNull { it.unitId == id }
    }
}

data class UnitStatusSummary(
    val unit: StudyUnit,
    val totalRecordsInUnit: Int,
    val playableEligibleCount: Int,
    val verifiedCount: Int,
    val needsReviewCount: Int,
    val conflictingCount: Int,
    val rejectedCount: Int
) {
    val isPlayable: Boolean get() = playableEligibleCount > 0
}

data class GlobalBankStats(
    val totalRecords: Int,
    val totalRawRowsImported: Int,
    val totalMergedDuplicates: Int,
    val verifiedCount: Int,
    val needsReviewCount: Int,
    val conflictingCount: Int,
    val pendingUnitMappingCount: Int
)

object BacGateCatalog {
    const val SUBJECT_HISTORY_ID = "history"
    const val SUBJECT_HISTORY_TITLE = "التاريخ"
    const val CATEGORY_DATES_ID = "dates"
    const val CATEGORY_DATES_TITLE = "التواريخ"
    const val MODE_01_ID = "mode_01"
    const val MODE_01_TITLE = "Date Quiz"
}
