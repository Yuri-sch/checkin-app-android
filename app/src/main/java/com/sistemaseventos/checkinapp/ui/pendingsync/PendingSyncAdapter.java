package com.sistemaseventos.checkinapp.ui.pendingsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import java.util.ArrayList;
import java.util.List;

public class PendingSyncAdapter extends RecyclerView.Adapter<PendingSyncAdapter.ViewHolder> {

    private List<PendingUserItem> items = new ArrayList<>();

    public void setItems(List<PendingUserItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingUserItem item = items.get(position);
        holder.name.setText(item.user.fullname);
        holder.cpf.setText("CPF: " + item.user.cpf);

        if (item.enrollments == null || item.enrollments.isEmpty()) {
            holder.events.setText("- Sem inscrições vinculadas");
        } else {
            StringBuilder sb = new StringBuilder();
            for (EnrollmentWithEvent e : item.enrollments) {
                String eventName = (e.event != null) ? e.event.eventName : "Evento Desconhecido";
                String status = (e.enrollment.checkIn != null) ? "[CHECK-IN FEITO]" : "[INSCRITO]";
                sb.append("• ").append(eventName).append(" ").append(status).append("\n");
            }
            holder.events.setText(sb.toString().trim());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, cpf, events;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txt_pending_user_name);
            cpf = itemView.findViewById(R.id.txt_pending_user_cpf);
            events = itemView.findViewById(R.id.txt_pending_user_events);
        }
    }
}
