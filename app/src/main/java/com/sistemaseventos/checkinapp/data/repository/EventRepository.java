package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EventDao;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.EventResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

public class EventRepository {

    private EventDao eventDao;
    private ApiService apiService;
    private static final String TAG = "EventRepository";

    public EventRepository(Context context) {
        this.eventDao = AppDatabase.getInstance(context).eventDao();
        this.apiService = RetrofitClient.getApiService(context);
    }

    public List<EventEntity> getAllEvents() {
        try {
            // 1. Tenta buscar na API
            Response<List<EventResponse>> response = apiService.getAllEvents().execute();

            if (response.isSuccessful() && response.body() != null) {
                List<EventEntity> events = new ArrayList<>();
                for (EventResponse res : response.body()) {
                    EventEntity e = new EventEntity();
                    // IMPORTANTE: Validação de ID para evitar sobrescrita errada
                    if (res.id == null) {
                        Log.e(TAG, "Evento vindo da API sem ID! Ignorando.");
                        continue;
                    }
                    e.id = res.id;
                    e.eventName = res.eventName;
                    e.eventDate = res.eventDate;
                    e.description = res.description;
                    e.duration = res.duration;
                    e.category = res.category;
                    e.eventLocal = res.eventLocal;
                    e.isSynced = true;
                    events.add(e);
                }

                // Atualiza o cache apenas se houver dados válidos
                if (!events.isEmpty()) {
                    eventDao.upsertAll(events);
                }
                return events;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro API Eventos (Offline). Usando cache.", e);
        }

        // 2. Fallback: Retorna do banco local
        return eventDao.getAllEvents();
    }

    public List<EventEntity> searchEventsByName(String query) {
        return eventDao.searchEventsByName("%" + query + "%");
    }
}
