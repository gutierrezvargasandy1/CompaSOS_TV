package com.example.compasos_tv.Screan

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.services.EstadoTv
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val ACard      = Color(0xFF0F1629)
private val ACardNueva = Color(0xFF0F1E2E)
private val ATexto     = Color(0xFFE0E0E0)
private val ASecund    = Color(0xFF9E9E9E)
private val ARojo      = Color(0xFFE53935)
private val AAzul      = Color(0xFF1976D2)
private val AVerde     = Color(0xFF4CAF50)

/**
 * Fila unificada de la bandeja de entrada: sirve tanto para representar una
 * [com.example.compasos_tv.data.entitys.AlertaTvEntity] como una
 * [com.example.compasos_tv.data.entitys.NotificacionTvEntity] con un mismo
 * modelo de UI, para poder mostrarlas juntas y ordenadas por fecha.
 *
 * @property id        id original de la alerta o notificación.
 * @property esAlerta  true si viene de la tabla de alertas (se dibuja en rojo).
 * @property titulo    título mostrado en la tarjeta.
 * @property subtitulo texto secundario (p. ej. "De: Juan"), solo en alertas.
 * @property detalle   descripción/mensaje del ítem.
 * @property coords    coordenadas formateadas, si el ítem trae ubicación.
 * @property fecha     fecha/hora del ítem, usada para ordenar y mostrar.
 * @property leida     true si el usuario ya la marcó como vista.
 */
data class ItemBandeja(
    val id: String,
    val esAlerta: Boolean,
    val titulo: String,
    val subtitulo: String?,
    val detalle: String?,
    val coords: String?,
    val fecha: String,
    val leida: Boolean
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel de [TvAlertasScreen]. Combina alertas y notificaciones de Room
 * en una sola bandeja ordenada, y expone el estado de conexión para poder
 * distinguir por qué la bandeja está vacía.
 *
 * @param app aplicación usada para obtener la instancia de [AppDatabaseTv].
 */
class TvAlertasViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabaseTv.getInstance(app)

    /**
     * Alertas + notificaciones en una sola bandeja ordenada por fecha.
     * La pantalla no toca MQTT: observa Room, y Room la despierta sola en
     * cuanto TvMqttService escribe algo. MQTT → Room → Flow → Compose.
     */
    val items: StateFlow<List<ItemBandeja>> = combine(
        db.alertaTvDao().observarTodas(),
        db.notificacionTvDao().observarTodas()
    ) { alertas, notifs ->
        val lista = mutableListOf<ItemBandeja>()

        alertas.forEach { a ->
            lista += ItemBandeja(
                id        = a.id,
                esAlerta  = true,
                titulo    = a.tipo ?: "SOS",
                subtitulo = a.emisorNombre?.let { "De: $it" },
                detalle   = a.descripcion,
                coords    = if (a.latitud != null && a.longitud != null)
                    "📍 %.4f, %.4f".format(a.latitud, a.longitud) else null,
                fecha     = a.fecha,
                leida     = a.leida
            )
        }
        notifs.forEach { n ->
            lista += ItemBandeja(
                id        = n.id,
                esAlerta  = false,
                titulo    = n.titulo,
                subtitulo = null,
                detalle   = n.mensaje,
                coords    = null,
                fecha     = n.fecha,
                leida     = n.leida
            )
        }
        lista.sortedByDescending { it.fecha }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Par (conectadoBroker, telefonoEnLinea), usado para distinguir las 3 causas de bandeja vacía. */
    val estadoConexion: StateFlow<Pair<Boolean, Boolean>> = combine(
        EstadoTv.conectadoBroker, EstadoTv.telefonoEnLinea
    ) { broker, telefono -> broker to telefono }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false to false)

    /** Marca un [ItemBandeja] como leído en la tabla correspondiente (alerta o notificación). */
    fun marcarLeida(item: ItemBandeja) {
        viewModelScope.launch {
            if (item.esAlerta) db.alertaTvDao().marcarLeida(item.id)
            else               db.notificacionTvDao().marcarLeida(item.id)
        }
    }

    /** Factory estándar para crear [TvAlertasViewModel] con `viewModel(factory = ...)`. */
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvAlertasViewModel(app) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Bandeja unificada de alertas de emergencia y notificaciones informativas,
 * con indicadores de conexión al servidor y al teléfono.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAlertasScreen() {
    val context = LocalContext.current
    val vm: TvAlertasViewModel = viewModel(
        factory = TvAlertasViewModel.Factory(context.applicationContext as Application)
    )
    val items    by vm.items.collectAsState()
    val conexion by vm.estadoConexion.collectAsState()
    val (brokerOk, telefonoOk) = conexion
    val noLeidas = items.count { !it.leida }

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera con estado de conexión — distingue las tres causas posibles
        // de "no llega nada" en vez de dejar la pantalla muda.
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Notifications, null, tint = ARojo, modifier = Modifier.size(30.dp))
            Text("Alertas y avisos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ATexto)

            if (noLeidas > 0) {
                Box(
                    modifier = Modifier
                        .background(ARojo, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$noLeidas nuevo${if (noLeidas > 1) "s" else ""}",
                        fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Punto(brokerOk); Spacer(Modifier.width(6.dp))
            Text("Servidor", fontSize = 12.sp, color = ASecund)
            Spacer(Modifier.width(16.dp))
            Punto(telefonoOk); Spacer(Modifier.width(6.dp))
            Text("Teléfono", fontSize = 12.sp, color = ASecund)
        }

        if (items.isEmpty()) {
            // El mensaje cambia según la causa real de por qué no hay datos,
            // en vez de mostrar siempre el mismo "sin alertas" genérico.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, null, tint = ASecund, modifier = Modifier.size(64.dp))
                    Text(
                        when {
                            !brokerOk   -> "Sin conexión al servidor"
                            !telefonoOk -> "Esperando al teléfono…"
                            else        -> "Conectado. Sin alertas registradas"
                        },
                        fontSize = 20.sp, color = ATexto, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Las alertas y avisos de tus familiares aparecerán aquí",
                        fontSize = 14.sp, color = ASecund
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items = items, key = { it.id }) { item ->
                    ItemBandejaCard(item = item, onClick = { vm.marcarLeida(item) })
                }
            }
        }
    }
}

/** Indicador circular verde/gris usado para mostrar un estado booleano de conexión. */
@Composable
private fun Punto(ok: Boolean) {
    Box(
        Modifier.size(9.dp).clip(CircleShape)
            .background(if (ok) AVerde else Color(0xFF424242))
    )
}

/**
 * Tarjeta de un ítem de la bandeja (alerta o notificación). El color de
 * acento y el icono cambian según [ItemBandeja.esAlerta]; el borde resalta
 * si el ítem no está leído o si tiene el foco del control remoto.
 *
 * @param item    ítem a mostrar.
 * @param onClick acción ejecutada al confirmar con el control remoto (marca como leída).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ItemBandejaCard(item: ItemBandeja, onClick: () -> Unit) {
    var tieneFoco by remember { mutableStateOf(false) }
    val acento = if (item.esAlerta) ARojo else AAzul

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (!item.leida) ACardNueva else ACard)
            .border(
                1.5.dp,
                when {
                    tieneFoco   -> AAzul
                    !item.leida -> acento.copy(alpha = 0.5f)
                    else        -> Color.Transparent
                },
                RoundedCornerShape(14.dp)
            )
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
            .padding(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(50.dp).background(acento.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (item.esAlerta) Icons.Default.Warning else Icons.Default.Info,
                null, tint = acento, modifier = Modifier.size(26.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(item.titulo, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = acento)
                if (!item.leida) {
                    Box(
                        modifier = Modifier
                            .background(acento, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("NUEVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            item.subtitulo?.let { Text(it, fontSize = 14.sp, color = ATexto) }
            item.detalle?.takeIf { it.isNotBlank() }
                ?.let { Text(it, fontSize = 13.sp, color = ASecund) }
            item.coords?.let { Text(it, fontSize = 12.sp, color = ASecund) }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(item.fecha.take(10), fontSize = 12.sp, color = ASecund)
            Text(
                item.fecha.drop(11).take(5),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ATexto
            )
        }
    }
}
