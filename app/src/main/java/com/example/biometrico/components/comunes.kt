package com.example.biometrico.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrico.ui.theme.*

// ── Icono con glow pulsante ──────────────────────────────────────
@Composable
fun ImprimirIcono(
    icono: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    tint: Color = SunsetOrange,
    alpha: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * 1.6f)
                .scale(glowScale)
                .clip(CircleShape)
                .background(SunsetOrangeGlow)
        )
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = tint.copy(alpha = alpha),
            modifier = modifier.size(size)
        )
    }
}

// ── Título con gradiente ─────────────────────────────────────────
@Composable
fun AppTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = textAlign,
        color = SunsetWhite,
        modifier = modifier
    )
}

// ── Subtítulo suave ──────────────────────────────────────────────
@Composable
fun AppSubtitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = SunsetWhite70,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// ── Botón principal con gradiente Sunset ─────────────────────────
@Composable
fun ActionsButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = SunsetWhite
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(SunsetOrange, SunsetMagenta)
                )
            )
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor   = contentColor
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

// ── Botón secundario tonal ───────────────────────────────────────
@Composable
fun FilledTonalBoton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = SunsetCard,
    contentColor: Color = SunsetWhite
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ── Tarjeta con borde gradiente ──────────────────────────────────
@Composable
fun SunsetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SunsetOrange.copy(alpha = 0.3f), SunsetMagenta.copy(alpha = 0.1f))
                )
            )
            .padding(1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(19.dp))
                .background(SunsetCard)
                .padding(20.dp),
            content = content
        )
    }
}