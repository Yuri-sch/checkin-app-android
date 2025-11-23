package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class CreateEnrollmentRequest {
    @SerializedName("users_id")
    public String userId;

    @SerializedName("events_id")
    public String eventId;

    public CreateEnrollmentRequest(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }
}
