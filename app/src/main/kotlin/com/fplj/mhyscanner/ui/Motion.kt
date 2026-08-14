package com.fplj.mhyscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 全局动效时长,统一节奏(按压/淡入/状态指示) */
object AppMotion {
    /** 按压反馈时长 */
    const val PressMs = 120
    /** 小元素进入/淡出 */
    const val FadeMs = 180
    /** 状态指示 */
    const val StatusMs = 220
    /** 呼吸点周期 */
    const val PulseMs = 900
    /** 页面切换 */
    const val PageInMs = 260
    const val PageOutMs = 180

    /** 进入用:快速起步缓出 */
    val Enter = FastOutSlowInEasing
    /** 退出用:快速结束缓入 */
    val Exit = FastOutLinearInEasing
    /** 呼吸点:线性往返 */
    val Pulse = LinearEasing
}

/**
 * 按压反馈:按住轻微缩小(0.97),松开弹簧回弹。
 * 临界阻尼无过冲 + 适中刚度:按压即响应,松开快速归位,
 * 不引入装饰性弹跳(弹跳只留给带惯性的手势)。
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    scaleTo: Float = 0.97f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this.then(Modifier.scale(scale))
}

/** 扫描/运行中呼吸圆点:仅透明度往返,线性匀速,符合 reduced-motion 温和降级 */
@Composable
fun BreathingDot(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (!active) {
        Box(modifier.size(8.dp).background(color, CircleShape))
        return
    }
    val transition = rememberInfiniteTransition(label = "breath")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppMotion.PulseMs, easing = AppMotion.Pulse),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )
    Box(modifier.size(8.dp).alpha(alpha).background(color, CircleShape))
}

/**
 * 状态指示条:扫描中呼吸点 + primaryContainer 底,空闲时弱化。
 * 状态变化属偶发反馈,轻量淡入淡出,不外跳。
 */
@Composable
fun StatusBanner(text: String, active: Boolean = false, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    val container = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val dot = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(AppMotion.StatusMs, easing = AppMotion.Enter)) +
            slideInVertically(tween(AppMotion.StatusMs, easing = AppMotion.Enter)) { it / 5 },
        exit = fadeOut(tween(AppMotion.PressMs, easing = AppMotion.Exit)) +
            slideOutVertically(tween(AppMotion.PressMs, easing = AppMotion.Exit)) { -it / 5 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = container,
            contentColor = content
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BreathingDot(active, dot)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}