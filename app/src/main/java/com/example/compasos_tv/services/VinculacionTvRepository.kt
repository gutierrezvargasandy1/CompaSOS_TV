package com.example.compasos_tv.services

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.compasos_tv.config.MqttConfig
import com.example.compasos_tv.config.MqttManager
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.data.entitys.ConfigTvEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VinculacionTvRepository(private val context: Context) {

    private val db    = AppDatabaseTv.getInstance(context)
    private val dao   = db.configTvDao()
    private val mqtt  = MqttManager()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observarConfig(): Flow<ConfigTvEntity?> = dao.observar()

    /**
     * El código lo genera y muestra el TELÉFONO. El usuario lo lee ahí
     * y lo escribe aquí en la TV. Con ese código, la TV publica su
     * solicitud y espera la confirmación del teléfono.
     */
    suspend fun solicitarVinculacion(
        codigo: String,
        onConfirmada: (usuarioId: String, nombre: String, email: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val tvId = TvMqttService.obtenerTvId(context)

        if (dao.obtener() == null) {
            dao.guardar(ConfigTvEntity(tvDeviceId = tvId))
        }
        dao.setCodigo(codigo)

        if (!mqtt.estaConectado) mqtt.conectar()

        // Suscribirse ANTES de publicar para no perder la respuesta
        val topicRespuesta = "${MqttConfig.TOPIC_VINCULACION}/tv/$codigo/respuesta"
        mqtt.suscribir(topicRespuesta) { _, payload ->
            scope.launch {
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
                    Log.d("VinculacionTv", "✅ Vinculación confirmada por el teléfono")
                } catch (e: Exception) {
                    Log.e("VinculacionTv", "Error procesando respuesta: ${e.message}")
                }
            }
        }

        val payload = JSONObject().apply {
            put("tvId",   tvId)
            put("codigo", codigo)
            put("modelo", Build.MODEL)
        }.toString()
        mqtt.publicar("${MqttConfig.TOPIC_VINCULACION}/tv/$codigo/solicitud", payload)
        Log.d("VinculacionTv", "Solicitud publicada con código: $codigo")
    }

    suspend fun desvincular() = withContext(Dispatchers.IO) {
        dao.desvincular()
    }
}