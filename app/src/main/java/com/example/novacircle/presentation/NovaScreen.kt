package com.example.novacircle.presentation

sealed class NovaScreen(val route: String) {
    data object Splash : NovaScreen("splash")
    data object Login : NovaScreen("login")
    data object Register : NovaScreen("register")
    data object Dashboard : NovaScreen("dashboard")
    data object CircleDetail : NovaScreen("circle/{circleId}") {
        fun createRoute(circleId: String) = "circle/$circleId"
    }
    data object Invites : NovaScreen("invites")
    data object Tracking : NovaScreen("tracking/{userId}") {
        fun createRoute(userId: String) = "tracking/$userId"
    }
}