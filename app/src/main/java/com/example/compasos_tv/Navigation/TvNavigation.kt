package com.example.compasos_tv.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.compasos_tv.Screan.*
@Composable
fun TvNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = TvScreen.Vinculacion.route
    ) {
        // ── Vinculación — siempre es el punto de entrada ──────────────────────
        // La propia pantalla redirige al Dashboard si ya estaba vinculado
        composable(TvScreen.Vinculacion.route) {
            VinculacionScreen(
                onVinculado = {
                    navController.navigate(TvScreen.Dashboard.route) {
                        popUpTo(TvScreen.Vinculacion.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard — mapa sin padding para que ocupe todo el área ──────────
        composable(TvScreen.Dashboard.route) {
            TvMainScreen(
                navController    = navController,
                rutaSeleccionada = TvScreen.Dashboard.route,
                padContent       = false          // el mapa ocupa todo el espacio
            ) {
                TvDashboardScreen()
            }
        }

        composable(TvScreen.AlertasRecibidas.route) {
            TvMainScreen(navController, TvScreen.AlertasRecibidas.route) {
                TvAlertasScreen()
            }
        }

        composable(TvScreen.FamiliaEnLinea.route) {
            TvMainScreen(navController, TvScreen.FamiliaEnLinea.route) {
                TvFamiliaScreen()
            }
        }

        composable(TvScreen.Configuracion.route) {
            TvMainScreen(navController, TvScreen.Configuracion.route) {
                TvConfiguracionScreen(
                    onDesvincular = {
                        navController.navigate(TvScreen.Vinculacion.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(TvScreen.Videos.route) {
            TvMainScreen(navController, TvScreen.Videos.route) {
                TvVideosScreen()
            }
        }

    }
    }
