package com.example.quickride.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.models.Payout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PayoutAdapter extends RecyclerView.Adapter<PayoutAdapter.ViewHolder> {

    private final List<Payout> payoutList;
    private final OnItemClickListener listener;
    private final Context context;

    // Define the interface
    public interface OnItemClickListener {
        void onItemClick(Payout payout, int position);
        void onWithdrawClick(Payout payout, int position);
    }

    public PayoutAdapter(List<Payout> payoutList, Context context, OnItemClickListener listener) {
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
        holder.bind(payout);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(payout, position);
            }
        });

        holder.btnWithdraw.setOnClickListener(v -> {
            if (listener != null && "available".equals(payout.getStatus())) {
                listener.onWithdrawClick(payout, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return payoutList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriod, tvAmount, tvRideCount, tvStatus, tvDate;
        ImageView ivStatusIcon;
        Button btnWithdraw;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvPeriod = itemView.findViewById(R.id.period);
            tvAmount = itemView.findViewById(R.id.amount);
            tvRideCount = itemView.findViewById(R.id.rideCount);
            tvStatus = itemView.findViewById(R.id.status);
            tvDate = itemView.findViewById(R.id.withdrawDate);
            ivStatusIcon = itemView.findViewById(R.id.statusIcon);
            btnWithdraw = itemView.findViewById(R.id.btnWithdraw);
        }

        void bind(Payout payout) {
            tvPeriod.setText(payout.getPeriod() != null ? payout.getPeriod() : "—");
            tvAmount.setText(String.format(Locale.getDefault(), "Rs. %.0f", payout.getAmount()));
            tvRideCount.setText(payout.getRideCount() + " " +
                    (payout.getRideCount() == 1 ? "ride" : "rides"));
            tvStatus.setText(payout.getStatus() != null ?
                    payout.getStatus().substring(0, 1).toUpperCase() +
                            payout.getStatus().substring(1) : "Unknown");

            // Format date
            if (payout.getRequestedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(payout.getRequestedAt())));
                tvDate.setVisibility(View.VISIBLE);
            } else {
                tvDate.setVisibility(View.GONE);
            }

            // Show withdraw button only for available payouts
            if ("available".equals(payout.getStatus()) && payout.getAmount() > 0) {
                btnWithdraw.setVisibility(View.VISIBLE);
            } else {
                btnWithdraw.setVisibility(View.GONE);
            }

            // Set status icon and color based on status
            String status = payout.getStatus() != null ? payout.getStatus().toLowerCase() : "";

            switch (status) {
                case "available":
                    ivStatusIcon.setImageResource(R.drawable.ic_available);
                    ivStatusIcon.setColorFilter(context.getColor(R.color.green_500));
                    tvStatus.setTextColor(context.getColor(R.color.green_500));
                    break;
                case "pending":
                    ivStatusIcon.setImageResource(R.drawable.ic_pending);
                    ivStatusIcon.setColorFilter(context.getColor(R.color.orange_500));
                    tvStatus.setTextColor(context.getColor(R.color.orange_500));
                    break;
                case "completed":
                case "paid":
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(context.getColor(R.color.blue_500));
                    tvStatus.setTextColor(context.getColor(R.color.blue_500));
                    break;
                default:
                    ivStatusIcon.setImageResource(R.drawable.ic_info);
                    ivStatusIcon.setColorFilter(context.getColor(R.color.grey_500));
                    tvStatus.setTextColor(context.getColor(R.color.grey_500));
                    break;
            }
        }
    }
}