package com.sistemaseventos.checkinapp.ui.userdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentAdapter extends RecyclerView.Adapter<EnrollmentAdapter.ViewHolder> {

    private List<EnrollmentEntity> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(EnrollmentEntity item);
    }

    public EnrollmentAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<EnrollmentEntity> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usando layout nativo do android para não precisar criar outro XML
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EnrollmentEntity item = list.get(position);
        holder.text1.setText("Evento ID: " + item.eventsId);
        holder.text2.setText("Status: " + item.status + " (Toque para Check-in)");
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ViewHolder(View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }
}