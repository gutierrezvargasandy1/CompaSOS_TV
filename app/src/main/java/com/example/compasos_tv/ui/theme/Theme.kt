package com.example.compasos_tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * Tema Material3 para TV de la app. Actualmente no se usa activamente en las
 * pantallas principales (que definen sus propios colores oscuros), pero
 * queda disponible como envoltorio base si se necesita en el futuro.
 *
 * @param isInDarkTheme si true usa la paleta oscura; por defecto sigue la
 *        configuración del sistema.
 * @param content       contenido Composable que se renderiza dentro del tema.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CompaSOS_TVTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isInDarkTheme) {
        darkColorScheme(
            primary = Purple80,
            secondary = PurpleGrey80,
            tertiary = Pink80
        )
    } else {
        lightColorScheme(
            primary = Purple40,
            secondary = PurpleGrey40,
            tertiary = Pink40
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
