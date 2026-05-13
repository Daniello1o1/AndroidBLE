package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.Models.Usuario;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.UsuarioRepository;

public class RegistroUsuarioActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etApellidos, etInstitucion;
    private TextInputEditText etEmail, etPassword, etConfirmarPassword;
    private TextView tvError;
    private MaterialButton btnRegistrar;
    private UsuarioRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuario);

        repository = new UsuarioRepository();

        etNombre            = findViewById(R.id.etNombre);
        etApellidos         = findViewById(R.id.etApellidos);
        etInstitucion       = findViewById(R.id.etInstitucion);
        etEmail             = findViewById(R.id.etEmail);
        etPassword          = findViewById(R.id.etPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        tvError             = findViewById(R.id.tvError);
        btnRegistrar        = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> registrar());

        findViewById(R.id.btnIrLogin).setOnClickListener(v -> finish());
    }

    private void registrar() {
        String nombre    = getText(etNombre);
        String apellidos = getText(etApellidos);
        String email     = getText(etEmail);
        String password  = getText(etPassword);
        String confirmar = getText(etConfirmarPassword);

        // Validaciones
        if (nombre.isEmpty()) {
            etNombre.setError("Campo requerido");
            return;
        }
        if (apellidos.isEmpty()) {
            etApellidos.setError("Campo requerido");
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Campo requerido");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Campo requerido");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            return;
        }
        if (!password.equals(confirmar)) {
            etConfirmarPassword.setError("Las contraseñas no coinciden");
            return;
        }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Creando cuenta...");
        tvError.setText("");

        Usuario usuario = new Usuario();
        usuario.nombre      = nombre;
        usuario.apellidos   = apellidos;
        usuario.institucion = getText(etInstitucion);

        repository.registrar(email, password, usuario,
                new UsuarioRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(RegistroUsuarioActivity.this,
                                    MenuPrincipalActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            btnRegistrar.setEnabled(true);
                            btnRegistrar.setText("Crear cuenta");
                            tvError.setText(traducirError(e.getMessage()));
                        });
                    }
                });
    }

    private String traducirError(String mensaje) {
        if (mensaje == null) return "Error desconocido";
        if (mensaje.contains("email address is already in use"))
            return "Ya existe una cuenta con ese correo";
        if (mensaje.contains("badly formatted"))
            return "El correo no tiene un formato válido";
        if (mensaje.contains("weak-password"))
            return "La contraseña es muy débil";
        return "Error al crear la cuenta";
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}