package com.example.quickride.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.models.RideHistory;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying ride history items with map preview
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<RideHistory> historyList;
    private final Context context;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onItemClick(RideHistory ride, int position);
    }

    public HistoryAdapter(List<RideHistory> historyList, Context context, OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.context = context;
        this.listener = listener;
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
        holder.bind(ride);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(ride, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    /**
     * ViewHolder for premium history items
     */
    class ViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {

        TextView tvRideId, tvTime, tvCar, tvPrice;
        TextView tvPickup, tvDestination;
        TextView tvStatusBadge, tvPaymentBadge;
        MapView mapView;
        GoogleMap googleMap;
        CardView cardView;
        View maskLayout;
        RideHistory currentRide;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvRideId        = itemView.findViewById(R.id.rideId);
            tvTime          = itemView.findViewById(R.id.time);
            tvCar           = itemView.findViewById(R.id.car);
            tvPrice         = itemView.findViewById(R.id.price);
            tvPickup        = itemView.findViewById(R.id.pickupText);
            tvDestination   = itemView.findViewById(R.id.destinationText);
            tvStatusBadge   = itemView.findViewById(R.id.statusBadge);
            tvPaymentBadge  = itemView.findViewById(R.id.paymentBadge);
            cardView        = itemView.findViewById(R.id.card_view);
            maskLayout      = itemView.findViewById(R.id.mask_layout);
            mapView         = itemView.findViewById(R.id.map);

            // Initialize MapView
            if (mapView != null) {
                mapView.onCreate(null);
                mapView.getMapAsync(this);
            }
        }

        void bind(RideHistory ride) {
            this.currentRide = ride;

            // ── Ride ID ──────────────────────────────────────────
            if (tvRideId != null) {
                String rideId = ride.getRideId();
                tvRideId.setText(rideId != null && rideId.length() > 8
                        ? "#" + rideId.substring(0, 8).toUpperCase()
                        : (rideId != null ? "#" + rideId.toUpperCase() : "#RIDE"));
            }

            // ── Date & Time ───────────────────────────────────────
            if (tvTime != null) {
                if (ride.getTimestamp() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault());
                    tvTime.setText(sdf.format(new Date(ride.getTimestamp())));
                } else {
                    tvTime.setText("Date unknown");
                }
            }

            // ── Vehicle / Car ────────────────────────────────────
            if (tvCar != null) {
                String car = ride.getCarInfo();
                tvCar.setText(car != null && !car.isEmpty() ? capitalize(car) : "Vehicle");
            }

            // ── Fare ─────────────────────────────────────────────
            if (tvPrice != null) {
                tvPrice.setText(ride.getFare() > 0
                        ? String.format(Locale.getDefault(), "Rs. %.0f", ride.getFare())
                        : "Rs. --");
            }

            // ── Pickup Address ───────────────────────────────────
            if (tvPickup != null) {
                String pickup = ride.getPickupAddress();
                tvPickup.setText(pickup != null && !pickup.isEmpty() ? pickup : "Pickup location");
            }

            // ── Destination Address ───────────────────────────────
            if (tvDestination != null) {
                String dest = ride.getDestinationAddress();
                tvDestination.setText(dest != null && !dest.isEmpty() ? dest : "Destination");
            }

            // ── Status Badge ────────────────────────────────────
            if (tvStatusBadge != null) {
                String status = ride.getStatus();
                if (status == null) status = "completed";
                switch (status.toLowerCase()) {
                    case "completed":
                        tvStatusBadge.setText("✓ Completed");
                        tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                        break;
                    case "cancelled":
                        tvStatusBadge.setText("✕ Cancelled");
                        tvStatusBadge.setBackgroundColor(Color.parseColor("#EF4444"));
                        break;
                    default:
                        tvStatusBadge.setText(capitalize(status));
                        tvStatusBadge.setBackgroundColor(Color.parseColor("#F59E0B"));
                        break;
                }
            }

            // ── Payment Badge ─────────────────────────────────────
            if (tvPaymentBadge != null) {
                String pm = ride.getPaymentMethod();
                if (pm == null || pm.isEmpty()) pm = "Cash";
                switch (pm.toLowerCase()) {
                    case "jazzcash":
                        tvPaymentBadge.setText("JazzCash");
                        tvPaymentBadge.setTextColor(Color.parseColor("#ED1C24"));
                        break;
                    case "easypaisa":
                        tvPaymentBadge.setText("EasyPaisa");
                        tvPaymentBadge.setTextColor(Color.parseColor("#107C10"));
                        break;
                    default:
                        tvPaymentBadge.setText("Cash");
                        tvPaymentBadge.setTextColor(Color.parseColor("#38A169"));
                        break;
                }
            }

            // Set map tag for later use
            if (mapView != null) {
                mapView.setTag(ride);
            }

            // Update map if already ready
            if (googleMap != null) {
                setMapLocation(ride);
            }
        }

        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            this.googleMap = googleMap;

            MapsInitializer.initialize(context);

            // Disable all gestures (static preview)
            googleMap.getUiSettings().setAllGesturesEnabled(false);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            if (currentRide != null) {
                setMapLocation(currentRide);
            } else if (mapView != null && mapView.getTag() instanceof RideHistory) {
                setMapLocation((RideHistory) mapView.getTag());
            }
        }

        /**
         * Set markers for pickup and destination on the map
         */
        private void setMapLocation(RideHistory ride) {
            if (googleMap == null || ride == null) return;
            if (ride.getPickupLat() == 0 && ride.getPickupLng() == 0) return;

            googleMap.clear();

            LatLng pickupLatLng = new LatLng(ride.getPickupLat(), ride.getPickupLng());
            LatLng destLatLng   = new LatLng(ride.getDestLat(), ride.getDestLng());

            // Pickup marker (green)
            googleMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Pickup")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Destination marker (red)
            googleMap.addMarker(new MarkerOptions()
                    .position(destLatLng)
                    .title("Destination")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Fit both markers in view
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickupLatLng);
            builder.include(destLatLng);
            LatLngBounds bounds = builder.build();

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60));
            googleMap.getUiSettings().setAllGesturesEnabled(false);
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
        }
    }
}