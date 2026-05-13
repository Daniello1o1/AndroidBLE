package com.upiiz.ble_sipi.Repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.upiiz.ble_sipi.Models.AntecedenteFamiliar;
import com.upiiz.ble_sipi.Models.AntecedentePersonal;
import com.upiiz.ble_sipi.Models.LesionPrevia;
import com.upiiz.ble_sipi.Models.Medicamento;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.Models.PerfilClinico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacienteRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COL_PACIENTES            = "pacientes";
    private static final String COL_PERFIL               = "perfilClinico";
    private static final String COL_ANT_PERSONAL         = "antecedentesPersonales";
    private static final String COL_ANT_FAMILIAR         = "antecedentesFamiliares";
    private static final String COL_MEDICAMENTOS         = "medicamentos";
    private static final String COL_LESIONES             = "lesionesPrevias";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // ================= PACIENTE =================

    public void crearPaciente(Paciente p, Callback<String> callback) {
        Map<String, Object> data = pacienteToMap(p);
        data.put("creadoEn", System.currentTimeMillis());

        db.collection(COL_PACIENTES)
                .add(data)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarPaciente(Paciente p, Callback<Void> callback) {
        db.collection(COL_PACIENTES)
                .document(p.id)
                .update(pacienteToMap(p))
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void borrarPaciente(String pacienteId, Callback<Void> callback) {
        db.collection(COL_PACIENTES)
                .document(pacienteId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerPacientes(Callback<List<Paciente>> callback) {
        db.collection(COL_PACIENTES)
                .orderBy("creadoEn", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Paciente> lista = new ArrayList<>();
                    snapshot.forEach(doc -> lista.add(mapToPaciente(doc.getId(), doc.getData())));
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void obtenerPaciente(String pacienteId, Callback<Paciente> callback) {
        db.collection(COL_PACIENTES)
                .document(pacienteId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onSuccess(mapToPaciente(doc.getId(), doc.getData()));
                    else callback.onError(new Exception("Paciente no encontrado"));
                })
                .addOnFailureListener(callback::onError);
    }

    // ================= PERFIL CLÍNICO =================

    public void guardarPerfil(String pacienteId, PerfilClinico p, Callback<Void> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_PERFIL).document("perfil")
                .set(perfilToMap(p))
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerPerfil(String pacienteId, Callback<PerfilClinico> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_PERFIL).document("perfil")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onSuccess(mapToPerfil(doc.getId(), doc.getData()));
                    else callback.onSuccess(new PerfilClinico());
                })
                .addOnFailureListener(callback::onError);
    }

    // ================= ANTECEDENTES PERSONALES =================

    public void agregarAntecedentePersonal(String pacienteId,
                                           AntecedentePersonal a,
                                           Callback<String> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_ANT_PERSONAL)
                .add(antecedentePersonalToMap(a))
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerAntecedentesPersonales(String pacienteId,
                                              Callback<List<AntecedentePersonal>> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_ANT_PERSONAL)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AntecedentePersonal> lista = new ArrayList<>();
                    snapshot.forEach(doc -> lista.add(
                            mapToAntecedentePersonal(doc.getId(), doc.getData())));
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void borrarAntecedentePersonal(String pacienteId,
                                          String antecedId,
                                          Callback<Void> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_ANT_PERSONAL).document(antecedId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ================= ANTECEDENTES FAMILIARES =================

    public void agregarAntecedenteFamiliar(String pacienteId,
                                           AntecedenteFamiliar a,
                                           Callback<String> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_ANT_FAMILIAR)
                .add(antecedenteFamiliarToMap(a))
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerAntecedentesFamiliares(String pacienteId,
                                              Callback<List<AntecedenteFamiliar>> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_ANT_FAMILIAR)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AntecedenteFamiliar> lista = new ArrayList<>();
                    snapshot.forEach(doc -> lista.add(
                            mapToAntecedenteFamiliar(doc.getId(), doc.getData())));
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void borrarAntecedenteFamiliar(String pacienteId,
                                          String antecedId,
                                          Callback<Void> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_ANT_FAMILIAR).document(antecedId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ================= MEDICAMENTOS =================

    public void agregarMedicamento(String pacienteId,
                                   Medicamento m,
                                   Callback<String> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_MEDICAMENTOS)
                .add(medicamentoToMap(m))
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerMedicamentos(String pacienteId,
                                    Callback<List<Medicamento>> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_MEDICAMENTOS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Medicamento> lista = new ArrayList<>();
                    snapshot.forEach(doc -> lista.add(
                            mapToMedicamento(doc.getId(), doc.getData())));
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void borrarMedicamento(String pacienteId,
                                  String medicId,
                                  Callback<Void> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_MEDICAMENTOS).document(medicId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ================= LESIONES PREVIAS =================

    public void agregarLesion(String pacienteId,
                              LesionPrevia l,
                              Callback<String> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_LESIONES)
                .add(lesionToMap(l))
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void obtenerLesiones(String pacienteId,
                                Callback<List<LesionPrevia>> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_LESIONES)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<LesionPrevia> lista = new ArrayList<>();
                    snapshot.forEach(doc -> lista.add(
                            mapToLesion(doc.getId(), doc.getData())));
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void borrarLesion(String pacienteId,
                             String lesionId,
                             Callback<Void> callback) {
        db.collection(COL_PACIENTES).document(pacienteId)
                .collection(COL_LESIONES).document(lesionId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ================= MAPEO =================

    private Map<String, Object> pacienteToMap(Paciente p) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre",          p.nombre);
        map.put("apellidos",       p.apellidos);
        map.put("fechaNacimiento", p.fechaNacimiento);
        map.put("edad",            p.edad);
        map.put("sexo",            p.sexo);
        map.put("peso",            p.peso);
        map.put("talla",           p.talla);
        map.put("observaciones",   p.observaciones);
        return map;
    }

    private Paciente mapToPaciente(String id, Map<String, Object> map) {
        Paciente p = new Paciente();
        p.id              = id;
        p.nombre          = (String) map.get("nombre");
        p.apellidos       = (String) map.get("apellidos");
        p.fechaNacimiento = (String) map.get("fechaNacimiento");
        p.edad            = toInt(map.get("edad"));
        p.sexo            = (String) map.get("sexo");
        p.peso            = toFloat(map.get("peso"));
        p.talla           = toFloat(map.get("talla"));
        p.observaciones   = (String) map.get("observaciones");
        p.creadoEn        = toLong(map.get("creadoEn"));
        return p;
    }

    private Map<String, Object> perfilToMap(PerfilClinico p) {
        Map<String, Object> map = new HashMap<>();
        map.put("fuma",                          p.fuma);
        map.put("tomaAlcohol",                   p.tomaAlcohol);
        map.put("consumeDrogas",                 p.consumeDrogas);
        map.put("tieneAntecedentesEnfermedades", p.tieneAntecedentesEnfermedades);
        map.put("sarcopenia",                    p.sarcopenia);
        map.put("debilidadMuscularCronica",      p.debilidadMuscularCronica);
        map.put("deficitVitaminaB12",            p.deficitVitaminaB12);
        map.put("deficitVitaminaD",              p.deficitVitaminaD);
        map.put("participaRehabilitacion",       p.participaRehabilitacion);
        map.put("alimentacionSaludable",         p.alimentacionSaludable);
        map.put("tipoEjercicio",                 p.tipoEjercicio);
        map.put("disartria",                     p.disartria);
        map.put("tipDisartria",                  p.tipDisartria);
        map.put("observaciones",                 p.observaciones);
        return map;
    }

    private PerfilClinico mapToPerfil(String id, Map<String, Object> map) {
        PerfilClinico p = new PerfilClinico();
        p.id                             = id;
        p.fuma                           = toBool(map.get("fuma"));
        p.tomaAlcohol                    = toBool(map.get("tomaAlcohol"));
        p.consumeDrogas                  = toBool(map.get("consumeDrogas"));
        p.tieneAntecedentesEnfermedades  = toBool(map.get("tieneAntecedentesEnfermedades"));
        p.sarcopenia                     = toBool(map.get("sarcopenia"));
        p.debilidadMuscularCronica       = toBool(map.get("debilidadMuscularCronica"));
        p.deficitVitaminaB12             = toBool(map.get("deficitVitaminaB12"));
        p.deficitVitaminaD               = toBool(map.get("deficitVitaminaD"));
        p.participaRehabilitacion        = toBool(map.get("participaRehabilitacion"));
        p.alimentacionSaludable          = toBool(map.get("alimentacionSaludable"));
        p.tipoEjercicio                  = (String) map.get("tipoEjercicio");
        p.disartria                      = toBool(map.get("disartria"));
        p.tipDisartria                   = (String) map.get("tipDisartria");
        p.observaciones                  = (String) map.get("observaciones");
        return p;
    }

    private Map<String, Object> antecedentePersonalToMap(AntecedentePersonal a) {
        Map<String, Object> map = new HashMap<>();
        map.put("enfermedad",      a.enfermedad);
        map.put("descripcion",     a.descripcion);
        map.put("diagnosticadoPor", a.diagnosticadoPor);
        map.put("fechaDiagnostico", a.fechaDiagnostico);
        return map;
    }

    private AntecedentePersonal mapToAntecedentePersonal(String id, Map<String, Object> map) {
        AntecedentePersonal a = new AntecedentePersonal();
        a.id               = id;
        a.enfermedad       = (String) map.get("enfermedad");
        a.descripcion      = (String) map.get("descripcion");
        a.diagnosticadoPor = (String) map.get("diagnosticadoPor");
        a.fechaDiagnostico = (String) map.get("fechaDiagnostico");
        return a;
    }

    private Map<String, Object> antecedenteFamiliarToMap(AntecedenteFamiliar a) {
        Map<String, Object> map = new HashMap<>();
        map.put("parentesco",    a.parentesco);
        map.put("enfermedad",    a.enfermedad);
        map.put("descripcion",   a.descripcion);
        map.put("siguePresente", a.siguePresente);
        return map;
    }

    private AntecedenteFamiliar mapToAntecedenteFamiliar(String id, Map<String, Object> map) {
        AntecedenteFamiliar a = new AntecedenteFamiliar();
        a.id           = id;
        a.parentesco   = (String) map.get("parentesco");
        a.enfermedad   = (String) map.get("enfermedad");
        a.descripcion  = (String) map.get("descripcion");
        a.siguePresente = toBool(map.get("siguePresente"));
        return a;
    }

    private Map<String, Object> medicamentoToMap(Medicamento m) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre",          m.nombre);
        map.put("tipoTratamiento", m.tipoTratamiento);
        map.put("dosis",           m.dosis);
        map.put("frecuencia",      m.frecuencia);
        map.put("motivo",          m.motivo);
        map.put("fechaInicio",     m.fechaInicio);
        map.put("fechaFin",        m.fechaFin);
        map.put("observaciones",   m.observaciones);
        return map;
    }

    private Medicamento mapToMedicamento(String id, Map<String, Object> map) {
        Medicamento m = new Medicamento();
        m.id              = id;
        m.nombre          = (String) map.get("nombre");
        m.tipoTratamiento = (String) map.get("tipoTratamiento");
        m.dosis           = (String) map.get("dosis");
        m.frecuencia      = (String) map.get("frecuencia");
        m.motivo          = (String) map.get("motivo");
        m.fechaInicio     = (String) map.get("fechaInicio");
        m.fechaFin        = (String) map.get("fechaFin");
        m.observaciones   = (String) map.get("observaciones");
        return m;
    }

    private Map<String, Object> lesionToMap(LesionPrevia l) {
        Map<String, Object> map = new HashMap<>();
        map.put("tipoLesion",   l.tipoLesion);
        map.put("zonaAfectada", l.zonaAfectada);
        map.put("fechaLesion",  l.fechaLesion);
        map.put("secuela",      l.secuela);
        map.put("descripcion",  l.descripcion);
        return map;
    }

    private LesionPrevia mapToLesion(String id, Map<String, Object> map) {
        LesionPrevia l = new LesionPrevia();
        l.id           = id;
        l.tipoLesion   = (String) map.get("tipoLesion");
        l.zonaAfectada = (String) map.get("zonaAfectada");
        l.fechaLesion  = (String) map.get("fechaLesion");
        l.secuela      = toBool(map.get("secuela"));
        l.descripcion  = (String) map.get("descripcion");
        return l;
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

    private long toLong(Object o) {
        if (o instanceof Long)    return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        return 0L;
    }

    private boolean toBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        return false;
    }
}