# Water Management Network

Repositorio de la práctica de Sistemas Distribuidos 2026-2027.

El proyecto implementará una solución distribuida para la gestión y monitorización de una red de estaciones de riego. La arquitectura está organizada como un proyecto Java multimódulo gestionado por Maven.

## Requisitos actuales

- Java 17.
- Maven 3.9 o compatible, o Maven integrado en Eclipse mediante m2e.
- Eclipse IDE con soporte Maven.
- Git.

La lógica funcional, las interfaces, la comunicación y el despliegue Docker se implementarán progresivamente. Actualmente el repositorio contiene la estructura Maven y las relaciones entre módulos.

## Estructura del repositorio

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
├── wm-ws-ui/
├── WaterManagement.md
└── practica_watermanagement.txt
```

El `pom.xml` situado en la raíz es el POM padre y agregador. No contiene código Java propio; coordina la compilación de todos los módulos.

## Módulos

### `wm-shared`

Biblioteca común para todos los componentes. Contendrá:

- Estados de las estaciones.
- Mensajes y DTOs.
- Códigos de operación.
- Contratos del protocolo de comunicación.
- Utilidades comunes.

Debe mantenerse independiente de los demás módulos de aplicación.

### `wm-central-persistence`

Capa de persistencia de Central. Inicialmente utilizará un fichero de texto para almacenar operadores, estaciones, ubicaciones, estados e información de riegos.

La persistencia está separada para poder sustituir posteriormente el fichero por SQLite u otra solución sin modificar la lógica principal de Central.

### `wm-central`

Proceso servidor principal de la solución. Será responsable de:

- Gestionar la lógica de negocio.
- Recibir registros de estaciones y peticiones de operadores.
- Coordinar riegos y cambios de estado.
- Comunicarse mediante sockets y Kafka.
- Utilizar `wm-central-persistence` para conservar la información.

### `wm-central-ui`

Interfaz independiente de Central. Mostrará las estaciones, estados, caudal, volumen, operadores e incidencias.

No debe acceder directamente al fichero de persistencia. Se comunicará con `wm-central` mediante la interfaz de comunicación definida para el sistema.

### `wm-fo`

Lógica y cliente de comunicación de los operadores de campo. Gestionará las peticiones de riego, respuestas, avisos e información de las estaciones.

### `wm-fo-ui`

Interfaz independiente de los operadores. Permitirá solicitar riegos, consultar su evolución y visualizar autorizaciones, denegaciones e incidencias.

Depende de `wm-fo`, pero la comunicación con Central se realizará entre procesos mediante el protocolo del sistema.

### `wm-ws-engine`

Proceso que simula los elementos físicos de una estación:

- Caudalímetro.
- Electroválvula.
- Consumo y volumen de agua.
- Inicio y finalización del riego.

### `wm-ws-monitor`

Proceso de monitorización de una estación. Gestionará el registro y autenticación con Central, los latidos, la supervisión del Engine y la detección de fugas o anomalías.

### `wm-ws-ui`

Interfaz local de una estación de riego. Permitirá activar o detener riegos y visualizar el estado local de la estación.

## Relaciones entre módulos

Las dependencias Maven son las siguientes:

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

Estas son dependencias de compilación. La comunicación entre aplicaciones desplegadas será mediante sockets y Kafka, no mediante acceso directo a clases en memoria.

Las interfaces tampoco deben acceder directamente a la persistencia. El flujo esperado será:

```text
wm-central-ui ── comunicación ── wm-central ── persistencia
wm-fo-ui      ── wm-fo         ── comunicación ── wm-central
wm-ws-ui      ── comunicación ── wm-ws-monitor
wm-ws-monitor ── wm-ws-engine
```

## Actualizar el proyecto en Eclipse

Después de añadir o modificar módulos o dependencias:

1. Haz clic derecho sobre el proyecto raíz o sobre cualquiera de los módulos.
2. Selecciona `Maven > Update Project...`.
3. Selecciona todos los módulos.
4. Marca `Force Update of Snapshots/Releases` si es necesario.
5. Pulsa `OK`.

Si el proyecto todavía no está importado:

1. Selecciona `File > Import...`.
2. Elige `Maven > Existing Maven Projects`.
3. Selecciona la carpeta raíz del repositorio.
4. Selecciona el `pom.xml` padre y todos los módulos detectados.
5. Pulsa `Finish`.

## Compilar el proyecto

### Desde Eclipse

Sobre el proyecto raíz, selecciona:

```text
Run As > Maven build...
```

Utiliza el objetivo:

```text
clean verify
```

Esto limpia los artefactos anteriores y compila todos los módulos del reactor Maven.

### Desde una terminal

Situado en la carpeta raíz del repositorio:

```bash
mvn clean verify
```

Para compilar únicamente un módulo y sus dependencias:

```bash
mvn -pl wm-central -am package
```

Para compilar, por ejemplo, la interfaz del operador y sus dependencias:

```bash
mvn -pl wm-fo-ui -am package
```

Los artefactos generados aparecerán en la carpeta `target` de cada módulo. Esta carpeta es temporal y está excluida del control de versiones.

## Estado del despliegue

La estructura está preparada para generar posteriormente un JAR y un contenedor Docker por aplicación desplegable.

El despliegue previsto será:

- Cloud/Railway: Central, fichero de persistencia con volumen persistente y Kafka.
- Laboratorio: interfaces de operadores y estaciones, Engine y Monitor.
- GitHub: control de versiones y trazabilidad.

La implementación de sockets, Kafka, interfaces, persistencia y Docker se incorporará en las siguientes fases de la práctica.
