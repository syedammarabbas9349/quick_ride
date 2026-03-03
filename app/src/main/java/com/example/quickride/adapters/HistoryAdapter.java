package com.example.quickride.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.history.HistorySingleActivity;
import com.example.quickride.models.RideHistory;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying ride history items with map preview
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<RideHistory> historyList;
    private Context context;

    public HistoryAdapter(List<RideHistory> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideHistory ride = historyList.get(position);
        holder.bind(ride, position);

        // Click listener to open details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("rideId", ride.getRideId());
            intent.putExtras(bundle);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    /**
     * ViewHolder for history items with map preview
     */
    class ViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {

        TextView tvRideId, tvDateTime, tvCar, tvPrice;
        MapView mapView;
        GoogleMap googleMap;
        CardView cardView;
        View maskLayout;
        RideHistory currentRide;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvRideId = itemView.findViewById(R.id.rideId);
            tvDateTime = itemView.findViewById(R.id.time);
            tvCar = itemView.findViewById(R.id.car);
            tvPrice = itemView.findViewById(R.id.price);
            cardView = itemView.findViewById(R.id.card_view);
            maskLayout = itemView.findViewById(R.id.mask_layout);
            mapView = itemView.findViewById(R.id.map);

            // Initialize MapView
            if (mapView != null) {
                mapView.onCreate(null);
                mapView.getMapAsync(this);
            }
        }

        void bind(RideHistory ride, int position) {
            this.currentRide = ride;

            // Set text data
            tvRideId.setText(ride.getRideId() != null ?
                    ride.getRideId().substring(0, Math.min(8, ride.getRideId().length())) : "Ride");

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvDateTime.setText(sdf.format(new Date(ride.getTimestamp())));

            // Set car info
            tvCar.setText(ride.getCarInfo() != null ? ride.getCarInfo() : "Vehicle");

            // Set price
            tvPrice.setText(String.format(Locale.getDefault(),
                    "Rs. %.0f", ride.getFare()));

            // Set map tag with ride data for later use
            mapView.setTag(ride);

            // Update map if already ready
            if (googleMap != null) {
                setMapLocation(ride);
            }
        }

        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            this.googleMap = googleMap;

            // Initialize maps
            MapsInitializer.initialize(context);

            // Disable all gestures (for static preview)
            googleMap.getUiSettings().setAllGesturesEnabled(false);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);

            // Set map type to normal (default)
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            // Set location if data exists
            if (currentRide != null) {
                setMapLocation(currentRide);
            } else if (mapView.getTag() instanceof RideHistory) {
                setMapLocation((RideHistory) mapView.getTag());
            }
        }

        /**
         * Set markers for pickup and destination on the map
         */
        private void setMapLocation(RideHistory ride) {
            if (googleMap == null || ride == null) return;

            // Clear existing markers
            googleMap.clear();

            // Create LatLng objects
            LatLng pickupLatLng = new LatLng(ride.getPickupLat(), ride.getPickupLng());
            LatLng destLatLng = new LatLng(ride.getDestLat(), ride.getDestLng());

            // Add pickup marker
            googleMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Pickup")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start)));

            // Add destination marker
            googleMap.addMarker(new MarkerOptions()
                    .position(destLatLng)
                    .title("Destination")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_finish)));

            // Build bounds to show both markers
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickupLatLng);
            builder.include(destLatLng);
            LatLngBounds bounds = builder.build();

            // Move camera with padding
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

            // Ensure gestures stay disabled
            googleMap.getUiSettings().setAllGesturesEnabled(false);
        }
    }
}