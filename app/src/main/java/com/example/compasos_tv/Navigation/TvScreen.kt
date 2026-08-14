package com.example.compasos_tv.Navigation

sealed class TvScreen(val route: String, val titulo: String) {
    object Vinculacion      : TvScreen("tv_vinculacion",        "Vincular")
    object Dashboard        : TvScreen("tv_dashboard",          "Inicio")
    object AlertasRecibidas : TvScreen("tv_alertas_recibidas",  "Alertas")
    object FamiliaEnLinea   : TvScreen("tv_familia",            "Familia")
    object Configuracion    : TvScreen("tv_configuracion",      "Configuración")
}