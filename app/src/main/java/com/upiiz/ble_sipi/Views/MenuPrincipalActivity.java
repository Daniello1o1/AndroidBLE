package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.upiiz.ble_sipi.R;

public class MenuPrincipalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        findViewById(R.id.cardPacientes).setOnClickListener(v ->
                startActivity(new Intent(this, ListaPacientesActivity.class)));

        findViewById(R.id.cardPruebas).setOnClickListener(v ->
                startActivity(new Intent(this, ListaPruebasActivity.class)));
    }
}