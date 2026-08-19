package com.example.compasos_tv.Screan

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.compasos_tv.services.VinculacionTvRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val CCard    = Color(0xFF0F1629)
private val CTexto   = Color(0xFFE0E0E0)
private val CSecund  = Color(0xFF9E9E9E)
private val CAzul    = Color(0xFF1565C0)
private val CAzulF   = Color(0xFF1976D2)
private val CRojo    = Color(0xFFE53935)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel de [TvConfiguracionScreen]. Expone la configuración/vinculación
 * actual y la acción de desvincular la TV.
 *
 * @param app aplicación usada para construir [VinculacionTvRepository].
 */
class TvConfigViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VinculacionTvRepository(app)
    /** Configuración/vinculación actual, observada reactivamente desde Room. */
    val config       = repo.observarConfig()

    /**
     * Desvincula la TV (borra la sesión guardada y limpia el retained MQTT)
     * y, al terminar, ejecuta [onListo] para que la pantalla navegue de
     * vuelta a la vinculación.
     */
    fun desvincular(onListo: () -> Unit) {
        viewModelScope.launch {
            repo.desvincular()
            onListo()
        }
    }

    /** Factory estándar para crear [TvConfigViewModel] con `viewModel(factory = ...)`. */
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvConfigViewModel(app) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Pantalla de configuración de la TV: muestra información del dispositivo,
 * la cuenta vinculada, y permite desvincular la pantalla (con confirmación
 * previa) mediante [onDesvincular].
 *
 * @param onDesvincular callback invocado cuando el usuario confirma la
 *        desvinculación, usado por la navegación para volver a la pantalla
 *        de vinculación.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvConfiguracionScreen(onDesvincular: () -> Unit) {
    val context = LocalContext.current
    val vm: TvConfigViewModel = viewModel(
        factory = TvConfigViewModel.Factory(context.applicationContext as Application)
    )
    val config by vm.config.collectAsState(initial = null)
    val tvId   = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    // Controla si se muestra el panel de "¿Desvincular esta pantalla?" antes
    // de ejecutar la acción, para evitar desvinculaciones accidentales.
    var confirmar by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // ── Info del dispositivo + cuenta ──────────────────────────────────────
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Settings, null, tint = CAzul, modifier = Modifier.size(28.dp))
                Text("Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CTexto)
            }

            // Dispositivo
            SeccionInfo("Dispositivo TV") {
                FilaInfo(Icons.Default.Tv,           "Tipo",    "Android TV")
                FilaInfo(Icons.Default.DevicesOther, "Modelo",  Build.MODEL)
                FilaInfo(Icons.Default.DevicesOther, "ID",      "tv_$tvId")
                FilaInfo(Icons.Default.DevicesOther, "Android", "API ${Build.VERSION.SDK_INT}")
            }

            // Cuenta vinculada
            SeccionInfo("Cuenta vinculada") {
                if (config?.vinculado == true) {
                    FilaInfo(Icons.Default.AccountCircle, "Nombre",  config?.nombreUsuario ?: "-")
                    FilaInfo(Icons.Default.AccountCircle, "Correo",  config?.emailUsuario  ?: "-")
                    FilaInfo(Icons.Default.AccountCircle, "Usuario", config?.usuarioIdVinculado ?: "-")
                    config?.ultimaActualizacion?.let { ts ->
                        FilaInfo(
                            Icons.Default.Settings, "Vinculado el",
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
                        )
                    }
                } else {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(CRojo, CircleShape))
                        Text("Sin cuenta vinculada", fontSize = 14.sp, color = CSecund)
                    }
                }
            }
        }

        // ── Acciones ──────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.width(300.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            if (config?.vinculado == true) {
                if (!confirmar) {
                    BotonConfig(
                        icono   = Icons.Default.LinkOff,
                        texto   = "Desvincular pantalla",
                        colorBg = CRojo.copy(alpha = 0.15f),
                        colorTx = CRojo,
                        onClick = { confirmar = true }
                    )
                } else {
                    // Panel de confirmación: evita que un solo click accidental
                    // desvincule la TV sin querer.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CCard, RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("¿Desvincular esta pantalla?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CTexto)
                        Text("Deberás vincularla nuevamente con tu teléfono.", fontSize = 13.sp, color = CSecund)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BotonConfig(
                                icono    = Icons.Default.LinkOff,
                                texto    = "Confirmar",
                                colorBg  = CRojo.copy(alpha = 0.2f),
                                colorTx  = CRojo,
                                modifier = Modifier.weight(1f),
                                onClick  = { vm.desvincular(onDesvincular) }
                            )
                            BotonConfig(
                                icono    = Icons.Default.Settings,
                                texto    = "Cancelar",
                                colorBg  = CCard,
                                colorTx  = CTexto,
                                modifier = Modifier.weight(1f),
                                onClick  = { confirmar = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Contenedor de tarjeta con título en mayúsculas, usado para agrupar filas
 * de información relacionadas (ver [FilaInfo]).
 *
 * @param titulo    encabezado de la sección (se muestra en mayúsculas).
 * @param contenido filas de información que se dibujan dentro de la tarjeta.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeccionInfo(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CCard, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(titulo.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CAzul, letterSpacing = 1.sp)
        contenido()
    }
}

/**
 * Fila simple "icono + etiqueta + valor", usada dentro de [SeccionInfo]
 * para mostrar datos de solo lectura (dispositivo, cuenta vinculada, etc.).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilaInfo(icono: ImageVector, etiqueta: String, valor: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icono, null, tint = CSecund, modifier = Modifier.size(16.dp))
        Text(etiqueta, fontSize = 13.sp, color = CSecund, modifier = Modifier.width(100.dp))
        Text(valor, fontSize = 13.sp, color = CTexto, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Botón enfocable con soporte de control remoto (Enter/DirectionCenter),
 * usado para las acciones de esta pantalla (desvincular, confirmar, cancelar).
 *
 * @param icono    icono mostrado a la izquierda del texto.
 * @param texto    etiqueta del botón.
 * @param colorBg  color de fondo en estado normal.
 * @param colorTx  color del texto/icono.
 * @param modifier modificador adicional (p. ej. `Modifier.weight(1f)` en filas).
 * @param onClick  acción ejecutada al confirmar con el control remoto.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BotonConfig(
    icono:    ImageVector,
    texto:    String,
    colorBg:  Color,
    colorTx:  Color,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    var tieneFoco by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (tieneFoco) CAzulF.copy(alpha = 0.3f) else colorBg,
                RoundedCornerShape(10.dp)
            )
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icono, null, tint = colorTx, modifier = Modifier.size(18.dp))
        Text(texto, fontSize = 13.sp, color = colorTx, fontWeight = FontWeight.SemiBold)
    }
}
