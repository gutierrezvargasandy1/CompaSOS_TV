package com.example.compasos_tv.Screan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.compasos_tv.Navigation.TvScreen

data class TvNavItem(val pantalla: TvScreen, val icono: ImageVector)

private val itemsNav = listOf(
    TvNavItem(TvScreen.Dashboard,        Icons.Filled.Home),
    TvNavItem(TvScreen.AlertasRecibidas, Icons.Filled.Notifications),
    TvNavItem(TvScreen.FamiliaEnLinea,   Icons.Filled.Face),
    TvNavItem(TvScreen.Configuracion,    Icons.Filled.Settings)
)

private val ColorFondo        = Color(0xFF0A0E1A)
private val ColorBarra        = Color(0xFF0F1629)
private val ColorSeleccionado = Color(0xFF1565C0)
private val ColorFocused      = Color(0xFF1976D2)
private val ColorTexto        = Color(0xFFE0E0E0)
private val ColorTextoSecund  = Color(0xFF9E9E9E)
private val ColorRojo         = Color(0xFFE53935)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMainScreen(
    navController:    NavController,
    rutaSeleccionada: String,
    padContent:       Boolean = true,          // ← nuevo parámetro
    contenido:        @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
    ) {
        // ── Barra lateral ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(ColorBarra)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.Lock,
                    contentDescription = null,
                    tint               = ColorRojo,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "CompaSOS",
                    color      = ColorTexto,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            itemsNav.forEach { item ->
                TvNavItemRow(
                    item         = item,
                    seleccionado = rutaSeleccionada == item.pantalla.route,
                    onClick      = {
                        if (rutaSeleccionada != item.pantalla.route) {
                            navController.navigate(item.pantalla.route) {
                                popUpTo(TvScreen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text     = "TV v1.0",
                color    = ColorTextoSecund,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.06f))
        )

        // ── Área de contenido (con o sin padding según pantalla) ──────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (padContent) Modifier.padding(32.dp) else Modifier)
        ) {
            contenido()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvNavItemRow(
    item:         TvNavItem,
    seleccionado: Boolean,
    onClick:      () -> Unit
) {
    var tieneFoco by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val colorFondo by animateColorAsState(
        targetValue = when {
            seleccionado -> ColorSeleccionado
            tieneFoco    -> ColorFocused.copy(alpha = 0.4f)
            else         -> Color.Transparent
        },
        animationSpec = tween(150),
        label         = "nav_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorFondo)
            .focusRequester(focusRequester)
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector        = item.icono,
            contentDescription = null,
            tint               = if (seleccionado || tieneFoco) Color.White else ColorTextoSecund,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text       = item.pantalla.titulo,
            color      = if (seleccionado || tieneFoco) Color.White else ColorTextoSecund,
            fontSize   = 14.sp,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
        )
        if (seleccionado) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
        }
    }
}