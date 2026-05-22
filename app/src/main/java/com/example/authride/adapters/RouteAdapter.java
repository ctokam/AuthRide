package com.example.authride.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authride.R;
import com.example.authride.model.Ride;
import com.example.authride.util.TimeUtils;

import java.util.List;

/**
 * Adapter για λίστα διαδρομών ({@link Ride}) πάνω στο item_route.
 * Το κουμπί δράσης αλλάζει ανά λειτουργία:
 *  - BOOK: "Κράτηση Θέσης" (επιβάτης, διαθέσιμες διαδρομές)
 *  - CANCEL_RIDE: "Ακύρωση Διαδρομής" (οδηγός, επόμενες)
 *  - NONE: κρυμμένο κουμπί + ένδειξη κατάστασης (ιστορικό)
 *
 * Προαιρετικά, με {@link #setOnRideClickListener} μπορεί να οριστεί ενέργεια
 * όταν ο χρήστης πατάει ολόκληρη την κάρτα (π.χ. προβολή προφίλ οδηγού ή
 * λίστας επιβατών).
 */
public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    public enum Mode { BOOK, CANCEL_RIDE, NONE }

    public interface OnRideActionListener {
        void onRideAction(Ride ride);
    }

    /** Listener για πάτημα ολόκληρης της κάρτας. */
    public interface OnRideClickListener {
        void onRideClick(Ride ride);
    }

    private final List<Ride> rides;
    private final Mode mode;
    private final OnRideActionListener listener;
    private OnRideClickListener clickListener;

    public RouteAdapter(List<Ride> rides, Mode mode, OnRideActionListener listener) {
        this.rides = rides;
        this.mode = mode;
        this.listener = listener;
    }

    public void setOnRideClickListener(OnRideClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder h, int position) {
        Ride ride = rides.get(position);

        h.tvDriverName.setText(ride.getDriverName());
        h.tvAvailableSeats.setText(String.valueOf(ride.getAvailableSeats()));
        h.tvDepartureTime.setText(TimeUtils.dateTime(ride.getDepartureMillis()));
        h.tvStartLocation.setText(ride.getStartLocation());
        h.tvEndLocation.setText(ride.getDestination());

        h.ivSeatsIcon.setVisibility(View.VISIBLE);
        h.tvAvailableSeats.setVisibility(View.VISIBLE);
        h.tvStatus.setVisibility(View.GONE);

        switch (mode) {
            case BOOK:
                h.btnAction.setVisibility(View.VISIBLE);
                tint(h.btnAction, R.color.green);
                boolean available = ride.hasAvailableSeats();
                h.btnAction.setEnabled(available);
                h.btnAction.setText(available
                        ? R.string.btn_book_seat
                        : R.string.seats_full);
                h.btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onRideAction(ride);
                });
                break;

            case CANCEL_RIDE:
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setEnabled(true);
                tint(h.btnAction, R.color.danger);
                h.btnAction.setText(R.string.btn_cancel_ride);
                h.btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onRideAction(ride);
                });
                break;

            case NONE:
            default:
                // Ιστορικό: κρύβουμε το κουμπί και δείχνουμε την κατάσταση.
                h.btnAction.setVisibility(View.GONE);
                h.tvStatus.setVisibility(View.VISIBLE);
                if (ride.isCancelled()) {
                    h.tvStatus.setText(R.string.status_cancelled);
                    h.tvStatus.setTextColor(color(h, R.color.danger));
                } else {
                    h.tvStatus.setText(R.string.status_completed);
                    h.tvStatus.setTextColor(color(h, R.color.green));
                }
                break;
        }

        // Πάτημα ολόκληρης της κάρτας (προαιρετικό).
        if (clickListener != null) {
            h.itemView.setOnClickListener(v -> clickListener.onRideClick(ride));
        } else {
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    private void tint(Button button, int colorRes) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(button.getContext(), colorRes)));
    }

    private int color(RouteViewHolder h, int colorRes) {
        return ContextCompat.getColor(h.itemView.getContext(), colorRes);
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDriverName, tvAvailableSeats, tvDepartureTime,
                tvStartLocation, tvEndLocation, tvStatus;
        final ImageView ivSeatsIcon;
        final Button btnAction;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvAvailableSeats = itemView.findViewById(R.id.tvAvailableSeats);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvStartLocation = itemView.findViewById(R.id.tvStartLocation);
            tvEndLocation = itemView.findViewById(R.id.tvEndLocation);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivSeatsIcon = itemView.findViewById(R.id.ivSeatsIcon);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}
