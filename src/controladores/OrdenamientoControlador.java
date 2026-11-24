package controladores;

import modelos.Documento;
import servicios.CronometroServicio;
import servicios.DocumentoServicio;
import vistas.OrdenamientoVista;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OrdenamientoControlador {

    private OrdenamientoVista vista;

    public OrdenamientoControlador(OrdenamientoVista vista) {
        this.vista = vista;

        this.vista.setOrdenarBurbuja(evento -> ordenarBurbuja());
        this.vista.setOrdenarRapido(evento -> ordenarRapido());
        this.vista.setOrdenarInsercion(evento -> ordenarInsercion());
        this.vista.setOrdenarMezcla(evento -> ordenarMezcla());
        this.vista.setBuscar(evento -> buscar());

        var rutaArchivo = DocumentoServicio.encontrarRutaCsv();
        DocumentoServicio.cargar(rutaArchivo);
        mostrar();
    }

    private void mostrar(){
        var datos=DocumentoServicio.getDatosDocumentos();
        vista.mostrarDatos(DocumentoServicio.getEncabezados(), datos);
    }

    private void ordenarBurbuja() {
        ejecutarOrdenamiento(this::ordenamientoBurbuja);
    }

    private void ordenarRapido() {
        ejecutarOrdenamiento(this::ordenamientoRapido);
    }

    private void ordenarInsercion() {
        ejecutarOrdenamiento(this::ordenamientoInsercion);
    }

    private void ordenarMezcla() {
        ejecutarOrdenamiento(this::ordenamientoMezcla);
    }

    private void ejecutarOrdenamiento(AlgoritmoOrdenamiento algoritmo) {
        int criterio = vista.getCriterio();
        CronometroServicio.iniciarCronometro();
        vista.setTiempo(CronometroServicio.getTextoTiempoCronometro());
        vista.setOrdenamientoEnCurso(true);

        Timer actualizadorTiempo = new Timer(1000,
                evento -> vista.setTiempo(CronometroServicio.getTextoTiempoCronometro()));
        actualizadorTiempo.start();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<Documento> documentos = new ArrayList<>(DocumentoServicio.getDocumentos());
                EntradaOrdenamiento[] entradas = prepararEntradas(documentos, criterio);
                algoritmo.ordenar(entradas);

                List<Documento> ordenados = new ArrayList<>(entradas.length);
                for (EntradaOrdenamiento entrada : entradas) {
                    ordenados.add(entrada.documento);
                }
                DocumentoServicio.setDocumentos(ordenados);
                return null;
            }

            @Override
            protected void done() {
                actualizadorTiempo.stop();
                try {
                    get();
                    vista.setTiempo(CronometroServicio.getTextoTiempoCronometro());
                    mostrar();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(vista, "Se interrumpió el ordenamiento.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(vista,
                            "No se pudo completar el ordenamiento: " + e.getCause().getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    vista.setOrdenamientoEnCurso(false);
                }
            }
        }.execute();
    }

    private EntradaOrdenamiento[] prepararEntradas(List<Documento> documentos, int criterio) {
        EntradaOrdenamiento[] entradas = new EntradaOrdenamiento[documentos.size()];
        for (int i = 0; i < documentos.size(); i++) {
            Documento documento = documentos.get(i);
            entradas[i] = new EntradaOrdenamiento(documento, clave(documento, criterio));
        }
        return entradas;
    }

    private static String clave(Documento documento, int criterio) {
        String nombreCompleto = documento.getApellido1() + " " + documento.getApellido2()
                + " " + documento.getNombre();
        String documentoTipo = documento.getDocumento();

        if (criterio == 0) {
            return normalizar(nombreCompleto) + "\n" + normalizar(documentoTipo);
        }
        return normalizar(documentoTipo) + "\n" + normalizar(nombreCompleto);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim();
    }

    private void ordenamientoBurbuja(EntradaOrdenamiento[] entradas) {
        int limite = entradas.length - 1;
        while (limite > 0) {
            int ultimoIntercambio = 0;
            for (int i = 0; i < limite; i++) {
                if (comparar(entradas[i], entradas[i + 1]) > 0) {
                    intercambiar(entradas, i, i + 1);
                    ultimoIntercambio = i;
                }
            }
            limite = ultimoIntercambio;
        }
    }

    private void ordenamientoInsercion(EntradaOrdenamiento[] entradas) {
        for (int i = 1; i < entradas.length; i++) {
            EntradaOrdenamiento actual = entradas[i];
            int inicio = 0;
            int fin = i;
            while (inicio < fin) {
                int medio = inicio + (fin - inicio) / 2;
                if (comparar(entradas[medio], actual) <= 0) {
                    inicio = medio + 1;
                } else {
                    fin = medio;
                }
            }
            System.arraycopy(entradas, inicio, entradas, inicio + 1, i - inicio);
            entradas[inicio] = actual;
        }
    }

    private void ordenamientoRapido(EntradaOrdenamiento[] entradas) {
        ordenarRapido(entradas, 0, entradas.length - 1);
    }

    private void ordenarRapido(EntradaOrdenamiento[] entradas, int inicio, int fin) {
        while (inicio < fin) {
            String pivote = mediana(
                    entradas[inicio].clave,
                    entradas[inicio + (fin - inicio) / 2].clave,
                    entradas[fin].clave);
            int menores = inicio;
            int actual = inicio;
            int mayores = fin;

            while (actual <= mayores) {
                int comparacion = entradas[actual].clave.compareTo(pivote);
                if (comparacion < 0) {
                    intercambiar(entradas, menores++, actual++);
                } else if (comparacion > 0) {
                    intercambiar(entradas, actual, mayores--);
                } else {
                    actual++;
                }
            }

            int finMenores = menores - 1;
            int inicioMayores = mayores + 1;
            if (finMenores - inicio < fin - inicioMayores) {
                ordenarRapido(entradas, inicio, finMenores);
                inicio = inicioMayores;
            } else {
                ordenarRapido(entradas, inicioMayores, fin);
                fin = finMenores;
            }
        }
    }

    private String mediana(String primero, String segundo, String tercero) {
        if (primero.compareTo(segundo) <= 0) {
            if (segundo.compareTo(tercero) <= 0) {
                return segundo;
            }
            return primero.compareTo(tercero) <= 0 ? tercero : primero;
        }
        if (primero.compareTo(tercero) <= 0) {
            return primero;
        }
        return segundo.compareTo(tercero) <= 0 ? tercero : segundo;
    }

    private void ordenamientoMezcla(EntradaOrdenamiento[] entradas) {
        int cantidad = entradas.length;
        EntradaOrdenamiento[] auxiliar = new EntradaOrdenamiento[cantidad];

        for (int ancho = 1; ancho < cantidad; ancho *= 2) {
            int inicio = 0;
            while (inicio < cantidad - ancho) {
                int medio = inicio + ancho;
                int fin = medio + Math.min(ancho, cantidad - medio);
                if (comparar(entradas[medio - 1], entradas[medio]) > 0) {
                    mezclar(entradas, auxiliar, inicio, medio, fin);
                }
                inicio = fin;
            }
            if (ancho > cantidad / 2) {
                break;
            }
        }
    }

    private void mezclar(EntradaOrdenamiento[] entradas, EntradaOrdenamiento[] auxiliar,
            int inicio, int medio, int fin) {
        int izquierda = inicio;
        int derecha = medio;
        int destino = inicio;

        while (izquierda < medio && derecha < fin) {
            if (comparar(entradas[izquierda], entradas[derecha]) <= 0) {
                auxiliar[destino++] = entradas[izquierda++];
            } else {
                auxiliar[destino++] = entradas[derecha++];
            }
        }
        while (izquierda < medio) {
            auxiliar[destino++] = entradas[izquierda++];
        }
        while (derecha < fin) {
            auxiliar[destino++] = entradas[derecha++];
        }
        System.arraycopy(auxiliar, inicio, entradas, inicio, fin - inicio);
    }

    private int comparar(EntradaOrdenamiento primero, EntradaOrdenamiento segundo) {
        return primero.clave.compareTo(segundo.clave);
    }

    private void intercambiar(EntradaOrdenamiento[] entradas, int primero, int segundo) {
        EntradaOrdenamiento temporal = entradas[primero];
        entradas[primero] = entradas[segundo];
        entradas[segundo] = temporal;
    }

    private interface AlgoritmoOrdenamiento {
        void ordenar(EntradaOrdenamiento[] entradas);
    }

    private static class EntradaOrdenamiento {
        private final Documento documento;
        private final String clave;

        private EntradaOrdenamiento(Documento documento, String clave) {
            this.documento = documento;
            this.clave = clave;
        }
    }

    private void buscar() {
        String texto = vista.getTextoBuscar();
        if (texto == null || texto.trim().isEmpty()) {
            mostrar();
            return;
        }

        String filtro = normalizar(texto);
        List<Documento> encontrados = DocumentoServicio.getDocumentos().stream()
                .filter(documento -> normalizar(documento.getNombreCompleto()).contains(filtro)
                        || normalizar(documento.getDocumento()).contains(filtro))
                .collect(java.util.stream.Collectors.toList());

        String[][] datos = convertirDatos(encontrados);
        vista.mostrarDatos(DocumentoServicio.getEncabezados(), datos);
    }

    private String[][] convertirDatos(List<Documento> documentos) {
        String[][] datos = new String[documentos.size()][5];
        for (int i = 0; i < documentos.size(); i++) {
            Documento documento = documentos.get(i);
            datos[i] = new String[]{
                    String.valueOf(i + 1),
                    documento.getApellido1(),
                    documento.getApellido2(),
                    documento.getNombre(),
                    documento.getDocumento()
            };
        }
        return datos;
    }

}
