package com.sistemaseventos.checkinapp.ui.userdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent; // Novo objeto
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnrollmentAdapter extends RecyclerView.Adapter<EnrollmentAdapter.ViewHolder> {

    private List<EnrollmentWithEvent> list = new ArrayList<>();
    private OnInteractionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnInteractionListener {
        void onCheckInClick(EnrollmentWithEvent item);
        void onCancelClick(EnrollmentWithEvent item);
    }

    public EnrollmentAdapter(OnInteractionListener listener) {
        this.listener = listener;
    }

    public void setList(List<EnrollmentWithEvent> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EnrollmentWithEvent item = list.get(position);

        // Dados do Evento (via Join)
        if (item.event != null) {
            holder.name.setText(item.event.eventName);
            holder.category.setText(item.event.category);
            holder.local.setText(item.event.eventLocal);
            if (item.event.eventDate != null) {
                holder.date.setText(dateFormat.format(item.event.eventDate));
            }
        } else {
            holder.name.setText("Evento ID: " + item.enrollment.eventsId);
            holder.category.setText("---");
            holder.local.setText("---");
        }

        // Status e Check-in
        String statusText = item.enrollment.status;
        if (item.enrollment.checkIn != null) {
            statusText += " (Confirmado)";
            holder.status.setTextColor(0xFF388E3C); // Verde
        } else if ("CANCELED".equals(item.enrollment.status)) {
            holder.status.setTextColor(0xFFD32F2F); // Vermelho
        } else {
            holder.status.setTextColor(0xFF666666); // Cinza
        }
        holder.status.setText(statusText);

        // Cliques
        holder.itemView.setOnClickListener(v -> listener.onCheckInClick(item));
        holder.btnCancel.setOnClickListener(v -> listener.onCancelClick(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, category, local, date, status;
        ImageButton btnCancel;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.text_event_name);
            category = v.findViewById(R.id.text_event_category);
            local = v.findViewById(R.id.text_event_local);
            date = v.findViewById(R.id.text_event_date);
            status = v.findViewById(R.id.text_status);
            btnCancel = v.findViewById(R.id.btn_cancel_enrollment);
        }
    }
}