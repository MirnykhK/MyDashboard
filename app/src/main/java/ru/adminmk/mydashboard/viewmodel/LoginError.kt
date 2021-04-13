package ru.adminmk.mydashboard.viewmodel

data class LoginError(
    var listOfErrors: Iterable<Int>? = null,
    var isDetected: Boolean = false,
    var isServerAdressBlank: Boolean = false,
    var isDataBaseBlank: Boolean = false,
    var isLoginBlank: Boolean = false,
    var isPasswordBlank: Boolean = false,
    var isIDBlank: Boolean = false,
    var throwableError: String? = null
) {

}