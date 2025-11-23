package com.sistemaseventos.checkinapp.data.db.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
// Removidos os ForeignKeys para evitar erro 787 em cache parcial
@Entity(
        tableName = "enrollments",
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

    public boolean isSynced = true;
}
