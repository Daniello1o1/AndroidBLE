package com.upiiz.ble_sipi.Views;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetallePacienteActivity extends AppCompatActivity {

    private Paciente paciente;
    private PacienteRepository repository;

    // Header
    private TextView tvInicial, tvNombreCompleto, tvInfoBasica;

    // Secciones acordeón
    private View seccionDatos, seccionPerfil;
    private View seccionAntPersonales, seccionAntFamiliares;
    private View seccionMedicamentos, seccionLesiones;

    // Datos cargados
    private PerfilClinico perfilActual;
    private List<AntecedentePersonal> antPersonalesActuales;
    private List<AntecedenteFamiliar> antFamiliaresActuales;
    private List<Medicamento> medicamentosActuales;
    private List<LesionPrevia> lesionesActuales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_paciente);

        paciente   = (Paciente) getIntent().getSerializableExtra("paciente");
        repository = new PacienteRepository();

        bindViews();
        configurarHeader();
        configurarAcordeon(seccionDatos,          "Datos generales");
        configurarAcordeon(seccionPerfil,         "Perfil clínico");
        configurarAcordeon(seccionAntPersonales,  "Antecedentes personales");
        configurarAcordeon(seccionAntFamiliares,  "Antecedentes familiares");
        configurarAcordeon(seccionMedicamentos,   "Medicamentos");
        configurarAcordeon(seccionLesiones,       "Lesiones previas");

        configurarContenidoDatos();
        cargarPerfil();
        cargarAntPersonales();
        cargarAntFamiliares();
        cargarMedicamentos();
        cargarLesiones();

        findViewById(R.id.btnBorrar).setOnClickListener(v -> confirmarBorrado());
        findViewById(R.id.btnEjecutarPrueba).setOnClickListener(v -> {
            Intent intent = new Intent(this, SeleccionarPruebaActivity.class);
            intent.putExtra("paciente", paciente);
            startActivity(intent);
        });
    }

    // ================= BIND =================

    private void bindViews() {
        tvInicial         = findViewById(R.id.tvInicial);
        tvNombreCompleto  = findViewById(R.id.tvNombreCompleto);
        tvInfoBasica      = findViewById(R.id.tvInfoBasica);
        seccionDatos          = findViewById(R.id.seccionDatos);
        seccionPerfil         = findViewById(R.id.seccionPerfil);
        seccionAntPersonales  = findViewById(R.id.seccionAntPersonales);
        seccionAntFamiliares  = findViewById(R.id.seccionAntFamiliares);
        seccionMedicamentos   = findViewById(R.id.seccionMedicamentos);
        seccionLesiones       = findViewById(R.id.seccionLesiones);
    }

    private void configurarHeader() {
        String inicial = paciente.nombre != null && !paciente.nombre.isEmpty()
                ? String.valueOf(paciente.nombre.charAt(0)).toUpperCase() : "?";
        tvInicial.setText(inicial);
        tvNombreCompleto.setText(paciente.getNombreCompleto());
        tvInfoBasica.setText(paciente.edad + " años  ·  " + paciente.sexo
                + "  ·  " + paciente.peso + "kg");
    }

    // ================= ACORDEÓN =================

    private void configurarAcordeon(View seccion, String titulo) {
        TextView tvTitulo   = seccion.findViewById(R.id.tvTituloSeccion);
        TextView tvFlecha   = seccion.findViewById(R.id.tvFlecha);
        View header         = seccion.findViewById(R.id.headerAcordeon);
        View contenido      = seccion.findViewById(R.id.contenidoAcordeon);

        tvTitulo.setText(titulo);

        header.setOnClickListener(v -> {
            boolean expandido = contenido.getVisibility() == View.VISIBLE;
            contenido.setVisibility(expandido ? View.GONE : View.VISIBLE);
            tvFlecha.setRotation(expandido ? 90 : 270);
        });
    }

    // ================= SECCIÓN DATOS GENERALES =================

    private void configurarContenidoDatos() {
        FrameLayout frame = seccionDatos.findViewById(R.id.frameContenido);
        View contenido = LayoutInflater.from(this)
                .inflate(R.layout.layout_paso1_datos_generales, frame, false);
        frame.addView(contenido);

        // Llenar campos
        TextInputEditText etNombre   = contenido.findViewById(R.id.etNombre);
        TextInputEditText etApellidos = contenido.findViewById(R.id.etApellidos);
        TextInputEditText etFecha    = contenido.findViewById(R.id.etFechaNacimiento);
        TextInputEditText etPeso     = contenido.findViewById(R.id.etPeso);
        TextInputEditText etTalla    = contenido.findViewById(R.id.etTalla);
        TextInputEditText etObs      = contenido.findViewById(R.id.etObservaciones);
        RadioGroup rgSexo            = contenido.findViewById(R.id.rgSexo);

        etNombre.setText(paciente.nombre);
        etApellidos.setText(paciente.apellidos);
        etFecha.setText(paciente.fechaNacimiento);
        etPeso.setText(String.valueOf(paciente.peso));
        etTalla.setText(String.valueOf(paciente.talla));
        etObs.setText(paciente.observaciones);

        if ("Masculino".equals(paciente.sexo))
            rgSexo.check(R.id.rbMasculino);
        else if ("Femenino".equals(paciente.sexo))
            rgSexo.check(R.id.rbFemenino);
        else rgSexo.check(R.id.rbOtro);

        // Deshabilitar campos inicialmente
        setEnabled(contenido, false);
        etFecha.setOnClickListener(v -> mostrarDatePicker(etFecha));

        // Botón editar
        MaterialButton btnEditar  = seccionDatos.findViewById(R.id.btnEditarSeccion);
        MaterialButton btnGuardar = seccionDatos.findViewById(R.id.btnGuardarSeccion);

        btnEditar.setOnClickListener(v -> {
            setEnabled(contenido, true);
            btnEditar.setVisibility(View.GONE);
            btnGuardar.setVisibility(View.VISIBLE);
        });

        btnGuardar.setOnClickListener(v -> {
            paciente.nombre          = getText(etNombre);
            paciente.apellidos       = getText(etApellidos);
            paciente.fechaNacimiento = getText(etFecha);
            paciente.observaciones   = getText(etObs);
            paciente.edad = calcularEdad(getText(etFecha));
            String pesoStr = getText(etPeso);
            paciente.peso  = pesoStr.isEmpty() ? 0f : Float.parseFloat(pesoStr);
            String tallaStr = getText(etTalla);
            paciente.talla  = tallaStr.isEmpty() ? 0f : Float.parseFloat(tallaStr);

            int sexoId = rgSexo.getCheckedRadioButtonId();
            if (sexoId == R.id.rbMasculino) paciente.sexo = "Masculino";
            else if (sexoId == R.id.rbFemenino) paciente.sexo = "Femenino";
            else paciente.sexo = "Otro";

            repository.actualizarPaciente(paciente, new PacienteRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void r) {
                    runOnUiThread(() -> {
                        configurarHeader();
                        setEnabled(contenido, false);
                        btnEditar.setVisibility(View.VISIBLE);
                        btnGuardar.setVisibility(View.GONE);
                        Toast.makeText(DetallePacienteActivity.this,
                                "Datos actualizados", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(DetallePacienteActivity.this,
                            "Error al guardar", Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
    private int calcularEdad(String fechaNacimiento) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            java.util.Date fechaNac = sdf.parse(fechaNacimiento);
            if (fechaNac == null) return 0;

            Calendar nacimiento = Calendar.getInstance();
            nacimiento.setTime(fechaNac);

            Calendar hoy = Calendar.getInstance();
            int edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR);

            // Ajustar si aún no ha cumplido años este año
            if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
                edad--;
            }
            return Math.max(0, edad);
        } catch (Exception e) {
            return 0;
        }
    }

    // ================= SECCIÓN PERFIL CLÍNICO =================

    private void cargarPerfil() {
        repository.obtenerPerfil(paciente.id, new PacienteRepository.Callback<PerfilClinico>() {
            @Override
            public void onSuccess(PerfilClinico perfil) {
                perfilActual = perfil;
                runOnUiThread(() -> configurarContenidoPerfil(perfil));
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void configurarContenidoPerfil(PerfilClinico perfil) {
        android.widget.FrameLayout frame = seccionPerfil.findViewById(R.id.frameContenido);
        frame.removeAllViews();
        View contenido = LayoutInflater.from(this)
                .inflate(R.layout.layout_paso2_perfil_clinico, frame, false);
        frame.addView(contenido);

        CheckBox cbFuma    = contenido.findViewById(R.id.cbFuma);
        CheckBox cbAlcohol = contenido.findViewById(R.id.cbAlcohol);
        CheckBox cbDrogas  = contenido.findViewById(R.id.cbDrogas);
        CheckBox cbAlim    = contenido.findViewById(R.id.cbAlimentacionSaludable);
        CheckBox cbAnt     = contenido.findViewById(R.id.cbAntecedentes);
        CheckBox cbSarco   = contenido.findViewById(R.id.cbSarcopenia);
        CheckBox cbDebil   = contenido.findViewById(R.id.cbDebilidadMuscular);
        CheckBox cbB12     = contenido.findViewById(R.id.cbDeficitB12);
        CheckBox cbD       = contenido.findViewById(R.id.cbDeficitD);
        CheckBox cbRehab   = contenido.findViewById(R.id.cbRehabilitacion);
        CheckBox cbDis     = contenido.findViewById(R.id.cbDisartria);
        TextInputEditText etEjercicio = contenido.findViewById(R.id.etTipoEjercicio);
        TextInputEditText etTipDis    = contenido.findViewById(R.id.etTipDisartria);
        TextInputEditText etObs       = contenido.findViewById(R.id.etObservacionesClinicas);
        TextInputLayout layoutTipDis  = contenido.findViewById(R.id.layoutTipDisartria);

        cbFuma.setChecked(perfil.fuma);
        cbAlcohol.setChecked(perfil.tomaAlcohol);
        cbDrogas.setChecked(perfil.consumeDrogas);
        cbAlim.setChecked(perfil.alimentacionSaludable);
        cbAnt.setChecked(perfil.tieneAntecedentesEnfermedades);
        cbSarco.setChecked(perfil.sarcopenia);
        cbDebil.setChecked(perfil.debilidadMuscularCronica);
        cbB12.setChecked(perfil.deficitVitaminaB12);
        cbD.setChecked(perfil.deficitVitaminaD);
        cbRehab.setChecked(perfil.participaRehabilitacion);
        cbDis.setChecked(perfil.disartria);
        etEjercicio.setText(perfil.tipoEjercicio);
        etTipDis.setText(perfil.tipDisartria);
        etObs.setText(perfil.observaciones);
        layoutTipDis.setVisibility(perfil.disartria ? View.VISIBLE : View.GONE);

        cbDis.setOnCheckedChangeListener((btn, checked) ->
                layoutTipDis.setVisibility(checked ? View.VISIBLE : View.GONE));

        setEnabled(contenido, false);

        MaterialButton btnEditar  = seccionPerfil.findViewById(R.id.btnEditarSeccion);
        MaterialButton btnGuardar = seccionPerfil.findViewById(R.id.btnGuardarSeccion);

        btnEditar.setOnClickListener(v -> {
            setEnabled(contenido, true);
            btnEditar.setVisibility(View.GONE);
            btnGuardar.setVisibility(View.VISIBLE);
        });

        btnGuardar.setOnClickListener(v -> {
            perfil.fuma                          = cbFuma.isChecked();
            perfil.tomaAlcohol                   = cbAlcohol.isChecked();
            perfil.consumeDrogas                 = cbDrogas.isChecked();
            perfil.alimentacionSaludable         = cbAlim.isChecked();
            perfil.tieneAntecedentesEnfermedades = cbAnt.isChecked();
            perfil.sarcopenia                    = cbSarco.isChecked();
            perfil.debilidadMuscularCronica      = cbDebil.isChecked();
            perfil.deficitVitaminaB12            = cbB12.isChecked();
            perfil.deficitVitaminaD              = cbD.isChecked();
            perfil.participaRehabilitacion       = cbRehab.isChecked();
            perfil.disartria                     = cbDis.isChecked();
            perfil.tipoEjercicio                 = getText(etEjercicio);
            perfil.tipDisartria                  = getText(etTipDis);
            perfil.observaciones                 = getText(etObs);

            repository.guardarPerfil(paciente.id, perfil,
                    new PacienteRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void r) {
                            runOnUiThread(() -> {
                                setEnabled(contenido, false);
                                btnEditar.setVisibility(View.VISIBLE);
                                btnGuardar.setVisibility(View.GONE);
                                Toast.makeText(DetallePacienteActivity.this,
                                        "Perfil actualizado", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> Toast.makeText(DetallePacienteActivity.this,
                                    "Error al guardar", Toast.LENGTH_SHORT).show());
                        }
                    });
        });
    }

    // ================= ANTECEDENTES PERSONALES =================

    private void cargarAntPersonales() {
        repository.obtenerAntecedentesPersonales(paciente.id,
                new PacienteRepository.Callback<List<AntecedentePersonal>>() {
                    @Override
                    public void onSuccess(List<AntecedentePersonal> lista) {
                        antPersonalesActuales = lista;
                        runOnUiThread(() -> mostrarAntPersonales(lista));
                    }
                    @Override
                    public void onError(Exception e) {}
                });
    }

    private void mostrarAntPersonales(List<AntecedentePersonal> lista) {
        android.widget.FrameLayout frame =
                seccionAntPersonales.findViewById(R.id.frameContenido);
        frame.removeAllViews();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        frame.addView(container);

        for (AntecedentePersonal a : lista) {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_antecedente_personal, container, false);

            ((EditText) fila.findViewById(R.id.etEnfermedad)).setText(a.enfermedad);
            ((EditText) fila.findViewById(R.id.etDescripcion)).setText(a.descripcion);
            ((EditText) fila.findViewById(R.id.etDiagnosticadoPor)).setText(a.diagnosticadoPor);
            ((EditText) fila.findViewById(R.id.etFechaDiagnostico)).setText(a.fechaDiagnostico);
            setEnabled(fila, false);

            fila.findViewById(R.id.btnEliminar).setOnClickListener(v -> {
                repository.borrarAntecedentePersonal(paciente.id, a.id,
                        new PacienteRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void r) {
                                runOnUiThread(() -> {
                                    container.removeView(fila);
                                    Toast.makeText(DetallePacienteActivity.this,
                                            "Eliminado", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override
                            public void onError(Exception e) {}
                        });
            });

            container.addView(fila);
        }

        // Botón agregar nuevo
        MaterialButton btnAgregar = new MaterialButton(this,
                null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton);
        btnAgregar.setText("+ Agregar antecedente");
        btnAgregar.setOnClickListener(v -> {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_antecedente_personal, container, false);

            TextInputEditText etFecha = fila.findViewById(R.id.etFechaDiagnostico);
            etFecha.setOnClickListener(vv -> mostrarDatePicker(etFecha));

            fila.findViewById(R.id.btnEliminar)
                    .setOnClickListener(vv -> {
                        AntecedentePersonal nuevo = new AntecedentePersonal();
                        nuevo.enfermedad       = getTextFromView(fila, R.id.etEnfermedad);
                        nuevo.descripcion      = getTextFromView(fila, R.id.etDescripcion);
                        nuevo.diagnosticadoPor = getTextFromView(fila, R.id.etDiagnosticadoPor);
                        nuevo.fechaDiagnostico = getTextFromView(fila, R.id.etFechaDiagnostico);

                        if (!nuevo.enfermedad.isEmpty()) {
                            repository.agregarAntecedentePersonal(paciente.id, nuevo,
                                    new PacienteRepository.Callback<String>() {
                                        @Override
                                        public void onSuccess(String id) {
                                            nuevo.id = id;
                                            runOnUiThread(() -> {
                                                setEnabled(fila, false);
                                                Toast.makeText(DetallePacienteActivity.this,
                                                        "Guardado", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                        @Override
                                        public void onError(Exception e) {}
                                    });
                        } else {
                            container.removeView(fila);
                        }
                    });

            container.addView(fila, container.getChildCount() - 1);
        });

        container.addView(btnAgregar);

        // Ocultar botones del acordeón — esta sección maneja sus propios botones
        seccionAntPersonales.findViewById(R.id.layoutBotonesSeccion)
                .setVisibility(View.GONE);
    }

    // ================= ANTECEDENTES FAMILIARES =================

    private void cargarAntFamiliares() {
        repository.obtenerAntecedentesFamiliares(paciente.id,
                new PacienteRepository.Callback<List<AntecedenteFamiliar>>() {
                    @Override
                    public void onSuccess(List<AntecedenteFamiliar> lista) {
                        antFamiliaresActuales = lista;
                        runOnUiThread(() -> mostrarAntFamiliares(lista));
                    }
                    @Override
                    public void onError(Exception e) {}
                });
    }

    private void mostrarAntFamiliares(List<AntecedenteFamiliar> lista) {
        android.widget.FrameLayout frame =
                seccionAntFamiliares.findViewById(R.id.frameContenido);
        frame.removeAllViews();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        frame.addView(container);

        for (AntecedenteFamiliar a : lista) {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_antecedente_familiar, container, false);

            ((EditText) fila.findViewById(R.id.etParentesco)).setText(a.parentesco);
            ((EditText) fila.findViewById(R.id.etEnfermedad)).setText(a.enfermedad);
            ((EditText) fila.findViewById(R.id.etDescripcion)).setText(a.descripcion);
            ((CheckBox) fila.findViewById(R.id.cbSiguePresente)).setChecked(a.siguePresente);
            setEnabled(fila, false);

            fila.findViewById(R.id.btnEliminar).setOnClickListener(v ->
                    repository.borrarAntecedenteFamiliar(paciente.id, a.id,
                            new PacienteRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void r) {
                                    runOnUiThread(() -> container.removeView(fila));
                                }
                                @Override
                                public void onError(Exception e) {}
                            }));

            container.addView(fila);
        }

        MaterialButton btnAgregar = new MaterialButton(this,
                null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton);
        btnAgregar.setOnClickListener(v -> {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_antecedente_familiar, container, false);

            fila.findViewById(R.id.btnEliminar).setOnClickListener(vv -> {
                AntecedenteFamiliar nuevo = new AntecedenteFamiliar();
                nuevo.parentesco    = getTextFromView(fila, R.id.etParentesco);
                nuevo.enfermedad    = getTextFromView(fila, R.id.etEnfermedad);
                nuevo.descripcion   = getTextFromView(fila, R.id.etDescripcion);
                nuevo.siguePresente = ((CheckBox) fila.findViewById(R.id.cbSiguePresente))
                        .isChecked();

                if (!nuevo.enfermedad.isEmpty()) {
                    repository.agregarAntecedenteFamiliar(paciente.id, nuevo,
                            new PacienteRepository.Callback<String>() {
                                @Override
                                public void onSuccess(String id) {
                                    nuevo.id = id;
                                    runOnUiThread(() -> setEnabled(fila, false));
                                }
                                @Override
                                public void onError(Exception e) {}
                            });
                } else {
                    container.removeView(fila);
                }
            });

            container.addView(fila, container.getChildCount() - 1);
        });

        container.addView(btnAgregar);
        seccionAntFamiliares.findViewById(R.id.layoutBotonesSeccion)
                .setVisibility(View.GONE);
    }

    // ================= MEDICAMENTOS =================

    private void cargarMedicamentos() {
        repository.obtenerMedicamentos(paciente.id,
                new PacienteRepository.Callback<List<Medicamento>>() {
                    @Override
                    public void onSuccess(List<Medicamento> lista) {
                        medicamentosActuales = lista;
                        runOnUiThread(() -> mostrarMedicamentos(lista));
                    }
                    @Override
                    public void onError(Exception e) {}
                });
    }

    private void mostrarMedicamentos(List<Medicamento> lista) {
        android.widget.FrameLayout frame =
                seccionMedicamentos.findViewById(R.id.frameContenido);
        frame.removeAllViews();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        frame.addView(container);

        for (Medicamento m : lista) {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_medicamento, container, false);

            ((EditText) fila.findViewById(R.id.etNombre)).setText(m.nombre);
            ((EditText) fila.findViewById(R.id.etTipoTratamiento)).setText(m.tipoTratamiento);
            ((EditText) fila.findViewById(R.id.etDosis)).setText(m.dosis);
            ((EditText) fila.findViewById(R.id.etFrecuencia)).setText(m.frecuencia);
            ((EditText) fila.findViewById(R.id.etMotivo)).setText(m.motivo);
            ((EditText) fila.findViewById(R.id.etFechaInicio)).setText(m.fechaInicio);
            ((EditText) fila.findViewById(R.id.etFechaFin)).setText(m.fechaFin);
            setEnabled(fila, false);

            fila.findViewById(R.id.btnEliminar).setOnClickListener(v ->
                    repository.borrarMedicamento(paciente.id, m.id,
                            new PacienteRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void r) {
                                    runOnUiThread(() -> container.removeView(fila));
                                }
                                @Override
                                public void onError(Exception e) {}
                            }));

            container.addView(fila);
        }

        MaterialButton btnAgregar = new MaterialButton(this,
                null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton);
        btnAgregar.setText("+ Agregar medicamento");
        btnAgregar.setOnClickListener(v -> {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_medicamento, container, false);

            TextInputEditText etInicio = fila.findViewById(R.id.etFechaInicio);
            TextInputEditText etFin    = fila.findViewById(R.id.etFechaFin);
            etInicio.setOnClickListener(vv -> mostrarDatePicker(etInicio));
            etFin.setOnClickListener(vv -> mostrarDatePicker(etFin));

            fila.findViewById(R.id.btnEliminar).setOnClickListener(vv -> {
                Medicamento nuevo = new Medicamento();
                nuevo.nombre          = getTextFromView(fila, R.id.etNombre);
                nuevo.tipoTratamiento = getTextFromView(fila, R.id.etTipoTratamiento);
                nuevo.dosis           = getTextFromView(fila, R.id.etDosis);
                nuevo.frecuencia      = getTextFromView(fila, R.id.etFrecuencia);
                nuevo.motivo          = getTextFromView(fila, R.id.etMotivo);
                nuevo.fechaInicio     = getTextFromView(fila, R.id.etFechaInicio);
                nuevo.fechaFin        = getTextFromView(fila, R.id.etFechaFin);

                if (!nuevo.nombre.isEmpty()) {
                    repository.agregarMedicamento(paciente.id, nuevo,
                            new PacienteRepository.Callback<String>() {
                                @Override
                                public void onSuccess(String id) {
                                    nuevo.id = id;
                                    runOnUiThread(() -> setEnabled(fila, false));
                                }
                                @Override
                                public void onError(Exception e) {}
                            });
                } else {
                    container.removeView(fila);
                }
            });

            container.addView(fila, container.getChildCount() - 1);
        });

        container.addView(btnAgregar);
        seccionMedicamentos.findViewById(R.id.layoutBotonesSeccion)
                .setVisibility(View.GONE);
    }

    // ================= LESIONES PREVIAS =================

    private void cargarLesiones() {
        repository.obtenerLesiones(paciente.id,
                new PacienteRepository.Callback<List<LesionPrevia>>() {
                    @Override
                    public void onSuccess(List<LesionPrevia> lista) {
                        lesionesActuales = lista;
                        runOnUiThread(() -> mostrarLesiones(lista));
                    }
                    @Override
                    public void onError(Exception e) {}
                });
    }

    private void mostrarLesiones(List<LesionPrevia> lista) {
        android.widget.FrameLayout frame =
                seccionLesiones.findViewById(R.id.frameContenido);
        frame.removeAllViews();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        frame.addView(container);

        for (LesionPrevia l : lista) {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_lesion, container, false);

            ((EditText) fila.findViewById(R.id.etTipoLesion)).setText(l.tipoLesion);
            ((EditText) fila.findViewById(R.id.etZonaAfectada)).setText(l.zonaAfectada);
            ((EditText) fila.findViewById(R.id.etFechaLesion)).setText(l.fechaLesion);
            ((CheckBox) fila.findViewById(R.id.cbSecuela)).setChecked(l.secuela);
            ((EditText) fila.findViewById(R.id.etDescripcion)).setText(l.descripcion);
            setEnabled(fila, false);

            fila.findViewById(R.id.btnEliminar).setOnClickListener(v ->
                    repository.borrarLesion(paciente.id, l.id,
                            new PacienteRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void r) {
                                    runOnUiThread(() -> container.removeView(fila));
                                }
                                @Override
                                public void onError(Exception e) {}
                            }));

            container.addView(fila);
        }

        MaterialButton btnAgregar = new MaterialButton(this,
                null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton);
        btnAgregar.setText("+ Agregar lesión");
        btnAgregar.setOnClickListener(v -> {
            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_lesion, container, false);

            TextInputEditText etFecha = fila.findViewById(R.id.etFechaLesion);
            etFecha.setOnClickListener(vv -> mostrarDatePicker(etFecha));

            fila.findViewById(R.id.btnEliminar).setOnClickListener(vv -> {
                LesionPrevia nueva = new LesionPrevia();
                nueva.tipoLesion   = getTextFromView(fila, R.id.etTipoLesion);
                nueva.zonaAfectada = getTextFromView(fila, R.id.etZonaAfectada);
                nueva.fechaLesion  = getTextFromView(fila, R.id.etFechaLesion);
                nueva.secuela      = ((CheckBox) fila.findViewById(R.id.cbSecuela)).isChecked();
                nueva.descripcion  = getTextFromView(fila, R.id.etDescripcion);

                if (!nueva.tipoLesion.isEmpty()) {
                    repository.agregarLesion(paciente.id, nueva,
                            new PacienteRepository.Callback<String>() {
                                @Override
                                public void onSuccess(String id) {
                                    nueva.id = id;
                                    runOnUiThread(() -> setEnabled(fila, false));
                                }
                                @Override
                                public void onError(Exception e) {}
                            });
                } else {
                    container.removeView(fila);
                }
            });

            container.addView(fila, container.getChildCount() - 1);
        });

        container.addView(btnAgregar);
        seccionLesiones.findViewById(R.id.layoutBotonesSeccion)
                .setVisibility(View.GONE);
    }

    // ================= BORRAR PACIENTE =================

    private void confirmarBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar paciente")
                .setMessage("¿Estás seguro? Esta acción no se puede deshacer.")
                .setPositiveButton("Borrar", (d, w) -> {
                    repository.borrarPaciente(paciente.id,
                            new PacienteRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void r) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(DetallePacienteActivity.this,
                                                "Paciente eliminado", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                                }
                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> Toast.makeText(
                                            DetallePacienteActivity.this,
                                            "Error al eliminar", Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ================= HELPERS =================

    private void mostrarDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                target.setText(String.format("%02d/%02d/%04d", day, month + 1, year)),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setEnabled(View root, boolean enabled) {
        if (root instanceof EditText) {
            root.setEnabled(enabled);
        } else if (root instanceof CheckBox) {
            root.setEnabled(enabled);
        } else if (root instanceof RadioGroup) {
            root.setEnabled(enabled);
            for (int i = 0; i < ((RadioGroup) root).getChildCount(); i++) {
                ((RadioGroup) root).getChildAt(i).setEnabled(enabled);
            }
        } else if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getTextFromView(View parent, int id) {
        EditText et = parent.findViewById(id);
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}