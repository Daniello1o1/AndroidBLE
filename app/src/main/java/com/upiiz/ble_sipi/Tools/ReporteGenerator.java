package com.upiiz.ble_sipi.Tools;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.upiiz.ble_sipi.Models.MuestraDato;
import com.upiiz.ble_sipi.Models.Prueba;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReporteGenerator {

    // Agrupa las muestras por fase
    public static Map<String, List<MuestraDato>> agruparPorFase(List<MuestraDato> muestras) {
        Map<String, List<MuestraDato>> grupos = new LinkedHashMap<>();
        for (MuestraDato m : muestras) {
            if (!grupos.containsKey(m.fase)) {
                grupos.put(m.fase, new ArrayList<>());
            }
            grupos.get(m.fase).add(m);
        }
        return grupos;
    }

    // ================= CSV =================

    public static File exportarCSV(Context context,
                                   Prueba config,
                                   List<MuestraDato> muestras) throws IOException {

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String nombre = config.nombre.replaceAll("\\s+", "_") + "_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File archivo = new File(dir, nombre);

        StringBuilder sb = new StringBuilder();

        // Encabezado
        sb.append("timestamp_ms,fase,emg,dinamometro,accX,accY,accZ," +
                "gyroX,gyroY,gyroZ,pitch,roll,yaw\n");

        // Datos
        for (MuestraDato m : muestras) {
            sb.append(m.timestampMs).append(",");
            sb.append(m.fase).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.emg)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.dinamometro)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.accX)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.accY)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.accZ)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.gyroX)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.gyroY)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.gyroZ)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.pitch)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.roll)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.yaw)).append("\n");
        }

        FileOutputStream fos = new FileOutputStream(archivo);
        fos.write(sb.toString().getBytes());
        fos.close();

        return archivo;
    }

    // ================= PDF =================

    public static File exportarPDF(Context context,
                                   Prueba config,
                                   List<MuestraDato> muestras,
                                   Map<String, float[]> metricasPorFase) throws IOException {

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String nombre = config.nombre.replaceAll("\\s+", "_") + "_" + timestamp + ".pdf";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File archivo = new File(dir, nombre);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        android.graphics.Canvas canvas = page.getCanvas();
        int y = 50;
        int margen = 40;

        // Título
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(20f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Reporte: " + config.nombre, margen, y, titlePaint);
        y += 30;

        // Info general
        Paint infoPaint = new Paint();
        infoPaint.setTextSize(12f);
        infoPaint.setColor(android.graphics.Color.GRAY);
        canvas.drawText("Duración: " + config.duracionTotalSegundos + "s" +
                        "   |   Muestras totales: " + muestras.size() +
                        "   |   " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()),
                margen, y, infoPaint);
        y += 30;

        // Línea separadora
        Paint linePaint = new Paint();
        linePaint.setColor(android.graphics.Color.LTGRAY);
        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 20;

        // Métricas por fase
        Paint sectionPaint = new Paint();
        sectionPaint.setTextSize(14f);
        sectionPaint.setFakeBoldText(true);

        Paint dataPaint = new Paint();
        dataPaint.setTextSize(11f);

        for (Map.Entry<String, float[]> entry : metricasPorFase.entrySet()) {
            // Si no cabe en la página, crear una nueva
            if (y > 780) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842,
                        document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            String fase = entry.getKey();
            float[] metricas = entry.getValue(); // [MAV, WL, OrderV, DynMAV]

            canvas.drawText("Fase: " + fase, margen, y, sectionPaint);
            y += 20;
            canvas.drawText(
                    String.format(Locale.US,
                            "EMG — MAV: %.4f   WL: %.4f   OrderV: %.4f   |   Dinamómetro MAV: %.4f",
                            metricas[0], metricas[1], metricas[2], metricas[3]),
                    margen, y, dataPaint);
            y += 30;
        }

        document.finishPage(page);

        FileOutputStream fos = new FileOutputStream(archivo);
        document.writeTo(fos);
        fos.close();
        document.close();

        return archivo;
    }
    public static File guardarCSVLocal(Context context,
                                       String pruebaId,
                                       String ejecucionId,
                                       List<MuestraDato> muestras) throws IOException {

        String nombreArchivo = pruebaId + "_" + ejecucionId + ".csv";
        File archivo = new File(context.getFilesDir(), nombreArchivo);

        StringBuilder sb = new StringBuilder();
        sb.append("timestamp_ms,fase,emg,dinamometro,accX,accY,accZ," +
                "gyroX,gyroY,gyroZ,pitch,roll,yaw\n");

        for (MuestraDato m : muestras) {
            sb.append(m.timestampMs).append(",");
            sb.append(m.fase).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.emg)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.dinamometro)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.accX)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.accY)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.accZ)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.gyroX)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.gyroY)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.gyroZ)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.pitch)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.roll)).append(",");
            sb.append(String.format(Locale.US, "%.4f", m.yaw)).append("\n");
        }

        FileOutputStream fos = new FileOutputStream(archivo);
        fos.write(sb.toString().getBytes());
        fos.close();

        return archivo;
    }

    // Leer CSV local y reconstruir muestras por fase
    public static Map<String, List<Float>> leerEMGPorFase(Context context,
                                                          String pruebaId,
                                                          String ejecucionId) {
        String nombreArchivo = pruebaId + "_" + ejecucionId + ".csv";
        File archivo = new File(context.getFilesDir(), nombreArchivo);
        if (!archivo.exists()) return null;

        Map<String, List<Float>> emgPorFase = new LinkedHashMap<>();

        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(archivo));
            String linea;
            boolean primera = true;

            while ((linea = reader.readLine()) != null) {
                if (primera) { primera = false; continue; } // saltar encabezado
                String[] cols = linea.split(",");
                if (cols.length < 3) continue;

                String fase = cols[1];
                float emg   = Float.parseFloat(cols[2]);

                if (!emgPorFase.containsKey(fase)) {
                    emgPorFase.put(fase, new ArrayList<>());
                }
                emgPorFase.get(fase).add(emg);
            }
            reader.close();

        } catch (Exception e) {
            android.util.Log.e("CSV", "Error leyendo CSV: " + e.getMessage());
            return null;
        }

        return emgPorFase;
    }

    // Verificar si existe el archivo local
    public static boolean existeCSVLocal(Context context,
                                         String pruebaId,
                                         String ejecucionId) {
        String nombreArchivo = pruebaId + "_" + ejecucionId + ".csv";
        return new File(context.getFilesDir(), nombreArchivo).exists();
    }
}