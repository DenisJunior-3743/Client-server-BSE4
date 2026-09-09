package com.trustbank.loanapp.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val PRODUCT_DETAIL = "productDetail/{productId}"
    const val APPLY = "apply/{productId}"
    const val APPLICATION_DETAIL = "applicationDetail/{applicationId}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    const val ARG_PRODUCT_ID = "productId"
    const val ARG_APPLICATION_ID = "applicationId"

    fun productDetail(productId: String) = "productDetail/$productId"
    fun apply(productId: String) = "apply/$productId"
    fun applicationDetail(applicationId: String) = "applicationDetail/$applicationId"
}
