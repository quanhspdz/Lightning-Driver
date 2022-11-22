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
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Driver;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Const;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    FrameLayout btnWorking, buttonCurrentOrder, buttonHistory, buttonIncome;
    CircleImageView imageProfile;

    ProgressDialog progressDialog;

    public static boolean pickUpActivityIsStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setStatusBarColor();
        init();
        getDriverInfo();
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
                FirebaseDatabase.getInstance().getReference().child("Trips")
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    Trip trip = dataSnapshot.getValue(Trip.class);
                                    if (trip != null) {
                                        if (trip.getDriverId() != null) {
                                            if (trip.getDriverId().equals(driverId)
                                                    && !trip.getStatus().equals(Const.searching)
                                                    && !trip.getStatus().equals(Const.driverFound)
                                                    && !trip.getStatus().equals(Const.waitingForAccept)
                                                    && !trip.getStatus().equals(Const.canceled)
                                                    && !trip.getStatus().equals(Const.success)
                                                    && !trip.getStatus().equals(Const.cancelByPassenger)
                                                    && !trip.getStatus().equals(Const.cancelByDriver)
                                                    && !PickUpActivity.isRunning) {
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
        });

        imageProfile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Sign out", Toast.LENGTH_SHORT).show();
                finish();
                return false;
            }
        });

        buttonIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DailyIncome.class);
                startActivity(intent);
            }
        });

        buttonHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OrderHistory.class);
                startActivity(intent);
            }
        });
    }

    private void init() {
        btnWorking = findViewById(R.id.buttonWorking);
        buttonCurrentOrder = findViewById(R.id.buttonCurrentOrder);
        buttonHistory = findViewById(R.id.buttonHistory);
        buttonIncome = findViewById(R.id.buttonInCome);
        imageProfile = findViewById(R.id.img_profile);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
    }

    private void getDriverInfo() {
        FirebaseDatabase.getInstance().getReference()
                .child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Driver driver = snapshot.getValue(Driver.class);
                        if (driver != null) {
                            Picasso.get().load(driver.getDriverImageUrl())
                                    .resize(1000, 1000)
                                            .centerCrop().into(imageProfile);

                            checkVehicleRegistration(driver);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkVehicleRegistration(Driver driver) {
        if (driver.getVehicleId() == null) {
            Intent intent = new Intent(MainActivity.this, VehicleRegistrationActivity.class);
            startActivity(intent);
            finish();
        }
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