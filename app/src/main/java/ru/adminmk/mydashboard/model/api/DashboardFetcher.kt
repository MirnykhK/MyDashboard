package ru.adminmk.mydashboard.model.api

import com.google.gson.GsonBuilder
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DashboardFetcher(login: String, password: String, baseUrl: String) {
    private val dashboardApi: DashboardApi

    init {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(BasicAuthInterceptor(login, password))
            .build()

        val gson = GsonBuilder().registerTypeAdapter(DataSet::class.java, DataSetDeserializer())
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()

        val retrofit: Retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

        dashboardApi = retrofit.create(DashboardApi::class.java)
    }

    fun createDashboardObservable(): Observable<DashboardResponse> {
        val body = "this is body"
        val map = mapOf(Pair("map - key", "map - value"))

        return dashboardApi.fetchData(map, body)
    }
}