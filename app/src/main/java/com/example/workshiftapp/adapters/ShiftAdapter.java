package com.example.workshiftapp.adapters;


import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshiftapp.R;
import com.example.workshiftapp.models.CardShift;

import java.util.ArrayList;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.myViewHolder> {
    private ArrayList<CardShift> arrLst;
    private OnItemClickListener listener;
    // Define an interface for click events
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    // Constructor to initialize the adapter
    public ShiftAdapter(ArrayList<CardShift> arr) {
        this.arrLst = arr;
    }
    public class myViewHolder extends RecyclerView.ViewHolder {
        TextView day;
        TextView date;
        TextView start;
        TextView end;
        TextView hours;
        public myViewHolder(View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.Card_Day);
            date = itemView.findViewById(R.id.Card_Date);
            start = itemView.findViewById(R.id.Card_Start_Time);
            end = itemView.findViewById(R.id.Card_End_Time);
            hours = itemView.findViewById(R.id.Card_Hours);
            // Set click listener on itemView
            itemView.setOnClickListener(view -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
    @NonNull
    @Override
    public ShiftAdapter.myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shift_card_view, parent, false);
        return new myViewHolder(view);
    }
    @Override
    public int getItemCount() {
        return arrLst.size();
    }
    @Override
    public void onBindViewHolder(@NonNull ShiftAdapter.myViewHolder holder, @SuppressLint("RecyclerView") int position) {
        // Bind data from the list
        holder.day.setText(arrLst.get(position).getDay());
        holder.date.setText(arrLst.get(position).getDate());
        holder.start.setText(arrLst.get(position).getStartTime());
        holder.end.setText(arrLst.get(position).getEndTime());
        holder.hours.setText(arrLst.get(position).getHours());
    }
}
