package com.example.compasos_tv.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.compasos_tv.config.MqttConfig
import com.example.compasos_tv.config.MqttManager
import com.example.compasos_tv.data.entitys.AlertaTvEntity
import com.example.compasos_tv.data.entitys.AppDatabaseTv
import com.example.compasos_tv.data.entitys.FamiliarTvEntity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TvMqttService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mqtt  = MqttManager()
    private lateinit var db: AppDatabaseTv
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val CANAL    = "compasos_tv_svc"
        private const val NOTIF_ID = 8001

        fun iniciar(context: Context) {
            val intent = Intent(context, TvMqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        /** ID único de esta TV basado en Android ID */
        fun obtenerTvId(context: Context): String {
            val androidId = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            )
            return "tv_$androidId"
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabaseTv.getInstance(applicationContext)
        crearCanal()
        startForeground(NOTIF_ID, notif())
        conectarYSuscribir()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        mqtt.desconectar()
        super.onDestroy()
    }

    // ── MQTT ──────────────────────────────────────────────────────────────────

    private fun conectarYSuscribir() {
        scope.launch {
            try {
                mqtt.conectar()
                val tvId = obtenerTvId(applicationContext)

                // Sesión + perfil del usuario vinculado
                mqtt.suscribir("${MqttConfig.TOPIC_TV}/$tvId/sesion") { _, payload ->
                    scope.launch { procesarSesion(payload) }
                }

                // Alerta recibida desde el teléfono
                mqtt.suscribir("${MqttConfig.TOPIC_TV}/$tvId/alerta") { _, payload ->
                    scope.launch { procesarAlerta(payload) }
                }

                // Ubicación en vivo de familiares
                mqtt.suscribir("${MqttConfig.TOPIC_TV}/$tvId/ubicacion") { _, payload ->
                    scope.launch { procesarUbicacion(payload) }
                }



                Log.d("TvMqttSvc", "✅ Suscrito a topics de TV: $tvId")

            } catch (e: Exception) {
                Log.e("TvMqttSvc", "Error MQTT: ${e.message}")
            }
        }
    }

    // ── Procesadores ──────────────────────────────────────────────────────────

    /** Sesión: usuario + lista de familiares que manda el teléfono */
    private suspend fun procesarSesion(payloadJson: String) {
        try {
            val json      = JSONObject(payloadJson)
            val usuarioId = json.optString("usuarioId")
            val nombre    = json.optString("nombre")
            val email     = json.optString("email")

            // Actualizar config con datos del usuario vinculado
            val config = db.configTvDao().obtener()
            if (config != null && config.vinculado) {
                db.configTvDao().guardar(
                    config.copy(
                        nombreUsuario       = nombre,
                        emailUsuario        = email,
                        ultimaActualizacion = System.currentTimeMillis()
                    )
                )
            }

            // Actualizar familiares
            val familiaresArray = json.optJSONArray("familiares") ?: return
            for (i in 0 until familiaresArray.length()) {
                val f = familiaresArray.getJSONObject(i)
                db.familiarTvDao().insertar(
                    FamiliarTvEntity(
                        usuarioId = f.optString("usuarioId"),
                        nombre = f.optString("nombre"),
                        apellido = f.optString("apellido"),
                        latitud = f.optDouble("latitud").takeIf { !it.isNaN() },
                        longitud = f.optDouble("longitud").takeIf { !it.isNaN() },
                        ultimaUbicacionFecha = f.optString("fecha"),
                        enLinea = f.optBoolean("enLinea", true)
                    )
                )
            }
            Log.d("TvMqttSvc", "Sesión actualizada: $nombre | ${familiaresArray.length()} familiar(es)")
        } catch (e: Exception) {
            Log.e("TvMqttSvc", "Error procesando sesión: ${e.message}")
        }
    }

    /** Ubicación en vivo de un familiar específico */
    private suspend fun procesarUbicacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            val id   = json.optString("usuarioId")
            val lat  = json.optDouble("latitud")
            val lng  = json.optDouble("longitud")
            if (lat.isNaN() || lng.isNaN()) return

            db.familiarTvDao().actualizarUbicacion(
                id    = id,
                lat   = lat,
                lng   = lng,
                fecha = json.optString("fecha", fmt.format(Date()))
            )
        } catch (e: Exception) {
            Log.e("TvMqttSvc", "Error procesando ubicación: ${e.message}")
        }
    }

    /** Alerta de emergencia enviada desde el teléfono */
    private suspend fun procesarAlerta(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            db.alertaTvDao().insertar(
                AlertaTvEntity(
                    id = json.optString("alertaId", UUID.randomUUID().toString()),
                    tipo = json.optString("tipoAlerta"),
                    descripcion = json.optString("descripcion"),
                    emisorNombre = json.optString("emisorNombre"),
                    emisorId = json.optString("emisorId"),
                    latitud = json.optDouble("latitud").takeIf { !it.isNaN() },
                    longitud = json.optDouble("longitud").takeIf { !it.isNaN() },
                    fecha = json.optString("fecha", fmt.format(Date())),
                    leida = false
                )
            )
            Log.d("TvMqttSvc", "Alerta recibida y guardada")
        } catch (e: Exception) {
            Log.e("TvMqttSvc", "Error procesando alerta: ${e.message}")
        }
    }

    /** El teléfono confirma la vinculación con los datos del usuario */
    private suspend fun procesarRespuestaVinculacion(payloadJson: String) {
        try {
            val json      = JSONObject(payloadJson)
            val usuarioId = json.optString("usuarioId")
            val nombre    = json.optString("nombre")
            val email     = json.optString("email")

            db.configTvDao().confirmarVinculacion(
                usuarioId = usuarioId,
                nombre    = nombre,
                email     = email,
                ts        = System.currentTimeMillis()
            )
            Log.d("TvMqttSvc", "✅ TV vinculada con usuario: $nombre")
        } catch (e: Exception) {
            Log.e("TvMqttSvc", "Error procesando vinculación: ${e.message}")
        }
    }

    // ── Notificación persistente ───────────────────────────────────────────────

    private fun notif() = NotificationCompat.Builder(this, CANAL)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS TV")
        .setContentText("Conectado — recibiendo datos del teléfono")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(CANAL, "Servicio TV", NotificationManager.IMPORTANCE_LOW)
                )
        }
    }
}