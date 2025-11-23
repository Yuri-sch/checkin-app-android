package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

// Arquivo: EnrollmentResponse.java
public class EnrollmentResponse {
    public String id;

    @SerializedName(value = "eventId", alternate = {"events_id", "event_id"})
    public String eventsId;

    @SerializedName(value = "userId", alternate = {"users_id", "user_id"})
    public String usersId;

    public Date checkIn;
    public String status;

    // ADICIONE ISTO: O objeto completo do evento
    @SerializedName("event")
    public EventResponse event;
}