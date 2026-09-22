# Water Management Network

## 1. Contexto y objetivo

La práctica consiste en desarrollar un sistema distribuido que simula la gestión centralizada del riego de parques y jardines de una ciudad.

El sistema, denominado **WaterManagement**, debe permitir controlar una red de estaciones de riego, gestionar peticiones de operarios, monitorizar el estado de las estaciones y detectar averías o fugas en tiempo real.

La práctica trabaja con varios paradigmas de comunicación:

- Sockets TCP para la comunicación directa entre componentes.
- Kafka para streaming de eventos y mensajería desacoplada.
- Un fichero de texto para la persistencia local de operadores, estaciones y estado de la Central.

El sistema debe ser asíncrono, resiliente, escalable y seguro. Todos los componentes deben poder enviar peticiones o eventos en cualquier momento, sin turnos estrictos.

## 2. Componentes del sistema

### 2.1. Central (`WM_Central`)

Es el núcleo de gobierno del sistema. Sus responsabilidades son:

- Mantener el panel de monitorización en tiempo real.
- Registrar y dar de alta estaciones de riego.
- Gestionar operadores y estaciones mediante el módulo de persistencia de fichero de texto.
- Recibir peticiones de riego.
- Validar que una estación está disponible antes de autorizar un riego.
- Coordinar las comunicaciones con las estaciones y los operadores.
- Mostrar caudal, volumen, operario y estado de cada estación.
- Iniciar, bloquear o activar una estación individual o todas las estaciones.
- Informar a los operadores de la autorización, denegación, evolución y resultado del riego.

Debe recibir por línea de parámetros:

- Puerto de escucha del servidor de sockets para el Monitor.
- IP y puerto del broker de Kafka.

Central debe permanecer ejecutándose indefinidamente, salvo fallo no controlado.

### 2.2. Estación de riego (`WM_WS`)

Cada estación representa un nodo de campo y está formada por dos componentes:

#### Engine (`WM_WS_E`)

Simula los elementos físicos de la estación:

- Caudalímetro.
- Detector y medición del suministro de agua.
- Actuador de electroválvula.
- Inicio y finalización del riego.
- Menú local para activar o detener el riego.

Debe recibir por línea de parámetros la IP y el puerto de Kafka y del Monitor de la estación.

#### Monitor (`WM_WS_M`)

Supervisa el estado del Engine y de la estación:

- Registra y autentica la estación en Central.
- Envía a Central el identificador y la ubicación de la estación.
- Mantiene la comunicación con Central.
- Comprueba cada segundo la salud del Engine.
- Detecta fugas o anomalías.
- Notifica averías a Central.
- Finaliza inmediatamente un riego si detecta una fuga.

Debe recibir por línea de parámetros:

- Puerto de su servidor de sockets para el Engine.
- IP y puerto de `WM_Central`.
- Identificador de la estación.

### 2.3. Operadores de campo (`WM_FO`)

Es la aplicación utilizada por los operarios de mantenimiento. Permite:

- Solicitar el riego de una estación concreta.
- Consultar el progreso del riego.
- Recibir autorizaciones, denegaciones y avisos de incidencias.
- Leer peticiones desde un fichero para automatizar las pruebas.

Debe recibir por línea de parámetros:

- IP y puerto de Kafka.
- Identificador del operador.

Cuando procese peticiones desde fichero debe esperar 4 segundos entre peticiones sucesivas.

## 3. Estados de una estación

Cada estación puede encontrarse en los siguientes estados:

- **Disponible / Activada:** funciona correctamente y espera una petición. Se muestra en verde.
- **Regando:** suministra agua. Se muestra en verde parpadeante y muestra:
  - caudal en litros por minuto;
  - volumen acumulado en litros;
  - identificador del operador.
- **Fuga:** está conectada, pero el sensor ha detectado una fuga. Se muestra en rojo y no se permite regar.
- **Fuera de servicio:** funciona correctamente, pero Central la ha bloqueado deliberadamente. Se muestra en naranja con la leyenda `Fuera de Servicio`.
- **Desconectada:** no existe comunicación con Central. Se muestra en gris.

Si una estación está almacenada en SQLite pero todavía no se ha conectado, Central debe mostrarla como desconectada, porque no puede conocer su estado real.

## 4. Mecánica de funcionamiento

1. Central arranca y consulta en el fichero de persistencia las estaciones registradas y sus ubicaciones.
2. Central muestra las estaciones conocidas. Las que no estén conectadas aparecen como desconectadas.
3. Central queda a la espera de registros de estaciones y peticiones de activación.
4. Una estación se conecta, se autentica y se registra enviando su identificador y ubicación.
5. La estación queda en reposo y disponible.
6. Un operador solicita el riego desde `WM_FO` o desde el menú local de la estación. Central comprueba que la estación esté activada, conectada y sin fugas.
7. Central autoriza o deniega la petición e informa de cada paso al operador y a la propia estación.
8. Si se autoriza, se abre la electroválvula y comienza el riego.
9. Durante el riego, la estación envía información a Central cada segundo:
   - caudal instantáneo;
   - volumen acumulado;
   - estado de la estación;
   - operador que inició el riego.
10. El riego finaliza cuando se alcanza el tiempo máximo establecido, se selecciona la opción de parada, Central bloquea la estación o se detecta una fuga.
11. Central envía al operador un resumen final con la duración, el volumen consumido y el motivo de finalización.
12. La estación vuelve al estado disponible, salvo que la finalización se deba a una fuga o a un bloqueo.
13. Si se detecta una fuga durante el riego, este debe detenerse inmediatamente y la incidencia debe mostrarse en Central y en el Monitor de la estación.

Central debe poder ordenar de forma arbitraria a una estación concreta o a todas:

- Iniciar un riego.
- Bloquear la estación y finalizar un riego activo.
- Activar una estación previamente bloqueada.

## 5. Comunicación y protocolo

La comunicación directa se realizará mediante sockets TCP. Se recomienda utilizar el siguiente formato de trama:

```text
<STX><DATA><ETX><LRC>
```

El contenido de `REQUEST` y `ANSWER` puede utilizar campos separados por delimitadores:

```text
CODIGO_OPERACION#campo1#campo2#...#campoN
```

`LRC` debe calcularse mediante XOR byte a byte para validar la integridad de la trama.

Las respuestas de protocolo deben utilizar:

```text
<ACK>
<NACK>
```

Los códigos de operación y los modelos de mensajes compartidos deben definirse en `wm-shared` para evitar duplicaciones entre módulos.

## 6. Kafka y eventos

Kafka será el sistema obligatorio de streaming de eventos. Debe utilizarse para desacoplar las comunicaciones que no requieran una conexión directa mediante sockets.

Los eventos deberían cubrir, como mínimo:

- Registro y autenticación de estaciones.
- Cambio de estado de una estación.
- Solicitud de riego.
- Autorización o denegación de un riego.
- Inicio y finalización del riego.
- Medidas de caudal y volumen.
- Detección y resolución de fugas.
- Latidos y comprobaciones de salud.

## 7. Persistencia

La persistencia de la Central se realizará inicialmente mediante un fichero de texto. Esta decisión permite mantener la práctica sencilla y sustituir posteriormente la implementación sin cambiar la lógica de negocio.

El módulo `wm-central-persistence` será responsable de leer y escribir el fichero. Central dependerá de este módulo a través de una abstracción de persistencia, evitando que la lógica de negocio conozca el formato físico del fichero.

El fichero debe almacenar, como mínimo:

- Operadores.
- Estaciones.
- Identificadores y ubicaciones.
- Estado persistido de las estaciones.
- Información relevante de los riegos realizados.

El formato del fichero debe ser consistente y soportar escritura segura, evitando perder información si Central se reinicia. Cuando se despliegue en un contenedor, el fichero deberá ubicarse en un volumen persistente.

## 8. Estructura Java y Maven

El repositorio utiliza un proyecto Maven multimódulo:

```text
Water_Management_Network/
├── pom.xml
├── wm-shared/
├── wm-central-persistence/
├── wm-central/
├── wm-central-ui/
├── wm-fo/
├── wm-fo-ui/
├── wm-ws-engine/
├── wm-ws-monitor/
└── wm-ws-ui/
```

Responsabilidad de cada módulo:

- `wm-shared`: protocolo, mensajes, estados, DTOs y utilidades comunes.
- `wm-central-persistence`: lectura y escritura del fichero de persistencia de Central.
- `wm-central`: servidor Central, lógica de negocio, sockets y Kafka. Depende de `wm-shared` y `wm-central-persistence`.
- `wm-central-ui`: interfaz de monitorización de Central. Es un cliente independiente y no accede directamente al fichero.
- `wm-fo`: lógica y cliente de comunicación del operador.
- `wm-fo-ui`: interfaz independiente del operador. Depende de `wm-fo`.
- `wm-ws-engine`: proceso independiente que simula el caudalímetro y la electroválvula.
- `wm-ws-monitor`: autenticación, registro, latidos y detección de fugas.
- `wm-ws-ui`: interfaz local de la estación.

Las interfaces deben comunicarse con sus procesos mediante sockets o Kafka, no mediante acceso directo a la persistencia ni compartiendo objetos en memoria. Cada aplicación desplegable tendrá su propio `main` y podrá generar su propio JAR. Las clases compartidas deben vivir en `wm-shared`.

Relaciones Maven previstas:

```text
wm-shared
├── wm-central-persistence
│   └── wm-central
├── wm-central-ui
├── wm-fo
│   └── wm-fo-ui
├── wm-ws-engine
├── wm-ws-monitor
└── wm-ws-ui
```

Las flechas representan dependencias de compilación. La comunicación entre procesos seguirá siendo por red, aunque dos módulos compartan contratos definidos en `wm-shared`.

## 9. Despliegue

El despliegue obligatorio es distribuido:

- **Railway / Cloud:** `WM_Central`, el volumen del fichero de persistencia y Kafka.
- **Laboratorio:** varios `WM_FO` en un equipo y varias estaciones `WM_WS` en otro equipo o equipos.
- **Docker:** todos los módulos deben estar contenerizados.
- **GitHub:** obligatorio para control de versiones y trazabilidad.

Cada componente debe configurarse mediante variables o argumentos, evitando direcciones IP, puertos e identificadores codificados en el código.

## 10. Entrega y demostración

La entrega debe incluir:

- Código fuente.
- Memoria de la práctica.
- Guía de despliegue.
- Configuración de Docker.
- Evidencias de la comunicación entre componentes.
- Demostración práctica presencial.

La fecha prevista de entrega es la semana del 26 de octubre de 2026.
