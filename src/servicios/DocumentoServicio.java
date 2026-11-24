package servicios;

import modelos.Documento;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentoServicio {
    private static String[] encabezados = new String[]{"#", "Primer Apellido", "Segundo Apellido", "Nombres",
            "Documento"};

    public static String[] getEncabezados() {
        return encabezados;
    }

    private static List<Documento> documentos = Collections.emptyList();

    public static void cargar(String nombreArchivo) {
        var ruta = Paths.get(nombreArchivo);

        try (Stream<String> lineas = Files.lines(ruta)) {
            documentos = lineas.skip(1)
                    .filter(linea -> linea != null && !linea.isBlank())
                    .map(DocumentoServicio::crearDocumento)
                    .filter(documento -> documento != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            documentos = Collections.emptyList();
            System.err.println("No se pudo cargar el archivo CSV: " + ruta);
            System.err.println(e.getMessage());
        }
    }

    public static String encontrarRutaCsv() {
        String directorioActual = Paths.get("").toAbsolutePath().toString();
        Path[] rutasPosibles = {
                Paths.get(directorioActual, "src", "datos", "Datos.csv"),
                Paths.get(directorioActual, "IUE_LP2_605_Ordenamiento", "src", "datos", "Datos.csv"),
                Paths.get(directorioActual, "datos", "Datos.csv"),
                Paths.get(directorioActual, "Datos.csv")
        };

        for (Path ruta : rutasPosibles) {
            if (Files.isRegularFile(ruta)) {
                return ruta.toAbsolutePath().toString();
            }
        }
        return rutasPosibles[0].toAbsolutePath().toString();
    }

    private static Documento crearDocumento(String linea) {
        String[] textos = linea.trim()
                .replace("\uFEFF", "")
                .split(";", -1);

        if (textos.length < 4) {
            textos = linea.trim().split(",\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        }

        if (textos.length < 4) {
            return null;
        }

        for (int i = 0; i < textos.length; i++) {
            textos[i] = textos[i].trim().replace("\"", "");
        }
        return new Documento(textos[0], textos[1], textos[2], textos[3]);
    }

    public static List<Documento> getDocumentos() {
        return documentos;
    }

    public static void setDocumentos(List<Documento> nuevosDocumentos) {
        documentos = new ArrayList<>(nuevosDocumentos);
    }

    public static String[][] getDatosDocumentos() {
        AtomicInteger fila = new AtomicInteger(1);
        return documentos.stream()
                .map(documento -> new String[]{
                        String.valueOf(fila.getAndIncrement()),
                        documento.getApellido1(),
                        documento.getApellido2(),
                        documento.getNombre(),
                        documento.getDocumento()})
                .collect(Collectors.toList())
                .toArray(String[][]::new);
    }
}
