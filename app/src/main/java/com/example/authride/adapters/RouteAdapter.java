package com.example.authride.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authride.R;
import com.example.authride.database.Route;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<Route> routeList;
    private OnRouteClickListener listener;

    //Interface για τα κλικ στο κουμπί
    public interface OnRouteClickListener {
        void onBookClick(Route route);
    }

    public RouteAdapter(List<Route> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Φορτώνουμε το σχέδιο της κάρτας (item_available_routes.xml)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_available_routes, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);

        //δεδομένα---> XML
        holder.tvStart.setText(route.getStartLocation());
        holder.tvEnd.setText(route.getEndLocation());
        holder.tvTime.setText(route.getDepartureTime());
        holder.tvSeats.setText(String.valueOf(route.getAvailableSeats()));

        // (Για να δείξουμε το κανονικό όνομα του οδηγού χρειάζεται πιο πολύπλοκο SQL ερώτημα. Προς το παρόν βάζουμε το ID του)
        holder.tvDriverName.setText("Οδηγός ID: " + route.getDriverId());

        // κουμπι "Κράτηση Θέσης"
        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(route);
            }
        });
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    //ID ----> item_available_routes.xml
    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvStart, tvEnd, tvTime, tvSeats;
        Button btnAction;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvStart = itemView.findViewById(R.id.tvStartLocation);
            tvEnd = itemView.findViewById(R.id.tvEndLocation);
            tvTime = itemView.findViewById(R.id.tvDepartureTime);
            tvSeats = itemView.findViewById(R.id.tvAvailableSeats);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}