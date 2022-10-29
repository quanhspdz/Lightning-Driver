package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Const;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    FrameLayout btnWorking, buttonCurrentOrder;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setStatusBarColor();
        init();
        listener();

    }

    private void listener() {
        btnWorking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WorkingActivity.class);
                startActivity(intent);
            }
        });

        buttonCurrentOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String driverId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                progressDialog.show();
                if (driverId != null) {
                    FirebaseDatabase.getInstance().getReference().child("Trips")
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                        Trip trip = dataSnapshot.getValue(Trip.class);
                                        if (trip != null) {
                                            if (trip.getDriverId() != null) {
                                                if (trip.getDriverId().equals(driverId) && trip.getStatus().equals(Const.waitingToPickUp)) {
                                                    Intent intent = new Intent(MainActivity.this, PickUpActivity.class);
                                                    intent.putExtra("tripId", trip.getId());
                                                    startActivity(intent);
                                                    progressDialog.dismiss();
                                                }
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }
        });
    }

    private void init() {
        btnWorking = findViewById(R.id.buttonWorking);
        buttonCurrentOrder = findViewById(R.id.buttonCurrentOrder);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
    }

    private void setStatusBarColor() {
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.blue_toolbar));
    }
}