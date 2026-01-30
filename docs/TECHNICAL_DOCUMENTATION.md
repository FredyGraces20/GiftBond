# GiftBond Plugin - Documentación Técnica

## 📋 Índice
1. [Arquitectura General](#arquitectura-general)
2. [Componentes Principales](#componentes-principales)
3. [Sistema de Permisos](#sistema-de-permisos)
4. [Sistema de Logging](#sistema-de-logging)
5. [Validación de Entrada](#validación-de-entrada)
6. [Métricas y Monitoreo](#métricas-y-monitoreo)
7. [Health Checks](#health-checks)
8. [API y Extensiones](#api-y-extensiones)

## 🏗️ Arquitectura General

GiftBond sigue una arquitectura modular basada en componentes separados por responsabilidades:

```
GiftBond/
├── commands/           # Comandos del plugin
├── events/            # Listeners de eventos
├── managers/          # Gestores de lógica de negocio
├── menus/             # Interfaces de usuario
├── models/            # Modelos de datos
├── permissions/       # Sistema de permisos
├── logging/           # Sistema de logging
├── validation/        # Validación de entrada
├── metrics/           # Métricas y estadísticas
├── health/            # Health checks
├── storage/           # Gestión de almacenamiento
└── utils/             # Utilidades auxiliares
```

## 🧩 Componentes Principales

### GiftBond (Clase Principal)
- **Responsabilidad**: Punto de entrada y coordinación general
- **Funciones clave**: Inicialización, registro de comandos, gestión de ciclo de vida

### Managers
- **GiftManager**: Lógica central de regalos y amistad
- **DatabaseManager**: Gestión de base de datos SQLite
- **ConfigManager**: Manejo de configuraciones YAML
- **FriendshipManager**: Sistema de puntos de amistad
- **EconomyManager**: Integración con economía (Vault)

### Comandos
- **RegaloCommand**: `/regalo` - Envío de regalos
- **MailboxCommand**: `/gb redeem` - Reclamo de regalos
- **AmistadCommand**: `/amistad` - Sistema de amistad
- **BoostCommand**: `/boost` - Sistema de boosts
- **GiftBondCommand**: `/giftbond` - Comandos administrativos

## 🔐 Sistema de Permisos

### Permisos Disponibles

#### Permisos de Usuario
```
giftbond.send          # Enviar regalos
giftbond.redeem        # Reclamar regalos
giftbond.amistad       # Comandos de amistad
giftbond.boost         # Sistema de boosts
giftbond.history       # Historial de regalos
giftbond.top           # Rankings
giftbond.mailbox       # Acceso al mailbox
```

#### Permisos Administrativos
```
giftbond.admin.reload  # Recargar configuración
giftbond.admin.save    # Guardar datos manualmente
giftbond.admin.debug   # Modo debug
giftbond.admin.*       # Todos los permisos admin
```

#### Permisos Especiales
```
giftbond.bypass.limits    # Bypassear límites
giftbond.bypass.cooldown  # Bypassear cooldowns
giftbond.premium          # Características premium
```

### Uso del PermissionManager
```java
// Verificar permisos
if (PermissionManager.canSendGifts(player)) {
    // Permitir enviar regalos
}

// Verificar permisos administrativos
if (PermissionManager.isAdmin(sender)) {
    // Permitir acciones admin
}
```

## 📝 Sistema de Logging

### Niveles de Log
```java
GiftBondLogger.debug("Mensaje de debug");
GiftBondLogger.info("Mensaje informativo");
GiftBondLogger.warn("Advertencia");
GiftBondLogger.error("Error");
GiftBondLogger.security("Evento de seguridad");
```

### Funcionalidades Especiales
```java
// Loggear comandos
GiftBondLogger.logCommand(playerName, command, success);

// Loggear eventos de regalos
GiftBondLogger.logGiftEvent(sender, receiver, giftName, points);

// Loggear eventos de seguridad
GiftBondLogger.logSecurityEvent(eventType, details);
```

## 🛡️ Validación de Entrada

### Validadores Disponibles
```java
// Validar nombres de jugador
InputValidator.validatePlayerName(name);

// Validar UUIDs
InputValidator.validateUUID(uuidString);

// Validar nombres de regalos
InputValidator.validateGiftName(giftName);

// Validar números enteros
InputValidator.validatePositiveInteger(value, fieldName, min, max);

// Validar argumentos de comandos
InputValidator.validateCommandArguments(args);

// Sanitizar entradas
InputValidator.sanitizeInput(input);
```

### Ejemplo de Uso
```java
var result = InputValidator.validatePlayerName(targetName);
if (!result.isValid()) {
    player.sendMessage(result.getErrorMessage());
    return true;
}
```

## 📊 Métricas y Monitoreo

### Sistema de Métricas
```java
MetricsManager metrics = new MetricsManager(plugin);

// Registrar eventos
metrics.recordGiftSent(sender, receiver, points);
metrics.recordGiftRedeemed(redeemer, points, itemCount);
metrics.recordCommandExecution(command, responseTime);

// Generar reportes
MetricsReport report = metrics.generateReport();
```

### Tipos de Métricas
- **Uso**: Total de regalos enviados/reclamados
- **Performance**: Tiempos de respuesta de comandos
- **Actividad**: Horas pico de uso
- **Popularidad**: Comandos más utilizados
- **Jugadores**: Estadísticas por jugador

## 🩺 Health Checks

### Sistema de Salud
```java
HealthCheckManager healthManager = new HealthCheckManager(plugin);

// Realizar health check completo
CompletableFuture<HealthReport> future = healthManager.performFullHealthCheck();

future.thenAccept(report -> {
    // Procesar reporte
    plugin.getLogger().info(report.getFormattedReport());
});
```

### Checks Disponibles
- **Plugin Status**: Estado general del plugin
- **Config Files**: Presencia de archivos de configuración
- **Permissions**: Registro correcto de permisos
- **Memory Usage**: Uso de memoria JVM
- **Thread Pool**: Estado del pool de threads

## 🔧 API y Extensiones

### Hooks para Desarrolladores
```java
// Obtener instancias principales
GiftManager giftManager = plugin.getGiftManager();
DatabaseManager dbManager = plugin.getDatabaseManager();

// Registrar listeners personalizados
plugin.getServer().getPluginManager().registerEvents(customListener, plugin);

// Acceder a servicios compartidos
MetricsManager metrics = plugin.getMetricsManager();
HealthCheckManager health = plugin.getHealthCheckManager();
```

### Eventos Personalizados
GiftBond dispara varios eventos que otros plugins pueden escuchar:
- `GiftSendEvent`: Cuando se envía un regalo
- `GiftRedeemEvent`: Cuando se reclama un regalo
- `FriendshipPointEvent`: Cambios en puntos de amistad

## ⚙️ Configuración

### Archivos de Configuración
- `config.yml`: Configuración principal
- `messages.yml`: Mensajes del plugin
- `gifts.yml`: Definición de regalos
- `database.yml`: Configuración de base de datos

### Variables de Entorno
```yaml
# Configuración de debugging
debug:
  enabled: true
  level: INFO

# Configuración de métricas
metrics:
  enabled: true
  interval: 3600  # segundos
```

## 🚀 Buenas Prácticas

### Desarrollo
- Usar siempre el sistema de permisos centralizado
- Implementar logging estructurado para todos los eventos importantes
- Validar todas las entradas de usuario
- Registrar métricas para funcionalidades clave

### Mantenimiento
- Ejecutar health checks periódicamente
- Monitorear logs para errores recurrentes
- Revisar métricas de uso para identificar problemas de performance
- Mantener actualizada la documentación

## 📞 Soporte

Para reportar issues o solicitar nuevas funcionalidades:
- GitHub Issues: [enlace al repositorio]
- Discord: [enlace al servidor]
- Email: [dirección de contacto]

---
*Última actualización: Versión 1.1.0*