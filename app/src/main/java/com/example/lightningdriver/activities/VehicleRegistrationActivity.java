package com.example.lightningdriver.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.example.lightningdriver.R;

public class VehicleRegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_registration);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Toast.makeText(this, "Please register your vehicle!", Toast.LENGTH_SHORT).show();
    }
}