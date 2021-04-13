package ru.adminmk.mydashboard.viewmodel

import ru.adminmk.mydashboard.model.api.DashboardResponse

class LoginState(val isLogedIn:Boolean?= false, val dashboardResponse: DashboardResponse?=null) {
}