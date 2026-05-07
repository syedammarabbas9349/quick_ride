package com.example.quickride.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quickride.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FixedRouteBookingAdapter extends RecyclerView.Adapter<FixedRouteBookingAdapter.BookingViewHolder> {

    private List<Map<String, Object>> bookingList;
    private OnBookingDeleteListener deleteListener;

    public interface OnBookingDeleteListener {
        void onDelete(Map<String, Object> booking);
    }

    public FixedRouteBookingAdapter(List<Map<String, Object>> bookingList, OnBookingDeleteListener deleteListener) {
        this.bookingList = bookingList;
        this.deleteListener = deleteListener;
    }


    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fixed_route_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Map<String, Object> booking = bookingList.get(position);
        
        holder.tvRiderName.setText((String) booking.get("riderName"));
        holder.tvRiderPhone.setText((String) booking.get("riderPhone"));
        
        Long timestamp = (Long) booking.get("timestamp");
        String routeInfo = (String) booking.get("routeInfo");
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            String displayInfo = "Booked at: " + sdf.format(new Date(timestamp));
            if (routeInfo != null && !routeInfo.isEmpty()) {
                displayInfo += " | Route: " + routeInfo;
            }
            holder.tvTimestamp.setText(displayInfo);
        }

        holder.btnDeleteBooking.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(booking);
            }
        });
    }


    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRiderName, tvRiderPhone, tvTimestamp;
        android.widget.ImageView btnDeleteBooking;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRiderName = itemView.findViewById(R.id.tvRiderName);
            tvRiderPhone = itemView.findViewById(R.id.tvRiderPhone);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnDeleteBooking = itemView.findViewById(R.id.btnDeleteBooking);
        }
    }

}
