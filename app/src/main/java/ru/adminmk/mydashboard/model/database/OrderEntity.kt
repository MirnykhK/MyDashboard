package ru.adminmk.mydashboard.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    var isSubmitted: Int = 1,
    var isVisible: Int = 1,
    var orderValue: Int
) : Comparable<OrderEntity> {

    override fun compareTo(other: OrderEntity): Int {
        if (isSubmitted == 1 && other.isSubmitted == 1) {
            return orderValue.compareTo(other.orderValue)
        } else if (isSubmitted == 0 && other.isSubmitted == 0) {
            return name.compareTo(other.name)
        } else if (this.isSubmitted == 1) {
            return -1
        }

        return 1
    }
}