package com.yosyoo.shopperassistant.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF166C51),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2D3),
    onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF56635B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7DC),
    onSecondaryContainer = Color(0xFF131F18),
    tertiary = Color(0xFF7A5D00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF8FBF7),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFF8FBF7),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDCE5DE),
    onSurfaceVariant = Color(0xFF404943),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD5B8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFFA8F2D3),
    secondary = Color(0xFFBDCBBF),
    onSecondary = Color(0xFF28332D),
    secondaryContainer = Color(0xFF3E4A42),
    onSecondaryContainer = Color(0xFFD9E7DC),
    tertiary = Color(0xFFEAC34E),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4400),
    onTertiaryContainer = Color(0xFFFFE08A),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF111411),
    onBackground = Color(0xFFE0E3DE),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE0E3DE),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C2),
)

@Composable
fun ShopperAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
