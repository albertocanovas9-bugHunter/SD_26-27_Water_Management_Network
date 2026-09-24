package com.watermanagement.central.server;

/** Punto de entrada de las peticiones; aquí se incorporará la lógica futura. */
public class RequestProcessor {

    /** Devuelve un acuse provisional para demostrar que la petición se recibió. */
    public String process(String request) {
        if (request == null || request.isBlank()) {
            return "NACK#PETICION_VACIA";
        }
        System.out.println("Petición recibida: " + request);
        return "ACK#RECIBIDO";
    }
}