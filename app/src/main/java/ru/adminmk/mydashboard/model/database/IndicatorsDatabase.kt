package ru.adminmk.mydashboard.model.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [OrderEntity::class], version = 1, exportSchema = false)
abstract class IndicatorsDatabase : RoomDatabase(){
    abstract fun indicatorDao(): IndicatorDao
}