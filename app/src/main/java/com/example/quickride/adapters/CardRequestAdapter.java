package com.example.quickride.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.models.RideRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying ride requests to drivers
 */
public class CardRequestAdapter extends RecyclerView.Adapter<CardRequestAdapter.ViewHolder> {

    private Context context;
    private List<RideRequest> rideRequests;
    private OnRequestActionListener listener;
    private Handler progressHandler = new Handler(Looper.getMainLooper());

    public interface OnRequestActionListener {
        void onAccept(RideRequest request, int position);
        void onDecline(RideRequest request, int position);
    }

    public CardRequestAdapter(List<RideRequest> rideRequests, OnRequestActionListener listener) {
        this.rideRequests = rideRequests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_ride_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest request = rideRequests.get(position);
        holder.bind(request, position);

        // Start progress animation for this item
        startProgressAnimation(holder, request, position);

        // Accept button click
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(request, position);
            }
        });

        // Decline button click
        holder.btnDecline.setOnClickListener(v -> {
            stopProgressAnimation(holder);
            if (listener != null) {
                listener.onDecline(request, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rideRequests.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        stopProgressAnimation(holder);
    }

    private void startProgressAnimation(ViewHolder holder, RideRequest request, int position) {
        // Cancel any existing runnable for this holder
        stopProgressAnimation(holder);

        // Create new progress runnable
        Runnable progressRunnable = new Runnable() {
            float progress = 0;

            @Override
            public void run() {
                if (position >= rideRequests.size() || !rideRequests.get(position).equals(request)) {
                    return;
                }

                progress += 0.5f;
                holder.progressBar.setProgress((int) progress);

                // Update time text
                int secondsLeft = (int) ((100 - progress) * 0.3); // Convert progress to seconds
                holder.tvTimeLeft.setText(String.format(Locale.getDefault(), "%ds", secondsLeft));

                if (progress < 100) {
                    progressHandler.postDelayed(this, 150);
                } else {
                    // Auto-decline when progress reaches 100
                    if (listener != null) {
                        listener.onDecline(request, position);
                    }
                }
            }
        };

        holder.progressRunnable = progressRunnable;
        progressHandler.post(progressRunnable);
    }

    private void stopProgressAnimation(ViewHolder holder) {
        if (holder.progressRunnable != null) {
            progressHandler.removeCallbacks(holder.progressRunnable);
            holder.progressRunnable = null;
        }
    }

    /**
     * ViewHolder for ride request items
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        // Customer info
        ImageView ivCustomerImage;
        TextView tvCustomerName, tvRating;

        // Ride details
        TextView tvPickupLocation, tvDestination, tvDistance, tvFare;

        // Time and progress
        CircularProgressIndicator progressBar;
        TextView tvTimeLeft;

        // Action buttons
        MaterialButton btnAccept, btnDecline;

        // Card view
        CardView cardView;

        // Runnable for progress animation
        Runnable progressRunnable;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Customer info
            ivCustomerImage = itemView.findViewById(R.id.customerImage);
            tvCustomerName = itemView.findViewById(R.id.customerName);
            tvRating = itemView.findViewById(R.id.ratingText);

            // Ride details
            tvPickupLocation = itemView.findViewById(R.id.pickupLocation);
            tvDestination = itemView.findViewById(R.id.destination);
            tvDistance = itemView.findViewById(R.id.distance);
            tvFare = itemView.findViewById(R.id.fare);

            // Time and progress
            progressBar = itemView.findViewById(R.id.progressBar);
            tvTimeLeft = itemView.findViewById(R.id.timeLeft);

            // Action buttons
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);

            // Card view
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(RideRequest request, int position) {
            // Set customer info
            tvCustomerName.setText(request.getCustomerName() != null ?
                    request.getCustomerName() : "Customer");

            // Set rating
            double rating = request.getCustomerRating() != null ?
                    request.getCustomerRating() : 4.5;
            tvRating.setText(String.format(Locale.getDefault(), "%.1f", rating));

            // Set ride details
            tvPickupLocation.setText(request.getPickupAddress() != null ?
                    request.getPickupAddress() : "Pickup location");
            tvDestination.setText(request.getDestinationAddress() != null ?
                    request.getDestinationAddress() : "Destination");

            // Set distance and fare
            if (request.getDistance() > 0) {
                tvDistance.setText(String.format(Locale.getDefault(),
                        "%.1f km", request.getDistance()));
            } else {
                tvDistance.setText("-- km");
            }

            if (request.getFare() > 0) {
                tvFare.setText(String.format(Locale.getDefault(),
                        "Rs. %.0f", request.getFare()));
            } else {
                tvFare.setText("Rs. --");
            }

            // Load customer image
            if (request.getCustomerImageUrl() != null &&
                    !request.getCustomerImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(request.getCustomerImageUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivCustomerImage);
            } else {
                ivCustomerImage.setImageResource(R.drawable.default_profile);
            }

            // Reset progress bar
            progressBar.setProgress(0);
            tvTimeLeft.setText("30s");
        }
    }
}