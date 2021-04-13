package ru.adminmk.mydashboard.model.api

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class BasicAuthInterceptor(login: String, password: String): Interceptor {
    private val credentials = Credentials.basic(login, password)


    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val authenticatedRequest: Request = request.newBuilder()
                .header("Authorization", credentials).build()
        return chain.proceed(authenticatedRequest)
    }
}