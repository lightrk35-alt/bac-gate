package com.example.data.repository

import android.content.Context
import com.example.data.model.BranchEligibility
import com.example.data.model.DateBankEnvelope
import com.example.data.model.DateEventRecord
import com.example.data.model.DatePrecision
import com.example.data.model.GlobalBankStats
import com.example.data.model.StudyUnit
import com.example.data.model.UnitStatusSummary
import com.example.data.model.VerificationStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface DateBankDataSource {
    fun loadBank(): DateBankEnvelope
}

class AssetDateBankDataSource(
    private val context: Context,
    private val assetFileName: String = "date_bank.json"
) : DateBankDataSource {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override fun loadBank(): DateBankEnvelope {
        val jsonText = context.assets.open(assetFileName).bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
        val adapter = moshi.adapter(DateBankEnvelope::class.java)
        return requireNotNull(adapter.fromJson(jsonText)) {
            "Failed to parse $assetFileName"
        }
    }
}

class InMemoryDateBankDataSource(
    private val envelope: DateBankEnvelope
) : DateBankDataSource {
    override fun loadBank(): DateBankEnvelope = envelope
}

class DateBankRepository(
    private val dataSource: DateBankDataSource
) {
    private val cachedEnvelope: DateBankEnvelope by lazy { dataSource.loadBank() }

    fun getAllRecords(): List<DateEventRecord> = cachedEnvelope.records

    fun getRecordsForUnit(unit: StudyUnit): List<DateEventRecord> =
        cachedEnvelope.records.filter { it.unitId == unit.unitId }

    fun getPendingUnitMappingRecords(): List<DateEventRecord> =
        cachedEnvelope.records.filter { it.unitId == "pending_unit_mapping" }

    fun isRecordVerifiedAndBranchEligible(record: DateEventRecord, unit: StudyUnit): Boolean {
        return record.unitId == unit.unitId &&
            record.verificationStatus == VerificationStatus.VERIFIED &&
            record.branchEligibility == BranchEligibility.EXPERIMENTAL_SCIENCES_VERIFIED &&
            record.datePrecision != DatePrecision.UNKNOWN &&
            record.dateDisplay.isNotBlank()
    }

    /**
     * Returns only records that are strictly verified, eligible for Experimental Sciences,
     * belong to [unit], AND have at least 3 distinct same-unit, same-precision distractors.
     */
    fun getPlayableRecordsForUnit(unit: StudyUnit): List<DateEventRecord> {
        val eligibleInUnit = cachedEnvelope.records.filter {
            isRecordVerifiedAndBranchEligible(it, unit)
        }
        val distinctDatesByPrecision = eligibleInUnit
            .groupBy { it.datePrecision }
            .mapValues { (_, list) ->
                list.map { it.normalizedDateKey() }.distinct().size
            }

        return eligibleInUnit.filter { record ->
            (distinctDatesByPrecision[record.datePrecision] ?: 0) >= 4
        }
    }

    fun getEligibleDistractorPool(unit: StudyUnit, precision: DatePrecision): List<DateEventRecord> {
        return cachedEnvelope.records.filter {
            isRecordVerifiedAndBranchEligible(it, unit) && it.datePrecision == precision
        }
    }

    fun getUnitStatusSummary(unit: StudyUnit): UnitStatusSummary {
        val unitRecords = getRecordsForUnit(unit)
        val playable = getPlayableRecordsForUnit(unit)
        return UnitStatusSummary(
            unit = unit,
            totalRecordsInUnit = unitRecords.size,
            playableEligibleCount = playable.size,
            verifiedCount = unitRecords.count { isRecordVerifiedAndBranchEligible(it, unit) },
            needsReviewCount = unitRecords.count {
                it.verificationStatus == VerificationStatus.NEEDS_REVIEW ||
                    (it.verificationStatus == VerificationStatus.VERIFIED &&
                        it.branchEligibility != BranchEligibility.EXPERIMENTAL_SCIENCES_VERIFIED)
            },
            conflictingCount = unitRecords.count { it.verificationStatus == VerificationStatus.CONFLICTING },
            rejectedCount = unitRecords.count { it.verificationStatus == VerificationStatus.REJECTED }
        )
    }

    fun getGlobalStats(): GlobalBankStats {
        val all = cachedEnvelope.records
        return GlobalBankStats(
            totalRecords = all.size,
            totalRawRowsImported = cachedEnvelope.totalRawRowsImported,
            totalMergedDuplicates = cachedEnvelope.totalMergedDuplicates,
            verifiedCount = all.count {
                it.verificationStatus == VerificationStatus.VERIFIED &&
                    it.branchEligibility == BranchEligibility.EXPERIMENTAL_SCIENCES_VERIFIED
            },
            needsReviewCount = all.count { it.verificationStatus == VerificationStatus.NEEDS_REVIEW },
            conflictingCount = all.count { it.verificationStatus == VerificationStatus.CONFLICTING },
            pendingUnitMappingCount = all.count { it.unitId == "pending_unit_mapping" }
        )
    }
}
