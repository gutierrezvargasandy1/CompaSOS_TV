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
import com.example.compasos_tv.data.entitys.AlertaTvEntity
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import kotlinx.coroutines.launch

private val ACard       = Color(0xFF0F1629)
private val ACardNueva  = Color(0xFF0F1E2E)
private val ATexto      = Color(0xFFE0E0E0)
private val ASecund     = Color(0xFF9E9E9E)
private val ARojo       = Color(0xFFE53935)
private val AAzulFoco   = Color(0xFF1976D2)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class TvAlertasViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabaseTv.getInstance(app)
    val alertas    = db.alertaTvDao().observarTodas()
    val noLeidas   = db.alertaTvDao().contarNoLeidas()

    fun marcarLeida(id: String) {
        viewModelScope.launch { db.alertaTvDao().marcarLeida(id) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvAlertasViewModel(app) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAlertasScreen() {
    val context = LocalContext.current
    val vm: TvAlertasViewModel = viewModel(
        factory = TvAlertasViewModel.Factory(context.applicationContext as Application)
    )
    val alertas  by vm.alertas.collectAsState(initial = emptyList())
    val noLeidas by vm.noLeidas.collectAsState(initial = 0)

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Notifications, null, tint = ARojo, modifier = Modifier.size(30.dp))
            Text("Alertas de emergencia", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ATexto)
            if (noLeidas > 0) {
                Box(
                    modifier = Modifier
                        .background(ARojo, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$noLeidas nueva${if (noLeidas > 1) "s" else ""}",
                        fontSize   = 12.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (alertas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, null, tint = ASecund, modifier = Modifier.size(64.dp))
                    Text("Sin alertas registradas", fontSize = 20.sp, color = ATexto, fontWeight = FontWeight.SemiBold)
                    Text("Las alertas de tus familiares aparecerán aquí", fontSize = 14.sp, color = ASecund)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items = alertas, key = { it.id }) { alerta ->
                    AlertaTvCard(alerta = alerta, onClick = { vm.marcarLeida(alerta.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AlertaTvCard(alerta: AlertaTvEntity, onClick: () -> Unit) {
    var tieneFoco by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (!alerta.leida) ACardNueva else ACard)
            .border(
                1.5.dp,
                when {
                    tieneFoco     -> AAzulFoco
                    !alerta.leida -> ARojo.copy(alpha = 0.5f)
                    else          -> Color.Transparent
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
            modifier = Modifier.size(50.dp).background(ARojo.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Warning, null, tint = ARojo, modifier = Modifier.size(26.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(alerta.tipo ?: "SOS", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ARojo)
                if (!alerta.leida) {
                    Box(
                        modifier = Modifier
                            .background(ARojo, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("NUEVA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            alerta.emisorNombre?.let { Text("De: $it", fontSize = 14.sp, color = ATexto) }
            alerta.descripcion?.let  { Text(it, fontSize = 13.sp, color = ASecund) }
            if (alerta.latitud != null && alerta.longitud != null) {
                Text("📍 %.4f, %.4f".format(alerta.latitud, alerta.longitud), fontSize = 12.sp, color = ASecund)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(alerta.fecha.take(10), fontSize = 12.sp, color = ASecund)
            Text(alerta.fecha.drop(11).take(5), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ATexto)
        }
    }
}