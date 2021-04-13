package ru.adminmk.mydashboard.model.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import io.reactivex.Single
import ru.adminmk.mydashboard.model.api.DataSet
import java.lang.StringBuilder
import kotlin.collections.ArrayList


private const val DATABASE_NAME = "dashboard-database"

class IndicatorsRepository private constructor(context: Context){

    private val database : IndicatorsDatabase = Room.databaseBuilder(
        context.applicationContext,
        IndicatorsDatabase::class.java,
        DATABASE_NAME).build()

    private val indicatorDao = database.indicatorDao()

    fun getIndicators() = indicatorDao.getIndicators()

    fun getSelectedIndicators(ids: List<String>) = indicatorDao.getSelectedIndicators(ids)

    fun getAbsentOrRenamedIndicators(listOfIndicators: List<DataSet>): Single<List<OrderEntity>?>{

        if(listOfIndicators.isEmpty()){
            return Single.create { emitter -> emitter.onSuccess(ArrayList<OrderEntity>()) }
        }else{
            val stringBuilder = StringBuilder("WITH T (id,name) AS (VALUES(?, ?)")

            val listOfArgs = mutableListOf<Any?>(listOfIndicators[0].dataIndicator?.ID , listOfIndicators[0].dataIndicator?.name)

            for (i in 1 until listOfIndicators.size) {
                stringBuilder.append(",(?, ?)")

                listOfArgs+=listOfIndicators[i].dataIndicator?.ID
                listOfArgs+=listOfIndicators[i].dataIndicator?.name
            }

            stringBuilder.append(") SELECT T.id, T.name, OrderEntity.id IS NOT NULL AS isSubmitted, 1 AS isVisible, 99 AS orderValue   FROM T LEFT JOIN OrderEntity ON T.id = OrderEntity.id WHERE OrderEntity.id IS NULL OR T.name != OrderEntity.name")


//            val query = SimpleSQLiteQuery("with T (id,name) as (
// values ("64493930-ac17-11eb-baf5-50eb712465df", ""),("64493930-ac17-11eb-baf5-50eb712465d2", "fffd")
//)select T.id, T.name, OrderEntity.id IS NOT NULL AS isSubmitted, 1 AS isVisible, 99 AS orderValue
// from T LEFT JOIN OrderEntity ON T.id = OrderEntity.id WHERE OrderEntity.id IS NULL OR T.name != OrderEntity.name", ids)
            val stringQuery = stringBuilder.toString()
            val query = SimpleSQLiteQuery(stringQuery, listOfArgs.toTypedArray())
            return indicatorDao.getAbsentOrRenamedIndicators(query)
        }
    }

    fun updateIndicator(indicator: OrderEntity){
        indicatorDao.updateIndicator(indicator)
    }



    fun addIndicator(indicator: OrderEntity){
        indicatorDao.addIndicator(indicator)
    }

    companion object {
        private var INSTANCE: IndicatorsRepository? = null

        fun initialize(context: Context){
            if(INSTANCE == null){
                INSTANCE = IndicatorsRepository(context)
            }
        }


        fun get(): IndicatorsRepository{
            return INSTANCE?: throw IllegalArgumentException("Database is not initialized!")
        }
    }

}