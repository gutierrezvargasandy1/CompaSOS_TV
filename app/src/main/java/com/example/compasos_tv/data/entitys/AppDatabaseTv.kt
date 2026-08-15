package com.example.compasos_tv.data.entitys

import android.content.Context
import androidx.room.*
import com.example.compasos_tv.data.entitys.dao.VideoTvDao

@Database(
    entities = [
        AlertaTvEntity::class,
        FamiliarTvEntity::class,
        ConfigTvEntity::class,
        VideoTvEntity::class

    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabaseTv : RoomDatabase() {

    abstract fun alertaTvDao():   AlertaTvDao
    abstract fun familiarTvDao(): FamiliarTvDao
    abstract fun configTvDao():   ConfigTvDao
    abstract fun videoTvDao(): VideoTvDao


    companion object {
        @Volatile private var INSTANCE: AppDatabaseTv? = null

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