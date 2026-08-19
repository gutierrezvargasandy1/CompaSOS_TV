package com.example.compasos_tv.services

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.compasos_tv.config.MqttConfig
import com.example.compasos_tv.config.MqttManager
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.data.entitys.ConfigTvEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/**
 * Repositorio encargado del flujo de vinculación TV ↔ teléfono a través de
 * MQTT: publica la solicitud con el código ingresado en la TV y espera la
 * confirmación del teléfono.
 *
 * Cambios respecto a la versión anterior:
 *
 * 1. Usa MqttManager.instancia — la MISMA conexión que TvMqttService. Antes
 *    creaba su propio manager: dos conexiones, y la confirmación llegaba a una
 *    mientras el servicio escuchaba en la otra.
 *
 * 2. REINTENTA la solicitud cada 3 s hasta 60 s. Antes se publicaba una sola
 *    vez: si el teléfono todavía no estaba suscrito en ese milisegundo, el
 *    mensaje se perdía y la TV se quedaba esperando para siempre.
 *
 * 3. Devuelve Boolean para que la pantalla sepa si hubo timeout.
 *
 * @param context contexto de la app, usado para Room y para obtener el id de la TV.
 */
class VinculacionTvRepository(private val context: Context) {

    private val db   = AppDatabaseTv.getInstance(context)
    private val dao  = db.configTvDao()
    private val mqtt = MqttManager.instancia   // ← compartida

    companion object {
        private const val TAG = "VinculacionTv"
        private const val REINTENTO_MS = 3_000L
        private const val TIMEOUT_MS   = 60_000L
    }

    /** Observa reactivamente la configuración/estado de vinculación actual. */
    fun observarConfig(): Flow<ConfigTvEntity?> = dao.observar()

    /**
     * El código lo genera y muestra el TELÉFONO. El usuario lo lee ahí y lo
     * escribe aquí en la TV.
     *
     * Publica repetidamente (cada [REINTENTO_MS]) la solicitud de
     * vinculación con [codigo] hasta que el teléfono responda o se cumpla
     * [TIMEOUT_MS], lo que ocurra primero.
     *
     * @param codigo      código ingresado por el usuario en la TV.
     * @param onConfirmada callback invocado con los datos del usuario en
     *        cuanto el teléfono confirma la vinculación.
     * @return true si el teléfono confirmó dentro del timeout, false si hubo
     *         timeout o no fue posible conectar al broker.
     */
    suspend fun solicitarVinculacion(
        codigo: String,
        onConfirmada: (usuarioId: String, nombre: String, email: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {

        val tvId = TvMqttService.obtenerTvId(context)

        if (dao.obtener() == null) {
            dao.guardar(ConfigTvEntity(tvDeviceId = tvId))
        }
        dao.setCodigo(codigo)

        if (!mqtt.estaConectado) {
            try {
                mqtt.conectar(clientId = "compasos_${tvId.take(24)}")
            } catch (e: Exception) {
                Log.e(TAG, "No hay broker: ${e.message}")
                return@withContext false
            }
        }

        val topicRespuesta = MqttConfig.topicRespuesta(codigo)
        val confirmada = CompletableDeferred<Boolean>()

        // Suscribirse ANTES de publicar para no perder la respuesta
        mqtt.suscribir(topicRespuesta) { _, payload ->
            // El callback corre en el hilo de Paho: no bloquear aquí.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json      = JSONObject(payload)
                    val usuarioId = json.optString("usuarioId")
                    val nombre    = json.optString("nombre")
                    val email     = json.optString("email")

                    dao.confirmarVinculacion(
                        usuarioId = usuarioId,
                        nombre    = nombre,
                        email     = email,
                        ts        = System.currentTimeMillis()
                    )
                    onConfirmada(usuarioId, nombre, email)
                    Log.d(TAG, "✅ Vinculación confirmada por el teléfono")
                    confirmada.complete(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando respuesta: ${e.message}")
                    confirmada.complete(false)
                }
            }
        }

        val payload = JSONObject().apply {
            put("tvId",   tvId)
            put("codigo", codigo)
            put("modelo", Build.MODEL)
        }.toString()

        // Corrutina hija que re-publica la solicitud cada REINTENTO_MS mientras
        // no llegue confirmación, para cubrir el caso en que el teléfono aún
        // no estaba suscrito al topic cuando se publicó el primer intento.
        val reintentos = launch {
            while (isActive) {
                mqtt.publicarSeguro(MqttConfig.topicSolicitud(codigo), payload)
                Log.d(TAG, "Solicitud publicada con código: $codigo")
                delay(REINTENTO_MS)
            }
        }

        val ok = withTimeoutOrNull(TIMEOUT_MS) { confirmada.await() } ?: false

        reintentos.cancel()
        if (!ok) {
            Log.w(TAG, "⏱ El teléfono no confirmó en ${TIMEOUT_MS / 1000}s")
            mqtt.desuscribir(topicRespuesta)
        }
        // Si sí confirmó, dejamos la suscripción viva: TvMqttService la reusa
        // para re-confirmaciones tras un reinicio.
        ok
    }

    /**
     * Desvincula esta TV: cancela la suscripción de respuesta, limpia el
     * mensaje retained del broker (para que la TV no reviva datos viejos al
     * reiniciar) y borra la configuración guardada en Room.
     */
    suspend fun desvincular() = withContext(Dispatchers.IO) {
        val tvId = TvMqttService.obtenerTvId(context)
        dao.obtener()?.codigoVinculacion?.takeIf { it.isNotBlank() }?.let {
            mqtt.desuscribir(MqttConfig.topicRespuesta(it))
        }
        // Limpia el retained del broker para que la TV no reviva datos viejos
        mqtt.publicarSeguro(MqttConfig.topicEstadoTv(tvId), "", retained = true)
        dao.desvincular()
    }
}
