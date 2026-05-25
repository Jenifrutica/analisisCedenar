package com.cedenar

import java.time.Instant

/**
 * Genera radicados para PQRS y reportes de falla.
 * Origen: RF-03, HU-003, HU-006.
 * El formato #XXXXX-XXXX-XXXX es el visible en la pantalla
 * "PQRS Registrado con Exito" del prototipo movil.
 */
class RadicadoService(
    private val auditLog: AuditLogger,
    private val notificaciones: NotificacionService
) {

    // Genera el numero de radicado en el formato del prototipo: #71906-3163-1682
    fun generarRadicado(tipo: String, zona: String, clienteId: String): Radicado {
        val numero = "#${(10000..99999).random()}-${(1000..9999).random()}-${(1000..9999).random()}"
        val radicado = Radicado(numero = numero, tipo = tipo, zona = zona)

        auditLog.registrar("RADICADO_CREADO", clienteId, numero)
        notificaciones.notificarCliente(clienteId, "Tu caso fue registrado. Radicado: $numero")

        return radicado
    }

    // Maneja el reporte de falla con fallback cuando no hay GPS.
    // Origen: HU-003 criterio "Reporte sin acceso a GPS".
    // Si el dispositivo no tiene señal GPS o el usuario nego el permiso,
    // se usa la direccion del contrato registrado en el sistema.
    fun reportarFalla(
        tipo: String,
        descripcion: String,
        gps: Pair<Double, Double>?,   // latitud, longitud — null si no hay GPS
        clienteId: String,
        codigoContrato: String,
        direccionContrato: String
    ): Radicado {
        val zona = if (gps != null) {
            determinarZonaPorGps(gps.first, gps.second)
        } else {
            // fallback: zona inferida desde el contrato del cliente
            determinarZonaPorContrato(codigoContrato, direccionContrato)
        }

        return generarRadicado(tipo, zona, clienteId)
    }

    private fun determinarZonaPorGps(lat: Double, lon: Double): String {
        // En produccion: consulta tabla de zonas de despacho tecnico de CEDENAR
        return "ZONA_GPS_${lat.toInt()}_${lon.toInt()}"
    }

    private fun determinarZonaPorContrato(codigo: String, direccion: String): String {
        // En produccion: lookup en base de datos comercial por codigoContrato
        return "ZONA_CONTRATO_$codigo"
    }
}
