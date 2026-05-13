package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.UsuarioRepository;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextView tvError;
    private MaterialButton btnLogin;
    private UsuarioRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repository = new UsuarioRepository();

        etEmail   = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError   = findViewById(R.id.tvError);
        btnLogin  = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());

        findViewById(R.id.btnIrRegistro).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroUsuarioActivity.class)));
    }

    private void login() {
        String email    = getText(etEmail);
        String password = getText(etPassword);

        if (email.isEmpty()) {
            etEmail.setError("Campo requerido");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Campo requerido");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");
        tvError.setText("");

        repository.login(email, password, new UsuarioRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(LoginActivity.this,
                            MenuPrincipalActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sesión");
                    tvError.setText(traducirError(e.getMessage()));
                });
            }
        });
    }

    private String traducirError(String mensaje) {
        if (mensaje == null) return "Error desconocido";
        if (mensaje.contains("no user record"))
            return "No existe una cuenta con ese correo";
        if (mensaje.contains("password is invalid") || mensaje.contains("INVALID_LOGIN_CREDENTIALS"))
            return "Contraseña incorrecta";
        if (mensaje.contains("badly formatted"))
            return "El correo no tiene un formato válido";
        return "Error al iniciar sesión";
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}