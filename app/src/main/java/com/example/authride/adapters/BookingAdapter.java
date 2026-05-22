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
import com.example.authride.model.Booking;
import com.example.authride.util.TimeUtils;

import java.util.List;

/**
 * Adapter για τις κρατήσεις του επιβάτη ({@link Booking}) πάνω στο item_route.
 * Κρύβει τις πληροφορίες θέσεων (δεν αφορούν την κράτηση).
 *  - CANCEL_BOOKING: "Ακύρωση Κράτησης" (επόμενες)
 *  - NONE: κρυμμένο κουμπί + ένδειξη κατάστασης (ιστορικό)
 *
 * Με {@link #setOnBookingClickListener} ορίζεται ενέργεια στο πάτημα της κάρτας
 * (π.χ. προβολή προφίλ οδηγού).
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public enum Mode { CANCEL_BOOKING, NONE }

    public interface OnBookingActionListener {
        void onBookingAction(Booking booking);
    }

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    private final List<Booking> bookings;
    private final Mode mode;
    private final OnBookingActionListener listener;
    private OnBookingClickListener clickListener;

    public BookingAdapter(List<Booking> bookings, Mode mode, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.mode = mode;
        this.listener = listener;
    }

    public void setOnBookingClickListener(OnBookingClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new BookingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder h, int position) {
        Booking booking = bookings.get(position);

        h.tvDriverName.setText(booking.getDriverName());
        h.tvDepartureTime.setText(TimeUtils.dateTime(booking.getDepartureMillis()));
        h.tvStartLocation.setText(booking.getStartLocation());
        h.tvEndLocation.setText(booking.getDestination());

        // Στην κράτηση δεν εμφανίζουμε θέσεις.
        h.ivSeatsIcon.setVisibility(View.GONE);
        h.tvAvailableSeats.setVisibility(View.GONE);
        h.tvStatus.setVisibility(View.GONE);

        if (mode == Mode.CANCEL_BOOKING) {
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText(R.string.btn_cancel_booking);
            h.btnAction.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(h.btnAction.getContext(), R.color.danger)));
            h.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onBookingAction(booking);
            });
        } else {
            // Ιστορικό: κρύβουμε το κουμπί και δείχνουμε την κατάσταση.
            h.btnAction.setVisibility(View.GONE);
            h.tvStatus.setVisibility(View.VISIBLE);
            if (booking.isCancelled()) {
                h.tvStatus.setText(R.string.status_cancelled);
                h.tvStatus.setTextColor(color(h, R.color.danger));
            } else {
                h.tvStatus.setText(R.string.status_completed);
                h.tvStatus.setTextColor(color(h, R.color.green));
            }
        }

        if (clickListener != null) {
            h.itemView.setOnClickListener(v -> clickListener.onBookingClick(booking));
        } else {
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    private int color(BookingViewHolder h, int colorRes) {
        return ContextCompat.getColor(h.itemView.getContext(), colorRes);
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDriverName, tvAvailableSeats, tvDepartureTime,
                tvStartLocation, tvEndLocation, tvStatus;
        final ImageView ivSeatsIcon;
        final Button btnAction;

        BookingViewHolder(@NonNull View itemView) {
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
