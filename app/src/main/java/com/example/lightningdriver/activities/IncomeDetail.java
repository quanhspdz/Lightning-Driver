package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.adapters.IncomeDetailAdapter;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Calculator;
import com.example.lightningdriver.tools.Const;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class IncomeDetail extends AppCompatActivity {

    RecyclerView recyclerViewOrders;
    RelativeLayout layoutBack;
    List<Trip> listTrips;
    String ordersDay;
    IncomeDetailAdapter incomeDetailAdapter;
    TextView textTotalMoney, textDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_detail);

        init();
        getListTrip();
        listener();
    }

    private void listener() {
        layoutBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IncomeDetail.this, DailyIncome.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });
    }

    private void getListTrip() {
        Intent intent = getIntent();
        ordersDay = intent.getStringExtra("Day");

        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd yyyy");
        Date date = new Date();
        if (ordersDay == null) {
            ordersDay = formatter.format(date);
        } else if (!ordersDay.equals(formatter.format(date))) {
            textDay.setText(ordersDay);
        }

        FirebaseDatabase.getInstance().getReference()
                .child("Trips")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listTrips.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Trip trip = dataSnapshot.getValue(Trip.class);
                            if (checkTrip(trip, ordersDay)) {
                                listTrips.add(trip);
                            }
                        }
                        incomeDetailAdapter.notifyDataSetChanged();
                        textTotalMoney.setText(Calculator.calculateTotalMoney(listTrips, getApplicationContext()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private boolean checkTrip(Trip trip, String ordersDay) {
        if (trip == null ) {
            return false;
        } else if (trip.getDriverId() == null) {
            return false;
        } else if (!trip.getDriverId().equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())) {
            return false;
        } else if (!trip.getStatus().equals(Const.success)) {
            return false;
        } else {
            String time = Calculator.getShortTime(trip.getCreateTime());
            return time.equals(ordersDay);
        }
    }

    private void init() {
        textTotalMoney = findViewById(R.id.text_total_money);
        textDay = findViewById(R.id.text_day);
        layoutBack = findViewById(R.id.layout_back);

        listTrips = new ArrayList<>();
        incomeDetailAdapter = new IncomeDetailAdapter(listTrips, this);

        recyclerViewOrders = findViewById(R.id.recycler_orders);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewOrders.setLayoutManager(linearLayoutManager);
        recyclerViewOrders.setAdapter(incomeDetailAdapter);
    }
}