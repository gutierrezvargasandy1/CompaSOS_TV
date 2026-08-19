package com.example.compasos_tv.Screan

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

private val FCard    = Color(0xFF0F1629)
private val FTexto   = Color(0xFFE0E0E0)
private val FSecund  = Color(0xFF9E9E9E)
private val FAzul    = Color(0xFF1565C0)
private val FVerde   = Color(0xFF4CAF50)
private val FGris    = Color(0xFF424242)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel de [TvFamiliaScreen]. Expone la lista de familiares y la
 * configuración actual directamente como Flows de Room, sin lógica
 * adicional: esta pantalla es de solo lectura.
 *
 * @param app aplicación usada para obtener la instancia de [AppDatabaseTv].
 */
class TvFamiliaViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabaseTv.getInstance(app)
    /** Lista reactiva de familiares vinculados con su última ubicación. */
    val familiares = db.familiarTvDao().observarTodos()
    /** Configuración/vinculación actual (usada para mostrar el nombre de cuenta). */
    val config     = db.configTvDao().observar()

    /** Factory estándar para crear [TvFamiliaViewModel] con `viewModel(factory = ...)`. */
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvFamiliaViewModel(app) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Pantalla que muestra la cuadrícula de integrantes de la familia vinculada,
 * con su estado de conexión y última ubicación conocida.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFamiliaScreen() {
    val context = LocalContext.current
    val vm: TvFamiliaViewModel = viewModel(
        factory = TvFamiliaViewModel.Factory(context.applicationContext as Application)
    )
    val familiares by vm.familiares.collectAsState(initial = emptyList())
    val config     by vm.config.collectAsState(initial = null)

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabecera
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.FamilyRestroom, null, tint = FAzul, modifier = Modifier.size(30.dp))
            Column {
                Text("Mi Familia", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FTexto)
                config?.nombreUsuario?.let {
                    Text("Cuenta de: $it", fontSize = 13.sp, color = FSecund)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(FAzul.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    "${familiares.size} integrante${if (familiares.size != 1) "s" else ""}",
                    fontSize   = 13.sp,
                    color      = FAzul,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (familiares.isEmpty()) {
            // Estado vacío: aún no llegó ningún snapshot de familiares por MQTT.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Person, null, tint = FSecund, modifier = Modifier.size(64.dp))
                    Text("Sin integrantes registrados", fontSize = 20.sp, color = FTexto, fontWeight = FontWeight.SemiBold)
                    Text("Los integrantes aparecerán aquí cuando el teléfono envíe datos", fontSize = 14.sp, color = FSecund)
                }
            }
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                items(items = familiares, key = { it.usuarioId }) { familiar ->
                    FamiliarCard(familiar)
                }
            }
        }
    }
}

/**
 * Tarjeta individual de un familiar: avatar con inicial, nombre, estado de
 * conexión y última ubicación conocida.
 *
 * @param familiar datos del familiar a mostrar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FamiliarCard(familiar: FamiliarTvEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FCard)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(70.dp).background(FAzul.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                familiar.nombre.firstOrNull()?.uppercase() ?: "?",
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
        }

        Text(
            "${familiar.nombre} ${familiar.apellido ?: ""}".trim(),
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = FTexto,
            maxLines   = 1
        )

        // Estado conexión
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(
                if (familiar.enLinea) FVerde else FGris, CircleShape
            ))
            Text(
                if (familiar.enLinea) "En línea" else "Sin conexión",
                fontSize = 12.sp,
                color    = if (familiar.enLinea) FVerde else FGris
            )
        }

        // Ubicación
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (familiar.latitud != null) Icons.Default.LocationOn else Icons.Default.LocationOff,
                contentDescription = null,
                tint     = if (familiar.latitud != null) FVerde else FGris,
                modifier = Modifier.size(15.dp)
            )
            Text(
                if (familiar.latitud != null)
                    "%.4f, %.4f".format(familiar.latitud, familiar.longitud)
                else
                    "Sin ubicación",
                fontSize = 11.sp,
                color    = FSecund
            )
        }

        familiar.ultimaUbicacionFecha?.let {
            Text(
                "Act: ${it.take(16)}",
                fontSize = 10.sp,
                color    = FSecund.copy(alpha = 0.6f)
            )
        }
    }
}
