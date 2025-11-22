package com.sistemaseventos.checkinapp.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
        tableName = "enrollments",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "usersId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = EventEntity.class, parentColumns = "id", childColumns = "eventsId", onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("usersId"), @Index("eventsId")}
)
public class EnrollmentEntity {
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String eventsId;

    @NonNull
    public String usersId;

    public java.util.Date checkIn;

    @NonNull
    public String status;

    // CAMPO OBRIGATÓRIO PARA A SINCRONIZAÇÃO
    public boolean isSynced = true;
}
