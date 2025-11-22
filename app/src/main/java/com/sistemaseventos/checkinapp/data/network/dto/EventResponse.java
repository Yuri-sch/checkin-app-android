package com.sistemaseventos.checkinapp.data.network.dto;

import java.util.Date;

public class EventResponse {
    public String id;
    public String eventName;
    public Date eventDate; // O GSON vai converter o TIMESTAMPTZ
    public String description;
    public String duration;
    public String category;
    public String eventLocal;
}
