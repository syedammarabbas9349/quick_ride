package com.example.quickride.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.quickride.R;
import com.example.quickride.models.PaymentMethod;
import com.example.quickride.payment.PaymentActivity;

import java.util.List;

/**
 * Adapter for displaying payment methods (JazzCash, EasyPaisa, Cash)
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private Activity activity;
    private List<PaymentMethod> paymentMethods;
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(PaymentMethod paymentMethod, int position);
        void onSetDefault(PaymentMethod paymentMethod, int position);
        void onDelete(PaymentMethod paymentMethod, int position);
    }

    public CardAdapter(List<PaymentMethod> paymentMethods, Activity activity, OnItemClickListener listener) {
        this.paymentMethods = paymentMethods;
        this.activity = activity;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_card, parent, false);
        return new ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentMethod method = paymentMethods.get(position);
        holder.bind(method, position);

        // Highlight selected item
        if (selectedPosition == position) {
            holder.cardView.setCardBackgroundColor(activity.getColor(R.color.selected_item_bg));
            holder.selectedIcon.setVisibility(View.VISIBLE);
        } else {
            holder.cardView.setCardBackgroundColor(activity.getColor(android.R.color.white));
            holder.selectedIcon.setVisibility(View.GONE);
        }

        // Show default badge
        if (method.isDefault()) {
            holder.defaultBadge.setVisibility(View.VISIBLE);
        } else {
            holder.defaultBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onItemClick(method, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDelete(method, position);
            }
            return true;
        });

        holder.setDefaultButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefault(method, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paymentMethods.size();
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for payment method items
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView paymentIcon, selectedIcon, setDefaultButton;
        TextView paymentName, paymentDetails, paymentExtra, defaultBadge;
        LinearLayout contentLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            paymentIcon = itemView.findViewById(R.id.paymentIcon);
            selectedIcon = itemView.findViewById(R.id.selectedIcon);
            setDefaultButton = itemView.findViewById(R.id.setDefaultButton);
            paymentName = itemView.findViewById(R.id.paymentName);
            paymentDetails = itemView.findViewById(R.id.paymentDetails);
            paymentExtra = itemView.findViewById(R.id.paymentExtra);
            defaultBadge = itemView.findViewById(R.id.defaultBadge);
            contentLayout = itemView.findViewById(R.id.contentLayout);
        }

        void bind(PaymentMethod method, int position) {
            paymentName.setText(method.getName());

            // Set icon and details based on payment type
            switch (method.getType()) {
                case "jazzcash":
                    paymentIcon.setImageResource(R.drawable.ic_jazzcash);
                    if (method.getMobileNumber() != null) {
                        paymentDetails.setText(method.getMobileNumber());
                        paymentExtra.setText("JazzCash Account");
                    }
                    break;

                case "easypaisa":
                    paymentIcon.setImageResource(R.drawable.ic_easypaisa);
                    if (method.getMobileNumber() != null) {
                        paymentDetails.setText(method.getMobileNumber());
                        paymentExtra.setText("EasyPaisa Account");
                    }
                    break;

                case "cash":
                    paymentIcon.setImageResource(R.drawable.ic_cash);
                    paymentDetails.setText("Pay with cash at dropoff");
                    paymentExtra.setText("No charges");
                    break;

                default:
                    paymentIcon.setImageResource(R.drawable.ic_credit_card);
                    paymentDetails.setText(method.getDetails() != null ?
                            method.getDetails() : "Payment Method");
                    break;
            }

            // Show account holder name if available
            if (method.getAccountHolderName() != null && !method.getAccountHolderName().isEmpty()) {
                paymentName.setText(method.getAccountHolderName());
            }
        }
    }
}