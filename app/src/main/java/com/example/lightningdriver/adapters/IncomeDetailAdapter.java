package com.example.lightningdriver.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Calculator;
import com.example.lightningdriver.tools.Const;

import java.util.List;

public class IncomeDetailAdapter extends RecyclerView.Adapter<IncomeDetailAdapter.ViewHolder>{

    List<Trip> listTrips;
    Context context;

    public IncomeDetailAdapter(List<Trip> listTrips, Context context) {
        this.listTrips = listTrips;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.each_order_income_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = listTrips.get(position);
        if (trip.getVehicleType().equals(Const.car)) {
            holder.imgVehicle.setImageResource(R.drawable.car);
        }
        holder.textDistance.setText(trip.getDistance());
        if (trip.getPaymentMethod() != null)
            holder.textPaymentMethod.setText(trip.getPaymentMethod());
        holder.textMoney.setText(trip.getCost());
        holder.textPickUp.setText(trip.getPickUpName());
        holder.textDropOff.setText(trip.getDropOffName());

        String duration = Calculator.getDurationFromTime(trip.getCreateTime(), trip.getEndTime());
        holder.textDuration.setText(duration);
    }

    @Override
    public int getItemCount() {
        return listTrips.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgVehicle;
        TextView textMoney, textDuration, textDistance, textPaymentMethod,
                textPickUp, textDropOff;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgVehicle = itemView.findViewById(R.id.img_vehicle);
            textMoney = itemView.findViewById(R.id.text_money);
            textDuration = itemView.findViewById(R.id.text_duration);
            textDistance = itemView.findViewById(R.id.text_distance);
            textPaymentMethod = itemView.findViewById(R.id.text_paymentMethod);
            textPickUp = itemView.findViewById(R.id.text_pick_up_location);
            textDropOff = itemView.findViewById(R.id.text_drop_off_location);

        }
    }
}
