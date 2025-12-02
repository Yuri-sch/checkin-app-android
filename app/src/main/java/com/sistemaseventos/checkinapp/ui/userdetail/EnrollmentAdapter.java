package com.sistemaseventos.checkinapp.ui.userdetail;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnrollmentAdapter extends RecyclerView.Adapter<EnrollmentAdapter.ViewHolder> {

    private List<EnrollmentWithEvent> list = new ArrayList<>();
    private final OnInteractionListener listener;

    public interface OnInteractionListener {
        void onCheckInClick(EnrollmentWithEvent item);
        // void onCancelClick(EnrollmentWithEvent item); // REMOVIDO
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

        // 1. Nome do Evento
        holder.eventName.setText(item.event != null ? item.event.eventName : "Evento ID: " + item.enrollment.eventsId);

        if ("CANCELED".equals(item.enrollment.status)) {
            holder.checkinStatus.setText("Inscrição Cancelada");
            holder.checkinStatus.setTextColor(Color.RED); // Texto vermelho
            holder.btnCheckin.setVisibility(View.GONE);   // ESCONDE o botão
        }
        // Se não cancelado, verifica se já fez check-in
        else if (item.enrollment.checkIn != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            holder.checkinStatus.setText("Check-in: REALIZADO (" + sdf.format(item.enrollment.checkIn) + ")");
            holder.checkinStatus.setTextColor(Color.parseColor("#2E7D32")); // Verde Escuro
            holder.btnCheckin.setVisibility(View.GONE); // Esconde botão pois já fez
        }
        // Se não cancelado e sem check-in, então está pendente
        else {
            holder.checkinStatus.setText("Check-in: PENDENTE");
            holder.checkinStatus.setTextColor(Color.parseColor("#EF6C00")); // Laranja
            holder.btnCheckin.setVisibility(View.VISIBLE); // MOSTRA o botão
            holder.btnCheckin.setEnabled(true);
            holder.btnCheckin.setText("Confirmar Presença");
        }

        // 3. Status de Sincronização (Permanece igual)
        if (item.enrollment.isSynced) {
            holder.syncStatus.setText("Sincronização: OK (Salvo na Nuvem)");
            holder.syncStatus.setTextColor(Color.parseColor("#757575"));
        } else {
            holder.syncStatus.setText("⚠ Sincronização: PENDENTE DE ENVIO");
            holder.syncStatus.setTextColor(Color.RED);
        }

        // Ação do Botão
        holder.btnCheckin.setOnClickListener(v -> {
            if (listener != null) listener.onCheckInClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, checkinStatus, syncStatus;
        Button btnCheckin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.text_event_name);
            checkinStatus = itemView.findViewById(R.id.text_checkin_status);
            syncStatus = itemView.findViewById(R.id.text_sync_status);
            btnCheckin = itemView.findViewById(R.id.btn_do_checkin);
        }
    }
}