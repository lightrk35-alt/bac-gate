package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BacGateCatalog
import com.example.data.model.GlobalBankStats
import com.example.data.model.StudyUnit
import com.example.data.model.UnitStatusSummary
import com.example.ui.components.BacGateTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.Zarqyne2DArenaBackground
import com.example.ui.components.ZarqyneActionButton
import com.example.ui.components.ZarqynePlate
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyElevated
import com.example.ui.theme.DeepOledBlack
import com.example.ui.theme.HairlineNavy
import com.example.ui.theme.HitEmerald
import com.example.ui.theme.HitEmeraldSurface
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ReviewAmber
import com.example.ui.theme.ReviewAmberSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.ZarqyneCyan
import com.example.ui.theme.ZarqyneCyanGlow
import com.example.ui.theme.ZarqyneViolet
import com.example.ui.theme.ZarqyneVioletGlow
import com.example.ui.theme.rememberChamferShape

@Composable
fun SubjectSelectionScreen(
    globalStats: GlobalBankStats?,
    onSelectHistory: () -> Unit
) {
    Zarqyne2DArenaBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BacGateTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.home_subtitle),
                canNavigateBack = false
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    ZarqynePlate(
                        cut = 20.dp,
                        containerColor = DarkNavyElevated,
                        borderColor = ZarqyneViolet.copy(alpha = 0.7f),
                        accentCornerColor = ZarqyneCyan,
                        borderWidth = 1.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatusPill(
                                label = "شعبة العلوم التجريبية • بوابة التحدي",
                                containerColor = ZarqyneCyanGlow,
                                contentColor = ZarqyneCyan
                            )
                            Text(
                                text = stringResource(R.string.choose_subject_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = SoftSilver
                            )
                            Text(
                                text = "ادخل إلى ساحة التاريخ واختبر سرعة استرجاعك للتواريخ المعلمية عبر جولات تفاعلية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSilver
                            )
                        }
                    }
                }

                item {
                    ZarqynePlate(
                        cut = 16.dp,
                        containerColor = DarkNavyCard,
                        borderColor = ZarqyneCyan.copy(alpha = 0.65f),
                        accentCornerColor = ZarqyneCyan,
                        borderWidth = 1.6.dp,
                        onClick = onSelectHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_history_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .background(ZarqyneCyanGlow, rememberChamferShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HistoryEdu,
                                    contentDescription = stringResource(R.string.subject_history),
                                    tint = ZarqyneCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.subject_history),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = SoftSilver
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.subject_history_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSilver
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = ZarqyneCyan
                            )
                        }
                    }
                }

                if (globalStats != null) {
                    item {
                        ZarqynePlate(
                            cut = 12.dp,
                            containerColor = DarkNavyCard.copy(alpha = 0.78f),
                            borderColor = HairlineNavy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "بنك البيانات المحلي • يعمل دون إنترنت",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZarqyneCyan
                                )
                                Text(
                                    text = "${globalStats.verifiedCount} تاريخًا معتمدًا للعب • ${globalStats.totalRecords} سجلًا مستوردًا • ${globalStats.conflictingCount} حالات تعارض معزولة.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSilver
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCategoriesScreen(
    globalStats: GlobalBankStats?,
    onSelectDatesCategory: () -> Unit,
    onOpenAuditRegistry: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Zarqyne2DArenaBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BacGateTopBar(
                title = stringResource(R.string.subject_history),
                subtitle = stringResource(R.string.choose_category_title),
                canNavigateBack = true,
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ZarqynePlate(
                        cut = 18.dp,
                        containerColor = DarkNavyElevated,
                        borderColor = ZarqyneCyan.copy(alpha = 0.75f),
                        accentCornerColor = ZarqyneViolet,
                        borderWidth = 1.6.dp,
                        onClick = onSelectDatesCategory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_dates_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(ZarqyneCyanGlow, rememberChamferShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = stringResource(R.string.category_dates),
                                    tint = ZarqyneCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                StatusPill(
                                    label = "MODE 01 • DATE QUIZ",
                                    containerColor = ZarqyneVioletGlow,
                                    contentColor = ZarqyneViolet
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.category_dates),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = SoftSilver
                                )
                                Text(
                                    text = stringResource(R.string.category_dates_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSilver
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = ZarqyneCyan
                            )
                        }
                    }
                }

                item {
                    ZarqynePlate(
                        cut = 14.dp,
                        containerColor = DarkNavyCard,
                        borderColor = HairlineNavy,
                        onClick = onOpenAuditRegistry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_registry_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ReviewAmberSurface, rememberChamferShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FactCheck,
                                    contentDescription = stringResource(R.string.audit_registry_title),
                                    tint = ReviewAmber
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.audit_registry_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SoftSilver
                                )
                                Text(
                                    text = if (globalStats != null) {
                                        "${globalStats.conflictingCount} تعارضات معزولة • ${globalStats.needsReviewCount} قيد التدقيق • ${globalStats.pendingUnitMappingCount} غير مصنفة الوحدة"
                                    } else {
                                        stringResource(R.string.audit_registry_desc)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSilver
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = MutedSilver
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitSelectionScreen(
    targetModeId: String,
    unitSummaries: List<UnitStatusSummary>,
    selectedUnit: StudyUnit?,
    onSelectUnit: (StudyUnit) -> Unit,
    onProceed: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Zarqyne2DArenaBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BacGateTopBar(
                title = stringResource(R.string.choose_unit_title),
                subtitle = "${BacGateCatalog.SUBJECT_HISTORY_TITLE} • ${BacGateCatalog.CATEGORY_DATES_TITLE}",
                canNavigateBack = true,
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            label = if (targetModeId == BacGateCatalog.MODE_01_ID) {
                                "النمط المحدد: MODE 01 — DATE QUIZ"
                            } else {
                                "النمط المحدد: $targetModeId"
                            },
                            containerColor = ZarqyneVioletGlow,
                            contentColor = ZarqyneViolet
                        )
                        Text(
                            text = stringResource(R.string.choose_unit_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSilver
                        )
                    }
                }

                items(unitSummaries, key = { it.unit.unitId }) { summary ->
                    val isSelected = summary.unit == selectedUnit
                    ZarqynePlate(
                        cut = 16.dp,
                        containerColor = if (isSelected) DarkNavyElevated else DarkNavyCard,
                        borderColor = if (isSelected) ZarqyneCyan else HairlineNavy,
                        accentCornerColor = if (isSelected) ZarqyneCyan else null,
                        borderWidth = if (isSelected) 2.dp else 1.2.dp,
                        onClick = { onSelectUnit(summary.unit) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unit_card_${summary.unit.unitId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isSelected) ZarqyneCyan else DarkNavyElevated,
                                        rememberChamferShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = summary.unit.arabicTitle,
                                    tint = if (isSelected) DeepOledBlack else ZarqyneCyan
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = summary.unit.arabicTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftSilver
                                )
                                if (summary.isPlayable) {
                                    StatusPill(
                                        label = "${summary.playableEligibleCount} سؤال جاهز للعب",
                                        containerColor = HitEmeraldSurface,
                                        contentColor = HitEmerald
                                    )
                                } else {
                                    StatusPill(
                                        label = "ينتظر المراجعة (${summary.needsReviewCount + summary.conflictingCount})",
                                        containerColor = ReviewAmberSurface,
                                        contentColor = ReviewAmber
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "محددة",
                                    tint = ZarqyneCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MutedSilver.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepOledBlack.copy(alpha = 0.88f))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                ZarqyneActionButton(
                    text = if (selectedUnit != null) {
                        "متابعة إلى ${BacGateCatalog.MODE_01_TITLE} (${selectedUnit.arabicTitle})"
                    } else {
                        "اختر وحدة دراسية أولاً"
                    },
                    onClick = onProceed,
                    enabled = selectedUnit != null,
                    isPrimaryCyan = true,
                    modifier = Modifier.testTag("proceed_unit_button")
                )
            }
        }
    }
}
