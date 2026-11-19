package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyElevated
import com.example.ui.theme.DeepOledBlack
import com.example.ui.theme.HairlineNavy
import com.example.ui.theme.HitEmerald
import com.example.ui.theme.MissCrimson
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.ZarqyneCyan
import com.example.ui.theme.ZarqyneViolet
import com.example.ui.theme.rememberChamferShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class StarMote(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val isCyan: Boolean
)

private val ARENA_MOTES = List(22) { idx ->
    StarMote(
        xFraction = ((idx * 37 + 13) % 100) / 100f,
        yFraction = ((idx * 53 + 29) % 100) / 100f,
        radiusDp = 1.1f + (idx % 3) * 0.7f,
        speedFactor = 0.4f + (idx % 4) * 0.25f,
        isCyan = idx % 3 == 0
    )
}

/**
 * Animated 2D ZARQYNE arena background:
 * Deep OLED black (#000000) + Dark Navy (#0B1020) gradient, subtle parallax starfield,
 * orbital gate arcs, and faint perspective chrono-grid.
 */
@Composable
fun Zarqyne2DArenaBackground(
    parallaxShiftFraction: Float = 0f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arena_bg")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_phase"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepOledBlack, DarkNavy, DeepOledBlack)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.34f + parallaxShiftFraction * 18.dp.toPx())

            // Subtle ambient Violet & Cyan glow pools (restrained so OLED contrast stays sharp)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ZarqyneViolet.copy(alpha = pulseAlpha * 0.75f), Color.Transparent),
                    center = Offset(w * 0.82f, h * 0.16f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.82f, h * 0.16f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ZarqyneCyan.copy(alpha = pulseAlpha * 0.55f), Color.Transparent),
                    center = Offset(w * 0.18f, h * 0.78f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.18f, h * 0.78f)
            )

            // Chrono-gate orbital rings
            val ringRadii = listOf(w * 0.36f, w * 0.54f, w * 0.76f)
            ringRadii.forEachIndexed { idx, radius ->
                val startDeg = (driftPhase * 360f * if (idx % 2 == 0) 1f else -1f) + idx * 45f
                drawArc(
                    color = if (idx % 2 == 0) ZarqyneCyan.copy(alpha = 0.09f) else ZarqyneViolet.copy(alpha = 0.09f),
                    startAngle = startDeg,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Perspective horizon grid at bottom of arena
            val horizonY = h * 0.68f
            val lineColor = HairlineNavy.copy(alpha = 0.45f)
            for (i in -4..4) {
                val topX = w * 0.5f + i * (w * 0.08f)
                val bottomX = w * 0.5f + i * (w * 0.24f)
                drawLine(
                    color = lineColor,
                    start = Offset(topX, horizonY),
                    end = Offset(bottomX, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (row in 0..4) {
                val t = row / 4f
                val y = horizonY + (h - horizonY) * (t * t)
                drawLine(
                    color = lineColor.copy(alpha = 0.25f + t * 0.2f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Subtle drifting 2D motes
            ARENA_MOTES.forEach { mote ->
                val y = ((mote.yFraction + driftPhase * mote.speedFactor) % 1f) * h
                val x = (mote.xFraction * w + parallaxShiftFraction * mote.speedFactor * 12.dp.toPx())
                    .coerceIn(0f, w)
                drawCircle(
                    color = if (mote.isCyan) ZarqyneCyan.copy(alpha = 0.26f) else SoftSilver.copy(alpha = 0.16f),
                    radius = mote.radiusDp.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Chamfered 2D game plate with optional click interaction, real press-scale bounce,
 * and technical corner bracket accents.
 */
@Composable
fun ZarqynePlate(
    modifier: Modifier = Modifier,
    cut: Dp = 14.dp,
    containerColor: Color = DarkNavyCard,
    borderColor: Color = HairlineNavy,
    accentCornerColor: Color? = null,
    borderWidth: Dp = 1.2.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = rememberChamferShape(cut)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && enabled && isPressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "plate_press_scale"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(containerColor)
            .border(borderWidth, borderColor, shape)
            .drawBehind {
                if (accentCornerColor != null) {
                    val bracket = 12.dp.toPx()
                    val stroke = 2.dp.toPx()
                    // Top-left & bottom-right tactical corner ticks
                    drawLine(
                        color = accentCornerColor,
                        start = Offset(0f, cut.toPx()),
                        end = Offset(cut.toPx(), 0f),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = accentCornerColor,
                        start = Offset(cut.toPx(), 0f),
                        end = Offset(cut.toPx() + bracket, 0f),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = accentCornerColor,
                        start = Offset(size.width - cut.toPx() - bracket, size.height),
                        end = Offset(size.width - cut.toPx(), size.height),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = accentCornerColor,
                        start = Offset(size.width - cut.toPx(), size.height),
                        end = Offset(size.width, size.height - cut.toPx()),
                        strokeWidth = stroke
                    )
                }
            }
            .then(clickModifier),
        content = content
    )
}

@Composable
fun ZarqyneActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimaryCyan: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val accent = if (isPrimaryCyan) ZarqyneCyan else ZarqyneViolet
    val bgColor = if (!enabled) {
        DarkNavyElevated.copy(alpha = 0.45f)
    } else if (isPrimaryCyan) {
        ZarqyneCyan
    } else {
        ZarqyneViolet
    }
    val textColor = if (!enabled) {
        SoftSilver.copy(alpha = 0.4f)
    } else if (isPrimaryCyan) {
        DeepOledBlack
    } else {
        SoftSilver
    }

    ZarqynePlate(
        cut = 12.dp,
        containerColor = bgColor,
        borderColor = if (enabled) accent.copy(alpha = 0.9f) else HairlineNavy,
        borderWidth = 1.5.dp,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                if (leadingContent != null) {
                    leadingContent()
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ChronoRingTimer(
    remainingSeconds: Int,
    progress: Float,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "timer_ring_sweep"
    )
    val isUrgent = remainingSeconds <= 5 && !isPaused
    val ringColor = when {
        isPaused -> ZarqyneViolet
        isUrgent -> MissCrimson
        else -> ZarqyneCyan
    }

    val pulseTransition = rememberInfiniteTransition(label = "urgent_pulse")
    val urgentScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isUrgent) 1.07f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "urgent_scale"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = urgentScale
                scaleY = urgentScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = HairlineNavy,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$remainingSeconds",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isUrgent) MissCrimson else SoftSilver
        )
    }
}

/**
 * Triggered 2D particle burst overlay tied directly to gameplay answer events.
 */
@Composable
fun ParticleBurstOverlay(
    triggerKey: Int,
    isHit: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
        }
    }

    if (triggerKey > 0 && progress.value < 1f) {
        val p = progress.value
        val particleColor = if (isHit) ZarqyneCyan else MissCrimson
        val secondaryColor = if (isHit) HitEmerald else ZarqyneViolet

        Canvas(modifier = modifier.fillMaxSize()) {
            val origin = Offset(size.width * 0.5f, size.height * 0.58f)
            val maxRadius = size.width * 0.42f
            val alpha = (1f - p).coerceIn(0f, 1f)

            // Expanding shockwave ring
            drawCircle(
                color = particleColor.copy(alpha = alpha * 0.45f),
                radius = 18.dp.toPx() + maxRadius * 0.8f * p,
                center = origin,
                style = Stroke(width = (3.dp.toPx() * (1f - p)).coerceAtLeast(1f))
            )

            // Geometric diamond & spark particles
            val count = if (isHit) 16 else 10
            for (i in 0 until count) {
                val angle = (2.0 * PI * i / count) + (if (i % 2 == 0) 0.15 else -0.15)
                val dist = (24.dp.toPx() + maxRadius * p * (0.65f + (i % 3) * 0.2f))
                val cx = origin.x + (cos(angle) * dist).toFloat()
                val cy = origin.y + (sin(angle) * dist).toFloat()
                val r = ((if (isHit) 5.dp else 3.5.dp).toPx() * (1f - p * 0.7f)).coerceAtLeast(1.5f)
                val c = if (i % 2 == 0) particleColor else secondaryColor

                val diamond = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                drawPath(path = diamond, color = c.copy(alpha = alpha))
            }
        }
    }
}

@Composable
fun AnimatedAccuracyGauge(
    accuracyPercent: Int,
    modifier: Modifier = Modifier
) {
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(accuracyPercent) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(
            targetValue = (accuracyPercent / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = HairlineNavy,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(ZarqyneCyan, ZarqyneViolet, ZarqyneCyan)),
                startAngle = -90f,
                sweepAngle = 360f * sweepAnim.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$accuracyPercent%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ZarqyneCyan
            )
            Text(
                text = "الدقة",
                style = MaterialTheme.typography.labelMedium,
                color = SoftSilver.copy(alpha = 0.75f)
            )
        }
    }
}
