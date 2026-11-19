package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BacGateCatalog
import com.example.data.model.DatePrecision
import com.example.data.model.StudyUnit
import com.example.domain.mode01.DateQuizOption
import com.example.domain.mode01.Mode01SessionState
import com.example.domain.mode01.RoundDurationOption
import com.example.ui.components.AnimatedAccuracyGauge
import com.example.ui.components.BacGateTopBar
import com.example.ui.components.ChronoRingTimer
import com.example.ui.components.MetricStatRow
import com.example.ui.components.ParticleBurstOverlay
import com.example.ui.components.StatusPill
import com.example.ui.components.Zarqyne2DArenaBackground
import com.example.ui.components.ZarqyneActionButton
import com.example.ui.components.ZarqynePlate
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyElevated
import com.example.ui.theme.DeepOledBlack
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
import com.example.ui.theme.rememberChamferShape
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Mode01SessionScreen(
    state: Mode01SessionState,
    onSelectDuration: (RoundDurationOption) -> Unit,
    onStartRound: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onReplaySameUnit: () -> Unit,
    onBackToUnitSelection: () -> Unit,
    onBackToDatesCategory: () -> Unit,
    onOpenAuditForUnit: (StudyUnit) -> Unit
) {
    BackHandler(onBack = onBackToUnitSelection)

    when (state) {
        is Mode01SessionState.Setup -> Mode01SetupView(
            state = state,
            onSelectDuration = onSelectDuration,
            onStartRound = onStartRound,
            onBackToUnits = onBackToUnitSelection,
            onOpenAuditForUnit = onOpenAuditForUnit
        )

        is Mode01SessionState.ActiveQuestion -> Mode01GameplayView(
            state = state,
            onSelectOption = onSelectOption,
            onNextQuestion = onNextQuestion,
            onBack = onBackToUnitSelection
        )

        is Mode01SessionState.Finished -> Mode01ResultsView(
            state = state,
            onReplaySameUnit = onReplaySameUnit,
            onBackToUnitSelection = onBackToUnitSelection,
            onBackToDatesCategory = onBackToDatesCategory
        )
    }
}

@Composable
private fun Mode01SetupView(
    state: Mode01SessionState.Setup,
    onSelectDuration: (RoundDurationOption) -> Unit,
    onStartRound: () -> Unit,
    onBackToUnits: () -> Unit,
    onOpenAuditForUnit: (StudyUnit) -> Unit
) {
    val summary = state.unitSummary

    Zarqyne2DArenaBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BacGateTopBar(
                title = "${BacGateCatalog.MODE_01_TITLE} • ${summary.unit.arabicTitle}",
                subtitle = "${BacGateCatalog.SUBJECT_HISTORY_TITLE} / ${BacGateCatalog.CATEGORY_DATES_TITLE}",
                canNavigateBack = true,
                onBack = onBackToUnits
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ZarqynePlate(
                        cut = 18.dp,
                        containerColor = DarkNavyElevated,
                        borderColor = ZarqyneViolet.copy(alpha = 0.7f),
                        accentCornerColor = ZarqyneCyan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(
                                label = "MODE 01 • ${summary.unit.arabicTitle}",
                                containerColor = ZarqyneVioletGlow,
                                contentColor = ZarqyneViolet
                            )
                            Text(
                                text = stringResource(R.string.mode_01_title),
                                style = MaterialTheme.typography.displaySmall,
                                color = SoftSilver
                            )
                            Text(
                                text = stringResource(R.string.mode_01_arabic_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSilver
                            )
                        }
                    }
                }

                if (!summary.isPlayable) {
                    item {
                        ZarqynePlate(
                            cut = 16.dp,
                            containerColor = DarkNavyCard,
                            borderColor = ReviewAmber.copy(alpha = 0.65f),
                            accentCornerColor = ReviewAmber,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unit_awaiting_review_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(ReviewAmberSurface, rememberChamferShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.HourglassEmpty,
                                            contentDescription = stringResource(R.string.unit_awaiting_review_title),
                                            tint = ReviewAmber
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.unit_awaiting_review_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftSilver
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.unit_awaiting_review_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSilver
                                )

                                MetricStatRow(
                                    label = "إجمالي السجلات المستوردة في ${summary.unit.arabicTitle}",
                                    value = "${summary.totalRecordsInUnit}"
                                )
                                MetricStatRow(
                                    label = "سجلات قيد المراجعة",
                                    value = "${summary.needsReviewCount}",
                                    valueColor = ReviewAmber
                                )
                                MetricStatRow(
                                    label = "سجلات متعارضة في المصادر",
                                    value = "${summary.conflictingCount}",
                                    valueColor = ZarqyneViolet
                                )

                                ZarqyneActionButton(
                                    text = "مراجعة وتدقيق سجلات هذه الوحدة",
                                    onClick = { onOpenAuditForUnit(summary.unit) },
                                    isPrimaryCyan = false,
                                    modifier = Modifier.testTag("open_audit_from_setup"),
                                    leadingContent = {
                                        Icon(Icons.Filled.FactCheck, contentDescription = null, tint = SoftSilver)
                                    }
                                )

                                ZarqyneActionButton(
                                    text = stringResource(R.string.back_to_units),
                                    onClick = onBackToUnits,
                                    isPrimaryCyan = true,
                                    modifier = Modifier.testTag("back_to_units_from_setup")
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.select_round_duration),
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftSilver
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RoundDurationOption.entries.forEach { option ->
                                val selected = option == state.selectedDuration
                                val tag = if (option == RoundDurationOption.SECONDS_30) {
                                    "duration_30s"
                                } else {
                                    "duration_60s"
                                }
                                ZarqynePlate(
                                    cut = 14.dp,
                                    containerColor = if (selected) ZarqyneCyanGlow else DarkNavyCard,
                                    borderColor = if (selected) ZarqyneCyan else HairlineNavy,
                                    accentCornerColor = if (selected) ZarqyneCyan else null,
                                    borderWidth = if (selected) 2.dp else 1.2.dp,
                                    onClick = { onSelectDuration(option) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = option.arabicLabel,
                                            tint = if (selected) ZarqyneCyan else MutedSilver,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = option.arabicLabel,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) ZarqyneCyan else SoftSilver
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        ZarqynePlate(
                            cut = 12.dp,
                            containerColor = DarkNavyCard,
                            borderColor = HairlineNavy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                MetricStatRow(
                                    label = "الأسئلة المؤهلة في ${summary.unit.arabicTitle}",
                                    value = "${summary.playableEligibleCount}"
                                )
                                MetricStatRow(
                                    label = "نظام النقاط",
                                    value = "+10 للصحيح / 0 للخطأ"
                                )
                                MetricStatRow(
                                    label = "الصعوبة التدريجية",
                                    value = "سهل ← متوسط ← صعب"
                                )
                            }
                        }
                    }
                }
            }

            if (summary.isPlayable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepOledBlack.copy(alpha = 0.85f))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    ZarqyneActionButton(
                        text = stringResource(R.string.start_round),
                        onClick = onStartRound,
                        isPrimaryCyan = true,
                        modifier = Modifier.testTag("start_round_button"),
                        leadingContent = {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DeepOledBlack)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Mode01GameplayView(
    state: Mode01SessionState.ActiveQuestion,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onBack: () -> Unit
) {
    val remainingSeconds = ceil(state.remainingMillis / 1000.0).toInt().coerceAtLeast(0)
    val timerProgress = (state.remainingMillis.toFloat() / (state.durationSeconds * 1000f))
        .coerceIn(0f, 1f)

    val animatedScore by animateIntAsState(
        targetValue = state.score,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "hud_score_counter"
    )

    // Track real answer events for particle burst and floating +10 popup
    var burstTrigger by remember { mutableIntStateOf(0) }
    var lastBurstIsHit by remember { mutableStateOf(true) }
    var showPlusTenPopup by remember { mutableStateOf(false) }

    LaunchedEffect(state.questionsShownCount, state.isAnswerLocked) {
        if (state.isAnswerLocked) {
            val isCorrect = state.selectedOptionIndex
                ?.let { state.question.options.getOrNull(it)?.isCorrect } == true
            lastBurstIsHit = isCorrect
            burstTrigger += 1
            showPlusTenPopup = isCorrect
        } else {
            showPlusTenPopup = false
        }
    }

    val parallaxOffset = ((state.questionsShownCount % 5) - 2) * 0.25f

    Zarqyne2DArenaBackground(parallaxShiftFraction = parallaxOffset) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // === TOP 2D GAME HUD ===
                ZarqyneGameHud(
                    unitTitle = state.unit.arabicTitle,
                    remainingSeconds = remainingSeconds,
                    timerProgress = timerProgress,
                    isPaused = state.isTimerPaused,
                    animatedScore = animatedScore,
                    showPlusTenPopup = showPlusTenPopup,
                    questionNumber = state.questionsShownCount,
                    difficultyLabel = state.difficultyTracker.currentDifficulty.arabicLabel,
                    onBack = onBack
                )

                // === CENTER & OPTIONS ARENA ===
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val useTwoColumnGrid = maxWidth >= 280.dp &&
                        state.question.datePrecision != DatePrecision.RANGE

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "question_stage_${state.question.eventId}_${state.questionsShownCount}") {
                            AnimatedContent(
                                targetState = state.question,
                                transitionSpec = {
                                    (slideInHorizontally(tween(320)) { -it / 3 } + fadeIn(tween(260)))
                                        .togetherWith(
                                            slideOutHorizontally(tween(240)) { it / 3 } + fadeOut(tween(200))
                                        )
                                },
                                label = "question_monolith_transition"
                            ) { question ->
                                QuestionMonolithCard(
                                    promptQuestion = question.promptQuestion,
                                    eventTitle = question.eventTitle,
                                    questionKey = "${question.eventId}_${state.questionsShownCount}"
                                )
                            }
                        }

                        item(key = "options_deck_${state.question.eventId}_${state.questionsShownCount}") {
                            val options = state.question.options
                            if (useTwoColumnGrid && options.size == 4) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TacticalOptionTile(
                                            index = 0,
                                            option = options[0],
                                            isSelected = state.selectedOptionIndex == 0,
                                            isLocked = state.isAnswerLocked,
                                            onClick = { onSelectOption(0) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        TacticalOptionTile(
                                            index = 1,
                                            option = options[1],
                                            isSelected = state.selectedOptionIndex == 1,
                                            isLocked = state.isAnswerLocked,
                                            onClick = { onSelectOption(1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TacticalOptionTile(
                                            index = 2,
                                            option = options[2],
                                            isSelected = state.selectedOptionIndex == 2,
                                            isLocked = state.isAnswerLocked,
                                            onClick = { onSelectOption(2) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        TacticalOptionTile(
                                            index = 3,
                                            option = options[3],
                                            isSelected = state.selectedOptionIndex == 3,
                                            isLocked = state.isAnswerLocked,
                                            onClick = { onSelectOption(3) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    options.forEachIndexed { idx, opt ->
                                        TacticalOptionTile(
                                            index = idx,
                                            option = opt,
                                            isSelected = state.selectedOptionIndex == idx,
                                            isLocked = state.isAnswerLocked,
                                            onClick = { onSelectOption(idx) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // === BOTTOM FEEDBACK & NEXT QUESTION LAUNCH DOCK ===
                AnimatedVisibility(
                    visible = state.isAnswerLocked,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 2 }
                ) {
                    val pickedCorrect = state.selectedOptionIndex
                        ?.let { state.question.options.getOrNull(it)?.isCorrect } == true

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, DeepOledBlack.copy(alpha = 0.96f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("feedback_panel")
                    ) {
                        ZarqynePlate(
                            cut = 14.dp,
                            containerColor = if (pickedCorrect) HitEmeraldSurface else MissCrimsonSurface,
                            borderColor = if (pickedCorrect) HitEmerald else MissCrimson,
                            accentCornerColor = if (pickedCorrect) ZarqyneCyan else MissCrimson,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (pickedCorrect) {
                                        "إصابة دقيقة! +10 نقاط"
                                    } else {
                                        "إجابة غير صحيحة (0 نقطة) — الصواب: ${state.question.correctDateDisplay}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pickedCorrect) HitEmerald else MissCrimson
                                )

                                Text(
                                    text = state.question.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SoftSilver.copy(alpha = 0.9f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("question_explanation")
                                )

                                ZarqyneActionButton(
                                    text = stringResource(R.string.next_question),
                                    onClick = onNextQuestion,
                                    isPrimaryCyan = true,
                                    modifier = Modifier.testTag("next_question_button")
                                )
                            }
                        }
                    }
                }
            }

            // Real 2D Canvas particle burst triggered on answer feedback
            ParticleBurstOverlay(
                triggerKey = burstTrigger,
                isHit = lastBurstIsHit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ZarqyneGameHud(
    unitTitle: String,
    remainingSeconds: Int,
    timerProgress: Float,
    isPaused: Boolean,
    animatedScore: Int,
    showPlusTenPopup: Boolean,
    questionNumber: Int,
    difficultyLabel: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZarqynePlate(
            cut = 12.dp,
            containerColor = DarkNavy.copy(alpha = 0.92f),
            borderColor = HairlineNavy,
            accentCornerColor = ZarqyneCyan.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Exit / Back mini-button + Question & Tier
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ZarqynePlate(
                        cut = 7.dp,
                        containerColor = DarkNavyElevated,
                        borderColor = HairlineNavy,
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.matchParentSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = SoftSilver,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "$unitTitle • السؤال $questionNumber",
                            style = MaterialTheme.typography.labelLarge,
                            color = SoftSilver,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المستوى: $difficultyLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZarqyneViolet,
                            modifier = Modifier.testTag("difficulty_badge")
                        )
                    }
                }

                // Chrono Ring Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChronoRingTimer(
                        remainingSeconds = remainingSeconds,
                        progress = timerProgress,
                        isPaused = isPaused
                    )
                    Text(
                        text = "$remainingSeconds ث",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (remainingSeconds <= 5 && !isPaused) MissCrimson else ZarqyneCyan,
                        modifier = Modifier.testTag("timer_text")
                    )
                }

                // Score Counter + Floating +10 burst
                Box(contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "النقاط",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSilver
                        )
                        Text(
                            text = "$animatedScore",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZarqyneCyan,
                            modifier = Modifier.testTag("score_text")
                        )
                    }

                    this@Row.AnimatedVisibility(
                        visible = showPlusTenPopup,
                        enter = fadeIn(tween(150)) + slideInVertically(tween(260)) { it / 2 },
                        exit = fadeOut(tween(200))
                    ) {
                        Text(
                            text = "+10",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HitEmerald,
                            modifier = Modifier.offset(y = (-18).dp)
                        )
                    }
                }
            }
        }

        // Custom Canvas energy progress bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .testTag("round_progress_bar")
        ) {
            drawLine(
                color = HairlineNavy,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height
            )
            val fillWidth = size.width * timerProgress.coerceIn(0f, 1f)
            if (fillWidth > 0f) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = if (remainingSeconds <= 5 && !isPaused) {
                            listOf(MissCrimson, ZarqyneViolet)
                        } else {
                            listOf(ZarqyneCyan, ZarqyneViolet)
                        }
                    ),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(fillWidth, size.height / 2f),
                    strokeWidth = size.height
                )
            }
        }
    }
}

@Composable
private fun QuestionMonolithCard(
    promptQuestion: String,
    eventTitle: String,
    questionKey: String
) {
    val flipAnim = remember(questionKey) { Animatable(0f) }
    LaunchedEffect(questionKey) {
        flipAnim.snapTo(0f)
        flipAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f)
        )
    }

    val p = flipAnim.value
    val rotY = (1f - p) * -18f
    val scale = 0.92f + p * 0.08f

    ZarqynePlate(
        cut = 20.dp,
        containerColor = DarkNavy.copy(alpha = 0.94f),
        borderColor = ZarqyneViolet.copy(alpha = 0.75f),
        accentCornerColor = ZarqyneCyan,
        borderWidth = 1.6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotY
                scaleX = scale
                scaleY = scale
                alpha = p.coerceIn(0.3f, 1f)
                cameraDistance = 12f * density
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPill(
                label = promptQuestion,
                containerColor = ZarqyneCyanGlow,
                contentColor = ZarqyneCyan,
                modifier = Modifier.testTag("question_prompt")
            )

            Text(
                text = eventTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SoftSilver,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_event_title")
            )
        }
    }
}

@Composable
private fun TacticalOptionTile(
    index: Int,
    option: DateQuizOption,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shake animation when wrong answer is selected
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isLocked, isSelected, option.isCorrect) {
        if (isLocked && isSelected && !option.isCorrect) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360)
            )
        }
    }
    val shakeOffsetPx = if (isLocked && isSelected && !option.isCorrect && shakeAnim.value < 1f) {
        (sin(shakeAnim.value * Math.PI * 5) * 14f * (1f - shakeAnim.value)).roundToInt()
    } else {
        0
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (isLocked && option.isCorrect) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "option_hit_scale"
    )

    val containerColor = when {
        !isLocked -> DarkNavyCard
        option.isCorrect -> HitEmeraldSurface
        isSelected && !option.isCorrect -> MissCrimsonSurface
        else -> DarkNavy.copy(alpha = 0.45f)
    }

    val borderColor = when {
        !isLocked -> HairlineNavy
        option.isCorrect -> HitEmerald
        isSelected && !option.isCorrect -> MissCrimson
        else -> HairlineNavy.copy(alpha = 0.45f)
    }

    val textColor = when {
        !isLocked -> SoftSilver
        option.isCorrect -> HitEmerald
        isSelected && !option.isCorrect -> MissCrimson
        else -> MutedSilver.copy(alpha = 0.65f)
    }

    val accentCorner = when {
        !isLocked -> if (index % 2 == 0) ZarqyneCyan.copy(alpha = 0.55f) else ZarqyneViolet.copy(alpha = 0.55f)
        option.isCorrect -> HitEmerald
        isSelected && !option.isCorrect -> MissCrimson
        else -> null
    }

    ZarqynePlate(
        cut = 12.dp,
        containerColor = containerColor,
        borderColor = borderColor,
        accentCornerColor = accentCorner,
        borderWidth = if (isLocked && (option.isCorrect || isSelected)) 2.dp else 1.2.dp,
        onClick = onClick,
        enabled = !isLocked,
        modifier = modifier
            .heightIn(min = 56.dp)
            .offset { IntOffset(shakeOffsetPx, 0) }
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .testTag("option_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option.dateDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )

            if (isLocked && option.isCorrect) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "إجابة صحيحة",
                    tint = HitEmerald
                )
            } else if (isLocked && isSelected && !option.isCorrect) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "إجابة خاطئة",
                    tint = MissCrimson
                )
            }
        }
    }
}

@Composable
private fun Mode01ResultsView(
    state: Mode01SessionState.Finished,
    onReplaySameUnit: () -> Unit,
    onBackToUnitSelection: () -> Unit,
    onBackToDatesCategory: () -> Unit
) {
    val summary = state.summary
    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(summary) {
        entryAnim.snapTo(0f)
        entryAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f)
        )
    }

    val animatedScore by animateIntAsState(
        targetValue = summary.totalScore,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "results_score_countup"
    )

    Zarqyne2DArenaBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BacGateTopBar(
                title = stringResource(R.string.results_title),
                subtitle = "${summary.subjectTitle} • ${summary.categoryTitle} • ${summary.unit.arabicTitle}",
                canNavigateBack = false
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        val s = 0.92f + entryAnim.value * 0.08f
                        scaleX = s
                        scaleY = s
                        alpha = entryAnim.value
                    },
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ZarqynePlate(
                        cut = 20.dp,
                        containerColor = DarkNavyElevated,
                        borderColor = ZarqyneCyan.copy(alpha = 0.75f),
                        accentCornerColor = ZarqyneViolet,
                        borderWidth = 1.6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = stringResource(R.string.results_title),
                                        tint = ZarqyneCyan
                                    )
                                    Text(
                                        text = "حصيلة الجولة",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MutedSilver
                                    )
                                }
                                Text(
                                    text = "$animatedScore نقطة",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftSilver,
                                    modifier = Modifier.testTag("results_score")
                                )
                                StatusPill(
                                    label = "أعلى مستوى: ${summary.highestDifficulty.arabicLabel}",
                                    containerColor = ZarqyneVioletGlow,
                                    contentColor = ZarqyneViolet
                                )
                            }

                            AnimatedAccuracyGauge(accuracyPercent = summary.accuracyPercent)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResultStatTile(
                            label = "صحيحة",
                            value = "${summary.correctCount}",
                            accent = HitEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ResultStatTile(
                            label = "خاطئة",
                            value = "${summary.wrongCount}",
                            accent = MissCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        ResultStatTile(
                            label = "الأسئلة",
                            value = "${summary.questionsShownCount}",
                            accent = ZarqyneCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    ZarqynePlate(
                        cut = 14.dp,
                        containerColor = DarkNavyCard,
                        borderColor = HairlineNavy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            MetricStatRow("المادة", summary.subjectTitle)
                            MetricStatRow("القسم", summary.categoryTitle)
                            MetricStatRow("الوحدة المختارة", summary.unit.arabicTitle)
                            MetricStatRow("مدة الجولة", "${summary.durationSeconds} ثانية")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepOledBlack.copy(alpha = 0.88f))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZarqyneActionButton(
                    text = stringResource(R.string.replay_same_unit),
                    onClick = onReplaySameUnit,
                    isPrimaryCyan = true,
                    modifier = Modifier.testTag("replay_same_unit_button"),
                    leadingContent = {
                        Icon(Icons.Filled.Replay, contentDescription = null, tint = DeepOledBlack)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ZarqyneActionButton(
                        text = stringResource(R.string.back_to_units),
                        onClick = onBackToUnitSelection,
                        isPrimaryCyan = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("back_to_unit_selection_button")
                    )

                    ZarqynePlate(
                        cut = 10.dp,
                        containerColor = DarkNavyElevated,
                        borderColor = HairlineNavy,
                        onClick = onBackToDatesCategory,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("back_to_dates_category_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.back_to_dates),
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftSilver,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    ZarqynePlate(
        cut = 12.dp,
        containerColor = DarkNavyCard,
        borderColor = accent.copy(alpha = 0.55f),
        accentCornerColor = accent,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MutedSilver
            )
        }
    }
}
