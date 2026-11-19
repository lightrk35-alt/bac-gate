package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AuditRegistryScreen
import com.example.ui.screens.HistoryCategoriesScreen
import com.example.ui.screens.Mode01SessionScreen
import com.example.ui.screens.SubjectSelectionScreen
import com.example.ui.screens.UnitSelectionScreen

@Composable
fun BacGateApp(viewModel: BacGateViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Crossfade(
        targetState = uiState.destination,
        label = "bacgate_nav"
    ) { dest ->
        when (dest) {
            is AppDestination.SubjectSelection -> {
                SubjectSelectionScreen(
                    globalStats = uiState.globalStats,
                    onSelectHistory = viewModel::openHistoryCategories
                )
            }

            is AppDestination.HistoryCategories -> {
                HistoryCategoriesScreen(
                    globalStats = uiState.globalStats,
                    onSelectDatesCategory = { viewModel.openUnitSelection() },
                    onOpenAuditRegistry = { viewModel.openAuditRegistry(null) },
                    onBack = viewModel::navigateBack
                )
            }

            is AppDestination.UnitSelection -> {
                UnitSelectionScreen(
                    targetModeId = dest.targetModeId,
                    unitSummaries = uiState.unitSummaries,
                    selectedUnit = uiState.selectedUnit,
                    onSelectUnit = viewModel::selectUnit,
                    onProceed = viewModel::proceedWithSelectedUnit,
                    onBack = viewModel::navigateBack
                )
            }

            is AppDestination.Mode01Session -> {
                val sessionState = uiState.mode01State
                if (sessionState != null) {
                    Mode01SessionScreen(
                        state = sessionState,
                        onSelectDuration = viewModel::selectRoundDuration,
                        onStartRound = viewModel::startRound,
                        onSelectOption = viewModel::selectAnswer,
                        onNextQuestion = viewModel::nextQuestion,
                        onReplaySameUnit = viewModel::replaySameUnit,
                        onBackToUnitSelection = { viewModel.openUnitSelection() },
                        onBackToDatesCategory = viewModel::openHistoryCategories,
                        onOpenAuditForUnit = { unit -> viewModel.openAuditRegistry(unit) }
                    )
                }
            }

            is AppDestination.AuditRegistry -> {
                AuditRegistryScreen(
                    records = uiState.auditRecords,
                    selectedTab = uiState.selectedAuditTab,
                    unitFilter = uiState.auditUnitFilter,
                    onSelectTab = viewModel::selectAuditTab,
                    onSelectUnitFilter = viewModel::selectAuditUnitFilter,
                    onBack = viewModel::navigateBack
                )
            }
        }
    }
}
