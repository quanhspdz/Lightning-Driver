package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
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

public class SettingActivity extends AppCompatActivity {

    CircleImageView imgProfile;
    TextView textName;
    RelativeLayout layoutCurrentOrder, layoutHistory, layoutWallet, layoutBack, layoutIncome;
    AppCompatButton buttonSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        init();
        loadUserData();
        listener();
    }

    private void listener() {
        layoutCurrentOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String driverId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                ProgressDialog progressDialog = new ProgressDialog(SettingActivity.this);
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
                                                Intent intent = new Intent(SettingActivity.this, PickUpActivity.class);
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
                                Toast.makeText(SettingActivity.this, "No current order!", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        });
            }
        });

        layoutHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingActivity.this, OrderHistory.class);
                startActivity(intent);
            }
        });

        layoutWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingActivity.this, WalletActivity.class);
                startActivity(intent);
            }
        });

        layoutBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                finish();
            }
        });

        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(SettingActivity.this, WelcomeActivity.class);
                startActivity(intent);
                Toast.makeText(SettingActivity.this, "Sign out", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        layoutIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingActivity.this, DailyIncome.class);
                startActivity(intent);
            }
        });
    }

    private void loadUserData() {
        FirebaseDatabase.getInstance().getReference()
                .child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Driver driver = snapshot.getValue(Driver.class);
                        if (driver != null) {
                            textName.setText(driver.getName());

                            Picasso.get().load(driver.getDriverImageUrl())
                                    .placeholder(R.drawable.user_blue)
                                    .into(imgProfile);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void init() {
        imgProfile = findViewById(R.id.img_profile);
        textName = findViewById(R.id.text_userName);
        layoutCurrentOrder = findViewById(R.id.layout_currentOrder);
        layoutHistory = findViewById(R.id.layout_History);
        layoutWallet = findViewById(R.id.layout_lWallet);
        buttonSignOut = findViewById(R.id.button_sign_out);
        layoutBack = findViewById(R.id.relative_back);
        layoutIncome = findViewById(R.id.layout_Income);
    }
}