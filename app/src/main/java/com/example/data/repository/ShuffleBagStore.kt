package com.example.data.repository

import com.example.data.local.QuestionOptionOrderEntity
import com.example.data.local.ShownQuestionEntity
import com.example.data.local.ShuffleBagDao
import com.example.data.local.UnitCycleStateEntity

data class CycleSnapshot(
    val cycleNumber: Int,
    val cycleStartedAtEpochMs: Long,
    val shownEventIds: Set<String>
)

interface ShuffleBagStore {
    suspend fun getCycleSnapshot(unitKey: String): CycleSnapshot
    suspend fun startNewCycle(unitKey: String, newCycleNumber: Int, startedAtEpochMs: Long)
    suspend fun markQuestionShown(unitKey: String, cycleNumber: Int, eventId: String, shownAtEpochMs: Long)
    suspend fun getLastOptionOrder(unitKey: String, eventId: String): List<String>?
    suspend fun saveLastOptionOrder(unitKey: String, eventId: String, optionKeys: List<String>)
}

class RoomShuffleBagStore(
    private val dao: ShuffleBagDao
) : ShuffleBagStore {
    override suspend fun getCycleSnapshot(unitKey: String): CycleSnapshot {
        val existing = dao.getCycleState(unitKey)
        val state = if (existing == null) {
            val initial = UnitCycleStateEntity(
                unitKey = unitKey,
                cycleNumber = 1,
                cycleStartedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertCycleState(initial)
            initial
        } else {
            existing
        }
        val shown = dao.getShownEventIds(unitKey, state.cycleNumber).toSet()
        return CycleSnapshot(
            cycleNumber = state.cycleNumber,
            cycleStartedAtEpochMs = state.cycleStartedAtEpochMs,
            shownEventIds = shown
        )
    }

    override suspend fun startNewCycle(unitKey: String, newCycleNumber: Int, startedAtEpochMs: Long) {
        dao.upsertCycleState(
            UnitCycleStateEntity(
                unitKey = unitKey,
                cycleNumber = newCycleNumber,
                cycleStartedAtEpochMs = startedAtEpochMs
            )
        )
    }

    override suspend fun markQuestionShown(
        unitKey: String,
        cycleNumber: Int,
        eventId: String,
        shownAtEpochMs: Long
    ) {
        dao.insertShownQuestion(
            ShownQuestionEntity(
                unitKey = unitKey,
                cycleNumber = cycleNumber,
                eventId = eventId,
                shownAtEpochMs = shownAtEpochMs
            )
        )
    }

    override suspend fun getLastOptionOrder(unitKey: String, eventId: String): List<String>? {
        val csv = dao.getLastOptionOrder(unitKey, eventId) ?: return null
        return csv.split(",").filter { it.isNotEmpty() }
    }

    override suspend fun saveLastOptionOrder(unitKey: String, eventId: String, optionKeys: List<String>) {
        dao.upsertLastOptionOrder(
            QuestionOptionOrderEntity(
                unitKey = unitKey,
                eventId = eventId,
                lastOptionKeysCsv = optionKeys.joinToString(",")
            )
        )
    }
}

class InMemoryShuffleBagStore : ShuffleBagStore {
    private val cycles = mutableMapOf<String, Pair<Int, Long>>()
    private val shown = mutableMapOf<Pair<String, Int>, MutableSet<String>>()
    private val optionOrders = mutableMapOf<Pair<String, String>, List<String>>()

    override suspend fun getCycleSnapshot(unitKey: String): CycleSnapshot {
        val (cycleNum, startedAt) = cycles.getOrPut(unitKey) { 1 to System.currentTimeMillis() }
        val shownIds = shown[unitKey to cycleNum]?.toSet() ?: emptySet()
        return CycleSnapshot(cycleNum, startedAt, shownIds)
    }

    override suspend fun startNewCycle(unitKey: String, newCycleNumber: Int, startedAtEpochMs: Long) {
        cycles[unitKey] = newCycleNumber to startedAtEpochMs
    }

    override suspend fun markQuestionShown(
        unitKey: String,
        cycleNumber: Int,
        eventId: String,
        shownAtEpochMs: Long
    ) {
        shown.getOrPut(unitKey to cycleNumber) { mutableSetOf() }.add(eventId)
    }

    override suspend fun getLastOptionOrder(unitKey: String, eventId: String): List<String>? {
        return optionOrders[unitKey to eventId]
    }

    override suspend fun saveLastOptionOrder(unitKey: String, eventId: String, optionKeys: List<String>) {
        optionOrders[unitKey to eventId] = optionKeys
    }
}
