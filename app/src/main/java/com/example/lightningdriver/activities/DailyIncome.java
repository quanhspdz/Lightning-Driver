package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.adapters.DailyIncomeAdapter;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Calculator;
import com.example.lightningdriver.tools.Const;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DailyIncome extends AppCompatActivity {

    RecyclerView recyclerViewDailyOrders;
    HashMap<String, List<Trip>> dailyOrderMap;
    List<String> listDays;
    DailyIncomeAdapter dailyIncomeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_icome);

        init();
        getListTrips();
    }

    private void getListTrips() {
        FirebaseDatabase.getInstance().getReference()
                .child("Trips")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        dailyOrderMap.clear();
                        listDays.clear();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Trip trip = dataSnapshot.getValue(Trip.class);
                            if (trip != null) {
                                if (checkTripInfo(trip)) {
                                    String time = Calculator.getShortTime(trip.getCreateTime());
                                    if (dailyOrderMap.containsKey(time)) {
                                        Objects.requireNonNull(dailyOrderMap.get(time)).add(trip);
                                    } else {
                                        dailyOrderMap.put(time, new ArrayList<>());
                                        Objects.requireNonNull(dailyOrderMap.get(time)).add(trip);
                                        listDays.add(time);
                                    }
                                }
                            }
                        }
                        dailyIncomeAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private boolean checkTripInfo(Trip trip) {
        if (trip.getDriverId() == null) {
            return false;
        } else if (!trip.getDriverId().equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())) {
            return false;
        } else return trip.getStatus().equals(Const.success);
    }

    private void init() {
        recyclerViewDailyOrders = findViewById(R.id.recycler_daily_orders);
        dailyOrderMap = new HashMap<>();
        listDays = new ArrayList<>();

        dailyIncomeAdapter = new DailyIncomeAdapter(this, listDays, dailyOrderMap);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewDailyOrders.setLayoutManager(linearLayoutManager);
        recyclerViewDailyOrders.setAdapter(dailyIncomeAdapter);
    }
}