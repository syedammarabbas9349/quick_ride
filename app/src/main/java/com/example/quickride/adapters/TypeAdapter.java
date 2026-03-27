package com.example.quickride.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.models.ServiceType;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying vehicle types (Economy, Premium, XL)
 */
public class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {

    private boolean[] sharingStates;
    private Context context;
    private List<ServiceType> typeList;
    private ServiceType selectedItem;
    private ArrayList<Double> routeData;
    private OnTypeSelectedListener listener;

    public interface OnTypeSelectedListener {
        void onTypeSelected(ServiceType type, int position, boolean sharingEnabled);
    }

    public TypeAdapter(List<ServiceType> typeList, Context context,
                       ArrayList<Double> routeData, OnTypeSelectedListener listener) {
        this.typeList = typeList;
        this.context = context;
        this.routeData = routeData;
        this.listener = listener;
        this.sharingStates = new boolean[typeList.size()];

        if (typeList != null && !typeList.isEmpty()) {
            this.selectedItem = typeList.get(0);
            this.sharingStates[0] = false;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceType type = typeList.get(position);
        boolean sharingEnabled = sharingStates[position];
        holder.bind(type, position, sharingEnabled);

        // Handle sharing switch
        holder.sharingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharingStates[position] = isChecked;
            holder.bind(type, position, isChecked);

            if (type.equals(selectedItem) && listener != null) {
                listener.onTypeSelected(type, position, isChecked);
            }
        });

        // Handle item selection
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = typeList.indexOf(selectedItem);
            selectedItem = type;
            notifyItemChanged(previousPosition);
            notifyItemChanged(position);

            if (listener != null) {
                listener.onTypeSelected(type, position, sharingStates[position]);
            }
        });
    }

    @Override
    public int getItemCount() {
        return typeList.size();
    }

    public boolean isSharingEnabled() {
        if (selectedItem == null) return false;
        int position = typeList.indexOf(selectedItem);
        return position >= 0 && sharingStates[position];
    }

    public ServiceType getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(ServiceType selectedItem) {
        this.selectedItem = selectedItem;
        notifyDataSetChanged();
    }

    public void setRouteData(ArrayList<Double> routeData) {
        this.routeData = routeData;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for vehicle type items
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivVehicleIcon, ivSelectedIcon;
        TextView tvVehicleName, tvCapacity, tvPrice, tvEta, tvSharingDiscount;
        SwitchMaterial sharingSwitch;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivVehicleIcon = itemView.findViewById(R.id.vehicleIcon);
            ivSelectedIcon = itemView.findViewById(R.id.selectedIcon);
            tvVehicleName = itemView.findViewById(R.id.vehicleName);
            tvCapacity = itemView.findViewById(R.id.capacity);
            tvPrice = itemView.findViewById(R.id.typePrice);
            tvEta = itemView.findViewById(R.id.eta);
            sharingSwitch = itemView.findViewById(R.id.sharingSwitch);
            tvSharingDiscount = itemView.findViewById(R.id.sharingDiscount);
        }

        void bind(ServiceType type, int position, boolean sharingEnabled) {
            // Set vehicle name
            tvVehicleName.setText(type.getName());

            // Set vehicle icon based on type
            switch (type.getVehicleType().toLowerCase()) {
                case "economy":
                    ivVehicleIcon.setImageResource(R.drawable.ic_economy_car);
                    break;
                case "premium":
                    ivVehicleIcon.setImageResource(R.drawable.ic_premium_car);
                    break;
                case "xl":
                    ivVehicleIcon.setImageResource(R.drawable.ic_suv);
                    break;
                case "bike":
                    ivVehicleIcon.setImageResource(R.drawable.ic_bike);
                    break;
                default:
                    ivVehicleIcon.setImageResource(R.drawable.ic_car);
                    break;
            }

            // Calculate fares
            double originalFare = calculateFare(type);
            double sharingFare = originalFare * (1 - type.getSharingDiscount());

            // Set price based on sharing state
            if (sharingEnabled) {
                tvPrice.setText(String.format("Rs. %.0f", sharingFare));
                tvSharingDiscount.setText(type.getSharingDiscountText());
                tvSharingDiscount.setVisibility(View.VISIBLE);
                tvCapacity.setText("Shared • " + type.getMaxSharedPassengers() + " seats");
                int eta = calculateEta();
                tvEta.setText((eta + 10) + " min");
            } else {
                tvPrice.setText(String.format("Rs. %.0f", originalFare));
                tvSharingDiscount.setVisibility(View.GONE);
                tvCapacity.setText(type.getCapacity() + " seats");
                int eta = calculateEta();
                tvEta.setText(eta + " min");
            }

            // Set sharing switch state
            sharingSwitch.setChecked(sharingEnabled);

            // Highlight selected item
            if (type.equals(selectedItem)) {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.selected_item_bg));
                cardView.setCardElevation(8);
                ivSelectedIcon.setVisibility(View.VISIBLE);
            } else {
                cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.white));
                cardView.setCardElevation(4);
                ivSelectedIcon.setVisibility(View.GONE);
            }
        }

        private double calculateFare(ServiceType type) {
            double baseFare = type.getBaseFare() > 0 ? type.getBaseFare() : 50;
            double perKmRate = type.getPricePerKm() > 0 ? type.getPricePerKm() : 15;
            double distance = 5.0;

            if (routeData != null && routeData.size() > 0 && routeData.get(0) != null) {
                distance = routeData.get(0);
            }

            double fare = baseFare + (distance * perKmRate);
            double minimumFare = type.getMinimumFare() > 0 ? type.getMinimumFare() : 100;

            return Math.max(fare, minimumFare);
        }

        private int calculateEta() {
            if (routeData != null && routeData.size() > 1 && routeData.get(1) != null) {
                return (int) Math.ceil(routeData.get(1) / 60);
            }
            return 5;
        }
    }
}