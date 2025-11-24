package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class CreateEnrollmentRequest {
    // Backend V3 espera "userId" e "eventId" (camelCase)
    @SerializedName("userId")
    public String userId;

    @SerializedName("eventId")
    public String eventId;

    public CreateEnrollmentRequest(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }
}
