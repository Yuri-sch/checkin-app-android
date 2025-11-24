package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class EnrollmentResponse {
    public String id;

    // Mapeia o JSON do backend para os campos do Android
    @SerializedName(value="eventsId", alternate={"event_id", "eventId", "event"})
    public String eventsId;

    @SerializedName(value = "userId", alternate = {"users_id"})
    public String usersId;

    @SerializedName("status")
    public String status;

    @SerializedName("checkIn")
    public Date checkIn;
}