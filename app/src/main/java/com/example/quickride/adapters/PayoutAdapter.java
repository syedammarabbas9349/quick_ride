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
import com.example.quickride.models.Payout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying driver payout history
 */
public class PayoutAdapter extends RecyclerView.Adapter<PayoutAdapter.ViewHolder> {

    private List<Payout> payoutList;
    private OnPayoutActionListener listener;
    private Context context;

    public interface OnPayoutActionListener {
        void onPayoutClick(Payout payout, int position);
        void onRequestWithdraw(Payout payout, int position);
    }

    public PayoutAdapter(List<Payout> payoutList, Context context, OnPayoutActionListener listener) {
        this.payoutList = payoutList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Payout payout = payoutList.get(position);
        holder.bind(payout, position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPayoutClick(payout, position);
            }
        });

        holder.btnWithdraw.setOnClickListener(v -> {
            if (listener != null && "available".equals(payout.getStatus())) {
                listener.onRequestWithdraw(payout, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return payoutList.size();
    }

    /**
     * ViewHolder for payout items
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvPeriod, tvAmount, tvRideCount, tvStatus, tvWithdrawDate;
        ImageView ivStatusIcon;
        MaterialButton btnWithdraw;
        View statusIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvPeriod = itemView.findViewById(R.id.period);
            tvAmount = itemView.findViewById(R.id.amount);
            tvRideCount = itemView.findViewById(R.id.rideCount);
            tvStatus = itemView.findViewById(R.id.status);
            tvWithdrawDate = itemView.findViewById(R.id.withdrawDate);
            ivStatusIcon = itemView.findViewById(R.id.statusIcon);
            btnWithdraw = itemView.findViewById(R.id.btnWithdraw);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }

        void bind(Payout payout, int position) {
            // Format currency
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));
            String formattedAmount = "Rs. " + String.format(Locale.getDefault(), "%.0f", payout.getAmount());

            // Set basic info
            tvPeriod.setText(payout.getPeriod() != null ? payout.getPeriod() : "Week");
            tvAmount.setText(formattedAmount);
            tvRideCount.setText(payout.getRideCount() + " " +
                    (payout.getRideCount() == 1 ? "ride" : "rides"));

            // Set status
            tvStatus.setText(payout.getStatus() != null ?
                    payout.getStatus().substring(0, 1).toUpperCase() +
                            payout.getStatus().substring(1) : "Unknown");

            // Configure based on status
            configureForStatus(payout);

            // Set withdraw button visibility
            if ("available".equals(payout.getStatus()) && payout.getAmount() > 0) {
                btnWithdraw.setVisibility(View.VISIBLE);
            } else {
                btnWithdraw.setVisibility(View.GONE);
            }

            // Format date if available
            if (payout.getProcessedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvWithdrawDate.setText("Processed: " + sdf.format(new Date(payout.getProcessedAt())));
                tvWithdrawDate.setVisibility(View.VISIBLE);
            } else if (payout.getRequestedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvWithdrawDate.setText("Requested: " + sdf.format(new Date(payout.getRequestedAt())));
                tvWithdrawDate.setVisibility(View.VISIBLE);
            } else {
                tvWithdrawDate.setVisibility(View.GONE);
            }
        }

        private void configureForStatus(Payout payout) {
            String status = payout.getStatus() != null ? payout.getStatus().toLowerCase() : "";

            switch (status) {
                case "available":
                    statusIndicator.setBackgroundColor(
                            itemView.getContext().getColor(R.color.green_500));
                    ivStatusIcon.setImageResource(R.drawable.ic_available);
                    ivStatusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.green_500));
                    cardView.setStrokeColor(
                            itemView.getContext().getColor(R.color.green_500));
                    break;

                case "pending":
                    statusIndicator.setBackgroundColor(
                            itemView.getContext().getColor(R.color.orange_500));
                    ivStatusIcon.setImageResource(R.drawable.ic_pending);
                    ivStatusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.orange_500));
                    cardView.setStrokeColor(
                            itemView.getContext().getColor(R.color.orange_500));
                    break;

                case "completed":
                case "paid":
                    statusIndicator.setBackgroundColor(
                            itemView.getContext().getColor(R.color.blue_500));
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.blue_500));
                    cardView.setStrokeColor(
                            itemView.getContext().getColor(R.color.blue_500));
                    break;

                default:
                    statusIndicator.setBackgroundColor(
                            itemView.getContext().getColor(R.color.grey_500));
                    ivStatusIcon.setImageResource(R.drawable.ic_info);
                    ivStatusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.grey_500));
                    cardView.setStrokeColor(
                            itemView.getContext().getColor(R.color.grey_500));
                    break;
            }
        }
    }
}