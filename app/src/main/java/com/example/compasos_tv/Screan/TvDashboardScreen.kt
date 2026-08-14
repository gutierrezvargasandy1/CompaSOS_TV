package com.example.compasos_tv.Screan

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.data.entitys.FamiliarTvEntity
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
// ✅ ELIMINADO: import com.mapbox.maps.MapboxMap       → conflicto con el compose
// ✅ ELIMINADO: import com.mapbox.maps.plugin.annotation.AnnotationType → no usado
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap       // única versión necesaria
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val DPanel    = Color(0xFF0F1629)
private val DTexto    = Color(0xFFE0E0E0)
private val DSecund   = Color(0xFF9E9E9E)
private val DAzul     = Color(0xFF1565C0)
private val DAzulFoco = Color(0xFF1976D2)
private val DVerde    = Color(0xFF4CAF50)
private val DGris     = Color(0xFF424242)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class TvDashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabaseTv.getInstance(app)
    val familiares = db.familiarTvDao().observarTodos()

    private val _seleccionado = MutableStateFlow<FamiliarTvEntity?>(null)
    val seleccionado: StateFlow<FamiliarTvEntity?> = _seleccionado.asStateFlow()

    fun seleccionar(familiar: FamiliarTvEntity) {
        _seleccionado.value =
            if (_seleccionado.value?.usuarioId == familiar.usuarioId) null else familiar
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvDashboardViewModel(app) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalTvMaterial3Api::class, MapboxExperimental::class)
@Composable
fun TvDashboardScreen() {
    val context = LocalContext.current
    val vm: TvDashboardViewModel = viewModel(
        factory = TvDashboardViewModel.Factory(context.applicationContext as Application)
    )
    val familiares   by vm.familiares.collectAsState(initial = emptyList())
    val seleccionado by vm.seleccionado.collectAsState()

    remember {
        MapboxOptions.accessToken =
            "pk.eyJ1IjoiYW5keXNzMTIiLCJhIjoiY21zbHVtYmJkMTh6YTJ4b29zb25pcjMzOSJ9.388hSDi65h3MrNr5v-rSgQ"
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-100.0, 22.0))
                .zoom(4.5)
                .build()
        )
    }

    LaunchedEffect(seleccionado) {
        val lat = seleccionado?.latitud  ?: return@LaunchedEffect
        val lng = seleccionado?.longitud ?: return@LaunchedEffect
        mapViewportState.setCameraOptions(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(14.0)
                .build()
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {

        // ── Lista de familiares ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(DPanel)
                .padding(vertical = 12.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.People, null, tint = DAzul, modifier = Modifier.size(20.dp))
                Text("Familiares", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DTexto)
                Spacer(Modifier.weight(1f))
                Text("${familiares.size}", fontSize = 13.sp, color = DAzul, fontWeight = FontWeight.Bold)
            }

            if (familiares.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin familiares registrados",
                        fontSize  = 13.sp,
                        color     = DSecund,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items = familiares, key = { it.usuarioId }) { familiar ->
                        FamiliarMapCard(
                            familiar     = familiar,
                            seleccionado = familiar.usuarioId == seleccionado?.usuarioId,
                            onClick      = { vm.seleccionar(familiar) }
                        )
                    }
                }
            }
        }

        // ── Separador ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.06f))
        )

        // ── Mapa ──────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            MapboxMap(
                modifier         = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                scaleBar         = {},
                style            = { MapStyle(style = Style.DARK) }
            ) {
                // ✅ Corregido: if en lugar de return@forEach (composables no pueden
                //    tener retornos anticipados dentro de lambdas no-composables)
                familiares.forEach { familiar ->
                    if (familiar.latitud != null && familiar.longitud != null) {
                        PointAnnotation(
                            point = Point.fromLngLat(familiar.longitud, familiar.latitud)
                        )
                    }
                }
            }

            // Overlay: info del familiar seleccionado
            seleccionado?.let { f ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .background(DPanel.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                        .border(1.dp, DAzul.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "${f.nombre} ${f.apellido ?: ""}".trim(),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = DTexto
                        )
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (f.enLinea) DVerde else DGris, CircleShape
                                    )
                            )
                            Text(
                                if (f.enLinea) "En línea" else "Sin conexión",
                                fontSize = 12.sp,
                                color    = if (f.enLinea) DVerde else DGris
                            )
                        }
                        f.latitud?.let { lat ->
                            f.longitud?.let { lng ->
                                Text(
                                    "%.5f, %.5f".format(lat, lng),
                                    fontSize = 11.sp,
                                    color    = DSecund
                                )
                            }
                        }
                        f.ultimaUbicacionFecha?.let {
                            Text(
                                "Últ. act: ${it.take(16)}",
                                fontSize = 11.sp,
                                color    = DSecund.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Hint cuando no hay selección pero sí hay ubicaciones
            if (seleccionado == null && familiares.any { it.latitud != null }) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .background(DPanel.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Selecciona un familiar para centrar el mapa",
                        fontSize = 13.sp,
                        color    = DSecund
                    )
                }
            }
        }
    }
}

// ── Tarjeta de familiar ───────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FamiliarMapCard(
    familiar:     FamiliarTvEntity,
    seleccionado: Boolean,
    onClick:      () -> Unit
) {
    var tieneFoco by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    seleccionado -> DAzul
                    tieneFoco    -> DAzulFoco.copy(alpha = 0.35f)
                    else         -> Color.Transparent
                }
            )
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (seleccionado) Color.White.copy(alpha = 0.25f)
                    else DAzul.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                familiar.nombre.firstOrNull()?.uppercase() ?: "?",
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${familiar.nombre} ${familiar.apellido ?: ""}".trim(),
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (seleccionado) Color.White else DTexto,
                maxLines   = 1
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            if (familiar.enLinea) DVerde else DGris, CircleShape
                        )
                )
                Text(
                    if (familiar.enLinea) "En línea" else "Sin conexión",
                    fontSize = 10.sp,
                    color    = if (seleccionado) Color.White.copy(alpha = 0.8f) else DSecund
                )
            }
        }

        Icon(
            imageVector        = if (familiar.latitud != null)
                Icons.Default.LocationOn else Icons.Default.LocationOff,
            contentDescription = null,
            tint               = if (familiar.latitud != null) DVerde else DGris,
            modifier           = Modifier.size(15.dp)
        )
    }
}