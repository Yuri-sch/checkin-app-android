package com.sistemaseventos.checkinapp.data.network.dto;

public class CreateEnrollmentRequest {
    public String userId;
    public String eventId;

    public CreateEnrollmentRequest(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }
}
