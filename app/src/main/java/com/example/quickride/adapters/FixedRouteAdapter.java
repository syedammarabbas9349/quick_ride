package com.example.quickride.adapters;

import android.content.Context;
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
import com.example.quickride.models.FixedRoute;
import android.widget.Button;
import java.util.List;

public class FixedRouteAdapter extends RecyclerView.Adapter<FixedRouteAdapter.RouteViewHolder> {

    private List<FixedRoute> routeList;
    private Context context;
    private OnJoinSeatClickListener listener;

    public interface OnJoinSeatClickListener {
        void onJoinSeatClick(FixedRoute route);
    }

    public FixedRouteAdapter(List<FixedRoute> routeList, Context context, OnJoinSeatClickListener listener) {
        this.routeList = routeList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fixed_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        FixedRoute route = routeList.get(position);

        holder.tvDriverName.setText(route.getDriverName());
        holder.tvRating.setText(route.getRating() + " ★");
        holder.tvFare.setText("Rs " + route.getFixedFare());
        holder.tvRoute.setText(route.getStartPoint() + " ➔ " + route.getDestination());
        holder.tvTime.setText("Departure: " + route.getDepartureTimeWindow());
        holder.tvSeats.setText("Seats left: " + route.getAvailableSeats());

        if (route.getDriverImageUrl() != null && !route.getDriverImageUrl().equals("default")) {
            Glide.with(context)
                    .load(route.getDriverImageUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.ivDriverProfile);
        }

        if (route.getAvailableSeats() <= 0) {
            holder.btnJoinSeat.setEnabled(false);
            holder.btnJoinSeat.setText("Full");
        } else {
            holder.btnJoinSeat.setEnabled(true);
            holder.btnJoinSeat.setText("Join Seat");
        }

        holder.btnJoinSeat.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJoinSeatClick(route);
            }
        });
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    public static class RouteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDriverProfile;
        TextView tvDriverName, tvRating, tvFare, tvRoute, tvTime, tvSeats;
        Button btnJoinSeat;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDriverProfile = itemView.findViewById(R.id.ivDriverProfile);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvFare = itemView.findViewById(R.id.tvFare);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            btnJoinSeat = itemView.findViewById(R.id.btnJoinSeat);
        }
    }
}
