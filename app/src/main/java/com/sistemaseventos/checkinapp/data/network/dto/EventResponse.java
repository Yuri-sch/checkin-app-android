package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class EventResponse {
    public String id;

    @SerializedName("event_name")
    public String eventName;

    @SerializedName("event_date")
    public Date eventDate;

    public String description;
    public String duration;
    public String category;

    @SerializedName("event_local")
    public String eventLocal;
}
