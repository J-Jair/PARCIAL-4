package servicios;

import java.text.DecimalFormat;

public class CronometroServicio {
    private static long inicio;

    public static void iniciarCronometro() {
        inicio = System.nanoTime();
    }

    public static long getTiempoCronometro() {
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    public static String getTextoTiempoCronometro() {
        long segundos = getTiempoCronometro() / 1000;
        long s = segundos % 60;
        long m = (segundos / 60) % 60;
        long h = segundos / 3600;
        DecimalFormat df = new DecimalFormat("00");
        return df.format(h) + ":" + df.format(m) + ":" + df.format(s);
    }

     public static void pausarMilisegundos(int milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
