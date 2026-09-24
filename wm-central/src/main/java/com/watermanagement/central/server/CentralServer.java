package com.watermanagement.central.server;

import com.watermanagement.central.config.CentralServerConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Servidor TCP concurrente que entrega cada cliente a un hilo independiente. */
public class CentralServer implements AutoCloseable {
    private final CentralServerConfig config;
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private final RequestProcessor requestProcessor = new RequestProcessor();
    private volatile boolean running;
    private ServerSocket serverSocket;

    /** Crea el servidor sin abrir todavía el puerto de escucha. */
    public CentralServer(CentralServerConfig config) {
        this.config = config;
    }

    /** Abre el puerto y acepta clientes hasta que se solicite el cierre. */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(config.host(), config.port()));
        running = true;
        System.out.printf("Servidor escuchando en %s:%d%n", config.host(), config.port());
        try {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                clientExecutor.submit(new ClientRequestHandler(clientSocket, requestProcessor));
            }
        } catch (IOException e) {
            if (running) {
                throw e;
            }
        } finally {
            close();
        }
    }

    /** Detiene el servidor y libera el socket y los hilos de clientes. */
    @Override
    public void close() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("No se pudo cerrar el servidor: " + e.getMessage());
            }
        }
        clientExecutor.shutdownNow();
    }
}