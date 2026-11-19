package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BacGateDatabase
import com.example.data.model.BacGateCatalog
import com.example.data.model.DateEventRecord
import com.example.data.model.GlobalBankStats
import com.example.data.model.StudyUnit
import com.example.data.model.UnitStatusSummary
import com.example.data.model.VerificationStatus
import com.example.data.repository.AssetDateBankDataSource
import com.example.data.repository.DateBankRepository
import com.example.data.repository.RoomShuffleBagStore
import com.example.data.repository.ShuffleBagStore
import com.example.domain.mode01.DifficultyTrackerState
import com.example.domain.mode01.Mode01SessionReducer
import com.example.domain.mode01.Mode01SessionState
import com.example.domain.mode01.QuestionSelector
import com.example.domain.mode01.RoundDurationOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppDestination {
    data object SubjectSelection : AppDestination
    data object HistoryCategories : AppDestination
    data class UnitSelection(
        val targetModeId: String = BacGateCatalog.MODE_01_ID
    ) : AppDestination
    data class Mode01Session(val unit: StudyUnit) : AppDestination
    data class AuditRegistry(val initialUnitFilter: StudyUnit? = null) : AppDestination
}

enum class AuditFilterTab(val arabicLabel: String) {
    ALL("الكل"),
    CONFLICTING("متعارض"),
    NEEDS_REVIEW("يحتاج مراجعة"),
    PENDING_UNIT("بانتظار تصنيف الوحدة")
}

data class BacGateUiState(
    val destination: AppDestination = AppDestination.SubjectSelection,
    val selectedUnit: StudyUnit? = null,
    val unitSummaries: List<UnitStatusSummary> = emptyList(),
    val globalStats: GlobalBankStats? = null,
    val mode01State: Mode01SessionState? = null,
    val auditRecords: List<DateEventRecord> = emptyList(),
    val selectedAuditTab: AuditFilterTab = AuditFilterTab.CONFLICTING,
    val auditUnitFilter: StudyUnit? = null
)

class BacGateViewModel(
    private val dateBankRepository: DateBankRepository,
    private val shuffleBagStore: ShuffleBagStore,
    private val questionSelector: QuestionSelector = QuestionSelector(dateBankRepository, shuffleBagStore),
    private val reducer: Mode01SessionReducer = Mode01SessionReducer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BacGateUiState())
    val uiState: StateFlow<BacGateUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastDisplayedEventId: String? = null

    init {
        refreshBankMetadata()
    }

    fun refreshBankMetadata() {
        val summaries = StudyUnit.entries.map { dateBankRepository.getUnitStatusSummary(it) }
        val stats = dateBankRepository.getGlobalStats()
        _uiState.update { state ->
            state.copy(
                unitSummaries = summaries,
                globalStats = stats,
                auditRecords = filterAuditRecords(
                    tab = state.selectedAuditTab,
                    unitFilter = state.auditUnitFilter
                )
            )
        }
    }

    fun openHistoryCategories() {
        cancelTimer()
        _uiState.update { it.copy(destination = AppDestination.HistoryCategories) }
    }

    fun openUnitSelection(targetModeId: String = BacGateCatalog.MODE_01_ID) {
        cancelTimer()
        refreshBankMetadata()
        _uiState.update {
            it.copy(
                destination = AppDestination.UnitSelection(targetModeId = targetModeId),
                mode01State = null
            )
        }
    }

    fun selectUnit(unit: StudyUnit) {
        _uiState.update { it.copy(selectedUnit = unit) }
    }

    fun proceedWithSelectedUnit() {
        val unit = _uiState.value.selectedUnit ?: return
        enterMode01ForUnit(unit)
    }

    fun enterMode01ForUnit(unit: StudyUnit) {
        cancelTimer()
        val summary = dateBankRepository.getUnitStatusSummary(unit)
        _uiState.update {
            it.copy(
                selectedUnit = unit,
                destination = AppDestination.Mode01Session(unit),
                mode01State = Mode01SessionState.Setup(
                    unitSummary = summary,
                    selectedDuration = RoundDurationOption.SECONDS_30
                )
            )
        }
    }

    fun selectRoundDuration(duration: RoundDurationOption) {
        val current = _uiState.value.mode01State as? Mode01SessionState.Setup ?: return
        _uiState.update {
            it.copy(mode01State = current.copy(selectedDuration = duration))
        }
    }

    fun startRound() {
        val setup = _uiState.value.mode01State as? Mode01SessionState.Setup ?: return
        if (!setup.unitSummary.isPlayable) return

        val unit = setup.unitSummary.unit
        val duration = setup.selectedDuration

        viewModelScope.launch {
            val initialDifficulty = DifficultyTrackerState()
            val selection = questionSelector.selectNextQuestion(
                unit = unit,
                targetDifficulty = initialDifficulty.currentDifficulty,
                lastDisplayedEventId = lastDisplayedEventId
            )

            val question = selection.question ?: return@launch
            lastDisplayedEventId = question.eventId

            val active = Mode01SessionState.ActiveQuestion(
                unit = unit,
                durationSeconds = duration.seconds,
                remainingMillis = duration.seconds * 1000L,
                score = 0,
                correctCount = 0,
                wrongCount = 0,
                questionsShownCount = 1,
                difficultyTracker = initialDifficulty,
                question = question,
                startedNewCycleNotice = selection.startedNewCycle
            )
            _uiState.update { it.copy(mode01State = active) }
            startTimerLoop()
        }
    }

    fun selectAnswer(optionIndex: Int) {
        val active = _uiState.value.mode01State as? Mode01SessionState.ActiveQuestion ?: return
        val updated = reducer.selectAnswer(active, optionIndex)
        if (updated !== active) {
            cancelTimer()
            _uiState.update { it.copy(mode01State = updated) }
        }
    }

    fun nextQuestion() {
        val active = _uiState.value.mode01State as? Mode01SessionState.ActiveQuestion ?: return
        if (!active.isAnswerLocked) return
        if (active.remainingMillis <= 0L) {
            finishRound(active)
            return
        }

        viewModelScope.launch {
            val selection = questionSelector.selectNextQuestion(
                unit = active.unit,
                targetDifficulty = active.difficultyTracker.currentDifficulty,
                lastDisplayedEventId = lastDisplayedEventId
            )
            val nextQuestion = selection.question
            if (nextQuestion == null) {
                finishRound(active)
                return@launch
            }
            lastDisplayedEventId = nextQuestion.eventId

            val resumed = active.copy(
                questionsShownCount = active.questionsShownCount + 1,
                question = nextQuestion,
                selectedOptionIndex = null,
                isAnswerLocked = false,
                isTimerPaused = false,
                startedNewCycleNotice = selection.startedNewCycle
            )
            _uiState.update { it.copy(mode01State = resumed) }
            startTimerLoop()
        }
    }

    fun replaySameUnit() {
        val finished = _uiState.value.mode01State as? Mode01SessionState.Finished ?: return
        val unit = finished.summary.unit
        val duration = RoundDurationOption.entries.firstOrNull {
            it.seconds == finished.summary.durationSeconds
        } ?: RoundDurationOption.SECONDS_30
        val summary = dateBankRepository.getUnitStatusSummary(unit)
        _uiState.update {
            it.copy(
                selectedUnit = unit,
                destination = AppDestination.Mode01Session(unit),
                mode01State = Mode01SessionState.Setup(
                    unitSummary = summary,
                    selectedDuration = duration
                )
            )
        }
    }

    fun openAuditRegistry(unitFilter: StudyUnit? = null) {
        cancelTimer()
        val tab = _uiState.value.selectedAuditTab
        _uiState.update {
            it.copy(
                destination = AppDestination.AuditRegistry(unitFilter),
                auditUnitFilter = unitFilter,
                auditRecords = filterAuditRecords(tab, unitFilter)
            )
        }
    }

    fun selectAuditTab(tab: AuditFilterTab) {
        val unitFilter = _uiState.value.auditUnitFilter
        _uiState.update {
            it.copy(
                selectedAuditTab = tab,
                auditRecords = filterAuditRecords(tab, unitFilter)
            )
        }
    }

    fun selectAuditUnitFilter(unit: StudyUnit?) {
        val tab = _uiState.value.selectedAuditTab
        _uiState.update {
            it.copy(
                auditUnitFilter = unit,
                auditRecords = filterAuditRecords(tab, unit)
            )
        }
    }

    fun navigateBack() {
        when (val dest = _uiState.value.destination) {
            is AppDestination.SubjectSelection -> Unit
            is AppDestination.HistoryCategories -> {
                _uiState.update { it.copy(destination = AppDestination.SubjectSelection) }
            }
            is AppDestination.UnitSelection -> {
                _uiState.update { it.copy(destination = AppDestination.HistoryCategories) }
            }
            is AppDestination.Mode01Session -> {
                cancelTimer()
                _uiState.update {
                    it.copy(
                        destination = AppDestination.UnitSelection(BacGateCatalog.MODE_01_ID),
                        mode01State = null
                    )
                }
            }
            is AppDestination.AuditRegistry -> {
                val returnToUnit = dest.initialUnitFilter
                if (returnToUnit != null) {
                    enterMode01ForUnit(returnToUnit)
                } else {
                    _uiState.update { it.copy(destination = AppDestination.HistoryCategories) }
                }
            }
        }
    }

    private fun finishRound(active: Mode01SessionState.ActiveQuestion) {
        cancelTimer()
        val summary = reducer.buildSummary(
            unit = active.unit,
            durationSeconds = active.durationSeconds,
            score = active.score,
            correctCount = active.correctCount,
            wrongCount = active.wrongCount,
            questionsShownCount = active.questionsShownCount,
            highestDifficulty = active.difficultyTracker.highestDifficultyReached
        )
        _uiState.update { it.copy(mode01State = Mode01SessionState.Finished(summary)) }
    }

    private fun startTimerLoop() {
        cancelTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val current = _uiState.value.mode01State as? Mode01SessionState.ActiveQuestion
                if (current == null || current.isTimerPaused || current.isAnswerLocked) {
                    break
                }
                val nextState = reducer.tickTimer(current, 100L)
                _uiState.update { it.copy(mode01State = nextState) }
                if (nextState is Mode01SessionState.Finished) {
                    break
                }
            }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    internal fun tickTimerForTest(elapsedMillis: Long) {
        val current = _uiState.value.mode01State as? Mode01SessionState.ActiveQuestion ?: return
        val nextState = reducer.tickTimer(current, elapsedMillis)
        _uiState.update { it.copy(mode01State = nextState) }
        if (nextState is Mode01SessionState.Finished) {
            cancelTimer()
        }
    }

    private fun filterAuditRecords(
        tab: AuditFilterTab,
        unitFilter: StudyUnit?
    ): List<DateEventRecord> {
        val base = dateBankRepository.getAllRecords()
        val byUnit = if (unitFilter != null) {
            base.filter { it.unitId == unitFilter.unitId }
        } else {
            base
        }
        return when (tab) {
            AuditFilterTab.ALL -> byUnit
            AuditFilterTab.CONFLICTING -> byUnit.filter {
                it.verificationStatus == VerificationStatus.CONFLICTING
            }
            AuditFilterTab.NEEDS_REVIEW -> byUnit.filter {
                it.verificationStatus == VerificationStatus.NEEDS_REVIEW
            }
            AuditFilterTab.PENDING_UNIT -> base.filter {
                it.unitId == "pending_unit_mapping"
            }
        }
    }

    override fun onCleared() {
        cancelTimer()
        super.onCleared()
    }
}

class BacGateViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = BacGateDatabase.getInstance(context)
        val repository = DateBankRepository(AssetDateBankDataSource(context))
        val shuffleBagStore = RoomShuffleBagStore(db.shuffleBagDao())
        return BacGateViewModel(
            dateBankRepository = repository,
            shuffleBagStore = shuffleBagStore
        ) as T
    }
}
