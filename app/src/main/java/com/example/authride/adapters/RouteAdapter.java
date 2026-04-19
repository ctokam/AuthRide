package com.example.authride.adapters; // Βάλε το δικό σου package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.authride.R; // Βάλε το δικό σου R package
import com.example.authride.Routes;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    // Εδώ θα αποθηκεύεται η λίστα με τις διαδρομές
    private List<Routes> routeList;

    public RouteAdapter(List<Routes> routeList) {
        this.routeList = routeList;
    }

    //item_available_routes.xml
    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_available_routes, parent, false);
        return new RouteViewHolder(view);
    }

    //Σύνδεση δεδομένων
    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Routes currentRoute = routeList.get(position); // Παίρνουμε τη διαδρομή στη συγκεκριμένη θέση

        holder.tvDriverName.setText(currentRoute.getDriverName());
        holder.tvDepartureTime.setText(currentRoute.getDepartureTime());
        holder.tvStartLocation.setText(currentRoute.getStartLocation());
        holder.tvEndLocation.setText(currentRoute.getEndLocation());
    }

    //Πόσες διαδρομές συνολικά;
    @Override
    public int getItemCount() {
        return routeList.size();
    }

    //κρατάει TextViews από XML (για να μην τα ψάχνουμε συνέχεια)
    public static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvDepartureTime, tvStartLocation, tvEndLocation;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);

            // Συνδεση τις μεταβλητες με τα IDs του item_available_routes.xml
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvStartLocation = itemView.findViewById(R.id.tvStartLocation);
            tvEndLocation = itemView.findViewById(R.id.tvEndLocation);
        }
    }
}