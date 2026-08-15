package com.example.compasos_tv.Screan

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.compasos_tv.data.entitys.VideoTvEntity
import com.example.compasos_tv.data.entitys.VideosSeguridadRepository
import com.example.compasos_tv.services.CategoriaVideo
import com.example.compasos_tv.services.CategoriasVideoSeguridad
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val VPanel    = Color(0xFF0F1629)
private val VTexto    = Color(0xFFE0E0E0)
private val VSecund   = Color(0xFF9E9E9E)
private val VAzul     = Color(0xFF1565C0)
private val VAzulFoco = Color(0xFF1976D2)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class TvVideosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VideosSeguridadRepository(app)
    val categorias   = CategoriasVideoSeguridad.TODAS

    private val _categoriaSeleccionada = MutableStateFlow(categorias.first())
    val categoriaSeleccionada: StateFlow<CategoriaVideo> = _categoriaSeleccionada.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val videos: StateFlow<List<VideoTvEntity>> = _categoriaSeleccionada
        .flatMapLatest { repo.observarCategoria(it.clave) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seleccionarCategoria(categorias.first())
    }

    fun seleccionarCategoria(categoria: CategoriaVideo) {
        _categoriaSeleccionada.value = categoria
        viewModelScope.launch {
            _cargando.value = true
            repo.refrescarSiHaceFalta(categoria)
            _cargando.value = false
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            TvVideosViewModel(app) as T
    }
}

// ── Reproducción — abre la app de YouTube (o navegador si no está instalada) ──

private fun reproducirEnYoutube(context: Context, videoId: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("force_fullscreen", true)
    }
    try {
        context.startActivity(appIntent)
        return
    } catch (e: ActivityNotFoundException) {
        Log.w("TvVideos", "App de YouTube no encontrada, probando navegador")
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.youtube.com/watch?v=$videoId")
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(webIntent)
    } catch (e: ActivityNotFoundException) {
        Log.e("TvVideos", "No hay ninguna app capaz de reproducir el video")
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvVideosScreen() {
    val context = LocalContext.current
    val vm: TvVideosViewModel = viewModel(
        factory = TvVideosViewModel.Factory(context.applicationContext as Application)
    )
    val categoriaSel by vm.categoriaSeleccionada.collectAsState()
    val videos       by vm.videos.collectAsState()
    val cargando     by vm.cargando.collectAsState()

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.VideoLibrary, null, tint = VAzul, modifier = Modifier.size(28.dp))
            Text("Cuidado personal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VTexto)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items = vm.categorias, key = { it.clave }) { categoria ->
                ChipCategoria(
                    categoria    = categoria,
                    seleccionada = categoria.clave == categoriaSel.clave,
                    onClick      = { vm.seleccionarCategoria(categoria) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                cargando && videos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VAzul)
                    }
                }
                videos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.VideoLibrary, null, tint = VSecund, modifier = Modifier.size(56.dp))
                            Text("Sin videos disponibles", fontSize = 18.sp, color = VTexto, fontWeight = FontWeight.SemiBold)
                            Text("Revisa la conexión a internet e inténtalo de nuevo", fontSize = 13.sp, color = VSecund)
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement   = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = videos, key = { it.id }) { video ->
                            VideoCard(video = video, onClick = { reproducirEnYoutube(context, video.id) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChipCategoria(categoria: CategoriaVideo, seleccionada: Boolean, onClick: () -> Unit) {
    var tieneFoco by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    seleccionada -> VAzul
                    tieneFoco    -> VAzulFoco.copy(alpha = 0.4f)
                    else         -> VPanel
                }
            )
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            categoria.titulo,
            fontSize   = 13.sp,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
            color      = if (seleccionada || tieneFoco) Color.White else VSecund
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoCard(video: VideoTvEntity, onClick: () -> Unit) {
    var tieneFoco by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VPanel)
            .border(
                2.dp,
                if (tieneFoco) VAzulFoco else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .focusable()
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(
                model              = video.thumbnailUrl,
                contentDescription = video.titulo,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            if (tieneFoco) {
                Box(
                    modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = "Reproducir",
                        tint     = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                video.titulo,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VTexto,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                video.canal,
                fontSize = 10.sp,
                color    = VSecund,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}