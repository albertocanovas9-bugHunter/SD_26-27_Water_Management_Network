import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Cliente inicial de una estación de riego.
 * Se conecta a WM_Central, registra la estación y muestra la respuesta.
 */
public class WM_WS_M {

    private static final String HOST = "localhost";
    private static final int PUERTO = 8010;
    private static final int TIMEOUT_MS = 15_000;

    public static void main(String[] args) {
        System.out.println("--- Monitor de la estación de riego ---");

        try (Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8)) {
            String idEstacion = pedirDato(
                    teclado,
                    "Introduce el ID de la estación (ejemplo WS-04): ");
            String ubicacion = pedirDato(
                    teclado,
                    "Introduce la ubicación (ejemplo River Park): ");

            String trama = "REGISTRO#" + idEstacion + "#" + ubicacion;
            String respuesta = registrarEnCentral(trama);

            System.out.println("Trama enviada: " + trama);
            System.out.println("Respuesta recibida: " + respuesta);

            if (respuesta.startsWith("STATUS#OK#")) {
                System.out.println("La estación se ha registrado correctamente.");
            } else {
                System.out.println("Central ha rechazado el registro.");
            }
        } catch (IOException error) {
            System.err.println("Error al conectar con WM_Central: " + error.getMessage());
        }
    }

    /** Pide un dato obligatorio que no contenga el separador del protocolo. */
    private static String pedirDato(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String dato = teclado.nextLine().trim();

            if (!dato.isEmpty() && !dato.contains("#")) {
                return dato;
            }

            System.out.println("El dato es obligatorio y no puede contener '#'.");
        }
    }

    /** Envía la trama a Central y devuelve su respuesta. */
    private static String registrarEnCentral(String trama) throws IOException {
        try (Socket conexion = new Socket()) {
            conexion.connect(new InetSocketAddress(HOST, PUERTO), TIMEOUT_MS);
            conexion.setSoTimeout(TIMEOUT_MS);

            OutputStream salida = conexion.getOutputStream();
            salida.write(trama.getBytes(StandardCharsets.UTF_8));
            salida.flush();
            conexion.shutdownOutput();

            InputStream entrada = conexion.getInputStream();
            byte[] bytesRespuesta = entrada.readAllBytes();

            if (bytesRespuesta.length == 0) {
                throw new IOException("Central cerró la conexión sin responder.");
            }

            return new String(bytesRespuesta, StandardCharsets.UTF_8).trim();
        }
    }
}
