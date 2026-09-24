package com.watermanagement.central;

import com.watermanagement.central.config.CentralServerConfig;
import com.watermanagement.central.server.CentralServer;

/** Punto de entrada que compone la configuración y arranca WM_Central. */
public class WM_Central {

    /** Evita crear instancias de la clase principal. */
    private WM_Central() {
    }

    /** Inicia el servidor usando [puerto] o la configuración por defecto. */
    public static void main(String[] args) {
        try {
            CentralServerConfig config = CentralServerConfig.from(args, System.getenv());
            CentralServer server = new CentralServer(config);
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "wm-central-shutdown"));
            System.out.printf("WM_Central iniciada en %s:%d%n", config.host(), config.port());
            server.start();
        } catch (IllegalArgumentException e) {
            System.err.println("Configuración incorrecta: " + e.getMessage());
            System.err.println("Uso: java -jar WM_Central.jar [puerto]");
            System.exit(2);
        } catch (Exception e) {
            System.err.println("No se pudo iniciar WM_Central: " + e.getMessage());
            System.exit(1);
        }
    }
}