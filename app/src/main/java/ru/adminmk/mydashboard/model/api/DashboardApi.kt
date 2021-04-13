package ru.adminmk.mydashboard.model.api

import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface DashboardApi {
    @POST("test/new")
    fun fetchData(@QueryMap options:Map<String, String>, @Body body:String): Observable<DashboardResponse>
}
