package com.upiiz.ble_sipi;

import org.jtransforms.fft.DoubleFFT_1D;
import java.util.List;

public class EMGFrequencyAnalyzer {
    private int fftSize;          // Tamaño de la FFT (potencia de 2)
    private int sampleRate;       // Frecuencia de muestreo (1000 Hz)
    private DoubleFFT_1D fft;
    private double[] window;      // Ventana de Hamming

    public EMGFrequencyAnalyzer(int fftSize, int sampleRate) {
        this.fftSize = fftSize;
        this.sampleRate = sampleRate;
        this.fft = new DoubleFFT_1D(fftSize);
        buildHammingWindow(fftSize);
    }

    /**
     * Construye una ventana de Hamming para reducir artefactos en los bordes
     */
    private void buildHammingWindow(int size) {
        window = new double[size];
        for (int i = 0; i < size; i++) {
            // Fórmula de la ventana de Hamming
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (size - 1));
        }
    }

    /**
     * Aplica la ventana a los datos de entrada
     */
    private double[] applyWindow(List<Float> samples, int startIndex) {
        double[] windowed = new double[fftSize];
        for (int i = 0; i < fftSize && (startIndex + i) < samples.size(); i++) {
            windowed[i] = samples.get(startIndex + i) * window[i];
        }
        return windowed;
    }

    /**
     * Realiza la FFT y retorna las magnitudes por frecuencia
     * @param samples Lista de muestras de EMG
     * @param startIndex Índice donde comenzar a tomar muestras
     * @return Array de magnitudes para cada frecuencia (solo hasta Nyquist)
     */
    public double[] computeMagnitudes(List<Float> samples, int startIndex) {
        if (startIndex + fftSize > samples.size()) {
            return null;  // No hay suficientes muestras
        }

        // 1. Aplicar ventana a los datos
        double[] windowedData = applyWindow(samples, startIndex);

        // 2. Preparar buffer para FFT (real e imaginario intercalados)
        double[] fftBuffer = new double[fftSize * 2];
        for (int i = 0; i < fftSize; i++) {
            fftBuffer[2 * i] = windowedData[i];      // Parte real
            fftBuffer[2 * i + 1] = 0.0;              // Parte imaginaria = 0
        }

        // 3. Ejecutar FFT (transformada compleja)
        fft.complexForward(fftBuffer);

        // 4. Calcular magnitudes para cada frecuencia (hasta Nyquist)
        double[] magnitudes = new double[fftSize / 2];
        for (int i = 0; i < fftSize / 2; i++) {
            double real = fftBuffer[2 * i];
            double imag = fftBuffer[2 * i + 1];
            magnitudes[i] = Math.sqrt(real * real + imag * imag);
        }

        return magnitudes;
    }

    /**
     * Encuentra la frecuencia dominante (pico de mayor magnitud)
     * @return Frecuencia en Hz
     */
    public double getDominantFrequency(List<Float> samples, int startIndex) {
        double[] magnitudes = computeMagnitudes(samples, startIndex);
        if (magnitudes == null) return 0;

        // Encontrar el índice de la magnitud máxima (ignorando DC en índice 0)
        int maxIndex = 1;  // Empezamos desde 1 para evitar DC
        double maxMagnitude = magnitudes[1];

        for (int i = 2; i < magnitudes.length; i++) {
            if (magnitudes[i] > maxMagnitude) {
                maxMagnitude = magnitudes[i];
                maxIndex = i;
            }
        }

        // Convertir índice a frecuencia: f = (i * sampleRate) / fftSize
        return (double) maxIndex * sampleRate / fftSize;
    }

    /**
     * Obtiene el espectro completo de frecuencias con sus magnitudes
     * @return Array de objetos FrequencyComponent
     */
    public FrequencyComponent[] getFrequencySpectrum(List<Float> samples, int startIndex) {
        double[] magnitudes = computeMagnitudes(samples, startIndex);
        if (magnitudes == null) return null;

        FrequencyComponent[] spectrum = new FrequencyComponent[fftSize / 2];
        for (int i = 0; i < fftSize / 2; i++) {
            double frequency = (double) i * sampleRate / fftSize;
            spectrum[i] = new FrequencyComponent(frequency, magnitudes[i]);
        }
        return spectrum;
    }

    /**
     * Clase auxiliar para almacenar pares (frecuencia, magnitud)
     */
    public static class FrequencyComponent {
        public final double frequency;
        public final double magnitude;

        public FrequencyComponent(double frequency, double magnitude) {
            this.frequency = frequency;
            this.magnitude = magnitude;
        }
    }
}