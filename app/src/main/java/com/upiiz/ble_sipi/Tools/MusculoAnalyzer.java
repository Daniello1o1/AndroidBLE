package com.upiiz.ble_sipi.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusculoAnalyzer {

    private static final int SAMPLE_RATE = 1000; // Hz

    // ================= RESULTADO =================


    public static class ResultadoAnalisis {
        // Agregar estos campos
        public float orderV;
        public float dynMav;
        // Dominio del tiempo — EMG
        public float rms;
        public float mav;
        public float wl;
        public float var;
        public int   zc;   // Zero Crossings
        public int   ssc;  // Slope Sign Changes

        // Dominio de la frecuencia — EMG
        public float frecuenciaMediana;
        public float frecuenciaMedia;
        public float potenciaTotal;
        public float ratioBandas;     // bajas / altas frecuencias

        // Fatiga
        public float indiceFatiga;        // pendiente de frecuencia mediana
        public float tasaDecaimientoRMS;  // pendiente del RMS

        // Dinamómetro
        public float fuerzaMaxima;
        public float fuerzaMinima;
        public float fuerzaPromedio;
        public float tiempoHastaPico;   // ms
        public float rfd;               // Tasa de desarrollo de fuerza
        public float impulso;           // área bajo curva fuerza-tiempo
        public float coeficienteVariacion;

        // Combinados
        public float eficienciaMusular; // fuerza / EMG RMS
        public float onsetMusular;      // ms entre inicio EMG y pico fuerza

        // Daniels
        public int   danielsEstimado;
        public int   danielsAsignado = -1; // -1 = no asignado aún
    }
    private static List<Float> filtrarNaN(List<Float> signal) {
        if (signal == null) return new ArrayList<>();
        List<Float> filtrada = new ArrayList<>();
        for (Float v : signal) {
            if (v != null && !Float.isNaN(v)) filtrada.add(v);
        }
        return filtrada;
    }

    private static boolean esValida(List<Float> signal) {
        return signal != null && !signal.isEmpty();
    }
    // ================= MÉTODO PRINCIPAL =================

    public static ResultadoAnalisis analizar(List<Float> emg,
                                             List<Float> dynamo,
                                             double[] magnitudesFFT) {
        ResultadoAnalisis r = new ResultadoAnalisis();

        // Filtrar NaN antes de cualquier cálculo
        List<Float> emgLimpio    = filtrarNaN(emg);
        List<Float> dynamoLimpio = filtrarNaN(dynamo);

        if (esValida(emgLimpio)) {
            r.mav    = calcularMAV(emgLimpio);
            r.wl     = calcularWL(emgLimpio);
            r.var    = calcularVarianza(emgLimpio);
            r.orderV = (float) Math.sqrt(r.var + r.mav * r.mav);
            r.rms    = calcularRMS(emgLimpio);
            r.zc     = calcularZC(emgLimpio);
            r.ssc    = calcularSSC(emgLimpio);
        } else {
            // Marcar como NaN si no hay datos válidos
            r.mav = r.wl = r.var = r.orderV = r.rms = Float.NaN;
            r.zc  = r.ssc = 0;
        }

        if (magnitudesFFT != null) {
            r.frecuenciaMediana = calcularFrecuenciaMediana(magnitudesFFT);
            r.frecuenciaMedia   = calcularFrecuenciaMedia(magnitudesFFT);
            r.potenciaTotal     = calcularPotenciaTotal(magnitudesFFT);
            r.ratioBandas       = calcularRatioBandas(magnitudesFFT);
        } else {
            r.frecuenciaMediana = r.frecuenciaMedia =
                    r.potenciaTotal     = r.ratioBandas = Float.NaN;
        }

        if (esValida(emgLimpio) && emgLimpio.size() >= SAMPLE_RATE) {
            r.indiceFatiga       = calcularIndiceFatiga(emgLimpio);
            r.tasaDecaimientoRMS = calcularTasaDecaimientoRMS(emgLimpio);
        } else {
            r.indiceFatiga = r.tasaDecaimientoRMS = Float.NaN;
        }

        if (esValida(dynamoLimpio)) {
            r.dynMav               = calcularMAV(dynamoLimpio);
            r.fuerzaMaxima         = calcularMax(dynamoLimpio);
            r.fuerzaMinima         = calcularMin(dynamoLimpio);
            r.fuerzaPromedio       = r.dynMav;
            r.tiempoHastaPico      = calcularTiempoHastaPico(dynamoLimpio);
            r.rfd                  = calcularRFD(dynamoLimpio);
            r.impulso              = calcularImpulso(dynamoLimpio);
            r.coeficienteVariacion = calcularCoeficienteVariacion(dynamoLimpio);
        } else {
            r.dynMav = r.fuerzaMaxima = r.fuerzaMinima =
                    r.tiempoHastaPico = r.rfd = r.impulso =
                            r.coeficienteVariacion = Float.NaN;
        }

        if (esValida(emgLimpio) && esValida(dynamoLimpio)) {
            r.eficienciaMusular = !Float.isNaN(r.rms) && r.rms > 0
                    ? r.fuerzaMaxima / r.rms : Float.NaN;
            r.onsetMusular = calcularOnsetMuscular(emgLimpio, dynamoLimpio);
        } else {
            r.eficienciaMusular = r.onsetMusular = Float.NaN;
        }

        r.danielsEstimado = estimarDaniels(r);

        return r;
    }

    // ================= DOMINIO DEL TIEMPO =================

    public static float calcularRMS(List<Float> signal) {
        if (signal.isEmpty()) return 0f;
        float suma = 0f;
        for (float v : signal) suma += v * v;
        return (float) Math.sqrt(suma / signal.size());
    }

    public static float calcularMAV(List<Float> signal) {
        if (signal.isEmpty()) return 0f;
        float suma = 0f;
        for (float v : signal) suma += Math.abs(v);
        return suma / signal.size();
    }

    public static float calcularWL(List<Float> signal) {
        if (signal.size() < 2) return 0f;
        float suma = 0f;
        for (int i = 0; i < signal.size() - 1; i++) {
            suma += Math.abs(signal.get(i + 1) - signal.get(i));
        }
        return suma;
    }

    public static float calcularVarianza(List<Float> signal) {
        if (signal.isEmpty()) return 0f;
        float media = calcularMAV(signal);
        float suma = 0f;
        for (float v : signal) suma += (v - media) * (v - media);
        return suma / signal.size();
    }

    public static int calcularZC(List<Float> signal) {
        int count = 0;
        float umbral = 0.01f;
        for (int i = 0; i < signal.size() - 1; i++) {
            if ((signal.get(i) > umbral && signal.get(i + 1) < -umbral) ||
                    (signal.get(i) < -umbral && signal.get(i + 1) > umbral)) {
                count++;
            }
        }
        return count;
    }

    public static int calcularSSC(List<Float> signal) {
        int count = 0;
        float umbral = 0.01f;
        for (int i = 1; i < signal.size() - 1; i++) {
            float diff1 = signal.get(i) - signal.get(i - 1);
            float diff2 = signal.get(i + 1) - signal.get(i);
            if (((diff1 > umbral && diff2 < -umbral) ||
                    (diff1 < -umbral && diff2 > umbral))) {
                count++;
            }
        }
        return count;
    }

    // ================= DOMINIO DE LA FRECUENCIA =================

    public static float calcularFrecuenciaMediana(double[] magnitudes) {
        double potenciaTotal = 0;
        for (double m : magnitudes) potenciaTotal += m * m;

        double acumulado = 0;
        double mitad = potenciaTotal / 2.0;
        float resolucion = (float) SAMPLE_RATE / (magnitudes.length * 2);

        for (int i = 0; i < magnitudes.length; i++) {
            acumulado += magnitudes[i] * magnitudes[i];
            if (acumulado >= mitad) {
                return i * resolucion;
            }
        }
        return 0f;
    }

    public static float calcularFrecuenciaMedia(double[] magnitudes) {
        double numerador = 0;
        double denominador = 0;
        float resolucion = (float) SAMPLE_RATE / (magnitudes.length * 2);

        for (int i = 0; i < magnitudes.length; i++) {
            double potencia = magnitudes[i] * magnitudes[i];
            numerador   += i * resolucion * potencia;
            denominador += potencia;
        }
        return denominador > 0 ? (float)(numerador / denominador) : 0f;
    }

    public static float calcularPotenciaTotal(double[] magnitudes) {
        double suma = 0;
        for (double m : magnitudes) suma += m * m;
        return (float) suma;
    }

    public static float calcularRatioBandas(double[] magnitudes) {
        // Bajas: 20-60Hz, Altas: 150-250Hz
        float resolucion = (float) SAMPLE_RATE / (magnitudes.length * 2);
        double bajas = 0, altas = 0;

        for (int i = 0; i < magnitudes.length; i++) {
            float freq = i * resolucion;
            double pot = magnitudes[i] * magnitudes[i];
            if (freq >= 20 && freq <= 60)   bajas += pot;
            if (freq >= 150 && freq <= 250) altas += pot;
        }
        return altas > 0 ? (float)(bajas / altas) : 0f;
    }

    // ================= FATIGA =================

    public static float calcularIndiceFatiga(List<Float> emg) {
        // Dividir en ventanas de 1 segundo y calcular frecuencia mediana de cada una
        int ventana = SAMPLE_RATE;
        int numVentanas = emg.size() / ventana;
        if (numVentanas < 2) return 0f;

        List<Float> frecuenciasMedianas = new ArrayList<>();
        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, SAMPLE_RATE);

        for (int i = 0; i < numVentanas; i++) {
            List<Float> segmento = emg.subList(i * ventana,
                    Math.min((i + 1) * ventana, emg.size()));
            if (segmento.size() >= 1024) {
                double[] mags = analyzer.computeMagnitudes(
                        new ArrayList<>(segmento), 0);
                if (mags != null) {
                    frecuenciasMedianas.add(calcularFrecuenciaMediana(mags));
                }
            }
        }

        if (frecuenciasMedianas.size() < 2) return 0f;

        // Calcular pendiente por regresión lineal simple
        return calcularPendiente(frecuenciasMedianas);
    }

    public static float calcularTasaDecaimientoRMS(List<Float> emg) {
        int ventana = SAMPLE_RATE / 2; // ventanas de 500ms
        int numVentanas = emg.size() / ventana;
        if (numVentanas < 2) return 0f;

        List<Float> rmsVentanas = new ArrayList<>();
        for (int i = 0; i < numVentanas; i++) {
            List<Float> seg = emg.subList(i * ventana,
                    Math.min((i + 1) * ventana, emg.size()));
            rmsVentanas.add(calcularRMS(new ArrayList<>(seg)));
        }

        return calcularPendiente(rmsVentanas);
    }

    // ================= DINAMÓMETRO =================

    public static float calcularMax(List<Float> signal) {
        return Collections.max(signal);
    }

    public static float calcularMin(List<Float> signal) {
        return Collections.min(signal);
    }

    public static float calcularTiempoHastaPico(List<Float> dynamo) {
        float max = calcularMax(dynamo);
        int indicePico = 0;
        for (int i = 0; i < dynamo.size(); i++) {
            if (dynamo.get(i) >= max) {
                indicePico = i;
                break;
            }
        }
        // Cada muestra = 1ms a 1000Hz
        return indicePico; // en ms
    }

    public static float calcularRFD(List<Float> dynamo) {
        if (dynamo.size() < 2) return 0f;
        float fuerzaInicio = dynamo.get(0);
        float fuerzaMax    = calcularMax(dynamo);
        float tiempoPico   = calcularTiempoHastaPico(dynamo);
        if (tiempoPico == 0) return 0f;
        // RFD en V/s (voltios por segundo, luego se puede convertir a N/s)
        return (fuerzaMax - fuerzaInicio) / (tiempoPico / 1000f);
    }

    public static float calcularImpulso(List<Float> dynamo) {
        // Integral trapezoidal — área bajo la curva fuerza-tiempo
        float suma = 0f;
        float dt = 1f / SAMPLE_RATE; // segundos entre muestras
        for (int i = 0; i < dynamo.size() - 1; i++) {
            suma += (dynamo.get(i) + dynamo.get(i + 1)) / 2f * dt;
        }
        return suma;
    }

    public static float calcularCoeficienteVariacion(List<Float> signal) {
        float media = calcularMAV(signal);
        if (media == 0) return 0f;
        float desv = (float) Math.sqrt(calcularVarianza(signal));
        return (desv / media) * 100f; // en porcentaje
    }

    // ================= COMBINADOS =================

    public static float calcularOnsetMuscular(List<Float> emg, List<Float> dynamo) {
        // Onset EMG: primera muestra donde RMS local supera 3x la línea base
        float lineaBase = calcularRMS(new ArrayList<>(emg.subList(0,
                Math.min(100, emg.size()))));
        float umbral = lineaBase * 3f;

        int onsetEMG = 0;
        for (int i = 0; i < emg.size(); i++) {
            if (Math.abs(emg.get(i)) > umbral) {
                onsetEMG = i;
                break;
            }
        }

        // Pico de fuerza
        int indicePico = 0;
        float max = calcularMax(dynamo);
        for (int i = 0; i < dynamo.size(); i++) {
            if (dynamo.get(i) >= max) {
                indicePico = i;
                break;
            }
        }

        return Math.max(0, indicePico - onsetEMG); // en ms
    }

    // ================= ESCALA DE DANIELS =================

    public static int estimarDaniels(ResultadoAnalisis r) {
        // Si no hay datos suficientes no se puede estimar
        if (Float.isNaN(r.rms) && Float.isNaN(r.fuerzaMaxima)) return -1;

        float rms        = Float.isNaN(r.rms)         ? 0f : r.rms;
        float fuerzaMax  = Float.isNaN(r.fuerzaMaxima) ? 0f : r.fuerzaMaxima;

        if (rms < 0.01f && fuerzaMax < 0.05f)              return 0;
        if (rms > 0.01f && fuerzaMax < 0.1f)               return 1;
        if (fuerzaMax < 0.3f && rms > 0.02f)               return 2;
        if (fuerzaMax >= 0.3f && fuerzaMax < 1.0f)         return 3;
        if (fuerzaMax >= 1.0f && fuerzaMax < 2.0f)         return 4;
        if (fuerzaMax >= 2.0f)                             return 5;

        return 3;
    }

    // ================= HELPERS =================

    private static float calcularPendiente(List<Float> valores) {
        int n = valores.size();
        float sumaX = 0, sumaY = 0, sumaXY = 0, sumaX2 = 0;
        for (int i = 0; i < n; i++) {
            sumaX  += i;
            sumaY  += valores.get(i);
            sumaXY += i * valores.get(i);
            sumaX2 += i * i;
        }
        float denom = n * sumaX2 - sumaX * sumaX;
        return denom != 0 ? (n * sumaXY - sumaX * sumaY) / denom : 0f;
    }
}