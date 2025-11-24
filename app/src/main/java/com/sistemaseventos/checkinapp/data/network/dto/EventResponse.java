package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class EventResponse {
    public String id;

    // AQUI ESTÁ O TRUQUE: 'alternate' permite ler "eventName" (do Java/Spring) OU "event_name" (do Banco/Python/Legacy)
    @SerializedName(value = "eventName", alternate = {"event_name", "name"})
    public String eventName;

    @SerializedName(value = "eventDate", alternate = {"event_date", "date", "data"})
    public Date eventDate;

    public String description;
    public String duration;
    public String category;

    @SerializedName(value = "eventLocal", alternate = {"event_local", "local", "location"})
    public String eventLocal;
}
