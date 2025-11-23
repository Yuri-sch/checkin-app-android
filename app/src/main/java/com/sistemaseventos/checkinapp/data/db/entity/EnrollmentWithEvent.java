package com.sistemaseventos.checkinapp.data.db.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

public class EnrollmentWithEvent {
    @Embedded
    public EnrollmentEntity enrollment;

    @Relation(
            parentColumn = "eventsId",
            entityColumn = "id"
    )
    public EventEntity event;
}
