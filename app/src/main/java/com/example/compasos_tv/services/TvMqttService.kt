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
import com.example.compasos_tv.data.entitys.NotificacionTvEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Estado vivo de la conexión, para que la UI distinga tres situaciones que se
 * ven igual si no las separas:
 *   a) sin broker            → "Sin conexión al servidor"
 *   b) broker pero sin móvil → "Esperando al teléfono…"
 *   c) todo bien, sin datos  → "Conectado, esperando familiares…"
 *
 * Es un `object` (singleton) porque el estado de conexión es único para toda
 * la app y se comparte entre servicio y pantallas Compose sin necesidad de
 * un ViewModel intermedio.
 */
object EstadoTv {
    private val _conectadoBroker = MutableStateFlow(false)
    /** true si hay conexión activa con el broker MQTT. */
    val conectadoBroker = _conectadoBroker.asStateFlow()

    private val _telefonoEnLinea = MutableStateFlow(false)
    /** true si el teléfono vinculado dio señales de vida recientemente. */
    val telefonoEnLinea = _telefonoEnLinea.asStateFlow()

    private val _ultimoMensaje = MutableStateFlow(0L)
    /** Timestamp (epoch ms) del último mensaje MQTT recibido de cualquier tipo. */
    val ultimoMensaje = _ultimoMensaje.asStateFlow()

    private val _ultimoError = MutableStateFlow<String?>(null)
    /** Último mensaje de error de conexión, o null si no hay errores pendientes. */
    val ultimoError = _ultimoError.asStateFlow()

    /** Actualiza el estado de conexión con el broker. */
    fun setBroker(v: Boolean)   { _conectadoBroker.value = v }
    /** Actualiza el estado de presencia del teléfono. */
    fun setTelefono(v: Boolean) { _telefonoEnLinea.value = v }
    /** Registra que acaba de llegar un mensaje (para el watchdog de presencia). */
    fun latido()                { _ultimoMensaje.value = System.currentTimeMillis() }
    /** Guarda el último error de conexión para mostrarlo en la UI. */
    fun setError(m: String?)    { _ultimoError.value = m }
}

/**
 * Servicio en primer plano (Foreground Service) que mantiene viva la
 * conexión MQTT de la TV y procesa todos los mensajes que llegan desde el
 * teléfono vinculado: sesión, ubicaciones, alertas, notificaciones,
 * presencia y confirmación de vinculación.
 *
 * Se inicia una sola vez desde `MainActivity.onCreate` mediante [iniciar] y
 * corre en segundo plano durante toda la vida de la app (START_STICKY).
 *
 * ═══════════════════════════════════════════════════════════════════════════
 *  LA PANTALLA ESPERANDO DATOS DEL TELÉFONO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Cambios respecto a la versión anterior, y qué resolvía cada uno:
 *
 * 1. Usa MqttManager.instancia — UNA sola conexión compartida con
 *    VinculacionTvRepository. Antes eran dos, y la confirmación de vinculación
 *    caía en la que nadie escuchaba.
 *
 * 2. procesarRespuestaVinculacion() YA SE LLAMA. Antes estaba escrita
 *    pero ninguna suscripción la invocaba: era código muerto.
 *
 * 3. Suscripción con wildcard `compasos/tv/{tvId}/#` y despacho por sufijo,
 *    en vez de tres suscripciones sueltas. Así llega también /notificacion.
 *
 * 4. Bucle de reintento con backoff. Antes, si el broker no estaba arriba al
 *    encender la TV, el catch se comía la excepción y la pantalla quedaba
 *    muerta hasta reiniciar la app.
 *
 * 5. guardarConservandoUbicacion() en vez de insertar(), para no borrar la
 *    ubicación en cada snapshot.
 */
class TvMqttService : Service() {

    /** Scope de corrutinas propio del servicio; se cancela completo en [onDestroy]. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Conexión MQTT compartida con el resto de la app (ver [MqttManager]). */
    private val mqtt  = MqttManager.instancia
    private lateinit var db: AppDatabaseTv
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val TAG       = "TvMqttSvc"
        private const val CANAL     = "compasos_tv_svc"
        private const val CANAL_SOS = "compasos_tv_sos"
        private const val NOTIF_ID  = 8001

        /**
         * Arranca este servicio como Foreground Service. Debe llamarse una
         * sola vez, típicamente desde `MainActivity.onCreate`.
         */
        fun iniciar(context: Context) {
            val intent = Intent(context, TvMqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        /**
         * ID único de esta TV basado en Android ID.
         * Se usa como sufijo en todos los topics MQTT propios de esta
         * pantalla (ver [MqttConfig]).
         */
        fun obtenerTvId(context: Context): String {
            val androidId = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            )
            return "tv_$androidId"
        }
    }

    /**
     * Inicializa la base de datos, los canales de notificación, arranca el
     * servicio en primer plano, conecta el MQTT y comienza a vigilar la
     * presencia del teléfono.
     */
    override fun onCreate() {
        super.onCreate()
        db = AppDatabaseTv.getInstance(applicationContext)
        crearCanales()
        startForeground(NOTIF_ID, notif())
        conectarYSuscribir()
        vigilarPresenciaTelefono()
    }

    /** El sistema debe volver a crear el servicio si lo mata por falta de recursos. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    /** No es un servicio "bindeable"; solo corre en background. */
    override fun onBind(intent: Intent?): IBinder? = null

    /** Libera corrutinas y cierra la conexión MQTT al destruirse el servicio. */
    override fun onDestroy() {
        scope.cancel()
        mqtt.desconectar()
        EstadoTv.setBroker(false)
        super.onDestroy()
    }

    // ── MQTT ──────────────────────────────────────────────────────────────────

    /**
     * Conecta al broker MQTT y suscribe todos los topics necesarios para
     * esta TV. Si la conexión falla, reintenta con backoff exponencial
     * (hasta 30 s entre intentos) en vez de morir silenciosamente.
     */
    private fun conectarYSuscribir() {
        scope.launch {
            val tvId = obtenerTvId(applicationContext)

            mqtt.alConectar { reconectado ->
                EstadoTv.setBroker(true)
                EstadoTv.setError(null)
                scope.launch {
                    // Publica el "online" retained en el topic de estado propio,
                    // así cualquier suscriptor nuevo sabe de inmediato que la TV está viva.
                    mqtt.publicarSeguro(
                        MqttConfig.topicEstadoTv(tvId),
                        JSONObject().put("online", true)
                            .put("ts", System.currentTimeMillis()).toString(),
                        retained = true
                    )
                }
                if (reconectado) Log.d(TAG, "Reconectado — suscripciones restauradas")
            }

            var intentos = 0
            while (isActive) {
                try {
                    mqtt.conectar(
                        clientId   = "compasos_${tvId.take(24)}",
                        lwtTopic   = MqttConfig.topicEstadoTv(tvId),
                        lwtPayload = JSONObject().put("online", false).toString()
                    )

                    // UNA suscripción para todo lo de esta TV
                    mqtt.suscribir(MqttConfig.topicTodoDeEstaTv(tvId)) { topic, payload ->
                        EstadoTv.latido()
                        scope.launch { despachar(topic, payload) }
                    }

                    // Respuesta de vinculación, si ya hay un código guardado.
                    // Esto es lo que faltaba para que procesarRespuestaVinculacion()
                    // sirviera de algo.
                    db.configTvDao().obtener()?.codigoVinculacion
                        ?.takeIf { it.isNotBlank() }
                        ?.let { cod ->
                            mqtt.suscribir(MqttConfig.topicRespuesta(cod)) { _, payload ->
                                scope.launch { procesarRespuestaVinculacion(payload) }
                            }
                        }

                    EstadoTv.setBroker(true)
                    Log.d(TAG, "✅ Suscrito a topics de TV: $tvId")
                    return@launch

                } catch (e: Exception) {
                    intentos++
                    EstadoTv.setBroker(false)
                    EstadoTv.setError("No se pudo conectar al servidor (${e.message})")
                    Log.e(TAG, "Error MQTT (intento $intentos): ${e.message}")
                    delay(minOf(5_000L * intentos, 30_000L))   // backoff hasta 30 s
                }
            }
        }
    }

    /**
     * Reparte el mensaje según el tramo del topic después del tvId, es decir,
     * según el cuarto segmento de rutas como:
     *   compasos/tv/{tvId}/sesion
     *   compasos/tv/{tvId}/ubicacion/{usuarioId}
     *   compasos/tv/{tvId}/alerta
     *   compasos/tv/{tvId}/notificacion
     *   compasos/tv/{tvId}/telefono_estado
     *   compasos/tv/{tvId}/estado          ← el nuestro, se ignora
     *
     * @param topic   topic completo del mensaje recibido.
     * @param payload cuerpo del mensaje (JSON en texto plano).
     */
    private suspend fun despachar(topic: String, payload: String) {
        val sub = topic.split("/").getOrNull(3) ?: return

        when (sub) {
            "sesion"          -> procesarSesion(payload)
            "ubicacion"       -> procesarUbicacion(payload)
            "alerta"          -> procesarAlerta(payload)
            "notificacion"    -> procesarNotificacion(payload)
            "telefono_estado" -> procesarEstadoTelefono(payload)
            "estado"          -> { /* nuestro propio retained */ }
            else              -> Log.d(TAG, "Subtopic no manejado: $sub")
        }
    }

    // ── Procesadores ──────────────────────────────────────────────────────────

    /**
     * Procesa un mensaje de sesión: datos del usuario vinculado más la lista
     * completa de familiares con su ubicación (snapshot periódico enviado
     * por el teléfono).
     *
     * @param payloadJson JSON con forma { nombre, email, familiares: [...] }.
     */
    private suspend fun procesarSesion(payloadJson: String) {
        try {
            val json   = JSONObject(payloadJson)
            val nombre = json.optString("nombre")
            val email  = json.optString("email")

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

            val familiaresArray = json.optJSONArray("familiares") ?: return
            for (i in 0 until familiaresArray.length()) {
                val f  = familiaresArray.getJSONObject(i)
                val id = f.optString("usuarioId")
                if (id.isBlank()) continue

                // ⚠️ guardarConservandoUbicacion, NO insertar():
                // insertar() con REPLACE borraba la ubicación en cada snapshot.
                db.familiarTvDao().guardarConservandoUbicacion(
                    FamiliarTvEntity(
                        usuarioId            = id,
                        nombre               = f.optString("nombre").ifBlank { "Familiar" },
                        apellido             = f.optString("apellido").ifBlank { null },
                        latitud              = f.optDouble("latitud").takeIf { !it.isNaN() },
                        longitud             = f.optDouble("longitud").takeIf { !it.isNaN() },
                        ultimaUbicacionFecha = f.optString("fecha").ifBlank { null },
                        enLinea              = f.optBoolean("enLinea", false)
                    )
                )
            }
            EstadoTv.setTelefono(true)
            Log.d(TAG, "Sesión actualizada: $nombre | ${familiaresArray.length()} familiar(es)")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando sesión: ${e.message}", e)
        }
    }

    /**
     * Ubicación en vivo de un familiar específico, enviada cada vez que el
     * teléfono de ese familiar reporta una nueva posición GPS.
     *
     * @param payloadJson JSON con forma { usuarioId, latitud, longitud, fecha }.
     */
    private suspend fun procesarUbicacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            val id   = json.optString("usuarioId")
            val lat  = json.optDouble("latitud")
            val lng  = json.optDouble("longitud")
            if (id.isBlank() || lat.isNaN() || lng.isNaN()) return

            val fecha = json.optString("fecha", fmt.format(Date()))

            db.familiarTvDao().actualizarUbicacion(id = id, lat = lat, lng = lng, fecha = fecha)
            db.familiarTvDao().marcarEnLinea(id, true)

            EstadoTv.setTelefono(true)
            Log.d(TAG, "📍 Ubicación de $id: $lat, $lng")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando ubicación: ${e.message}", e)
        }
    }

    /**
     * Alerta de emergencia (SOS) enviada desde el teléfono. Se guarda en
     * Room, opcionalmente actualiza la ubicación del emisor, y dispara una
     * notificación de alta prioridad ([mostrarNotifAlerta]).
     *
     * @param payloadJson JSON con forma { alertaId, tipoAlerta, descripcion,
     *        emisorNombre, emisorId, latitud, longitud, fecha }.
     */
    private suspend fun procesarAlerta(payloadJson: String) {
        try {
            val json         = JSONObject(payloadJson)
            val emisorNombre = json.optString("emisorNombre").ifBlank { "Un familiar" }
            val tipo         = json.optString("tipoAlerta").ifBlank { "SOS" }
            val emisorId     = json.optString("emisorId")
            val lat          = json.optDouble("latitud")
            val lng          = json.optDouble("longitud")
            val fecha        = json.optString("fecha", fmt.format(Date()))

            db.alertaTvDao().insertar(
                AlertaTvEntity(
                    id           = json.optString("alertaId", UUID.randomUUID().toString()),
                    tipo         = tipo,
                    descripcion  = json.optString("descripcion"),
                    emisorNombre = emisorNombre,
                    emisorId     = emisorId,
                    latitud      = lat.takeIf { !it.isNaN() },
                    longitud     = lng.takeIf { !it.isNaN() },
                    fecha        = fecha,
                    leida        = false
                )
            )

            // Si venía con ubicación, aprovéchala para el mapa del emisor
            if (emisorId.isNotBlank() && !lat.isNaN() && !lng.isNaN()) {
                db.familiarTvDao().actualizarUbicacion(emisorId, lat, lng, fecha)
            }

            mostrarNotifAlerta(tipo, emisorNombre)
            EstadoTv.setTelefono(true)
            Log.d(TAG, "🚨 Alerta recibida de $emisorNombre")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando alerta: ${e.message}", e)
        }
    }

    /**
     * Notificación informativa (no emergencia), p. ej. avisos o cambios en
     * la familia.
     *
     * @param payloadJson JSON con forma { notificacionId, alertaId, titulo,
     *        mensaje, tipo, fecha }.
     */
    private suspend fun procesarNotificacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            db.notificacionTvDao().insertar(
                NotificacionTvEntity(
                    id       = json.optString("notificacionId", UUID.randomUUID().toString()),
                    alertaId = json.optString("alertaId").ifBlank { null },
                    titulo   = json.optString("titulo").ifBlank { "CompaSOS" },
                    mensaje  = json.optString("mensaje"),
                    tipo     = json.optString("tipo", "info"),
                    fecha    = json.optString("fecha", fmt.format(Date())),
                    leida    = false
                )
            )
            EstadoTv.setTelefono(true)
            Log.d(TAG, "🔔 Notificación recibida")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación: ${e.message}", e)
        }
    }

    /**
     * Presencia del teléfono (incluye su Last Will cuando se desconecta
     * abruptamente, p. ej. se apaga o pierde red).
     *
     * @param payloadJson JSON con forma { online: Boolean }.
     */
    private fun procesarEstadoTelefono(payloadJson: String) {
        try {
            val online = JSONObject(payloadJson).optBoolean("online", false)
            EstadoTv.setTelefono(online)
            Log.d(TAG, if (online) "📱 Teléfono en línea" else "📱 Teléfono desconectado")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando estado del teléfono: ${e.message}")
        }
    }

    /**
     * El teléfono confirma la vinculación con los datos del usuario.
     * Este método existía en versiones anteriores pero nunca se llamaba
     * (código muerto); ahora sí se invoca desde la suscripción registrada en
     * [conectarYSuscribir].
     *
     * @param payloadJson JSON con forma { usuarioId, nombre, email }.
     */
    private suspend fun procesarRespuestaVinculacion(payloadJson: String) {
        try {
            val json = JSONObject(payloadJson)
            db.configTvDao().confirmarVinculacion(
                usuarioId = json.optString("usuarioId"),
                nombre    = json.optString("nombre"),
                email     = json.optString("email"),
                ts        = System.currentTimeMillis()
            )
            Log.d(TAG, "✅ TV vinculada con usuario: ${json.optString("nombre")}")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando vinculación: ${e.message}")
        }
    }

    // ── Vigilancia de presencia ───────────────────────────────────────────────

    /**
     * Si el teléfono deja de mandar el latido, degradamos la UI en vez de
     * seguir mostrando ubicaciones viejas como si fueran de ahorita.
     *
     * Corre en un loop infinito dentro del [scope] del servicio, revisando
     * cada 20 segundos si pasó más de [MqttConfig.TIMEOUT_TELEFONO_MS] desde
     * el último mensaje recibido.
     */
    private fun vigilarPresenciaTelefono() {
        scope.launch {
            while (isActive) {
                delay(20_000)
                val ultimo = EstadoTv.ultimoMensaje.value
                if (ultimo > 0 &&
                    System.currentTimeMillis() - ultimo > MqttConfig.TIMEOUT_TELEFONO_MS
                ) {
                    EstadoTv.setTelefono(false)
                    val corte = fmt.format(
                        Date(System.currentTimeMillis() - MqttConfig.TIMEOUT_TELEFONO_MS)
                    )
                    runCatching { db.familiarTvDao().marcarInactivosAntesDe(corte) }
                }
            }
        }
    }

    // ── Notificaciones ────────────────────────────────────────────────────────

    /** Muestra una notificación de alta prioridad para una alerta de emergencia. */
    private fun mostrarNotifAlerta(tipo: String, emisor: String) {
        val notif = NotificationCompat.Builder(this, CANAL_SOS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Alerta $tipo")
            .setContentText("$emisor necesita ayuda")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    /** Notificación persistente y de baja prioridad requerida por el Foreground Service. */
    private fun notif() = NotificationCompat.Builder(this, CANAL)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS TV")
        .setContentText("Conectado — recibiendo datos del teléfono")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    /** Crea los canales de notificación requeridos en Android 8+ (servicio y alertas SOS). */
    private fun crearCanales() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Servicio TV", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CANAL_SOS, "Alertas de emergencia",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}
