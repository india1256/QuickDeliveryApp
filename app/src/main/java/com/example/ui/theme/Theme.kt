package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryTeal = Color(0xFF00A896)
val DarkTeal = Color(0xFF028090)
val BrandOrange = Color(0xFFFF6B35)
val DeepNavy = Color(0xFF0D1B2A)
val LightBackground = Color(0xFFF4F9F9)

private val QuickDeliveryLightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.White,
    tertiary = DarkTeal,
    background = LightBackground,
    surface = Color.White,
    onBackground = DeepNavy,
    onSurface = DeepNavy
)

@Composable
fun QuickDeliveryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = QuickDeliveryLightColorScheme,
        content = content
    )
}
