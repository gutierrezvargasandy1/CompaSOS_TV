package com.example.compasos_tv.data.entitys

import android.content.Context
import android.util.Log
import com.example.compasos_tv.R
import com.example.compasos_tv.services.CategoriaVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Repositorio que combina caché local (Room) con la YouTube Data API v3
 * para mostrar videos de prevención/seguridad en `TvVideosScreen`.
 *
 * Patrón usado: "cache-then-network". La UI siempre observa Room a través de
 * [observarCategoria] (funciona incluso sin internet); por separado,
 * [refrescarSiHaceFalta] dispara la llamada de red solo cuando el caché
 * expiró, y si tiene éxito reemplaza los datos en Room, lo que hace que el
 * Flow emita automáticamente los nuevos resultados.
 *
 * @param context contexto de la app, usado para leer la API key y acceder a Room.
 */
class VideosSeguridadRepository(private val context: Context) {

    private val dao = AppDatabaseTv.getInstance(context).videoTvDao()

    // 30 min: evita repetir búsquedas y gastar cuota de la API en cada visita a la pantalla
    private val vigenciaCacheMs = 60 * 1000L // 1 min mientras desarrollas — sube a 30 min antes de entregar

    /**
     * Observa (reactivamente) los videos guardados en Room para [categoria].
     * No dispara ninguna llamada de red; para refrescar el contenido usar
     * [refrescarSiHaceFalta].
     */
    fun observarCategoria(categoria: String): Flow<List<VideoTvEntity>> =
        dao.observarPorCategoria(categoria)

    /**
     * Refresca la categoría desde YouTube Data API v3 si el caché expiró.
     * Si falla la red, no propaga la excepción: la UI se queda con lo
     * que ya haya en Room (observarCategoria sigue funcionando offline).
     *
     * @param categoria categoría a refrescar (clave + consulta de búsqueda curada).
     */
    suspend fun refrescarSiHaceFalta(categoria: CategoriaVideo) = withContext(Dispatchers.IO) {
        try {
            val ultima = dao.ultimaActualizacion(categoria.clave) ?: 0L
            if (System.currentTimeMillis() - ultima < vigenciaCacheMs) return@withContext

            val apiKey = context.getString(R.string.youtube_api_key)
            if (apiKey.isBlank() || apiKey == "TU_YOUTUBE_API_KEY_AQUI") {
                Log.w("VideosSeguridad", "Configura youtube_api_key en developer-config.xml")
                return@withContext
            }

            val query = URLEncoder.encode(categoria.consultaBusqueda, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet&type=video&maxResults=10" +
                    "&relevanceLanguage=es&safeSearch=strict&order=relevance" +
                    "&videoEmbeddable=true&videoSyndicated=true" +
                    "&q=$query&key=$apiKey"

            Log.d("VideosSeguridad", "URL: ${url.replace(apiKey, "***")}")
            val respuesta = obtenerJson(url)
            val items = respuesta.optJSONArray("items") ?: return@withContext

            val videos = mutableListOf<VideoTvEntity>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val videoId = item.optJSONObject("id")?.optString("videoId") ?: continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val thumbs  = snippet.optJSONObject("thumbnails")
                val thumb   = thumbs?.optJSONObject("high")?.optString("url")
                    ?: thumbs?.optJSONObject("default")?.optString("url")
                    ?: ""

                videos += VideoTvEntity(
                    id = videoId,
                    categoria = categoria.clave,
                    titulo = snippet.optString("title"),
                    canal = snippet.optString("channelTitle"),
                    thumbnailUrl = thumb,
                    fechaPublicacion = snippet.optString("publishedAt")
                )
            }

            if (videos.isNotEmpty()) {
                // Se borra la categoría completa y se re-inserta: evita dejar
                // videos "huérfanos" que ya no aparecen en los resultados nuevos.
                dao.borrarCategoria(categoria.clave)
                dao.insertarTodos(videos)
                Log.d("VideosSeguridad", "${videos.size} video(s) cargados para '${categoria.clave}'")
            }
        } catch (e: Exception) {
            Log.e("VideosSeguridad", "Error consultando YouTube: ${e.message}")
        }
    }

    /**
     * Ejecuta un GET a [urlStr] y devuelve el cuerpo parseado como JSON.
     * Función interna de bajo nivel usada únicamente por [refrescarSiHaceFalta].
     *
     * @throws Exception si la conexión falla o el cuerpo no es JSON válido.
     */
    private fun obtenerJson(urlStr: String): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.requestMethod = "GET"
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val texto = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) Log.e("VideosSeguridad", "YouTube API respondió $code: $texto")
            JSONObject(texto)
        } finally {
            conn.disconnect()
        }
    }
}
