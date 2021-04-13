
import com.google.gson.annotations.SerializedName

data class AAA(
    @SerializedName("CalcTime")
    val calcTime: String,
    @SerializedName("DataSet")
    val dataSet: List<DataSet>
)