package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class EnrollmentResponse {
    public String id;

    @SerializedName("events_id")
    public String eventsId;

    @SerializedName("users_id")
    public String usersId;

    @SerializedName("check_in")
    public Date checkIn;

    public String status;
}