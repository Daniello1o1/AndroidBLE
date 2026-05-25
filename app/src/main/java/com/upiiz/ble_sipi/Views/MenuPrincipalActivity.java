package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.Models.Usuario;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PacienteRepository;
import com.upiiz.ble_sipi.Repository.PruebaRepository;
import com.upiiz.ble_sipi.Repository.UsuarioRepository;
import com.upiiz.ble_sipi.Tools.ReporteGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MenuPrincipalActivity extends AppCompatActivity {

    // Declarar como campo de la clase
    private MaterialButton btnExportarDataset;
    private UsuarioRepository usuarioRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        btnExportarDataset = findViewById(R.id.btnExportarDataset);
        usuarioRepo        = new UsuarioRepository();

        findViewById(R.id.cardPacientes).setOnClickListener(v ->
                startActivity(new Intent(this, ListaPacientesActivity.class)));

        findViewById(R.id.cardPruebas).setOnClickListener(v ->
                startActivity(new Intent(this, ListaPruebasActivity.class)));

        btnExportarDataset.setOnClickListener(v -> exportarDatasetCompleto());

        // Mostrar nombre del usuario
        usuarioRepo.obtenerUsuarioActual(new UsuarioRepository.Callback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                runOnUiThread(() -> {
                    TextView tvUsuario = findViewById(R.id.tvUsuario);
                    tvUsuario.setText("Dr. " + usuario.getNombreCompleto());
                });
            }
            @Override
            public void onError(Exception e) {}
        });

        // Cerrar sesión
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Estás seguro?")
                        .setPositiveButton("Sí", (d, w) -> {
                            usuarioRepo.logout();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show());
    }

    private void cargarTodasLasEjecuciones(Consumer<List<Ejecucion>> callback) {
        FirebaseFirestore.getInstance()
                .collectionGroup("ejecuciones")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Ejecucion> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Ejecucion e = new Ejecucion();
                        e.id                = doc.getId();
                        e.danielsAsignado   = toInt(doc.get("danielsAsignado"));
                        e.danielsEstimado   = toInt(doc.get("danielsEstimado"));
                        e.rms               = toFloat(doc.get("rms"));
                        e.emgMAVTotal       = toFloat(doc.get("emgMAVTotal"));
                        e.emgWLTotal        = toFloat(doc.get("emgWLTotal"));
                        e.emgOrderVTotal    = toFloat(doc.get("emgOrderVTotal"));
                        e.frecuenciaMediana = toFloat(doc.get("frecuenciaMediana"));
                        e.frecuenciaMedia   = toFloat(doc.get("frecuenciaMedia"));
                        e.rfd               = toFloat(doc.get("rfd"));
                        e.fuerzaMaxima      = toFloat(doc.get("fuerzaMaxima"));
                        e.impulso           = toFloat(doc.get("impulso"));
                        e.indiceFatiga      = toFloat(doc.get("indiceFatiga"));
                        e.eficienciaMusular = toFloat(doc.get("eficienciaMusular"));
                        lista.add(e);
                    }
                    runOnUiThread(() -> callback.accept(lista));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error cargando ejecuciones: "
                            + e.getMessage(), Toast.LENGTH_SHORT).show();
                    runOnUiThread(() -> callback.accept(new ArrayList<>()));
                });
    }

    private float toFloat(Object o) {
        if (o instanceof Double) return ((Double) o).floatValue();
        if (o instanceof Float)  return (Float) o;
        return 0f;
    }

    private int toInt(Object o) {
        if (o instanceof Long)    return ((Long) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        return -1;
    }
    private void exportarDatasetCompleto() {
        btnExportarDataset.setEnabled(false);
        btnExportarDataset.setText("Preparando dataset...");

        // Cargar ejecuciones, pacientes y pruebas en paralelo
        List<Ejecucion>[] ejecucionesRef = new List[]{null};
        List<Paciente>[]  pacientesRef   = new List[]{null};
        List<Prueba>[]    pruebasRef     = new List[]{null};
        int[]             pendientes     = {3};

        Runnable verificar = () -> {
            pendientes[0]--;
            if (pendientes[0] == 0) {
                // Todo cargado — generar CSV
                try {
                    File archivo = ReporteGenerator.exportarCSVGlobal(
                            this,
                            ejecucionesRef[0],
                            pacientesRef[0],
                            pruebasRef[0]);

                    Uri uri = FileProvider.getUriForFile(
                            this, getPackageName() + ".provider", archivo);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Exportar dataset"));

                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } finally {
                    runOnUiThread(() -> {
                        btnExportarDataset.setEnabled(true);
                        btnExportarDataset.setText("Exportar dataset completo");
                    });
                }
            }
        };

        // Cargar ejecuciones
        cargarTodasLasEjecuciones(ejecuciones -> {
            ejecucionesRef[0] = ejecuciones;
            verificar.run();
        });

        // Cargar pacientes
        new PacienteRepository().obtenerPacientes(
                new PacienteRepository.Callback<List<Paciente>>() {
                    @Override
                    public void onSuccess(List<Paciente> lista) {
                        pacientesRef[0] = lista;
                        runOnUiThread(verificar);
                    }
                    @Override
                    public void onError(Exception e) {
                        pacientesRef[0] = new ArrayList<>();
                        runOnUiThread(verificar);
                    }
                });

        // Cargar pruebas
        new PruebaRepository().obtenerPruebas(
                new PruebaRepository.Callback<List<Prueba>>() {
                    @Override
                    public void onSuccess(List<Prueba> lista) {
                        pruebasRef[0] = lista;
                        runOnUiThread(verificar);
                    }
                    @Override
                    public void onError(Exception e) {
                        pruebasRef[0] = new ArrayList<>();
                        runOnUiThread(verificar);
                    }
                });
    }
}