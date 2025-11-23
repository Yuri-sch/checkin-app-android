package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class CreateEnrollmentRequest {
    // O novo backend provavelmente espera camelCase
    @SerializedName("userId")
    public String userId;

    @SerializedName("eventId")
    public String eventId;

    public CreateEnrollmentRequest(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }
}
