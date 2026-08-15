package com.example.compasos_tv.Screan

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.services.VinculacionTvRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val vFondo  = Color(0xFF0A0E1A)
private val vPanel  = Color(0xFF0F1629)
private val vTexto  = Color(0xFFE0E0E0)
private val vSecund = Color(0xFF9E9E9E)
private val vAzul   = Color(0xFF1565C0)
private val vRojo   = Color(0xFFE53935)

sealed class EstadoVinculacion {
    object Ingresando                   : EstadoVinculacion()
    object Esperando                    : EstadoVinculacion()
    data class Error(val mensaje: String) : EstadoVinculacion()
}

class VinculacionTvViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VinculacionTvRepository(app)
    val config       = repo.observarConfig()

    private val _estado = MutableStateFlow<EstadoVinculacion>(EstadoVinculacion.Ingresando)
    val estado: StateFlow<EstadoVinculacion> = _estado.asStateFlow()

    fun confirmarCodigo(codigo: String) {
        if (codigo.isBlank()) return
        viewModelScope.launch {
            _estado.value = EstadoVinculacion.Esperando
            try {
                repo.solicitarVinculacion(codigo) { _, _, _ -> }
            } catch (e: Exception) {
                _estado.value = EstadoVinculacion.Error(e.message ?: "Sin conexión al broker MQTT")
            }
        }
    }

    fun reintentar() {
        _estado.value = EstadoVinculacion.Ingresando
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            VinculacionTvViewModel(app) as T
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VinculacionScreen(onVinculado: () -> Unit) {
    val context = LocalContext.current
    val vm: VinculacionTvViewModel = viewModel(
        factory = VinculacionTvViewModel.Factory(context.applicationContext as Application)
    )
    val config by vm.config.collectAsState(initial = null)
    val estado by vm.estado.collectAsState()
    val tvId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    var codigoIngresado by remember { mutableStateOf("") }
    var mostrarDialogoCodigo by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        val cfg = AppDatabaseTv.getInstance(context).configTvDao().obtener()
        if (cfg?.vinculado == true) onVinculado()
    }

    LaunchedEffect(config) {
        if (config?.vinculado == true) onVinculado()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vFondo),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(60.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.Tv, null, tint = vRojo, modifier = Modifier.size(42.dp))
                    Text("compasos tv", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = vTexto)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "para vincular esta pantalla:",
                    fontSize = 18.sp,
                    color = vTexto,
                    fontWeight = FontWeight.SemiBold
                )

                PasoVinculacion("1", "abre la app compasos en tu teléfono")
                PasoVinculacion("2", "ve a dispositivos → vincular pantalla tv")
                PasoVinculacion("3", "selecciona el recuadro azul para ingresar el código")

                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Wifi, null, tint = vSecund, modifier = Modifier.size(16.dp))
                    Text(
                        "modelo: ${Build.MODEL}  ·  id: tv_$tvId",
                        fontSize = 12.sp,
                        color = vSecund
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(420.dp)
                    .background(vPanel, RoundedCornerShape(24.dp))
                    .border(2.dp, vAzul.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 44.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (val e = estado) {
                        is EstadoVinculacion.Ingresando -> {
                            Text("código del teléfono", fontSize = 15.sp, color = vSecund)

                            Surface(
                                onClick = { mostrarDialogoCodigo = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shape = ClickableSurfaceDefaults.shape(
                                    shape = RoundedCornerShape(12.dp)
                                ),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = vFondo,
                                    focusedContainerColor = vAzul.copy(alpha = 0.3f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.dp, vAzul.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (codigoIngresado.isEmpty()) {
                                        Text(
                                            "presiona enter para ingresar",
                                            fontSize = 18.sp,
                                            color = vSecund,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            codigoIngresado.chunked(3).joinToString("  "),
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White,
                                            letterSpacing = 4.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        if (codigoIngresado.length >= 4) {
                                            vm.confirmarCodigo(codigoIngresado)
                                        }
                                    },
                                    enabled = codigoIngresado.length >= 4,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("vincular", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { codigoIngresado = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("limpiar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is EstadoVinculacion.Esperando -> {
                            CircularProgressIndicator(color = vAzul, modifier = Modifier.size(52.dp))

                            Text(
                                codigoIngresado.chunked(3).joinToString("  "),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                letterSpacing = 3.sp
                            )

                            PuntosAnimados()

                            Text(
                                "esperando confirmación del teléfono...",
                                fontSize = 13.sp,
                                color = vSecund,
                                textAlign = TextAlign.Center
                            )

                            Button(onClick = { vm.reintentar() }) {
                                Text("cambiar código", fontSize = 14.sp)
                            }
                        }

                        is EstadoVinculacion.Error -> {
                            Text("sin conexión", fontSize = 16.sp, color = vRojo)
                            Text(e.mensaje, fontSize = 12.sp, color = vSecund, textAlign = TextAlign.Center)
                            Button(onClick = { vm.reintentar() }) {
                                Text("reintentar", fontSize = 16.sp)
                            }
                        }
                    }

                    Text(
                        "el código lo genera la app en tu teléfono",
                        fontSize = 11.sp,
                        color = vSecund.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (mostrarDialogoCodigo) {
        DialogCodigo(
            codigo = codigoIngresado,
            onCodigoChange = { nuevo ->
                if (nuevo.length <= 6) {
                    codigoIngresado = nuevo.uppercase()
                }
            },
            onConfirmar = {
                mostrarDialogoCodigo = false
                focusManager.clearFocus()
                if (codigoIngresado.length >= 4) {
                    vm.confirmarCodigo(codigoIngresado)
                }
            },
            onCancelar = {
                mostrarDialogoCodigo = false
                focusManager.clearFocus()
            },
            onLimpiar = {
                codigoIngresado = ""
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun DialogCodigo(
    codigo: String,
    onCodigoChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    onLimpiar: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(500.dp)
                    .background(vPanel, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "ingresa el código",
                    fontSize = 20.sp,
                    color = vTexto,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = codigo,
                    onValueChange = onCodigoChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (codigo.length >= 4) {
                                onConfirmar()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = vFondo,
                        unfocusedContainerColor = vFondo,
                        focusedBorderColor = vAzul,
                        unfocusedBorderColor = vAzul.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = vAzul
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancelar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("cancelar", fontSize = 16.sp)
                    }

                    Button(
                        onClick = onLimpiar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("limpiar", fontSize = 16.sp)
                    }

                    Button(
                        onClick = onConfirmar,
                        enabled = codigo.length >= 4,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("aceptar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PasoVinculacion(numero: String, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(vAzul, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(numero, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(texto, fontSize = 15.sp, color = Color(0xFF9E9E9E))
    }
}

@Composable
private fun PuntosAnimados() {
    var puntos by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            puntos = if (puntos >= 3) 1 else puntos + 1
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (i < puntos) vAzul else vAzul.copy(alpha = 0.25f))
            )
        }
    }
}