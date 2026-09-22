# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Maven multi-module repository. The root `pom.xml` aggregates the modules:

- `wm-shared`: shared states, messages, DTOs, and protocol contracts.
- `wm-central`: Central server and business logic.
- `wm-central-persistence`: text-file persistence adapter for Central.
- `wm-central-ui`: Central monitoring interface.
- `wm-fo` / `wm-fo-ui`: field-operator client logic and interface.
- `wm-ws-engine`, `wm-ws-monitor`, `wm-ws-ui`: watering-station engine, health monitor, and local interface.

Use Maven layout in every module: `src/main/java`, `src/main/resources`, and `src/test/java`. Keep generated `target/` and Eclipse metadata out of commits.

## Build, Test, and Development Commands

Run from the repository root:

```bash
mvn clean verify                 # clean and build every module
mvn -pl wm-central -am package   # build Central and its dependencies
mvn -pl wm-fo-ui -am package     # build the operator UI and dependencies
```

In Eclipse, use `Maven > Update Project...` after changing a POM, then run `Run As > Maven build...` with `clean verify`.

## Coding Style & Naming Conventions

Use four-space indentation, UTF-8, and Java 17 conventions. Classes use `PascalCase`, methods and variables use `camelCase`, and constants use `UPPER_SNAKE_CASE`. Use descriptive packages under `com.watermanagement`. Keep shared contracts free of infrastructure-specific dependencies.

## Testing Guidelines

No test framework is configured yet. New tests belong in each module’s `src/test/java` and should use JUnit 5 once testing dependencies are added. Name test classes after the unit under test, for example `StationStateTest`.

## Commit & Pull Request Guidelines

Existing commits use short imperative Spanish descriptions, such as `Arquitectura inicial del proyecto`. Follow that style and keep each commit focused. Pull requests should explain the affected modules, include build/test results, document configuration changes, and include UI screenshots when interfaces change.

## Architecture & Configuration

Interfaces must communicate with their processes through the defined sockets/Kafka protocol; they must not access Central persistence directly. Keep broker addresses, ports, station IDs, and persistence paths configurable. Never commit credentials or environment-specific secrets.
