package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Const;
import com.example.lightningdriver.services.MyLocationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class NewTripFoundActivity extends AppCompatActivity {
    TextView textMoney, textDistance, textVehicleType, textPaymentMethod,
                textPickUp, textDropOff, textTimeRemain;

    AppCompatButton buttonAccept, buttonReject;

    Trip trip;

    boolean waitingStatusUpdated = false, searchingStatusUpdated = false, driverFoundUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_trip_found);

        init();
        getTripInfo();
        listener();
    }

    private void listener() {
        buttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!driverFoundUpdated) {
                    driverFoundUpdated = true;
                    updateTripStatusAndDriver(Const.waitingPickUp);
                }

                Intent intent = new Intent(NewTripFoundActivity.this, PickUpActivity.class);
                intent.putExtra("tripId", trip.getId());
                startActivity(intent);
                finish();
            }
        });

        buttonReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!searchingStatusUpdated) {
                    searchingStatusUpdated = true;
                    MyLocationService.isFindingTrip = true;
                    MyLocationService.rejectedTrips.put(trip.getId(), trip);
                    updateTripStatus(Const.searching);
                }
                finish();
            }
        });
    }

    private void updateTripStatus(String status) {
        trip.setStatus(status);

        FirebaseDatabase.getInstance().getReference().child("Trips")
                .child(trip.getId())
                .setValue(trip);
    }

    private void updateTripStatusAndDriver(String status) {
        trip.setStatus(status);
        trip.setDriverId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        FirebaseDatabase.getInstance().getReference().child("Trips")
                .child(trip.getId())
                .setValue(trip);
    }

    private void getTripInfo() {
        Intent intent = getIntent();
        String tripId = intent.getStringExtra("tripId");

        if (tripId != null) {
            FirebaseDatabase.getInstance().getReference().child("Trips")
                    .child(tripId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            trip = snapshot.getValue(Trip.class);
                            if (trip != null) {
                                loadTripInfoData(trip);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }
    }

    private void loadTripInfoData(Trip trip) {
        textMoney.setText(trip.getCost());
        textDistance.setText(trip.getDistance());
        textVehicleType.setText(trip.getVehicleType().toUpperCase());
        textPickUp.setText(trip.getPickUpName());
        textDropOff.setText(trip.getDropOffName());

        startTimeCounter();

        if (!waitingStatusUpdated) {
            updateTripStatus(Const.waitingForAccept);
            waitingStatusUpdated = true;
        }
    }

    private void startTimeCounter() {
        final int[] timeRemain = {15};
        new Thread() {
            public void run() {
                while (timeRemain[0] >= 0) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textTimeRemain.setText("(" + timeRemain[0] + ")");
                            }
                        });
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timeRemain[0]--;
                }
                if (!searchingStatusUpdated && !driverFoundUpdated) {
                    searchingStatusUpdated = true;
                    MyLocationService.isFindingTrip = true;
                    MyLocationService.rejectedTrips.put(trip.getId(), trip);
                    updateTripStatus(Const.searching);
                }
                finish();
            }
        }.start();
    }

    private void init() {
        textMoney = findViewById(R.id.text_total_earn);
        textDistance = findViewById(R.id.text_distance);
        textVehicleType = findViewById(R.id.text_vehicleType);
        textPaymentMethod = findViewById(R.id.text_paymentMethod);
        textPickUp = findViewById(R.id.text_pick_up_location);
        textDropOff = findViewById(R.id.text_drop_off_location);
        textTimeRemain = findViewById(R.id.text_remain_time);
        buttonAccept = findViewById(R.id.button_accept);
        buttonReject = findViewById(R.id.button_reject);
    }
}