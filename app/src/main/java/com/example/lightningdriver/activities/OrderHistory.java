package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.example.lightningdriver.R;
import com.example.lightningdriver.adapters.OrderHistoryAdapter;
import com.example.lightningdriver.models.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderHistory extends AppCompatActivity {

    RecyclerView recyclerViewOrders;
    List<Trip> listTrips;
    OrderHistoryAdapter orderHistoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        init();
        getListTrips();
    }

    private void getListTrips() {
        String driverId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseDatabase.getInstance().getReference()
                .child("Trips")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listTrips.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Trip trip = dataSnapshot.getValue(Trip.class);
                            if (trip != null) {
                                if (trip.getDriverId() != null) {
                                    if (trip.getDriverId().equals(driverId)) {
                                        listTrips.add(trip);
                                    }
                                }
                            }
                        }
                        orderHistoryAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void init() {
        recyclerViewOrders = findViewById(R.id.recycler_orders);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewOrders.setLayoutManager(linearLayoutManager);

        listTrips = new ArrayList<>();
        orderHistoryAdapter = new OrderHistoryAdapter(listTrips, this);
        recyclerViewOrders.setAdapter(orderHistoryAdapter);
    }
}