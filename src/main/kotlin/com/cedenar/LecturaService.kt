package com.cedenar

import java.time.Instant

/**
 * Nucleo de logica de negocio del operario de campo.
 * Cubre los tres flujos principales visibles en el prototipo movil:
 *   1. Validacion de lectura con semaforo (pantalla de captura)
 *   2. Guardado offline con sincronizacion automatica (pantalla verde)
 *   3. Transicion de estados del ciclo de vida de la lectura (seccion 4.4)
 */
class LecturaService(
    private val db: LecturaRepository,
    private val api: CedenarApi,
    private val auditLog: AuditLogger
) {

    // --- Funcion 1: Validacion con semaforo ---
    // Origen: RF-05, HU-005, pantalla de captura con indicador verde/amarillo/rojo.
    // Compara la lectura nueva contra el promedio historico del medidor.
    // Un refresco > 50% dispara ROJO; entre 20-50% dispara AMARILLO.
    fun validarLectura(nueva: Int, historico: List<Int>): Semaforo {
        if (historico.isEmpty()) return Semaforo.VERDE
        val promedio = historico.average()
        val desviacion = (nueva - promedio) / promedio * 100
        return when {
            desviacion > 50 -> Semaforo.ROJO
            desviacion > 20 -> Semaforo.AMARILLO
            else            -> Semaforo.VERDE
        }
    }

    // --- Funcion 2: Guardado offline con cola de reintentos ---
    // Origen: RF-05, RNF-04, pantalla "Lectura guardada localmente" (fondo verde).
    // Siempre persiste local primero. Si hay señal, sincroniza de inmediato.
    // Si no hay señal, WorkManager reintenta en segundo plano al recuperarla.
    fun registrarLectura(lectura: Lectura, hayConexion: Boolean): EstadoLectura {
        db.guardarLocal(lectura)

        return if (hayConexion) {
            sincronizarPendientes()
            EstadoLectura.SINCRONIZADA
        } else {
            // La pantalla verde se muestra cuando el estado es PENDIENTE_SYNC
            EstadoLectura.PENDIENTE_SYNC
        }
    }

    // Recorre todas las lecturas no sincronizadas y las envía al servidor.
    // Marca como SINCRONIZADA solo al recibir HTTP 200 (criterio DoD HU-005).
    fun sincronizarPendientes() {
        val pendientes = db.obtenerNoSincronizadas()
        pendientes.forEach { lectura ->
            val exitoso = api.enviarLectura(lectura)
            if (exitoso) {
                db.marcarSincronizada(lectura.id)
                auditLog.registrar("LECTURA_SINCRONIZADA", lectura.recaudadorId, lectura.id)
            }
        }
    }

    // --- Funcion 3: Maquina de estados ---
    // Origen: diagrama de estados seccion 4.4.
    // Cada par (estado actual, evento) produce exactamente un estado siguiente.
    // Si la transicion no existe, el sistema la rechaza en lugar de quedar en estado invalido.
    fun transicionarEstado(lectura: Lectura, evento: EventoLectura): Lectura {
        val nuevoEstado = when (Pair(lectura.estado, evento)) {
            Pair(EstadoLectura.NUEVA,            EventoLectura.INICIAR_CAPTURA) -> EstadoLectura.EN_CAPTURA
            Pair(EstadoLectura.EN_CAPTURA,       EventoLectura.CONFIRMAR)       -> EstadoLectura.CAPTURADA_LOCAL
            Pair(EstadoLectura.CAPTURADA_LOCAL,  EventoLectura.SIN_SEÑAL)       -> EstadoLectura.PENDIENTE_SYNC
            Pair(EstadoLectura.CAPTURADA_LOCAL,  EventoLectura.CON_SEÑAL)       -> EstadoLectura.EN_VALIDACION
            Pair(EstadoLectura.EN_VALIDACION,    EventoLectura.CONSISTENTE)     -> EstadoLectura.VALIDADA
            Pair(EstadoLectura.EN_VALIDACION,    EventoLectura.ANOMALA)         -> EstadoLectura.OBSERVADA
            Pair(EstadoLectura.VALIDADA,         EventoLectura.HTTP_200)        -> EstadoLectura.SINCRONIZADA
            else -> throw IllegalStateException(
                "Transicion invalida: ${lectura.estado} + $evento"
            )
        }
        return lectura.copy(estado = nuevoEstado)
    }

    // --- Funcion auxiliar: crear anomalia ---
    // Origen: diagrama de clases 3.1, diagrama de secuencia 4.3.
    // Se invoca cuando transicionarEstado produce OBSERVADA.
    fun crearAnomalia(lectura: Lectura, semaforo: Semaforo): Anomalia {
        require(semaforo != Semaforo.VERDE) { "Lectura consistente no genera anomalia" }
        return Anomalia(
            lecturaId  = lectura.id,
            contadorId = lectura.contadorId,
            tipo       = if (semaforo == Semaforo.ROJO) TipoAnomalia.CONSUMO_DESPROPORCIONADO
                         else TipoAnomalia.CONSUMO_ELEVADO,
            estado     = EstadoAnomalia.PENDIENTE,
            timestamp  = Instant.now()
        )
    }
}
