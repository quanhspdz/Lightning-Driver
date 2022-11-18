package com.example.lightningdriver.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Calculator;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class DailyIncomeAdapter extends RecyclerView.Adapter<DailyIncomeAdapter.ViewHolder> {
    Context context;
    List<String> listDays;
    HashMap<String, List<Trip>> listOrdersMap;

    public DailyIncomeAdapter(Context context, List<String> listDays, HashMap<String, List<Trip>> listOrdersMap) {
        this.context = context;
        this.listDays = listDays;
        this.listOrdersMap = listOrdersMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.each_day_income_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        List<Trip> listTrips = listOrdersMap.get(listDays.get(position));
        holder.textTime.setText(listDays.get(position));
        if (listTrips != null) {
            if (!listTrips.isEmpty()) {
                holder.textNumOrders.setText(listTrips.size() + " orders complete");
//                String totalMoney = Calculator.calculateTotalMoney(listTrips);
//                holder.textMoney.setText("đ " + totalMoney);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listDays.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView textTime, textMoney, textNumOrders;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textTime = itemView.findViewById(R.id.text_time);
            textMoney = itemView.findViewById(R.id.text_money);
            textNumOrders = itemView.findViewById(R.id.text_orders_number);
        }
    }
}
