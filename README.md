# CEDENAR — Snippets de Lógica de Negocio

Este repositorio contiene los fragmentos de código que respaldan el diseño del sistema propuesto para CEDENAR (Corporación Eléctrica de Nariño). Cada función está trazada a un requerimiento funcional, una historia de usuario y una pantalla del prototipo móvil en Figma.

---

## Por qué Kotlin

El prototipo es una app Android. Kotlin es el lenguaje oficial de Android desde 2017 y reemplaza a Java en proyectos nuevos. Elegirlo hace que el código sea directamente portable al desarrollo real sin traducción.

Además, Kotlin se lee casi como pseudocódigo: `data class`, `when`, `copy()` y los tipos nullables (`?`) expresan la lógica de negocio sin ruido sintáctico, lo que facilita la lectura en una presentación académica.

---

## Estructura

```
src/main/kotlin/com/cedenar/
├── Modelos.kt         — Entidades y enumeraciones del dominio (mapeadas al diagrama de clases, sección 3.1)
├── Interfaces.kt      — Contratos de infraestructura (repositorio, API, logger, notificaciones)
├── LecturaService.kt  — Lógica del operario de campo: validación, offline-first, máquina de estados
└── RadicadoService.kt — Generación de radicados y reporte de fallas con fallback GPS
```

---

## Funciones principales

### `validarLectura()` — LecturaService
Compara la lectura nueva contra el histórico del medidor y devuelve un semáforo (VERDE / AMARILLO / ROJO). Es la lógica detrás del indicador visual en la pantalla de captura del prototipo.

**Trazabilidad:** RF-05 · HU-005 · pantalla de captura con semáforo

---

### `registrarLectura()` + `sincronizarPendientes()` — LecturaService
Siempre guarda la lectura en almacenamiento local primero. Si hay señal, sincroniza de inmediato; si no, el estado queda en `PENDIENTE_SYNC` y la sincronización ocurre en segundo plano al recuperar conexión.

**Trazabilidad:** RF-05 · RNF-04 · pantalla "Lectura guardada localmente" (fondo verde)

---

### `transicionarEstado()` — LecturaService
Implementa la máquina de estados del ciclo de vida de una lectura. Cada par (estado actual + evento) produce un único estado siguiente. Si la transición no existe, lanza excepción.

**Trazabilidad:** Diagrama de máquina de estados, sección 4.4

---

### `generarRadicado()` + `reportarFalla()` — RadicadoService
Genera el número de radicado en el formato visible en la pantalla "PQRS Registrado con Éxito" (`#71906-3163-1682`). `reportarFalla()` incluye fallback: si no hay GPS disponible, determina la zona de despacho técnico a partir del código de contrato del cliente.

**Trazabilidad:** RF-03 · HU-003 · HU-006 · pantalla "PQRS Registrado con Éxito"

---

## Prototipo Figma

- Interactivo: https://www.figma.com/proto/evDOdF75swECN4ijDjgQ6N/CEDENAR_?node-id=0-1&t=CcAxX2niYOhNCSoK-1
- Diseño completo: https://www.figma.com/design/evDOdF75swECN4ijDjgQ6N/CEDENAR_?node-id=0-1&t=CcAxX2niYOhNCSoK-1
