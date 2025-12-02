package com.sistemaseventos.checkinapp.ui.eventlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
    private List<EventEntity> list = new ArrayList<>();
    private List<String> enrolledEventIds = new ArrayList<>();
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private Map<String, String> eventStatusMap = new HashMap<>();

    public interface OnItemClickListener {
        void onItemClick(EventEntity item);
    }

    public EventListAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<EventEntity> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setEnrollmentStatus(Map<String, String> statusMap) {
        this.eventStatusMap = statusMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // --- CORREÇÃO AQUI ---
        // Mudamos de R.layout.item_event para R.layout.item_event_selection
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_selection, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventEntity item = list.get(position);

        // Agora não dará mais erro, pois o layout item_event_selection tem esses IDs
        holder.name.setText(item.eventName);
        holder.category.setText(item.category != null ? item.category : "Geral");

        if (item.eventLocal != null && !item.eventLocal.equals("null")) {
            holder.local.setText(item.eventLocal);
        } else {
            holder.local.setText("Local a definir");
        }

        if (item.eventDate != null) {
            holder.date.setText(dateFormat.format(item.eventDate));
        } else {
            holder.date.setText("");
        }

        // Verifica qual o status desse evento para o usuário (pode ser null se não tiver)
        String status = eventStatusMap.get(item.id);

        // Bloquear SÓ se tiver status E não for cancelado
        boolean isActiveEnrollment = (status != null && !"CANCELED".equals(status));

        if (isActiveEnrollment) {
            holder.itemView.setAlpha(0.5f);
            holder.name.setText(item.eventName + " (Já Inscrito)");
            holder.itemView.setOnClickListener(null); // Remove o clique
        } else {
            holder.itemView.setAlpha(1.0f);
            if ("CANCELED".equals(status)) {
                holder.name.setText(item.eventName + " (Reinscrever)");
            } else {
                holder.name.setText(item.eventName);
            }

            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, category, local, date;
        ViewHolder(View v) {
            super(v);
            // Esses IDs devem existir em item_event_selection.xml
            name = v.findViewById(R.id.text_event_name);
            category = v.findViewById(R.id.text_event_category);
            local = v.findViewById(R.id.text_event_local);
            date = v.findViewById(R.id.text_event_date);
        }
    }
}
