package com.example.novacircle.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.novacircle.presentation.auth.AuthViewModel
import com.example.novacircle.presentation.auth.LoginScreen
import com.example.novacircle.presentation.auth.RegisterScreen
import com.example.novacircle.presentation.auth.SessionState
import com.example.novacircle.presentation.auth.SplashViewModel
import com.example.novacircle.presentation.circledetail.CircleDetailScreen
import com.example.novacircle.presentation.dashboard.DashboardScreen
import com.example.novacircle.presentation.invites.InvitesScreen
import com.example.novacircle.presentation.splash.SplashScreen
import com.example.novacircle.presentation.tracking.TrackingScreen

@Composable
fun NovaNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NovaScreen.Splash.route
    ) {
        composable(NovaScreen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val sessionState by splashViewModel.sessionState.collectAsState()
            var animationFinished by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                splashViewModel.checkSession()
            }

            SplashScreen(
                onAnimationFinished = { animationFinished = true }
            )

            LaunchedEffect(animationFinished, sessionState) {
                if (!animationFinished) return@LaunchedEffect
                if (sessionState == SessionState.Checking) return@LaunchedEffect

                when (sessionState) {
                    SessionState.LoggedIn -> {
                        navController.navigate(NovaScreen.Dashboard.route) {
                            popUpTo(NovaScreen.Splash.route) { inclusive = true }
                        }
                    }
                    SessionState.LoggedOut -> {
                        navController.navigate(NovaScreen.Login.route) {
                            popUpTo(NovaScreen.Splash.route) { inclusive = true }
                        }
                    }
                    SessionState.Checking -> Unit
                }
            }
        }

        composable(NovaScreen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NovaScreen.Dashboard.route) {
                        popUpTo(NovaScreen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NovaScreen.Register.route)
                }
            )
        }

        composable(NovaScreen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NovaScreen.Dashboard.route) {
                        popUpTo(NovaScreen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(NovaScreen.Dashboard.route) {
            val authViewModel: AuthViewModel = hiltViewModel()

            DashboardScreen(
                onOpenCircle = { circleId -> navController.navigate(NovaScreen.CircleDetail.createRoute(circleId)) },
                onOpenInvites = { navController.navigate(NovaScreen.Invites.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NovaScreen.Login.route) {
                        popUpTo(NovaScreen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NovaScreen.CircleDetail.route,
            arguments = listOf(navArgument("circleId") { type = NavType.StringType })
        ) {
            CircleDetailScreen(
                onBack = { navController.popBackStack() },
                onMemberClick = { userId -> navController.navigate(NovaScreen.Tracking.createRoute(userId)) },
                onLeftCircle = { navController.popBackStack() }
            )
        }
        composable(NovaScreen.Invites.route) {
            InvitesScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = NovaScreen.Tracking.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            TrackingScreen(onBack = { navController.popBackStack() })
        }
    }
}