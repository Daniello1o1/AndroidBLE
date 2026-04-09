package com.upiiz.ble_sipi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class ReportActivity extends AppCompatActivity {
    Float emgMAV, emgWL, emgOrderV, dynMAV;
    TextView tvEmgMAV, tvEmgWl, tvEmgOrderV, tvDynMAV;
    Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        emgMAV = getIntent().getFloatExtra("emg_MAV",0.0f);
        emgWL = getIntent().getFloatExtra("emg_WL",0.0f);
        emgOrderV = getIntent().getFloatExtra("emg_OrderV",0.0f);
        dynMAV = getIntent().getFloatExtra("dyn_MAV",0.0f);

        tvEmgMAV = findViewById(R.id.tvEmgMAV);
        tvEmgWl = findViewById(R.id.tvEmgWl);
        tvEmgOrderV = findViewById(R.id.tvEmgOrderV);
        tvDynMAV = findViewById(R.id.tvDynMAV);

        tvEmgMAV.setText("MAV: "+emgMAV);
        tvEmgWl.setText("WL: "+emgWL);
        tvEmgOrderV.setText("Order V: "+emgOrderV);
        tvDynMAV.setText("MAV: "+dynMAV);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v->{finish();});
    }


}