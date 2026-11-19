package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.DateEventRecord
import com.example.data.model.StudyUnit
import com.example.data.model.VerificationStatus
import com.example.ui.AuditFilterTab
import com.example.ui.components.BacGateTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.Zarqyne2DArenaBackground
import com.example.ui.components.ZarqynePlate
import com.example.ui.theme.ConflictViolet
import com.example.ui.theme.ConflictVioletSurface
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyElevated
import com.example.ui.theme.HairlineNavy
import com.example.ui.theme.HitEmerald
import com.example.ui.theme.HitEmeraldSurface
import com.example.ui.theme.MissCrimson
import com.example.ui.theme.MissCrimsonSurface
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ReviewAmber
import com.example.ui.theme.ReviewAmberSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.ZarqyneCyan
import com.example.ui.theme.ZarqyneCyanGlow
import com.example.ui.theme.ZarqyneViolet
import com.example.ui.theme.ZarqyneVioletGlow

@Composable
fun AuditRegistryScreen(
    records: List<DateEventRecord>,
    selectedTab: AuditFilterTab,
    unitFilter: StudyUnit?,
    onSelectTab: (AuditFilterTab) -> Unit,
    onSelectUnitFilter: (StudyUnit?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Zarqyne2DArenaBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BacGateTopBar(
                title = stringResource(R.string.audit_registry_title),
                subtitle = "${records.size} سجل",
                canNavigateBack = true,
                onBack = onBack
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditFilterTab.entries.forEach { tab ->
                    ZarqyneFilterTag(
                        label = tab.arabicLabel,
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.testTag("audit_tab_${tab.name}")
                    )
                }
            }

            if (selectedTab != AuditFilterTab.PENDING_UNIT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZarqyneFilterTag(
                        label = "كل الوحدات",
                        selected = unitFilter == null,
                        onClick = { onSelectUnitFilter(null) }
                    )
                    StudyUnit.entries.forEach { unit ->
                        ZarqyneFilterTag(
                            label = unit.arabicTitle,
                            selected = unitFilter == unit,
                            onClick = { onSelectUnitFilter(unit) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.id }) { rec ->
                    AuditRecordCard(record = rec)
                }
            }
        }
    }
}

@Composable
private fun ZarqyneFilterTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZarqynePlate(
        cut = 8.dp,
        containerColor = if (selected) ZarqyneCyanGlow else DarkNavyElevated,
        borderColor = if (selected) ZarqyneCyan else HairlineNavy,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) ZarqyneCyan else SoftSilver
            )
        }
    }
}

@Composable
private fun AuditRecordCard(record: DateEventRecord) {
    val (statusBg, statusFg) = when (record.verificationStatus) {
        VerificationStatus.VERIFIED -> HitEmeraldSurface to HitEmerald
        VerificationStatus.NEEDS_REVIEW -> ReviewAmberSurface to ReviewAmber
        VerificationStatus.CONFLICTING -> ConflictVioletSurface to ConflictViolet
        VerificationStatus.REJECTED -> MissCrimsonSurface to MissCrimson
    }
    val unitLabel = StudyUnit.fromId(record.unitId)?.arabicTitle ?: "بانتظار تحديد الوحدة"

    ZarqynePlate(
        cut = 14.dp,
        containerColor = DarkNavyCard,
        borderColor = HairlineNavy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(
                    label = record.verificationStatus.arabicLabel,
                    containerColor = statusBg,
                    contentColor = statusFg
                )
                StatusPill(
                    label = unitLabel,
                    containerColor = ZarqyneVioletGlow,
                    contentColor = ZarqyneViolet
                )
            }

            Text(
                text = record.eventTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftSilver
            )

            Text(
                text = "التاريخ الأصلي: ${record.rawDate}  •  العرض: ${record.dateDisplay}",
                style = MaterialTheme.typography.bodyMedium,
                color = ZarqyneCyan
            )

            if (record.sourceReferences.size > 1) {
                HorizontalDivider(color = HairlineNavy)
                Text(
                    text = "المراجع المسجلة (${record.sourceReferences.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftSilver
                )
                record.sourceReferences.forEach { ref ->
                    Text(
                        text = "• ${ref.sourceName}: ${ref.rawDate} — ${ref.rawEventText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSilver
                    )
                }
            } else {
                Text(
                    text = "المصدر: ${record.sourceName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSilver
                )
            }

            if (record.reviewNotes.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.reviewNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSilver
                )
            }
        }
    }
}
