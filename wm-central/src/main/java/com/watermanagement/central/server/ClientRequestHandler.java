package com.watermanagement.central.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Atiende una conexión TCP concreta usando un protocolo sencillo por líneas. */
public class ClientRequestHandler implements Runnable {
    private final Socket clientSocket;
    private final RequestProcessor requestProcessor;

    /** Crea el manejador de un cliente conectado. */
    public ClientRequestHandler(Socket clientSocket, RequestProcessor requestProcessor) {
        this.clientSocket = clientSocket;
        this.requestProcessor = requestProcessor;
    }

    /** Lee peticiones hasta la desconexión y devuelve una respuesta por línea. */
    @Override
    public void run() {
        String client = clientSocket.getRemoteSocketAddress().toString();
        System.out.println("Cliente conectado: " + client);
        try (Socket socket = clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String request;
            while ((request = reader.readLine()) != null) {
                writer.write(requestProcessor.process(request));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.printf("Error atendiendo a %s: %s%n", client, e.getMessage());
        } finally {
            System.out.println("Cliente desconectado: " + client);
        }
    }
}