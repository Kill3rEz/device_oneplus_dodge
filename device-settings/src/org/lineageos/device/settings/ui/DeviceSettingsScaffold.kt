/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.lineageos.device.settings.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.lineageos.device.settings.ui.banner.CollapsedBanner
import org.lineageos.device.settings.ui.banner.LiveBanner

/** Expanded and collapsed banner heights per unit of width. */
private const val BANNER_ASPECT = 662f / 1080f
private const val STRIP_ASPECT = 182f / 1080f

/** 22 units on a 358-wide banner. */
private const val CORNER = 22f / 358f

private val CARD_EDGE = Color.White.copy(alpha = 0.06f)
private val BACK_SCRIM = Color.Black.copy(alpha = 0.3f)
private val BACK_EDGE = Color.White.copy(alpha = 0.1f)

/** Original small icon button. 22.dp screen start = 16.dp art gutter + 6.dp extra. */
private val BACK_SIZE = 40.dp
private val BACK_ICON = 24.dp
private val BACK_START = 6.dp
private val BACK_TOP = 10.dp

/** Shared collapsing header: expanded LiveBanner, collapsed CollapsedBanner strip. */
@Composable
fun DeviceSettingsScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    // The default snap is a spring and it overshoots the expanded and collapsed heights.
    val snapSpec = remember { tween<Float>(durationMillis = 220) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        snapAnimationSpec = snapSpec,
    )
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val artWidth = (maxWidth - 32.dp).coerceAtLeast(1.dp)
        val artHeight = artWidth * BANNER_ASPECT
        val stripHeight = artWidth * STRIP_ASPECT
        val shape = RoundedCornerShape(artWidth * CORNER)
        val statusPx = WindowInsets.statusBars.getTop(density).toFloat()
        val artHeightPx = with(density) { artHeight.toPx() }
        val stripHeightPx = with(density) { stripHeight.toPx() }
        val expandedPx = statusPx + artHeightPx
        val collapsedPx = statusPx + stripHeightPx
        val statusDp = with(density) { statusPx.toDp() }
        val pageColor = MaterialTheme.colorScheme.surfaceContainer
        val limit = collapsedPx - expandedPx
        SideEffect {
            scrollBehavior.state.heightOffsetLimit = limit
        }
        val barHeight = with(density) {
            (expandedPx + scrollBehavior.state.heightOffset).toDp()
        }
        val expandedAlpha =
            (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)
        val collapsedAlpha =
            scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = pageColor,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clipToBounds(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(artHeight)
                            .padding(horizontal = 16.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = statusDp)
                            .clip(shape),
                    ) {
                        LiveBanner(
                            active = expandedAlpha > 0.01f,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.TopCenter)
                                .alpha(expandedAlpha)
                                .border(1.dp, CARD_EDGE, shape),
                        )
                        CollapsedBanner(
                            active = collapsedAlpha > 0.01f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(stripHeight)
                                .align(Alignment.TopCenter)
                                .alpha(collapsedAlpha)
                                .clip(shape)
                                .border(1.dp, CARD_EDGE, shape),
                        )
                        Surface(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = BACK_START, top = BACK_TOP)
                                .size(BACK_SIZE),
                            shape = CircleShape,
                            color = BACK_SCRIM,
                            contentColor = Color.White,
                            border = BorderStroke(1.dp, BACK_EDGE),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(BACK_ICON),
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(statusDp)
                            .align(Alignment.TopCenter)
                            .background(pageColor),
                    )
                }
            },
            content = content,
        )
    }
}
