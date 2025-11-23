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
    // Formatador de data para ficar bonito (ex: 20/11/2025)
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
        // AGORA USA O NOSSO LAYOUT BONITO
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventEntity item = list.get(position);

        holder.name.setText(item.eventName);
        holder.category.setText(item.category != null ? item.category : "Geral");

        // Trata o "null" do local
        if (item.eventLocal != null && !item.eventLocal.equals("null")) {
            holder.local.setText(item.eventLocal);
        } else {
            holder.local.setText("Local a definir");
        }

        // Formata a data se existir
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
            name = v.findViewById(R.id.text_event_name);
            category = v.findViewById(R.id.text_event_category);
            local = v.findViewById(R.id.text_event_local);
            date = v.findViewById(R.id.text_event_date);
        }
    }
}
