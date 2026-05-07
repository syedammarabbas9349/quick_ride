package com.example.quickride.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.models.SharedPassenger;

import java.util.List;

public class PassengerAdapter extends RecyclerView.Adapter<PassengerAdapter.ViewHolder> {

    private List<SharedPassenger> passengers;

    public PassengerAdapter(List<SharedPassenger> passengers) {
        this.passengers = passengers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_passenger, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SharedPassenger passenger = passengers.get(position);

        holder.tvPassengerName.setText(passenger.getName());
        holder.tvPickupLocation.setText("Pickup: " + passenger.getPickupAddress());
        holder.tvDropoffLocation.setText("Dropoff: " + passenger.getDropoffAddress());
        holder.tvFareShare.setText(String.format("Rs. %.0f", passenger.getFareShare()));

        // Status badge styling
        if ("pending".equals(passenger.getStatus())) {
            holder.tvStatus.setText("Waiting");
            holder.tvStatus.setBackgroundColor(holder.itemView.getContext().getColor(R.color.orange_500));
        } else if ("onboard".equals(passenger.getStatus())) {
            holder.tvStatus.setText("Onboard");
            holder.tvStatus.setBackgroundColor(holder.itemView.getContext().getColor(R.color.green_500));
        } else {
            holder.tvStatus.setText("Dropped");
            holder.tvStatus.setBackgroundColor(holder.itemView.getContext().getColor(R.color.grey_500));
        }

        // Load profile image
        if (passenger.getProfileImageUrl() != null && !passenger.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(passenger.getProfileImageUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile)
                    .into(holder.ivPassengerImage);
        }
    }

    @Override
    public int getItemCount() {
        return passengers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPassengerImage;
        TextView tvPassengerName, tvPickupLocation, tvDropoffLocation, tvFareShare, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPassengerImage = itemView.findViewById(R.id.ivPassengerImage);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvPickupLocation = itemView.findViewById(R.id.tvPickupLocation);
            tvDropoffLocation = itemView.findViewById(R.id.tvDropoffLocation);
            tvFareShare = itemView.findViewById(R.id.tvFareShare);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}