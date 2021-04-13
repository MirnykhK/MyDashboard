package ru.adminmk.mydashboard.model.database


import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import io.reactivex.Single

@Dao
interface IndicatorDao {
    @Query("SELECT * FROM OrderEntity")
    fun getIndicators(): Single<List<OrderEntity>?>

    @Query("SELECT * FROM OrderEntity WHERE OrderEntity.id IN (:ids)")
    fun getSelectedIndicators(ids: List<String>): List<OrderEntity>?

    @RawQuery()
    fun getAbsentIndicators(query: SupportSQLiteQuery): Single<List<String>?>
    @RawQuery()
    fun getAbsentOrRenamedIndicators(query: SupportSQLiteQuery): Single<List<OrderEntity>?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateIndicator(indicator: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addIndicator(indicator: OrderEntity)
}