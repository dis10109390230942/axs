package com.example.axs_player.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity

@Composable
fun rememberPullDownToCollapse(threshold: Dp, onCollapse: () -> Unit): NestedScrollConnection {
    val currentOnCollapse by rememberUpdatedState(onCollapse)
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    return remember(thresholdPx) {
        object : NestedScrollConnection {
            private var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y > 0f) {
                    pulled += available.y
                    if (pulled > thresholdPx) {
                        pulled = 0f
                        currentOnCollapse()
                    }
                } else if (consumed.y != 0f || available.y < 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
}

@Composable
fun rememberPullUpToExpand(threshold: Dp, onExpand: () -> Unit): NestedScrollConnection {
    val currentOnExpand by rememberUpdatedState(onExpand)
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    return remember(thresholdPx) {
        object : NestedScrollConnection {
            private var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f) {
                    pulled += -available.y
                    if (pulled > thresholdPx) {
                        pulled = 0f
                        currentOnExpand()
                    }
                } else if (consumed.y != 0f || available.y > 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
}
