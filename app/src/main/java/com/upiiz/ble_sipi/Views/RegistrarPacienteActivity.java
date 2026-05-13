package com.upiiz.ble_sipi.Views;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.upiiz.ble_sipi.Models.AntecedenteFamiliar;
import com.upiiz.ble_sipi.Models.AntecedentePersonal;
import com.upiiz.ble_sipi.Models.LesionPrevia;
import com.upiiz.ble_sipi.Models.Medicamento;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.Models.PerfilClinico;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PacienteRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RegistrarPacienteActivity extends AppCompatActivity {

    private int pasoActual = 1;
    private static final int TOTAL_PASOS = 4;

    private PacienteRepository repository;

    // Vistas de navegación
    private TextView tvTituloPaso, tvSubtituloPaso;
    private View paso1Indicator, paso2Indicator, paso3Indicator, paso4Indicator;
    private View layoutPaso1, layoutPaso2, layoutPaso3, layoutPaso4;
    private MaterialButton btnAnterior, btnSiguiente;

    // ===== PASO 1 =====
    private TextInputEditText etNombre, etApellidos, etFechaNacimiento;
    private TextInputEditText etEdad, etPeso, etTalla, etObservaciones;
    private RadioGroup rgSexo;

    // ===== PASO 2 =====
    private CheckBox cbFuma, cbAlcohol, cbDrogas, cbAlimentacionSaludable;
    private CheckBox cbAntecedentes, cbSarcopenia, cbDebilidadMuscular;
    private CheckBox cbDeficitB12, cbDeficitD, cbRehabilitacion, cbDisartria;
    private TextInputEditText etTipoEjercicio, etTipDisartria, etObservacionesClinicas;
    private TextInputLayout layoutTipDisartria;

    // ===== PASO 3 =====
    private LinearLayout containerAntPersonales, containerAntFamiliares;

    // ===== PASO 4 =====
    private LinearLayout containerMedicamentos, containerLesiones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_paciente);

        repository = new PacienteRepository();

        bindViews();
        configurarPaso2Listeners();
        actualizarVistaPaso();

        btnAnterior.setOnClickListener(v -> navegarAnterior());
        btnSiguiente.setOnClickListener(v -> navegarSiguiente());
    }

    // ================= BIND =================

    private void bindViews() {
        tvTituloPaso    = findViewById(R.id.tvTituloPaso);
        tvSubtituloPaso = findViewById(R.id.tvSubtituloPaso);
        paso1Indicator  = findViewById(R.id.paso1Indicator);
        paso2Indicator  = findViewById(R.id.paso2Indicator);
        paso3Indicator  = findViewById(R.id.paso3Indicator);
        paso4Indicator  = findViewById(R.id.paso4Indicator);
        layoutPaso1     = findViewById(R.id.layoutPaso1);
        layoutPaso2     = findViewById(R.id.layoutPaso2);
        layoutPaso3     = findViewById(R.id.layoutPaso3);
        layoutPaso4     = findViewById(R.id.layoutPaso4);
        btnAnterior     = findViewById(R.id.btnAnterior);
        btnSiguiente    = findViewById(R.id.btnSiguiente);

        // Paso 1
        etNombre           = layoutPaso1.findViewById(R.id.etNombre);
        etApellidos        = layoutPaso1.findViewById(R.id.etApellidos);
        etFechaNacimiento  = layoutPaso1.findViewById(R.id.etFechaNacimiento);
        etEdad             = layoutPaso1.findViewById(R.id.etEdad);
        etPeso             = layoutPaso1.findViewById(R.id.etPeso);
        etTalla            = layoutPaso1.findViewById(R.id.etTalla);
        etObservaciones    = layoutPaso1.findViewById(R.id.etObservaciones);
        rgSexo             = layoutPaso1.findViewById(R.id.rgSexo);

        etFechaNacimiento.setOnClickListener(v -> mostrarDatePicker(etFechaNacimiento));

        // Paso 2
        cbFuma                  = layoutPaso2.findViewById(R.id.cbFuma);
        cbAlcohol               = layoutPaso2.findViewById(R.id.cbAlcohol);
        cbDrogas                = layoutPaso2.findViewById(R.id.cbDrogas);
        cbAlimentacionSaludable = layoutPaso2.findViewById(R.id.cbAlimentacionSaludable);
        cbAntecedentes          = layoutPaso2.findViewById(R.id.cbAntecedentes);
        cbSarcopenia            = layoutPaso2.findViewById(R.id.cbSarcopenia);
        cbDebilidadMuscular     = layoutPaso2.findViewById(R.id.cbDebilidadMuscular);
        cbDeficitB12            = layoutPaso2.findViewById(R.id.cbDeficitB12);
        cbDeficitD              = layoutPaso2.findViewById(R.id.cbDeficitD);
        cbRehabilitacion        = layoutPaso2.findViewById(R.id.cbRehabilitacion);
        cbDisartria             = layoutPaso2.findViewById(R.id.cbDisartria);
        etTipoEjercicio         = layoutPaso2.findViewById(R.id.etTipoEjercicio);
        etTipDisartria          = layoutPaso2.findViewById(R.id.etTipDisartria);
        etObservacionesClinicas = layoutPaso2.findViewById(R.id.etObservacionesClinicas);
        layoutTipDisartria      = layoutPaso2.findViewById(R.id.layoutTipDisartria);

        // Paso 3
        containerAntPersonales = layoutPaso3.findViewById(R.id.containerAntPersonales);
        containerAntFamiliares = layoutPaso3.findViewById(R.id.containerAntFamiliares);

        layoutPaso3.findViewById(R.id.btnAgregarAntPersonal)
                .setOnClickListener(v -> agregarFilaAntPersonal());
        layoutPaso3.findViewById(R.id.btnAgregarAntFamiliar)
                .setOnClickListener(v -> agregarFilaAntFamiliar());

        // Paso 4
        containerMedicamentos = layoutPaso4.findViewById(R.id.containerMedicamentos);
        containerLesiones     = layoutPaso4.findViewById(R.id.containerLesiones);

        layoutPaso4.findViewById(R.id.btnAgregarMedicamento)
                .setOnClickListener(v -> agregarFilaMedicamento());
        layoutPaso4.findViewById(R.id.btnAgregarLesion)
                .setOnClickListener(v -> agregarFilaLesion());
    }

    private void configurarPaso2Listeners() {
        cbDisartria.setOnCheckedChangeListener((btn, checked) ->
                layoutTipDisartria.setVisibility(checked ? View.VISIBLE : View.GONE));
    }

    // ================= NAVEGACIÓN =================

    private void navegarSiguiente() {
        if (!validarPasoActual()) return;
        if (pasoActual < TOTAL_PASOS) {
            pasoActual++;
            actualizarVistaPaso();
        } else {
            guardarTodo();
        }
    }

    private void navegarAnterior() {
        if (pasoActual > 1) {
            pasoActual--;
            actualizarVistaPaso();
        }
    }

    private void actualizarVistaPaso() {
        // Mostrar/ocultar pasos
        layoutPaso1.setVisibility(pasoActual == 1 ? View.VISIBLE : View.GONE);
        layoutPaso2.setVisibility(pasoActual == 2 ? View.VISIBLE : View.GONE);
        layoutPaso3.setVisibility(pasoActual == 3 ? View.VISIBLE : View.GONE);
        layoutPaso4.setVisibility(pasoActual == 4 ? View.VISIBLE : View.GONE);

        // Botón anterior
        btnAnterior.setVisibility(pasoActual > 1 ? View.VISIBLE : View.GONE);

        // Botón siguiente
        btnSiguiente.setText(pasoActual == TOTAL_PASOS ? "Guardar paciente" : "Siguiente");

        // Títulos
        String[] titulos = {
                "Datos generales",
                "Perfil clínico",
                "Antecedentes",
                "Medicamentos y lesiones"
        };
        tvTituloPaso.setText(titulos[pasoActual - 1]);
        tvSubtituloPaso.setText("Paso " + pasoActual + " de " + TOTAL_PASOS);

        // Indicadores
        int activo   = 0xFF4CAF50;
        int inactivo = 0xFFE0E0E0;
        paso1Indicator.setBackgroundColor(pasoActual >= 1 ? activo : inactivo);
        paso2Indicator.setBackgroundColor(pasoActual >= 2 ? activo : inactivo);
        paso3Indicator.setBackgroundColor(pasoActual >= 3 ? activo : inactivo);
        paso4Indicator.setBackgroundColor(pasoActual >= 4 ? activo : inactivo);
    }

    // ================= VALIDACIÓN =================

    private boolean validarPasoActual() {
        if (pasoActual == 1) {
            if (getText(etNombre).isEmpty()) {
                etNombre.setError("Campo requerido");
                return false;
            }
            if (getText(etApellidos).isEmpty()) {
                etApellidos.setError("Campo requerido");
                return false;
            }
        }
        return true;
    }

    // ================= FILAS DINÁMICAS =================

    private void agregarFilaAntPersonal() {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_antecedente_personal, containerAntPersonales, false);

        TextInputEditText etFecha = fila.findViewById(R.id.etFechaDiagnostico);
        etFecha.setOnClickListener(v -> mostrarDatePicker(etFecha));

        fila.findViewById(R.id.btnEliminar)
                .setOnClickListener(v -> containerAntPersonales.removeView(fila));

        containerAntPersonales.addView(fila);
    }

    private void agregarFilaAntFamiliar() {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_antecedente_familiar, containerAntFamiliares, false);

        fila.findViewById(R.id.btnEliminar)
                .setOnClickListener(v -> containerAntFamiliares.removeView(fila));

        containerAntFamiliares.addView(fila);
    }

    private void agregarFilaMedicamento() {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_medicamento, containerMedicamentos, false);

        TextInputEditText etInicio = fila.findViewById(R.id.etFechaInicio);
        TextInputEditText etFin    = fila.findViewById(R.id.etFechaFin);
        etInicio.setOnClickListener(v -> mostrarDatePicker(etInicio));
        etFin.setOnClickListener(v -> mostrarDatePicker(etFin));

        fila.findViewById(R.id.btnEliminar)
                .setOnClickListener(v -> containerMedicamentos.removeView(fila));

        containerMedicamentos.addView(fila);
    }

    private void agregarFilaLesion() {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_lesion, containerLesiones, false);

        TextInputEditText etFecha = fila.findViewById(R.id.etFechaLesion);
        etFecha.setOnClickListener(v -> mostrarDatePicker(etFecha));

        fila.findViewById(R.id.btnEliminar)
                .setOnClickListener(v -> containerLesiones.removeView(fila));

        containerLesiones.addView(fila);
    }

    // ================= DATE PICKER =================

    private void mostrarDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            target.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ================= GUARDAR TODO =================

    private void guardarTodo() {
        btnSiguiente.setEnabled(false);
        btnSiguiente.setText("Guardando...");

        Paciente paciente = leerPaso1();
        PerfilClinico perfil = leerPaso2();
        List<AntecedentePersonal> antPersonales = leerAntPersonales();
        List<AntecedenteFamiliar> antFamiliares = leerAntFamiliares();
        List<Medicamento> medicamentos = leerMedicamentos();
        List<LesionPrevia> lesiones = leerLesiones();

        // 1. Crear paciente
        repository.crearPaciente(paciente, new PacienteRepository.Callback<String>() {
            @Override
            public void onSuccess(String pacienteId) {
                paciente.id = pacienteId;

                // 2. Guardar perfil clínico
                repository.guardarPerfil(pacienteId, perfil,
                        new PacienteRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void r) {
                                // 3. Guardar antecedentes personales
                                guardarLista(pacienteId, antPersonales,
                                        antFamiliares, medicamentos, lesiones);
                            }
                            @Override
                            public void onError(Exception e) { mostrarError(); }
                        });
            }
            @Override
            public void onError(Exception e) { mostrarError(); }
        });
    }

    private void guardarLista(String pacienteId,
                              List<AntecedentePersonal> antPersonales,
                              List<AntecedenteFamiliar> antFamiliares,
                              List<Medicamento> medicamentos,
                              List<LesionPrevia> lesiones) {

        // Contador para saber cuándo terminaron todos los guardados
        int[] pendientes = {antPersonales.size() + antFamiliares.size()
                + medicamentos.size() + lesiones.size()};

        if (pendientes[0] == 0) {
            finalizarRegistro();
            return;
        }

        Runnable checkDone = () -> {
            pendientes[0]--;
            if (pendientes[0] == 0) finalizarRegistro();
        };

        for (AntecedentePersonal a : antPersonales) {
            repository.agregarAntecedentePersonal(pacienteId, a,
                    new PacienteRepository.Callback<String>() {
                        @Override public void onSuccess(String id) { checkDone.run(); }
                        @Override public void onError(Exception e) { checkDone.run(); }
                    });
        }

        for (AntecedenteFamiliar a : antFamiliares) {
            repository.agregarAntecedenteFamiliar(pacienteId, a,
                    new PacienteRepository.Callback<String>() {
                        @Override public void onSuccess(String id) { checkDone.run(); }
                        @Override public void onError(Exception e) { checkDone.run(); }
                    });
        }

        for (Medicamento m : medicamentos) {
            repository.agregarMedicamento(pacienteId, m,
                    new PacienteRepository.Callback<String>() {
                        @Override public void onSuccess(String id) { checkDone.run(); }
                        @Override public void onError(Exception e) { checkDone.run(); }
                    });
        }

        for (LesionPrevia l : lesiones) {
            repository.agregarLesion(pacienteId, l,
                    new PacienteRepository.Callback<String>() {
                        @Override public void onSuccess(String id) { checkDone.run(); }
                        @Override public void onError(Exception e) { checkDone.run(); }
                    });
        }
    }

    private void finalizarRegistro() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Paciente registrado", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void mostrarError() {
        runOnUiThread(() -> {
            btnSiguiente.setEnabled(true);
            btnSiguiente.setText("Guardar paciente");
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        });
    }

    // ================= LEER DATOS =================

    private Paciente leerPaso1() {
        Paciente p = new Paciente();
        p.nombre          = getText(etNombre);
        p.apellidos       = getText(etApellidos);
        p.fechaNacimiento = getText(etFechaNacimiento);
        p.observaciones   = getText(etObservaciones);

        String edadStr = getText(etEdad);
        p.edad = edadStr.isEmpty() ? 0 : Integer.parseInt(edadStr);

        String pesoStr = getText(etPeso);
        p.peso = pesoStr.isEmpty() ? 0f : Float.parseFloat(pesoStr);

        String tallaStr = getText(etTalla);
        p.talla = tallaStr.isEmpty() ? 0f : Float.parseFloat(tallaStr);

        int sexoId = rgSexo.getCheckedRadioButtonId();
        if (sexoId == R.id.rbMasculino)  p.sexo = "Masculino";
        else if (sexoId == R.id.rbFemenino) p.sexo = "Femenino";
        else p.sexo = "Otro";

        return p;
    }

    private PerfilClinico leerPaso2() {
        PerfilClinico p = new PerfilClinico();
        p.fuma                          = cbFuma.isChecked();
        p.tomaAlcohol                   = cbAlcohol.isChecked();
        p.consumeDrogas                 = cbDrogas.isChecked();
        p.alimentacionSaludable         = cbAlimentacionSaludable.isChecked();
        p.tieneAntecedentesEnfermedades = cbAntecedentes.isChecked();
        p.sarcopenia                    = cbSarcopenia.isChecked();
        p.debilidadMuscularCronica      = cbDebilidadMuscular.isChecked();
        p.deficitVitaminaB12            = cbDeficitB12.isChecked();
        p.deficitVitaminaD              = cbDeficitD.isChecked();
        p.participaRehabilitacion       = cbRehabilitacion.isChecked();
        p.disartria                     = cbDisartria.isChecked();
        p.tipoEjercicio                 = getText(etTipoEjercicio);
        p.tipDisartria                  = getText(etTipDisartria);
        p.observaciones                 = getText(etObservacionesClinicas);
        return p;
    }

    private List<AntecedentePersonal> leerAntPersonales() {
        List<AntecedentePersonal> lista = new ArrayList<>();
        for (int i = 0; i < containerAntPersonales.getChildCount(); i++) {
            View fila = containerAntPersonales.getChildAt(i);
            AntecedentePersonal a = new AntecedentePersonal();
            a.enfermedad       = getTextFromView(fila, R.id.etEnfermedad);
            a.descripcion      = getTextFromView(fila, R.id.etDescripcion);
            a.diagnosticadoPor = getTextFromView(fila, R.id.etDiagnosticadoPor);
            a.fechaDiagnostico = getTextFromView(fila, R.id.etFechaDiagnostico);
            if (!a.enfermedad.isEmpty()) lista.add(a);
        }
        return lista;
    }

    private List<AntecedenteFamiliar> leerAntFamiliares() {
        List<AntecedenteFamiliar> lista = new ArrayList<>();
        for (int i = 0; i < containerAntFamiliares.getChildCount(); i++) {
            View fila = containerAntFamiliares.getChildAt(i);
            AntecedenteFamiliar a = new AntecedenteFamiliar();
            a.parentesco    = getTextFromView(fila, R.id.etParentesco);
            a.enfermedad    = getTextFromView(fila, R.id.etEnfermedad);
            a.descripcion   = getTextFromView(fila, R.id.etDescripcion);
            a.siguePresente = ((CheckBox) fila.findViewById(R.id.cbSiguePresente)).isChecked();
            if (!a.enfermedad.isEmpty()) lista.add(a);
        }
        return lista;
    }

    private List<Medicamento> leerMedicamentos() {
        List<Medicamento> lista = new ArrayList<>();
        for (int i = 0; i < containerMedicamentos.getChildCount(); i++) {
            View fila = containerMedicamentos.getChildAt(i);
            Medicamento m = new Medicamento();
            m.nombre          = getTextFromView(fila, R.id.etNombre);
            m.tipoTratamiento = getTextFromView(fila, R.id.etTipoTratamiento);
            m.dosis           = getTextFromView(fila, R.id.etDosis);
            m.frecuencia      = getTextFromView(fila, R.id.etFrecuencia);
            m.motivo          = getTextFromView(fila, R.id.etMotivo);
            m.fechaInicio     = getTextFromView(fila, R.id.etFechaInicio);
            m.fechaFin        = getTextFromView(fila, R.id.etFechaFin);
            if (!m.nombre.isEmpty()) lista.add(m);
        }
        return lista;
    }

    private List<LesionPrevia> leerLesiones() {
        List<LesionPrevia> lista = new ArrayList<>();
        for (int i = 0; i < containerLesiones.getChildCount(); i++) {
            View fila = containerLesiones.getChildAt(i);
            LesionPrevia l = new LesionPrevia();
            l.tipoLesion   = getTextFromView(fila, R.id.etTipoLesion);
            l.zonaAfectada = getTextFromView(fila, R.id.etZonaAfectada);
            l.fechaLesion  = getTextFromView(fila, R.id.etFechaLesion);
            l.secuela      = ((CheckBox) fila.findViewById(R.id.cbSecuela)).isChecked();
            l.descripcion  = getTextFromView(fila, R.id.etDescripcion);
            if (!l.tipoLesion.isEmpty()) lista.add(l);
        }
        return lista;
    }

    // ================= HELPERS =================

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getTextFromView(View parent, int id) {
        EditText et = parent.findViewById(id);
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}