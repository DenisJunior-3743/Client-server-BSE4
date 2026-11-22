package com.trustbank.loanapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trustbank.loanapp.ui.screens.applications.ApplicationDetailScreen
import com.trustbank.loanapp.ui.screens.apply.NewApplicationScreen
import com.trustbank.loanapp.ui.screens.auth.LoginScreen
import com.trustbank.loanapp.ui.screens.auth.RegisterScreen
import com.trustbank.loanapp.ui.screens.auth.VerifyPhoneScreen
import com.trustbank.loanapp.ui.screens.main.MainScreen
import com.trustbank.loanapp.ui.screens.notifications.NotificationsScreen
import com.trustbank.loanapp.ui.screens.onboarding.OnboardingScreen
import com.trustbank.loanapp.ui.screens.products.ProductDetailScreen
import com.trustbank.loanapp.ui.screens.settings.SettingsScreen
import com.trustbank.loanapp.ui.screens.splash.SplashScreen
import com.trustbank.loanapp.data.AppContainer

private val signedOutRoutes = setOf(Routes.SPLASH, Routes.ONBOARDING, Routes.LOGIN, Routes.REGISTER, Routes.VERIFY_PHONE)

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val user by AppContainer.session.currentUser.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    LaunchedEffect(user?.id, route) {
        if (user == null && route != null && route !in signedOutRoutes) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id)
                launchSingleTop = true
            }
        }
    }
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigate = { isLoggedIn ->
                    val destination = if (isLoggedIn) Routes.MAIN else Routes.ONBOARDING
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
                onVerificationNeeded = { navController.navigate(Routes.VERIFY_PHONE) },
            )
        }

        composable(Routes.VERIFY_PHONE) {
            VerifyPhoneScreen(
                onVerified = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onProductClick = { productId -> navController.navigate(Routes.productDetail(productId)) },
                onApplicationClick = { applicationId -> navController.navigate(Routes.applicationDetail(applicationId)) },
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(Routes.ARG_PRODUCT_ID) ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onApplyClick = { id -> navController.navigate(Routes.apply(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.APPLY,
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(Routes.ARG_PRODUCT_ID) ?: return@composable
            NewApplicationScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onSubmitted = { applicationId ->
                    navController.navigate(Routes.applicationDetail(applicationId)) {
                        popUpTo(Routes.MAIN)
                    }
                },
            )
        }

        composable(
            route = Routes.APPLICATION_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_APPLICATION_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments?.getString(Routes.ARG_APPLICATION_ID) ?: return@composable
            ApplicationDetailScreen(applicationId = applicationId, onBack = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}
