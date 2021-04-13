package ru.adminmk.mydashboard.model.api

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList


data class DashboardResponse(
    @SerializedName("CalcTime") val calcTime: Date,
    @SerializedName("DataSet") val listOfIndicators: List<DataSet>
) : Serializable, Parcelable {
    constructor(parcel: Parcel) : this(Date(parcel.readLong()), convertParcelToList(parcel))


    companion object {
        private const val serialVersionUID = 1L

        @JvmField
        val CREATOR: Parcelable.Creator<DashboardResponse> =
            object : Parcelable.Creator<DashboardResponse> {
                override fun createFromParcel(source: Parcel): DashboardResponse {
                    return DashboardResponse(source)
                }

                override fun newArray(size: Int): Array<DashboardResponse?> {
                    return arrayOfNulls<DashboardResponse>(size)
                }
            }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(calcTime.time)
        dest.writeInt(listOfIndicators.size)
        for (curData in listOfIndicators) {
            dest.writeStringArray(
                arrayOf(
                    curData.dataIndicator?.name,
                    curData.dataIndicator?.ID,
                    curData.direction.toString(),
                    curData.value
                )
            )
        }
    }


}

private fun convertParcelToList(parcel: Parcel): List<DataSet> {
    val result = ArrayList<DataSet>()

    val lenght = parcel.readInt()
    for (i in 0 until lenght) {
        val array = arrayOfNulls<String>(4)
        parcel.readStringArray(array)
        result.add(DataSet(array))
    }

    return result
}


data class DataSet(
    val dataIndicator: DataIndicator?,
    @SerializedName("Direction") val direction: DirectionOfChange?,
    @SerializedName("Value") val value: String?
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    constructor(stringArray: Array<String?>) : this(
        dataIndicator = DataIndicator(
            name = stringArray[0],
            ID = stringArray[1]
        ), direction = DirectionOfChange.valueOf(stringArray[2]!!), value = stringArray[3]
    )
}

data class DataIndicator(
    @SerializedName("Name") val name: String?,
    val ID: String?
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

class DataSetDeserializer : JsonDeserializer<DataSet> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): DataSet? {
        val jObject = json?.asJsonObject
        jObject?.let {
            val name = jObject.get("Name").asString
            val ID = jObject.get("ID").asString
            val value = jObject.get("Value").asString
            val direction = jObject.get("Direction").asString

            return DataSet(arrayOf(name, ID, direction, value))
        }
        return null
    }
}

enum class DirectionOfChange {
    UP, NONE, DOWN
}