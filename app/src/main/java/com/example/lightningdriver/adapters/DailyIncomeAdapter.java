package com.example.lightningdriver.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lightningdriver.R;
import com.example.lightningdriver.activities.IncomeDetail;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.tools.Calculator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
        String day = listDays.get(position);

        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd yyyy");
        Date date = new Date();
        String strNow = formatter.format(date);
        if (strNow.equals(listDays.get(position))) {
            holder.textTime.setText("Today");
            holder.textTime.setTextColor(context.getResources().getColor(R.color.green));
        } else {
            holder.textTime.setText(listDays.get(position));
        }

        if (listTrips != null) {
            if (!listTrips.isEmpty()) {
                if (listTrips.size() == 1) {
                    holder.textNumOrders.setText(listTrips.size() + " order completed");
                } else {
                    holder.textNumOrders.setText(listTrips.size() + " orders completed");
                }
                String totalMoney = Calculator.calculateTotalMoney(listTrips, context);
                holder.textMoney.setText(totalMoney);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, IncomeDetail.class);
                intent.putExtra("Day", day);
                context.startActivity(intent);
            }
        });
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
