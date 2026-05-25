package com.cedenar

// Contratos que las capas de infraestructura deben implementar.
// El codigo de negocio depende de estas interfaces, no de implementaciones concretas.

interface LecturaRepository {
    fun guardarLocal(lectura: Lectura)
    fun obtenerNoSincronizadas(): List<Lectura>
    fun marcarSincronizada(lecturaId: String)
}

interface CedenarApi {
    // Retorna true si el servidor respondio HTTP 200
    fun enviarLectura(lectura: Lectura): Boolean
}

interface AuditLogger {
    // RNF-05: todos los eventos relevantes quedan registrados con marca de tiempo.
    // El repositorio de auditoria solo permite INSERT, nunca UPDATE ni DELETE.
    fun registrar(accion: String, usuarioId: String, detalle: String)
}

interface NotificacionService {
    fun notificarCliente(clienteId: String, mensaje: String)
}
