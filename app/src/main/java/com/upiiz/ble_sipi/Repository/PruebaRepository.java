package com.upiiz.ble_sipi.Repository;

import static android.icu.util.UniversalTimeScale.toLong;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.FasePrueba;
import com.upiiz.ble_sipi.Models.Prueba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PruebaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COL_PRUEBAS    = "pruebas";
    private static final String COL_EJECUCIONES = "ejecuciones";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // ================= PRUEBAS =================

    public void crearPrueba(Prueba prueba, Callback<String> callback) {
        Map<String, Object> data = pruebaToMap(prueba);
        data.put("creadoEn", Timestamp.now());

        db.collection(COL_PRUEBAS)
                .add(data)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarPrueba(Prueba prueba, Callback<Void> callback) {
        db.collection(COL_PRUEBAS)
                .document(prueba.id)
                .update(pruebaToMap(prueba))
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void borrarPrueba(String pruebaId, Callback<Void> callback) {
        db.collection(COL_PRUEBAS)
                .document(pruebaId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerPruebas(Callback<List<Prueba>> callback) {
        db.collection(COL_PRUEBAS)
                .orderBy("creadoEn", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Prueba> lista = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Prueba p = mapToPrueba(doc.getId(), doc.getData());
                        lista.add(p);
                    });
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    // ================= EJECUCIONES =================

    public void guardarEjecucion(String pruebaId,
                                 Ejecucion ejecucion,
                                 Callback<String> callback) {
        Map<String, Object> data = ejecucionToMap(ejecucion);
        data.put("fechaEjecucion", Timestamp.now());
        data.put("pruebaId", pruebaId);

        db.collection(COL_PRUEBAS)
                .document(pruebaId)
                .collection(COL_EJECUCIONES)
                .add(data)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerEjecuciones(String pruebaId, Callback<List<Ejecucion>> callback) {
        db.collection(COL_PRUEBAS)
                .document(pruebaId)
                .collection(COL_EJECUCIONES)
                .orderBy("fechaEjecucion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Ejecucion> lista = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Ejecucion e = mapToEjecucion(doc.getId(), doc.getData());
                        lista.add(e);
                    });
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    // ================= MAPEO =================

    private Map<String, Object> pruebaToMap(Prueba p) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre",                 p.nombre);
        map.put("duracionTotalSegundos",  p.duracionTotalSegundos);
        map.put("tieneIntervalos",        p.tieneIntervalos);
        map.put("usarEMG",                p.usarEMG);
        map.put("usarDinamometro",        p.usarDinamometro);
        map.put("usarAcelerometro",       p.usarAcelerometro);
        map.put("usarGiroscopio",         p.usarGiroscopio);
        map.put("usarOrientacion",        p.usarOrientacion);

        List<Map<String, Object>> fases = new ArrayList<>();
        for (FasePrueba f : p.fases) {
            Map<String, Object> fase = new HashMap<>();
            fase.put("nombre",            f.nombre);
            fase.put("duracionSegundos",  f.duracionSegundos);
            fases.add(fase);
        }
        map.put("fases", fases);
        return map;
    }

    private Prueba mapToPrueba(String id, Map<String, Object> map) {
        Prueba p = new Prueba();
        p.id                     = id;
        p.nombre                 = (String)  map.get("nombre");
        p.duracionTotalSegundos  = toInt(map.get("duracionTotalSegundos"));
        p.tieneIntervalos        = toBool(map.get("tieneIntervalos"));
        p.usarEMG                = toBool(map.get("usarEMG"));
        p.usarDinamometro        = toBool(map.get("usarDinamometro"));
        p.usarAcelerometro       = toBool(map.get("usarAcelerometro"));
        p.usarGiroscopio         = toBool(map.get("usarGiroscopio"));
        p.usarOrientacion        = toBool(map.get("usarOrientacion"));
        p.creadoEn = toLong(map.get("creadoEn"));

        p.fases = new ArrayList<>();
        Object fasesObj = map.get("fases");
        if (fasesObj instanceof List) {
            for (Object item : (List<?>) fasesObj) {
                if (item instanceof Map) {
                    Map<?, ?> faseMap = (Map<?, ?>) item;
                    FasePrueba f = new FasePrueba(
                            (String) faseMap.get("nombre"),
                            toInt(faseMap.get("duracionSegundos"))
                    );
                    p.fases.add(f);
                }
            }
        }
        return p;
    }

    private Map<String, Object> ejecucionToMap(Ejecucion e) {
        Map<String, Object> map = new HashMap<>();
        map.put("duracionReal",    e.duracionReal);
        map.put("pacienteId", e.pacienteId);
        map.put("totalMuestras",   e.totalMuestras);
        map.put("emgMAVTotal",     e.emgMAVTotal);
        map.put("emgWLTotal",      e.emgWLTotal);
        map.put("emgOrderVTotal",  e.emgOrderVTotal);
        map.put("dynMAVTotal",     e.dynMAVTotal);
        map.put("metricasPorFase", e.metricasPorFase);
        map.put("fechaEjecucion", System.currentTimeMillis());
        return map;
    }
    private long toLong(Object o) {
        if (o instanceof Long)    return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        // Por si hay documentos viejos con Timestamp
        if (o instanceof com.google.firebase.Timestamp)
            return ((com.google.firebase.Timestamp) o).toDate().getTime();
        return 0L;
    }

    @SuppressWarnings("unchecked")
    private Ejecucion mapToEjecucion(String id, Map<String, Object> map) {
        Ejecucion e = new Ejecucion();
        e.id              = id;
        e.pacienteId = (String) map.get("pacienteId");
        e.pruebaId        = (String)    map.get("pruebaId");
        e.fechaEjecucion = toLong(map.get("fechaEjecucion"));
        e.duracionReal    = toInt(map.get("duracionReal"));
        e.totalMuestras   = toInt(map.get("totalMuestras"));
        e.emgMAVTotal     = toFloat(map.get("emgMAVTotal"));
        e.emgWLTotal      = toFloat(map.get("emgWLTotal"));
        e.emgOrderVTotal  = toFloat(map.get("emgOrderVTotal"));
        e.dynMAVTotal     = toFloat(map.get("dynMAVTotal"));
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> metricasRaw =
                (Map<String, Map<String, Object>>) map.get("metricasPorFase");

        if (metricasRaw != null) {
            e.metricasPorFase = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> fase : metricasRaw.entrySet()) {
                Map<String, Float> convertida = new HashMap<>();
                for (Map.Entry<String, Object> metrica : fase.getValue().entrySet()) {
                    Object v = metrica.getValue();
                    float f = (v instanceof Double) ? ((Double) v).floatValue()
                            : (v instanceof Float)  ? (Float) v : 0f;
                    convertida.put(metrica.getKey(), f);
                }
                e.metricasPorFase.put(fase.getKey(), convertida);
            }
        }
        return e;
    }
    public void obtenerEjecucionesPorPaciente(String pacienteId,
                                              Callback<List<Ejecucion>> callback) {
        db.collectionGroup("ejecuciones")
                .whereEqualTo("pacienteId", pacienteId)
                .orderBy("fechaEjecucion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Ejecucion> lista = new ArrayList<>();
                    snapshot.forEach(doc ->
                            lista.add(mapToEjecucion(doc.getId(), doc.getData())));
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    // ================= HELPERS =================

    private int toInt(Object o) {
        if (o instanceof Long)    return ((Long) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        return 0;
    }

    private float toFloat(Object o) {
        if (o instanceof Double) return ((Double) o).floatValue();
        if (o instanceof Float)  return (Float) o;
        return 0f;
    }

    private boolean toBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        return false;
    }
}