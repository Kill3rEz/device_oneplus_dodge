/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.device.settings.ui.banner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import org.lineageos.device.settings.R
import java.util.Locale

/** Layout is in units of a 358-wide banner. */
private const val DESIGN_W = 358f

private const val RR_MARK_W = 716f
private const val RR_MARK =
    "M430.4 0L703.1 0A12.2 12.2 0 0 1 714.6 16.2C697.7 65.5 668.3 118 632.3 118L437.3 118" +
        "C372 118 321.8 173.6 321.5 174L137.5 398C132.6 404 84.6 442 3.7 390.8A8 8 0 0 1 1.8 379" +
        "L217 117C276 45.2 356.2 0 430.4 0Z" +
        "M483.5 187L697.6 187A11.1 11.1 0 0 1 708 201.8C681.8 274.3 654.4 283 641.2 283L482.9 283" +
        "C454.1 283 414.3 301.8 387.7 334.2L245.5 507.3C245.2 507.7 208.2 545 139.2 502" +
        "A8.2 8.2 0 0 1 137.2 489.8L307 283.1C361.8 216.4 426.6 187 483.5 187Z"

/** The 1+ in an 86-unit box whose origin is (14, 9). */
private const val ONE_PLUS = "M59 26H22V86H82V50M44 47L53 42V72M78 15V44M64 29.5H92"

private val rrMark: Path by lazy { PathParser().parsePathString(RR_MARK).toPath() }
private val onePlus: Path by lazy { PathParser().parsePathString(ONE_PLUS).toPath() }

private val COL_LABEL = Color(0xFFA9AEB5)
private val COL_VALUE = Color.White
private val COL_TRACK = Color.White.copy(alpha = 0.09f)
private val CARD_BASE = Color(0xFF0A0A0C)

private val Poppins = FontFamily(Font(R.font.poppins_semibold, FontWeight.SemiBold))

private val Tight = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Immutable
private class BannerColors(
    val accent: Color,
    val bright: Color,
    val text: Color,
    val deep: Color,
)

@Composable
private fun bannerColors() = BannerColors(
    accent = colorResource(android.R.color.system_accent1_400),
    bright = colorResource(android.R.color.system_accent1_300),
    text = colorResource(android.R.color.system_accent1_200),
    deep = colorResource(android.R.color.system_accent1_900),
)

/** Expanded header: neon RR card with the device line and four ring gauges. */
@Composable
fun LiveBanner(
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = bannerColors()
    val sku = remember { BannerFacts.productSku() }
    val model = remember(sku) { BannerFacts.modelName(sku) }
    var snap by remember { mutableStateOf(BannerSnapshot()) }
    var health by remember { mutableIntStateOf(-1) }
    DisposableEffect(active) {
        if (active) {
            snap = BannerFacts.readVolatile(context)
            if (health < 0) {
                health = BannerFacts.readHealth()
            }
        }
        onDispose { }
    }
    BoxWithConstraints(modifier.cardBackground(colors)) {
        val u = maxWidth / DESIGN_W
        val ts = { n: Float -> with(density) { (u * n).toSp() } }
        Canvas(Modifier.fillMaxSize()) {
            drawMarkOutline(
                left = size.width - (u * 130).toPx(),
                top = -(u * 34).toPx(),
                width = (u * 180).toPx(),
                color = colors.accent,
                alpha = 0.45f,
            )
        }
        Column(
            modifier = Modifier.padding(start = u * 18, top = u * 54),
            verticalArrangement = Arrangement.spacedBy(u * 6),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u * 7),
            ) {
                OnePlusGlyph(size = u * 14, color = colors.bright)
                Text(
                    text = "SETTINGS",
                    color = colors.text,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = ts(10f),
                    lineHeight = ts(10f),
                    letterSpacing = 0.2.em,
                    maxLines = 1,
                    style = Tight,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u * 9),
            ) {
                Text(
                    text = "OnePlus $model",
                    color = COL_VALUE,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = ts(24f),
                    lineHeight = ts(26f),
                    maxLines = 1,
                    softWrap = false,
                    style = Tight,
                )
                if (sku != "--") {
                    Text(
                        text = sku,
                        color = Color(0xFFE6E1E5),
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = ts(10f),
                        lineHeight = ts(10f),
                        maxLines = 1,
                        style = Tight,
                        modifier = Modifier
                            .border(u * 1, colors.accent.copy(alpha = 0.5f), CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                            .padding(horizontal = u * 8, vertical = u * 5),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = u * 14, end = u * 14, bottom = u * 14),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Gauge(
                progress = ratio(snap.ramUsedGb, snap.ramTotalGb),
                big = if (snap.ramTotalGb <= 0f) "--"
                else String.format(Locale.US, "%.1fG", snap.ramUsedGb),
                small = if (snap.ramTotalGb <= 0f) ""
                else String.format(Locale.US, "of %.0fG", snap.ramTotalGb),
                label = "RAM",
                u = u,
                ts = ts,
                colors = colors,
            )
            Gauge(
                progress = ratio(snap.storageUsedGb, snap.storageTotalGb),
                big = if (snap.storageTotalGb <= 0f) "--"
                else String.format(Locale.US, "%.0fG", snap.storageUsedGb),
                small = if (snap.storageTotalGb <= 0f) ""
                else String.format(Locale.US, "of %.0fG", snap.storageTotalGb),
                label = "STORAGE",
                u = u,
                ts = ts,
                colors = colors,
            )
            Gauge(
                progress = snap.batteryPct.coerceAtLeast(0) / 100f,
                big = if (snap.batteryPct < 0) "--" else "${snap.batteryPct}%",
                small = if (snap.batteryTemp.isNaN()) ""
                else String.format(Locale.US, "%.1f°C", snap.batteryTemp),
                label = "BATTERY",
                u = u,
                ts = ts,
                colors = colors,
            )
            Gauge(
                progress = health.coerceAtLeast(0) / 100f,
                big = if (health < 0) "--" else "$health%",
                small = "capacity",
                label = "HEALTH",
                u = u,
                ts = ts,
                colors = colors,
            )
        }
    }
}

/** Collapsed header: the device line and a battery ring on the same card. */
@Composable
fun CollapsedBanner(
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = bannerColors()
    val model = remember { BannerFacts.modelName(BannerFacts.productSku()) }
    var battery by remember { mutableIntStateOf(-1) }
    DisposableEffect(active) {
        if (active) {
            battery = BannerFacts.readVolatile(context).batteryPct
        }
        onDispose { }
    }
    BoxWithConstraints(modifier.cardBackground(colors)) {
        val u = maxWidth / DESIGN_W
        val ts = { n: Float -> with(density) { (u * n).toSp() } }
        Canvas(Modifier.fillMaxSize()) {
            drawMarkOutline(
                left = size.width - (u * 102).toPx(),
                top = -(u * 16).toPx(),
                width = (u * 120).toPx(),
                color = colors.accent,
                alpha = 0.45f,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = u * 58),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u * 9),
        ) {
            OnePlusGlyph(size = u * 20, color = colors.bright)
            Text(
                text = "OnePlus $model",
                color = COL_VALUE,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = ts(17f),
                lineHeight = ts(17f),
                maxLines = 1,
                softWrap = false,
                style = Tight,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = u * 14)
                .size(u * 38),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawGauge(
                    progress = battery.coerceAtLeast(0) / 100f,
                    radius = size.width * 25f / 60f,
                    stroke = size.width * 6f / 60f,
                    color = colors.accent,
                    track = Color.White.copy(alpha = 0.12f),
                )
            }
            Text(
                text = if (battery < 0) "--" else "$battery",
                color = COL_VALUE,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = ts(11f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Gauge(
    progress: Float,
    big: String,
    small: String,
    label: String,
    u: Dp,
    ts: (Float) -> TextUnit,
    colors: BannerColors,
) {
    Column(
        modifier = Modifier.width(u * 70),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(u * 58),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawGauge(
                    progress = progress,
                    radius = size.width * 26f / 60f,
                    stroke = size.width * 4.5f / 60f,
                    color = colors.accent,
                    track = COL_TRACK,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u * 3),
            ) {
                Text(
                    text = big,
                    color = COL_VALUE,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = ts(14f),
                    lineHeight = ts(14f),
                    maxLines = 1,
                    softWrap = false,
                    style = Tight,
                )
                if (small.isNotEmpty()) {
                    Text(
                        text = small,
                        color = COL_LABEL,
                        fontSize = ts(8.5f),
                        lineHeight = ts(9f),
                        maxLines = 1,
                        softWrap = false,
                        style = Tight,
                    )
                }
            }
        }
        Spacer(Modifier.height(u * 6))
        Text(
            text = label,
            color = COL_LABEL,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = ts(9f),
            lineHeight = ts(9f),
            letterSpacing = 0.14.em,
            maxLines = 1,
            style = Tight,
        )
    }
}

@Composable
private fun OnePlusGlyph(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 86f
        withTransform({
            scale(k, k, pivot = Offset.Zero)
            translate(-14f, -9f)
        }) {
            drawPath(
                onePlus,
                color.copy(alpha = 0.3f),
                style = Stroke(width = 22f, join = StrokeJoin.Round),
            )
            drawPath(onePlus, color, style = Stroke(width = 9f, join = StrokeJoin.Round))
        }
    }
}

private fun Modifier.cardBackground(colors: BannerColors): Modifier = drawBehind {
    drawRect(
        Brush.linearGradient(
            0f to colors.deep,
            0.72f to CARD_BASE,
            start = Offset(size.width * 0.3f, 0f),
            end = Offset(size.width * 0.7f, size.height),
        ),
    )
    drawRect(
        Brush.radialGradient(
            0f to colors.accent.copy(alpha = 0.42f),
            0.42f to colors.accent.copy(alpha = 0.1f),
            0.64f to Color.Transparent,
            center = Offset(size.width, 0f),
            radius = size.width * 0.85f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            0f to colors.accent.copy(alpha = 0.14f),
            0.62f to Color.Transparent,
            center = Offset(0f, size.height),
            radius = size.width * 0.6f,
        ),
    )
}

private fun DrawScope.drawMarkOutline(
    left: Float,
    top: Float,
    width: Float,
    color: Color,
    alpha: Float,
) {
    val k = width / RR_MARK_W
    withTransform({
        translate(left, top)
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawPath(rrMark, color.copy(alpha = alpha * 0.2f), style = Stroke(width = 12f))
        drawPath(rrMark, color.copy(alpha = alpha), style = Stroke(width = 4f))
    }
}

private fun DrawScope.drawGauge(
    progress: Float,
    radius: Float,
    stroke: Float,
    color: Color,
    track: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arc = Size(radius * 2, radius * 2)
    drawCircle(track, radius, style = Stroke(stroke))
    if (progress <= 0f) return
    val sweep = 360f * progress.coerceAtMost(1f)
    drawArc(
        color.copy(alpha = 0.25f), -90f, sweep, false, topLeft, arc,
        style = Stroke(stroke * 2.6f, cap = StrokeCap.Round),
    )
    drawArc(
        color, -90f, sweep, false, topLeft, arc,
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
}

private fun ratio(used: Float, total: Float): Float =
    if (total <= 0f) 0f else (used / total).coerceIn(0f, 1f)
