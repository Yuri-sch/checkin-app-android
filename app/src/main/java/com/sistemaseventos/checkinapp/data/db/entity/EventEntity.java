package com.sistemaseventos.checkinapp.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import java.util.Date;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey
    @NonNull
    public String id; // ID único do evento (UUID)

    public String eventName;
    public Date eventDate;
    public String description;
    public String duration;
    public String category;
    public String eventLocal;

    public boolean isSynced = true; // Controle de sincronização
}
