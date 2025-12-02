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
import java.util.List;
import java.util.Locale;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {

    private List<EventEntity> list = new ArrayList<>();
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

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

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
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
