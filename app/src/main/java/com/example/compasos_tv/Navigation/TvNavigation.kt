package com.example.compasos_tv.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.compasos_tv.Screan.*

/**
 * Grafo de navegación de la app de TV.
 *
 * Declara todas las rutas definidas en [TvScreen] y asocia cada una con su
 * pantalla (composable). Todas las pantallas, salvo [TvScreen.Vinculacion],
 * se envuelven en [TvMainScreen], que dibuja el menú lateral común.
 *
 * No recibe parámetros: crea y recuerda su propio [rememberNavController].
 */
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
                    // Al vincularse, se reemplaza la pantalla de vinculación en el
                    // back stack para que el botón "atrás" no regrese a ella.
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

        // ── Bandeja de alertas y notificaciones ────────────────────────────────
        composable(TvScreen.AlertasRecibidas.route) {
            TvMainScreen(navController, TvScreen.AlertasRecibidas.route) {
                TvAlertasScreen()
            }
        }

        // ── Listado de familiares vinculados ───────────────────────────────────
        composable(TvScreen.FamiliaEnLinea.route) {
            TvMainScreen(navController, TvScreen.FamiliaEnLinea.route) {
                TvFamiliaScreen()
            }
        }

        // ── Configuración / desvinculación de la TV ────────────────────────────
        composable(TvScreen.Configuracion.route) {
            TvMainScreen(navController, TvScreen.Configuracion.route) {
                TvConfiguracionScreen(
                    onDesvincular = {
                        // Al desvincular se limpia TODO el back stack (popUpTo(0))
                        // para que la TV vuelva a la pantalla de vinculación como
                        // si se hubiera reiniciado la app.
                        navController.navigate(TvScreen.Vinculacion.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ── Catálogo de videos de prevención/seguridad ─────────────────────────
        composable(TvScreen.Videos.route) {
            TvMainScreen(navController, TvScreen.Videos.route) {
                TvVideosScreen()
            }
        }

    }
    }
