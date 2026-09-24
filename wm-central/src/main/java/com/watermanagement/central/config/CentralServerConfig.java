package com.watermanagement.central.config;

/** Configuración mínima del servidor TCP de WM_Central. */
public record CentralServerConfig(String host, int port) {

    public static final int DEFAULT_PORT = 8010;
    public static final String DEFAULT_HOST = "0.0.0.0";

    /** Valida la dirección y el puerto al construir la configuración. */
    public CentralServerConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("El host no puede estar vacío");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535");
        }
    }

    /** Crea la configuración a partir de [puerto] y de variables de entorno. */
    public static CentralServerConfig from(String[] args, java.util.Map<String, String> environment) {
        String host = valueOrDefault(environment.get("WM_CENTRAL_HOST"), DEFAULT_HOST);
        String configuredPort = valueOrDefault(environment.get("WM_CENTRAL_PORT"), null);

        if (args != null && args.length > 1) {
            throw new IllegalArgumentException("Solo se admite un argumento: [puerto]");
        }
        if (args != null && args.length == 1 && !args[0].isBlank()) {
            configuredPort = args[0];
        }

        int port = DEFAULT_PORT;
        if (configuredPort != null) {
            try {
                port = Integer.parseInt(configuredPort);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El puerto debe ser un número entero", e);
            }
        }
        return new CentralServerConfig(host, port);
    }

    /** Devuelve el valor recibido o el valor por defecto si está vacío. */
    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}