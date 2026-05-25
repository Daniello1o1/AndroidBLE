package com.upiiz.ble_sipi.Tools;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;

import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.MuestraDato;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.Models.Prueba;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
    public static class DatosFase {
        public String fase;
        public int numMuestras;
        public float[] metricas;          // [MAV, WL, OrderV, DynMAV]
        public MusculoAnalyzer.ResultadoAnalisis analisis;
        public android.graphics.Bitmap fftBitmap;
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
        sb.append("prueba_id,prueba_nombre,ejecucion_id,fecha_ejecucion,")
                .append("paciente_id,paciente_nombre,paciente_edad,paciente_sexo,")
                .append("fase,timestamp_ms,")
                .append("emg,dinamometro,")
                .append("accX,accY,accZ,")
                .append("gyroX,gyroY,gyroZ,")
                .append("pitch,roll,yaw\n");

        for (MuestraDato m : muestras) {
            sb.append(m.pruebaId).append(",");
            sb.append(escaparCSV(m.pruebaNombre)).append(",");
            sb.append(m.ejecucionId != null ? m.ejecucionId : "").append(",");
            sb.append(m.timestampMs).append(",");
            sb.append(m.pacienteId != null ? m.pacienteId : "").append(",");
            sb.append(escaparCSV(m.pacienteNombre)).append(",");
            sb.append(m.pacienteEdad).append(",");
            sb.append(escaparCSV(m.pacienteSexo)).append(",");
            sb.append(escaparCSV(m.fase)).append(",");
            sb.append(m.timestampMs).append(",");
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
        fos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        fos.close();

        return archivo;
    }
    public static File exportarCSVGlobal(Context context,
                                         List<Ejecucion> ejecuciones,
                                         List<Paciente> pacientes,
                                         List<Prueba> pruebas) throws IOException {

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File salida = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS),
                "dataset_global_" + timestamp + ".csv");

        // Mapas para lookup rápido
        Map<String, Paciente> mapaPacientes = new HashMap<>();
        for (Paciente p : pacientes) mapaPacientes.put(p.id, p);

        Map<String, Prueba> mapaPruebas = new HashMap<>();
        for (Prueba p : pruebas) mapaPruebas.put(p.id, p);

        // Calcular número máximo de fases para definir columnas dinámicas
        int maxFases = 1;
        for (Ejecucion e : ejecuciones) {
            if (e.metricasPorFase != null) {
                maxFases = Math.max(maxFases, e.metricasPorFase.size());
            }
        }

        StringBuilder sb = new StringBuilder();

        // ===== ENCABEZADO =====
        // Contexto paciente
        sb.append("paciente_id,nombre,apellidos,edad,sexo,peso,talla,");
        sb.append("fuma,alcohol,drogas,sarcopenia,debilidad_muscular,");
        sb.append("deficit_b12,deficit_d,rehabilitacion,alimentacion_saludable,");
        sb.append("tipo_ejercicio,disartria,");

        // Contexto prueba
        sb.append("prueba_id,prueba_nombre,duracion_segundos,sensores_usados,");
        sb.append("fecha_ejecucion,num_muestras,");

        // Métricas globales EMG
        sb.append("emg_rms,emg_mav,emg_wl,emg_orderv,emg_varianza,");
        sb.append("emg_zc,emg_ssc,");
        sb.append("emg_frec_mediana,emg_frec_media,");
        sb.append("emg_potencia_total,emg_ratio_bandas,");
        sb.append("emg_indice_fatiga,emg_decaimiento_rms,");

        // Métricas globales dinamómetro
        sb.append("dyn_fuerza_max,dyn_fuerza_min,dyn_fuerza_promedio,");
        sb.append("dyn_tiempo_pico_ms,dyn_rfd,dyn_impulso,dyn_cv,");

        // Métricas combinadas
        sb.append("eficiencia_muscular,onset_muscular_ms,");

        // Evaluación clínica
        sb.append("daniels_estimado,daniels_asignado,");

        // Columnas dinámicas por fase
        for (int i = 1; i <= maxFases; i++) {
            sb.append("fase_").append(i).append("_nombre,");
            sb.append("fase_").append(i).append("_emg_mav,");
            sb.append("fase_").append(i).append("_emg_wl,");
            sb.append("fase_").append(i).append("_emg_orderv,");
            sb.append("fase_").append(i).append("_emg_rms,");
            sb.append("fase_").append(i).append("_emg_frec_mediana,");
            sb.append("fase_").append(i).append("_dyn_fuerza_max,");
            sb.append("fase_").append(i).append("_dyn_rfd,");
            sb.append("fase_").append(i).append("_daniels");
            if (i < maxFases) sb.append(",");
        }
        sb.append("\n");

        // ===== FILAS =====
        for (Ejecucion e : ejecuciones) {
            Paciente paciente = mapaPacientes.get(e.pacienteId);
            Prueba   prueba   = mapaPruebas.get(e.pruebaId);

            // Contexto paciente
            if (paciente != null) {
                sb.append(escaparCSV(paciente.id)).append(",");
                sb.append(escaparCSV(paciente.nombre)).append(",");
                sb.append(escaparCSV(paciente.apellidos)).append(",");
                sb.append(paciente.edad).append(",");
                sb.append(escaparCSV(paciente.sexo)).append(",");
                sb.append(formatearValor(paciente.peso)).append(",");
                sb.append(formatearValor(paciente.talla)).append(",");
            } else {
                sb.append(",,,,,,,,");
            }

            // Perfil clínico del paciente — se carga aparte
            // Por ahora dejamos los campos vacíos si no tenemos el perfil
            // Se puede enriquecer después con PerfilClinico
            sb.append(",,,,,,,,,");  // 9 campos de perfil clínico

            // Contexto prueba
            if (prueba != null) {
                sb.append(escaparCSV(prueba.id)).append(",");
                sb.append(escaparCSV(prueba.nombre)).append(",");
                sb.append(prueba.duracionTotalSegundos).append(",");
                sb.append(escaparCSV(construirSensoresUsados(prueba))).append(",");
            } else {
                sb.append(",,,,");
            }

            // Fecha ejecución
            String fecha = e.fechaEjecucion != 0
                    ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new java.util.Date(e.fechaEjecucion))
                    : "";
            sb.append(escaparCSV(fecha)).append(",");
            sb.append(e.totalMuestras).append(",");

            // Métricas globales EMG
            sb.append(formatearValor(e.rms)).append(",");
            sb.append(formatearValor(e.emgMAVTotal)).append(",");
            sb.append(formatearValor(e.emgWLTotal)).append(",");
            sb.append(formatearValor(e.emgOrderVTotal)).append(",");
            sb.append(formatearValor(e.var)).append(",");
            sb.append(e.zc).append(",");
            sb.append(e.ssc).append(",");
            sb.append(formatearValor(e.frecuenciaMediana)).append(",");
            sb.append(formatearValor(e.frecuenciaMedia)).append(",");
            sb.append(formatearValor(e.potenciaTotal)).append(",");
            sb.append(formatearValor(e.ratioBandas)).append(",");
            sb.append(formatearValor(e.indiceFatiga)).append(",");
            sb.append(formatearValor(e.tasaDecaimientoRMS)).append(",");

            // Métricas globales dinamómetro
            sb.append(formatearValor(e.fuerzaMaxima)).append(",");
            sb.append(formatearValor(e.fuerzaMinima)).append(",");
            sb.append(formatearValor(e.dynMAVTotal)).append(",");
            sb.append(formatearValor(e.tiempoHastaPico)).append(",");
            sb.append(formatearValor(e.rfd)).append(",");
            sb.append(formatearValor(e.impulso)).append(",");
            sb.append(formatearValor(e.coeficienteVariacion)).append(",");

            // Combinadas
            sb.append(formatearValor(e.eficienciaMusular)).append(",");
            sb.append(formatearValor(e.onsetMusular)).append(",");

            // Daniels
            sb.append(formatearValorInt(e.danielsEstimado)).append(",");
            sb.append(formatearValorInt(e.danielsAsignado)).append(",");

            // Métricas por fase
            int fasesEscritas = 0;
            if (e.metricasPorFase != null) {
                for (Map.Entry<String, Map<String, Float>> entry
                        : e.metricasPorFase.entrySet()) {

                    String fase          = entry.getKey();
                    Map<String, Float> m = entry.getValue();

                    sb.append(escaparCSV(fase)).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "emgMAV"))).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "emgWL"))).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "emgOrderV"))).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "rms"))).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "frecuenciaMediana"))).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "fuerzaMax"))).append(",");
                    sb.append(formatearValor(getFloatFromMap(m, "rfd"))).append(",");
                    sb.append(formatearValorInt(
                            (int) getFloatFromMap(m, "danielsEstimado")));

                    fasesEscritas++;
                    if (fasesEscritas < maxFases) sb.append(",");
                }
            }

            // Rellenar fases vacías si esta ejecución tiene menos fases que el máximo
            for (int i = fasesEscritas; i < maxFases; i++) {
                if (i > 0) sb.append(",");
                sb.append(",,,,,,,, ");
            }

            sb.append("\n");
        }

        FileOutputStream fos = new FileOutputStream(salida);
        fos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        fos.close();

        return salida;
    }

    private static String construirSensoresUsados(Prueba p) {
        List<String> sensores = new ArrayList<>();
        if (p.usarEMG)          sensores.add("EMG");
        if (p.usarDinamometro)  sensores.add("Dinamometro");
        if (p.usarAcelerometro) sensores.add("Acelerometro");
        if (p.usarGiroscopio)   sensores.add("Giroscopio");
        if (p.usarOrientacion)  sensores.add("Orientacion");
        return String.join("|", sensores);
    }

    private static String formatearValorInt(int valor) {
        return valor == -1 ? "" : String.valueOf(valor);
    }

    private static String construirMetricasEjecucion(Ejecucion e) {
        if (e == null) {
            // Todas las columnas vacías
            return ",,,,,,,,,,,,";
        }
        return String.format(Locale.US,
                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                formatearValorInt(e.danielsAsignado),
                formatearValorInt(e.danielsEstimado),
                formatearValor(e.rms),
                formatearValor(e.emgMAVTotal),
                formatearValor(e.emgWLTotal),
                formatearValor(e.emgOrderVTotal),
                formatearValor(e.frecuenciaMediana),
                formatearValor(e.frecuenciaMedia),
                formatearValor(e.rfd),
                formatearValor(e.fuerzaMaxima),
                formatearValor(e.impulso),
                formatearValor(e.indiceFatiga),
                formatearValor(e.eficienciaMusular));
    }


    // ================= PDF =================

    public static File exportarPDFHistorial(Context context,
                                            Prueba prueba,
                                            Ejecucion ejecucion) throws IOException {

        // Leer muestras del CSV local — siempre existe si la prueba se ejecutó aquí
        List<MuestraDato> muestras = leerMuestrasCompletas(context,
                prueba.id, ejecucion.id);

        if (muestras != null && !muestras.isEmpty()) {
            // Calcular métricas igual que en ResumenPruebaActivity
            Map<String, List<MuestraDato>> muestrasPorFase = agruparPorFase(muestras);
            Map<String, float[]> metricasPorFase = new LinkedHashMap<>();
            EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, 1000);

            for (Map.Entry<String, List<MuestraDato>> entry : muestrasPorFase.entrySet()) {
                String fase             = entry.getKey();
                List<Float> emg         = new ArrayList<>();
                List<Float> dynamo      = new ArrayList<>();
                for (MuestraDato m : entry.getValue()) {
                    emg.add(m.emg);
                    dynamo.add(m.dinamometro);
                }
                double[] mags = emg.size() >= 1024
                        ? analyzer.computeMagnitudes(emg, 0) : null;
                MusculoAnalyzer.ResultadoAnalisis r =
                        MusculoAnalyzer.analizar(emg, dynamo, mags);
                metricasPorFase.put(fase, new float[]{
                        r.mav, r.wl, r.orderV, r.dynMav});
            }

            // Reutilizar exactamente el mismo PDF que la prueba en vivo
            return exportarPDF(context, prueba, muestras, metricasPorFase);
        }

        // Solo si no existe el CSV (cambio de dispositivo)
        return exportarPDFSoloMetricas(context, prueba, ejecucion);
    }
    public static List<MuestraDato> leerMuestrasCompletas(Context context,
                                                          String pruebaId,
                                                          String ejecucionId) {
        String nombreArchivo = pruebaId + "_" + ejecucionId + ".csv";
        File archivo = new File(context.getFilesDir(), nombreArchivo);
        if (!archivo.exists()) return null;

        List<MuestraDato> muestras = new ArrayList<>();

        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(archivo));
            String linea;
            boolean primera = true;

            while ((linea = reader.readLine()) != null) {
                if (primera) { primera = false; continue; }
                String[] cols = linea.split(",");
                if (cols.length < 22) continue;

                // Estructura: prueba_id, prueba_nombre, ejecucion_id, fecha_ejecucion,
                //             paciente_id, paciente_nombre, paciente_edad, paciente_sexo,
                //             fase, timestamp_ms, emg, dinamometro,
                //             accX, accY, accZ, gyroX, gyroY, gyroZ, pitch, roll, yaw
                MuestraDato m = new MuestraDato(
                        Long.parseLong(cols[9].trim()),
                        cols[8].trim());

                m.pruebaId       = cols[0].trim();
                m.pruebaNombre   = cols[1].trim();
                m.ejecucionId    = cols[2].trim();
                m.pacienteId     = cols[4].trim();
                m.pacienteNombre = cols[5].trim();
                m.pacienteEdad   = Integer.parseInt(cols[6].trim());
                m.pacienteSexo   = cols[7].trim();

                m.emg         = Float.parseFloat(cols[10].trim());
                m.dinamometro = Float.parseFloat(cols[11].trim());
                m.accX        = Float.parseFloat(cols[12].trim());
                m.accY        = Float.parseFloat(cols[13].trim());
                m.accZ        = Float.parseFloat(cols[14].trim());
                m.gyroX       = Float.parseFloat(cols[15].trim());
                m.gyroY       = Float.parseFloat(cols[16].trim());
                m.gyroZ       = Float.parseFloat(cols[17].trim());
                m.pitch       = Float.parseFloat(cols[18].trim());
                m.roll        = Float.parseFloat(cols[19].trim());
                m.yaw         = Float.parseFloat(cols[20].trim());

                muestras.add(m);

            }
            reader.close();

        } catch (Exception e) {
            android.util.Log.e("CSV", "Error leyendo muestras: " + e.getMessage());
            return null;
        }

        return muestras;
    }

    private static File exportarPDFSoloMetricas(Context context,
                                                Prueba prueba,
                                                Ejecucion ejecucion) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String nombre = prueba.nombre.replaceAll("\\s+", "_")
                + "_historial_" + timestamp + ".pdf";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File archivo = new File(dir, nombre);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        dibujarPortadaHistorial(page.getCanvas(), prueba, ejecucion);
        document.finishPage(page);

        FileOutputStream fos = new FileOutputStream(archivo);
        document.writeTo(fos);
        fos.close();
        document.close();

        return archivo;
    }
    private static void dibujarPortadaHistorial(android.graphics.Canvas canvas,
                                                Prueba prueba,
                                                Ejecucion ejecucion) {
        int margen = 48;
        int y = 60;

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(android.graphics.Color.BLACK);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setTextSize(14f);
        subtitlePaint.setColor(android.graphics.Color.GRAY);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(12f);
        bodyPaint.setColor(android.graphics.Color.BLACK);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(12f);
        boldPaint.setFakeBoldText(true);
        boldPaint.setColor(android.graphics.Color.BLACK);

        Paint linePaint = new Paint();
        linePaint.setColor(android.graphics.Color.LTGRAY);
        linePaint.setStrokeWidth(1f);

        Paint headerBg = new Paint();
        headerBg.setColor(android.graphics.Color.rgb(66, 133, 244));

        Paint headerPaint = new Paint();
        headerPaint.setTextSize(11f);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(android.graphics.Color.WHITE);

        Paint rowBg = new Paint();

        // Título
        canvas.drawText("Reporte de prueba ortopédica", margen, y, titlePaint);
        y += 28;
        canvas.drawText(prueba.nombre, margen, y, subtitlePaint);
        y += 20;

        String fecha = ejecucion.fechaEjecucion != 0
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new java.util.Date(ejecucion.fechaEjecucion))
                : "";
        canvas.drawText("Ejecutado: " + fecha, margen, y, subtitlePaint);
        y += 24;

        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 24;

        // Info general
        canvas.drawText("Duración: " + ejecucion.duracionReal + "s",
                margen, y, bodyPaint);
        y += 18;
        canvas.drawText("Total de muestras: " + ejecucion.totalMuestras,
                margen, y, bodyPaint);
        y += 30;

        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 24;

        // Métricas totales
        canvas.drawText("Métricas totales", margen, y, boldPaint);
        y += 20;

        canvas.drawRect(margen, y - 14, 595 - margen, y + 4, headerBg);
        canvas.drawText("MAV EMG",         margen + 4,   y, headerPaint);
        canvas.drawText("WL EMG",          margen + 130, y, headerPaint);
        canvas.drawText("OrderV EMG",      margen + 256, y, headerPaint);
        canvas.drawText("MAV Dinamómetro", margen + 382, y, headerPaint);
        y += 20;

        rowBg.setColor(android.graphics.Color.rgb(232, 240, 254));
        canvas.drawRect(margen, y - 14, 595 - margen, y + 4, rowBg);
        canvas.drawText(String.format(Locale.US, "%.4f", ejecucion.emgMAVTotal),
                margen + 4, y, bodyPaint);
        canvas.drawText(String.format(Locale.US, "%.4f", ejecucion.emgWLTotal),
                margen + 130, y, bodyPaint);
        canvas.drawText(String.format(Locale.US, "%.4f", ejecucion.emgOrderVTotal),
                margen + 256, y, bodyPaint);
        canvas.drawText(String.format(Locale.US, "%.4f", ejecucion.dynMAVTotal),
                margen + 382, y, bodyPaint);
        y += 36;

        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 24;

        // Tabla por fase
        if (ejecucion.metricasPorFase != null && !ejecucion.metricasPorFase.isEmpty()) {
            canvas.drawText("Métricas por fase", margen, y, boldPaint);
            y += 20;

            canvas.drawRect(margen, y - 14, 595 - margen, y + 4, headerBg);
            canvas.drawText("Fase",        margen + 4,   y, headerPaint);
            canvas.drawText("MAV EMG",     margen + 120, y, headerPaint);
            canvas.drawText("WL EMG",      margen + 220, y, headerPaint);
            canvas.drawText("OrderV",      margen + 310, y, headerPaint);
            canvas.drawText("MAV Dinamo",  margen + 400, y, headerPaint);
            y += 20;

            boolean fondo = false;
            for (Map.Entry<String, Map<String, Float>> entry
                    : ejecucion.metricasPorFase.entrySet()) {

                rowBg.setColor(fondo
                        ? android.graphics.Color.rgb(232, 240, 254)
                        : android.graphics.Color.WHITE);
                canvas.drawRect(margen, y - 14, 595 - margen, y + 4, rowBg);

                canvas.drawText(entry.getKey(),
                        margen + 4, y, bodyPaint);
                canvas.drawText(String.format(Locale.US, "%.4f",
                                getFloatFromMap(entry.getValue(), "emgMAV")),
                        margen + 120, y, bodyPaint);
                canvas.drawText(String.format(Locale.US, "%.4f",
                                getFloatFromMap(entry.getValue(), "emgWL")),
                        margen + 220, y, bodyPaint);
                canvas.drawText(String.format(Locale.US, "%.4f",
                                getFloatFromMap(entry.getValue(), "emgOrderV")),
                        margen + 310, y, bodyPaint);
                canvas.drawText(String.format(Locale.US, "%.4f",
                                getFloatFromMap(entry.getValue(), "dynMAV")),
                        margen + 400, y, bodyPaint);

                y += 20;
                fondo = !fondo;
            }
        }
    }
    private static float getFloatFromMap(Map<String, Float> map, String key) {
        if (map == null) return 0f;
        Object val = map.get(key);
        if (val instanceof Double) return ((Double) val).floatValue();
        if (val instanceof Float)  return (Float) val;
        return 0f;
    }
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
        int numeroPagina = 1;

        // Agrupar muestras por fase
        Map<String, List<MuestraDato>> muestrasPorFase = agruparPorFase(muestras);
        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, 1000);

        // Calcular análisis por fase
        List<DatosFase> datosFases = new ArrayList<>();
        for (Map.Entry<String, List<MuestraDato>> entry : muestrasPorFase.entrySet()) {
            String fase          = entry.getKey();
            List<MuestraDato> datos = entry.getValue();

            List<Float> emgFase    = new ArrayList<>();
            List<Float> dynamoFase = new ArrayList<>();
            for (MuestraDato m : datos) {
                emgFase.add(m.emg);
                dynamoFase.add(m.dinamometro);
            }

            double[] magnitudes = emgFase.size() >= 1024
                    ? analyzer.computeMagnitudes(emgFase, 0) : null;

            DatosFase df = new DatosFase();
            df.fase        = fase;
            df.numMuestras = datos.size();
            df.metricas    = metricasPorFase.get(fase);
            df.analisis    = MusculoAnalyzer.analizar(emgFase, dynamoFase, magnitudes);
            df.fftBitmap   = magnitudes != null
                    ? generarBitmapFFT(context, analyzer, emgFase) : null;

            datosFases.add(df);
        }

        // ===== PÁGINA 1: Portada =====
        PdfDocument.PageInfo portadaInfo = new PdfDocument.PageInfo.Builder(
                595, 842, numeroPagina++).create();
        PdfDocument.Page portada = document.startPage(portadaInfo);
        dibujarPortada(portada.getCanvas(), config, muestras, metricasPorFase);
        document.finishPage(portada);

        // ===== UNA PÁGINA POR FASE =====
        for (DatosFase df : datosFases) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    595, 842, numeroPagina++).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            dibujarPaginaFase(page.getCanvas(), df, config);
            document.finishPage(page);
        }

        FileOutputStream fos = new FileOutputStream(archivo);
        document.writeTo(fos);
        fos.close();
        document.close();

        return archivo;
    }


// ===== PORTADA =====

    private static void dibujarPortada(android.graphics.Canvas canvas,
                                       Prueba config,
                                       List<MuestraDato> muestras,
                                       Map<String, float[]> metricasPorFase) {
        int margen = 48;
        int y = 60;

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(android.graphics.Color.BLACK);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setTextSize(14f);
        subtitlePaint.setColor(android.graphics.Color.GRAY);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(12f);
        bodyPaint.setColor(android.graphics.Color.BLACK);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(12f);
        boldPaint.setFakeBoldText(true);
        boldPaint.setColor(android.graphics.Color.BLACK);

        Paint linePaint = new Paint();
        linePaint.setColor(android.graphics.Color.LTGRAY);
        linePaint.setStrokeWidth(1f);

        // Título
        canvas.drawText("Reporte de prueba ortopédica", margen, y, titlePaint);
        y += 28;
        canvas.drawText(config.nombre, margen, y, subtitlePaint);
        y += 20;

        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        canvas.drawText(fecha, margen, y, subtitlePaint);
        y += 24;

        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 24;

        // Info general
        canvas.drawText("Duración: " + config.duracionTotalSegundos + "s", margen, y, bodyPaint);
        y += 18;
        canvas.drawText("Total de muestras: " + muestras.size(), margen, y, bodyPaint);
        y += 18;
        canvas.drawText("Fases: " + metricasPorFase.size(), margen, y, bodyPaint);
        y += 30;

        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 24;

        // Tabla resumen de métricas totales
        canvas.drawText("Resumen total", margen, y, boldPaint);
        y += 20;

        // Encabezado tabla
        Paint headerPaint = new Paint();
        headerPaint.setTextSize(11f);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(android.graphics.Color.WHITE);

        Paint headerBg = new Paint();
        headerBg.setColor(android.graphics.Color.rgb(66, 133, 244));

        canvas.drawRect(margen, y - 14, 595 - margen, y + 4, headerBg);
        canvas.drawText("Fase", margen + 4, y, headerPaint);
        canvas.drawText("MAV EMG", margen + 120, y, headerPaint);
        canvas.drawText("WL EMG", margen + 220, y, headerPaint);
        canvas.drawText("OrderV EMG", margen + 310, y, headerPaint);
        canvas.drawText("MAV Dinamo", margen + 410, y, headerPaint);
        y += 20;

        // Filas
        boolean fondo = false;
        Paint rowBg = new Paint();

        for (Map.Entry<String, float[]> entry : metricasPorFase.entrySet()) {
            String fase    = entry.getKey();
            float[] vals   = entry.getValue();

            rowBg.setColor(fondo
                    ? android.graphics.Color.rgb(232, 240, 254)
                    : android.graphics.Color.WHITE);
            canvas.drawRect(margen, y - 14, 595 - margen, y + 4, rowBg);

            canvas.drawText(fase, margen + 4, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", vals[0]), margen + 120, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", vals[1]), margen + 220, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", vals[2]), margen + 310, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", vals[3]), margen + 410, y, bodyPaint);

            y += 20;
            fondo = !fondo;
        }
    }

// ===== PÁGINA POR FASE =====

    private static void dibujarPaginaFase(android.graphics.Canvas canvas,
                                          DatosFase df,
                                          Prueba config) {
        int margen = 48;
        int y = 50;

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(android.graphics.Color.BLACK);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(11f);
        bodyPaint.setColor(android.graphics.Color.BLACK);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(11f);
        boldPaint.setFakeBoldText(true);
        boldPaint.setColor(android.graphics.Color.BLACK);

        Paint grayPaint = new Paint();
        grayPaint.setTextSize(10f);
        grayPaint.setColor(android.graphics.Color.GRAY);

        Paint linePaint = new Paint();
        linePaint.setColor(android.graphics.Color.LTGRAY);
        linePaint.setStrokeWidth(1f);

        Paint headerBg = new Paint();
        headerBg.setColor(android.graphics.Color.rgb(66, 133, 244));

        Paint headerPaint = new Paint();
        headerPaint.setTextSize(10f);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(android.graphics.Color.WHITE);

        Paint rowBg = new Paint();

        // ===== TÍTULO =====
        canvas.drawText("Fase: " + df.fase, margen, y, titlePaint);
        y += 18;
        canvas.drawText(config.nombre + "  ·  " + df.numMuestras + " muestras",
                margen, y, grayPaint);
        y += 16;
        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 16;

        // ===== MÉTRICAS BÁSICAS =====
        canvas.drawText("Métricas básicas", margen, y, boldPaint);
        y += 16;

        canvas.drawRect(margen, y - 12, 595 - margen, y + 4, headerBg);
        canvas.drawText("MAV EMG", margen + 4, y, headerPaint);
        canvas.drawText("WL EMG", margen + 120, y, headerPaint);
        canvas.drawText("OrderV EMG", margen + 236, y, headerPaint);
        canvas.drawText("MAV Dinamómetro", margen + 352, y, headerPaint);
        y += 16;

        rowBg.setColor(android.graphics.Color.rgb(232, 240, 254));
        canvas.drawRect(margen, y - 12, 595 - margen, y + 4, rowBg);
        if (df.metricas != null) {
            canvas.drawText(String.format(Locale.US, "%.4f", df.metricas[0]),
                    margen + 4, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.metricas[1]),
                    margen + 120, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.metricas[2]),
                    margen + 236, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.metricas[3]),
                    margen + 352, y, bodyPaint);
        }
        y += 24;

        // ===== ANÁLISIS MUSCULAR =====
        if (df.analisis != null) {
            canvas.drawLine(margen, y, 595 - margen, y, linePaint);
            y += 12;
            canvas.drawText("Análisis muscular", margen, y, boldPaint);
            y += 16;

            // Fila 1: EMG tiempo
            canvas.drawRect(margen, y - 12, 595 - margen, y + 4, headerBg);
            canvas.drawText("RMS", margen + 4, y, headerPaint);
            canvas.drawText("Varianza", margen + 90, y, headerPaint);
            canvas.drawText("ZC", margen + 190, y, headerPaint);
            canvas.drawText("SSC", margen + 260, y, headerPaint);
            canvas.drawText("Frec. Mediana", margen + 330, y, headerPaint);
            canvas.drawText("Frec. Media", margen + 440, y, headerPaint);
            y += 16;

            rowBg.setColor(android.graphics.Color.rgb(232, 240, 254));
            canvas.drawRect(margen, y - 12, 595 - margen, y + 4, rowBg);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.rms),
                    margen + 4, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.var),
                    margen + 90, y, bodyPaint);
            canvas.drawText(String.valueOf(df.analisis.zc),
                    margen + 190, y, bodyPaint);
            canvas.drawText(String.valueOf(df.analisis.ssc),
                    margen + 260, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.1f Hz",
                    df.analisis.frecuenciaMediana), margen + 330, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.1f Hz",
                    df.analisis.frecuenciaMedia), margen + 440, y, bodyPaint);
            y += 20;

            // Fila 2: Dinamómetro
            canvas.drawRect(margen, y - 12, 595 - margen, y + 4, headerBg);
            canvas.drawText("F. Máx", margen + 4, y, headerPaint);
            canvas.drawText("F. Mín", margen + 90, y, headerPaint);
            canvas.drawText("T. Pico (ms)", margen + 190, y, headerPaint);
            canvas.drawText("RFD (V/s)", margen + 300, y, headerPaint);
            canvas.drawText("Impulso", margen + 390, y, headerPaint);
            canvas.drawText("CV (%)", margen + 470, y, headerPaint);
            y += 16;

            rowBg.setColor(android.graphics.Color.WHITE);
            canvas.drawRect(margen, y - 12, 595 - margen, y + 4, rowBg);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.fuerzaMaxima),
                    margen + 4, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.fuerzaMinima),
                    margen + 90, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.0f", df.analisis.tiempoHastaPico),
                    margen + 190, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.2f", df.analisis.rfd),
                    margen + 300, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.impulso),
                    margen + 390, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.1f", df.analisis.coeficienteVariacion),
                    margen + 470, y, bodyPaint);
            y += 20;

            // Fila 3: Combinados y fatiga
            canvas.drawRect(margen, y - 12, 595 - margen, y + 4, headerBg);
            canvas.drawText("Eficiencia", margen + 4, y, headerPaint);
            canvas.drawText("Onset (ms)", margen + 120, y, headerPaint);
            canvas.drawText("Índice fatiga", margen + 240, y, headerPaint);
            canvas.drawText("Decaim. RMS", margen + 360, y, headerPaint);
            canvas.drawText("Ratio bandas", margen + 460, y, headerPaint);
            y += 16;

            rowBg.setColor(android.graphics.Color.rgb(232, 240, 254));
            canvas.drawRect(margen, y - 12, 595 - margen, y + 4, rowBg);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.eficienciaMusular),
                    margen + 4, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.0f", df.analisis.onsetMusular),
                    margen + 120, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.indiceFatiga),
                    margen + 240, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.tasaDecaimientoRMS),
                    margen + 360, y, bodyPaint);
            canvas.drawText(String.format(Locale.US, "%.4f", df.analisis.ratioBandas),
                    margen + 460, y, bodyPaint);
            y += 20;

            // Daniels
            canvas.drawLine(margen, y, 595 - margen, y, linePaint);
            y += 12;

            Paint danielsPaint = new Paint();
            danielsPaint.setTextSize(13f);
            danielsPaint.setFakeBoldText(true);
            danielsPaint.setColor(android.graphics.Color.rgb(66, 133, 244));

            canvas.drawText("Escala de Daniels estimada: "
                            + df.analisis.danielsEstimado + " / 5",
                    margen, y, danielsPaint);
            y += 20;
        }

        // ===== FFT =====
        canvas.drawLine(margen, y, 595 - margen, y, linePaint);
        y += 12;
        canvas.drawText("Espectro de frecuencias (FFT)", margen, y, boldPaint);
        y += 12;

        if (df.fftBitmap != null) {
            int anchoDisponible = 595 - margen * 2;
            int altoGrafica = Math.min(280, 842 - y - 30);

            android.graphics.Rect src = new android.graphics.Rect(
                    0, 0, df.fftBitmap.getWidth(), df.fftBitmap.getHeight());
            android.graphics.Rect dst = new android.graphics.Rect(
                    margen, y, margen + anchoDisponible, y + altoGrafica);

            canvas.drawBitmap(df.fftBitmap, src, dst, null);
            y += altoGrafica + 8;
            canvas.drawText("Frecuencia (Hz)", 595 / 2 - 40, y, grayPaint);
        } else {
            canvas.drawText("Datos insuficientes para FFT (mínimo 1024 muestras)",
                    margen, y, grayPaint);
        }
    }

// ===== GENERAR BITMAP FFT =====

    private static android.graphics.Bitmap generarBitmapFFT(Context context,
                                                            EMGFrequencyAnalyzer analyzer,
                                                            List<Float> emgFase) {
        double[] magnitudes = analyzer.computeMagnitudes(emgFase, 0);
        if (magnitudes == null) return null;

        // Crear chart offscreen
        com.github.mikephil.charting.charts.LineChart chart =
                new com.github.mikephil.charting.charts.LineChart(context);

        chart.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY));
        chart.layout(0, 0, 800, 400);

        ArrayList<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
        int maxFreqIndex = Math.min(magnitudes.length, 250);
        for (int i = 0; i < maxFreqIndex; i++) {
            entries.add(new com.github.mikephil.charting.data.Entry(
                    i * (1000f / 1024f), (float) magnitudes[i]));
        }

        com.github.mikephil.charting.data.LineDataSet dataSet =
                new com.github.mikephil.charting.data.LineDataSet(entries, "FFT");
        dataSet.setColor(android.graphics.Color.rgb(255, 87, 34));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.rgb(255, 87, 34));
        dataSet.setFillAlpha(80);

        chart.setData(new com.github.mikephil.charting.data.LineData(dataSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        com.github.mikephil.charting.components.XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(9f);

        com.github.mikephil.charting.components.YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setTextSize(9f);

        chart.invalidate();

        // Capturar como bitmap
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                800, 400, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        canvas.drawColor(android.graphics.Color.WHITE);
        chart.draw(canvas);

        return bitmap;
    }
    public static File guardarCSVLocal(Context context,
                                       String pruebaId,
                                       String ejecucionId,
                                       List<MuestraDato> muestras) throws IOException {

        String nombreArchivo = pruebaId + "_" + ejecucionId + ".csv";
        File archivo = new File(context.getFilesDir(), nombreArchivo);

        StringBuilder sb = new StringBuilder();

        // Encabezado completo para ML
        sb.append("prueba_id,prueba_nombre,ejecucion_id,fecha_ejecucion,")
                .append("paciente_id,paciente_nombre,paciente_edad,paciente_sexo,")
                .append("fase,timestamp_ms,")
                .append("emg,dinamometro,")
                .append("accX,accY,accZ,")
                .append("gyroX,gyroY,gyroZ,")
                .append("pitch,roll,yaw\n");

        for (MuestraDato m : muestras) {
            sb.append(m.pruebaId).append(",");
            sb.append(escaparCSV(m.pruebaNombre)).append(",");
            sb.append(m.ejecucionId).append(",");
            sb.append(m.timestampMs).append(",");
            sb.append(m.pacienteId).append(",");
            sb.append(escaparCSV(m.pacienteNombre)).append(",");
            sb.append(m.pacienteEdad).append(",");
            sb.append(escaparCSV(m.pacienteSexo)).append(",");
            sb.append(escaparCSV(m.fase)).append(",");
            sb.append(m.timestampMs).append(",");
            sb.append(formatearValor(m.emg)).append(",");
            sb.append(formatearValor(m.dinamometro)).append(",");
            sb.append(formatearValor(m.accX)).append(",");
            sb.append(formatearValor(m.accY)).append(",");
            sb.append(formatearValor(m.accZ)).append(",");
            sb.append(formatearValor(m.gyroX)).append(",");
            sb.append(formatearValor(m.gyroY)).append(",");
            sb.append(formatearValor(m.gyroZ)).append(",");
            sb.append(formatearValor(m.pitch)).append(",");
            sb.append(formatearValor(m.roll)).append(",");
            sb.append(formatearValor(m.yaw)).append("\n");
        }

        FileOutputStream fos = new FileOutputStream(archivo);
        fos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        fos.close();

        return archivo;
    }

    // Escapar campos con comas o comillas para CSV válido
    private static String escaparCSV(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
    private static String formatearValor(float valor) {
        if (Float.isNaN(valor)) return "";  // celda vacía = dato ausente
        return String.format(Locale.US, "%.4f", valor);
    }
    public static void subirCSVAStorage(Context context,
                                        String pruebaId,
                                        String ejecucionId,
                                        OnSubidaListener listener) {

        File archivoLocal = new File(context.getFilesDir(),
                pruebaId + "_" + ejecucionId + ".csv");

        if (!archivoLocal.exists()) {
            listener.onError(new Exception("CSV local no encontrado"));
            return;
        }

        // Comprimir con GZIP antes de subir
        File archivoGzip = new File(context.getCacheDir(),
                pruebaId + "_" + ejecucionId + ".csv.gz");

        try {
            comprimirGzip(archivoLocal, archivoGzip);
        } catch (IOException e) {
            listener.onError(e);
            return;
        }

        // Subir a Firebase Storage
        com.google.firebase.storage.FirebaseStorage storage =
                com.google.firebase.storage.FirebaseStorage.getInstance();

        com.google.firebase.storage.StorageReference ref = storage.getReference()
                .child("pruebas")
                .child(pruebaId)
                .child(ejecucionId + ".csv.gz");

        Uri archivoUri = Uri.fromFile(archivoGzip);

        ref.putFile(archivoUri)
                .addOnSuccessListener(snapshot -> {
                    // Eliminar archivo temporal
                    archivoGzip.delete();
                    listener.onExito();
                    android.util.Log.d("STORAGE",
                            "CSV subido: pruebas/" + pruebaId + "/" + ejecucionId + ".csv.gz");
                })
                .addOnFailureListener(e -> {
                    archivoGzip.delete();
                    listener.onError(e);
                })
                .addOnProgressListener(snapshot -> {
                    double progreso = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    listener.onProgreso((int) progreso);
                });
    }

    // Descargar CSV de Storage y guardarlo localmente
    public static void descargarCSVDeStorage(Context context,
                                             String pruebaId,
                                             String ejecucionId,
                                             OnDescargaListener listener) {

        File archivoLocal = new File(context.getFilesDir(),
                pruebaId + "_" + ejecucionId + ".csv");

        // Si ya existe localmente no descargar
        if (archivoLocal.exists()) {
            listener.onExito(archivoLocal);
            return;
        }

        File archivoGzip = new File(context.getCacheDir(),
                pruebaId + "_" + ejecucionId + ".csv.gz");

        com.google.firebase.storage.FirebaseStorage storage =
                com.google.firebase.storage.FirebaseStorage.getInstance();

        com.google.firebase.storage.StorageReference ref = storage.getReference()
                .child("pruebas")
                .child(pruebaId)
                .child(ejecucionId + ".csv.gz");

        ref.getFile(archivoGzip)
                .addOnSuccessListener(snapshot -> {
                    try {
                        descomprimirGzip(archivoGzip, archivoLocal);
                        archivoGzip.delete();
                        listener.onExito(archivoLocal);
                    } catch (IOException e) {
                        listener.onError(e);
                    }
                })
                .addOnFailureListener(listener::onError)
                .addOnProgressListener(snapshot -> {
                    double progreso = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    listener.onProgreso((int) progreso);
                });
    }

// ===== GZIP =====

    private static void comprimirGzip(File entrada, File salida) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(entrada);
             java.io.FileOutputStream fos = new java.io.FileOutputStream(salida);
             java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
        }
    }

    private static void descomprimirGzip(File entrada, File salida) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(entrada);
             java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(fis);
             java.io.FileOutputStream fos = new java.io.FileOutputStream(salida)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

// ===== LISTENERS =====

    public interface OnSubidaListener {
        void onExito();
        void onError(Exception e);
        void onProgreso(int porcentaje);
    }

    public interface OnDescargaListener {
        void onExito(File archivo);
        void onError(Exception e);
        void onProgreso(int porcentaje);
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