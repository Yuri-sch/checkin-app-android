package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class EnrollmentResponse {
    public String id;

    // Ajustado para aceitar o padrão do novo DTO
    @SerializedName(value = "eventId", alternate = {"events_id", "event_id"})
    public String eventsId;

    @SerializedName(value = "userId", alternate = {"users_id", "user_id"})
    public String usersId;

    @SerializedName(value = "checkIn", alternate = {"check_in"})
    public Date checkIn;

    public String status;
}