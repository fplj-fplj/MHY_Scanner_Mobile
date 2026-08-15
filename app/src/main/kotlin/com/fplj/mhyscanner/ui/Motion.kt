package com.fplj.mhyscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 全局动效节奏。
 *
 * 弹簧动画是可中断的:数值被新目标取代时,动画从"当前值"出发重新计算,
 * 天然支持快速反向与打断,符合"动效始终可中断"的交互原则。
 */
object AppSpring {
    /** 常规 UI 状态变化:临界阻尼(无过冲)+ 适中刚度,response≈0.34s。 */
    val Default: FiniteAnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 820f)

    /** 小元素快速反馈(按压、勾选):临界阻尼,response≈0.25s。 */
    val Quick: FiniteAnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /** 位移过渡(slide 系):与大元素同节奏,可中断。 */
    val Slide: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /**
     * 动量手势专用:0.8 阻尼允许轻微过冲。
     * 仅用于带动量的手势(拖动松手),勿用于普通 UI 变化。
     */
    val Momentum: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 1000f)
}

/** 呼吸圆点周期,仅供 BreathingDot 使用 */
object AppMotion {
    const val PulseMs = 900
    val Pulse = LinearEasing
}

/** 系统是否开启"减弱动态效果":开启时动效应退化为轻量淡入淡出。 */
@Composable
fun isReduceMotionEnabled(): Boolean =
    LocalAccessibilityManager.current?.reduceMotion ?: false

/**
 * 按压反馈:按住轻微缩小,松开弹簧回弹。
 * 临界阻尼无过冲 + 适中刚度:按压即响应、可中断、快速归位,
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
        animationSpec = AppSpring.Quick,
        label = "pressScale"
    )
    return this.then(Modifier.scale(scale))
}

/** 扫描/运行中呼吸圆点:仅透明度往返,线性匀速;系统减弱动效时退化为静止圆点。 */
@Composable
fun BreathingDot(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (!active) {
        Box(modifier.size(8.dp).background(color, CircleShape))
        return
    }
    if (isReduceMotionEnabled()) {
        Box(modifier.size(8.dp).alpha(0.6f).background(color, CircleShape))
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
 * 状态变化属偶发反馈,以弹簧轻量进出,进出同路径、可中断。
 */
@Composable
fun StatusBanner(text: String, active: Boolean = false, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    val container = if (active) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
    val content = if (active) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer
    val dot = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(AppSpring.Default) + slideInVertically(AppSpring.Slide) { it / 5 },
        exit = fadeOut(AppSpring.Default) + slideOutVertically(AppSpring.Slide) { -it / 5 },
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
