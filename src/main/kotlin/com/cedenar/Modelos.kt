package com.cedenar

import java.time.Instant

// --- Enumeraciones de estado (mapeadas al diagrama de máquina de estados, sección 4.4) ---

enum class EstadoLectura {
    NUEVA,
    EN_CAPTURA,
    CAPTURADA_LOCAL,
    PENDIENTE_SYNC,     // pantalla verde "Lectura guardada localmente"
    EN_VALIDACION,
    VALIDADA,
    OBSERVADA,          // lectura con anomalía detectada
    SINCRONIZADA
}

enum class EventoLectura {
    INICIAR_CAPTURA,
    CONFIRMAR,
    SIN_SEÑAL,
    CON_SEÑAL,
    CONSISTENTE,
    ANOMALA,
    HTTP_200
}

// Verde = dentro del rango normal, Amarillo = revisar, Rojo = anomalía grave
enum class Semaforo { VERDE, AMARILLO, ROJO }

enum class TipoAnomalia { CONSUMO_ELEVADO, CONSUMO_DESPROPORCIONADO, FRAUDE_SOSPECHOSO, MEDIDOR_ROTO }

enum class EstadoAnomalia { PENDIENTE, RECHAZADA, CORREGIDA }

enum class MetodoEnvio { WHATSAPP, CORREO }

// --- Entidades de dominio (mapeadas al diagrama de clases, sección 3.1) ---

data class Lectura(
    val id: String,
    val contadorId: String,
    val recaudadorId: String,
    val valorKwh: Int,
    val fotoUrl: String,
    val estado: EstadoLectura = EstadoLectura.NUEVA,
    val timestamp: Instant = Instant.now()
)

data class Anomalia(
    val lecturaId: String,
    val contadorId: String,
    val tipo: TipoAnomalia,
    val estado: EstadoAnomalia,
    val timestamp: Instant
)

data class Radicado(
    val numero: String,
    val tipo: String,
    val zona: String,
    val estado: String = "PENDIENTE",
    val creadoEn: Instant = Instant.now()
)
