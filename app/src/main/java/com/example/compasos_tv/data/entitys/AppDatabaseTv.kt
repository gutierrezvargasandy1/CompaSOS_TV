package com.example.compasos_tv.data.entitys

import android.content.Context
import androidx.room.*
import com.example.compasos_tv.data.entitys.dao.NotificacionTvDao
import com.example.compasos_tv.data.entitys.dao.VideoTvDao

/**
 * Base de datos Room de la app de TV. Punto único de acceso a todas las
 * tablas locales: alertas, familiares, configuración, videos cacheados y
 * notificaciones.
 *
 * Se obtiene siempre a través de [getInstance], que implementa el patrón
 * singleton para que toda la app comparta la misma conexión SQLite.
 */
@Database(
    entities = [
        AlertaTvEntity::class,
        FamiliarTvEntity::class,
        ConfigTvEntity::class,
        VideoTvEntity::class,
        NotificacionTvEntity::class          // ← NUEVO
    ],
    version = 3,                             // ← 2 → 3
    exportSchema = false
)
abstract class AppDatabaseTv : RoomDatabase() {

    /** DAO de alertas de emergencia. */
    abstract fun alertaTvDao():       AlertaTvDao
    /** DAO de familiares y sus ubicaciones. */
    abstract fun familiarTvDao():     FamiliarTvDao
    /** DAO de configuración/vinculación (fila única). */
    abstract fun configTvDao():       ConfigTvDao
    /** DAO de videos de seguridad cacheados. */
    abstract fun videoTvDao():        VideoTvDao
    /** DAO de notificaciones informativas. */
    abstract fun notificacionTvDao(): NotificacionTvDao   // ← NUEVO

    companion object {
        /** Instancia única compartida por toda la app (patrón singleton). */
        @Volatile private var INSTANCE: AppDatabaseTv? = null

        /**
         * Devuelve la instancia única de la base de datos, creándola la
         * primera vez que se solicita.
         *
         * @param context contexto de la app; se usa `applicationContext`
         *        internamente para no filtrar referencias a Activities.
         */
        fun getInstance(context: Context): AppDatabaseTv =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabaseTv::class.java,
                    "compasos_tv.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
