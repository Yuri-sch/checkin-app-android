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
            Response<List<EventResponse>> response = apiService.getAllEvents().execute();

            if (response.isSuccessful() && response.body() != null) {
                List<EventEntity> apiEvents = new ArrayList<>();
                for (EventResponse res : response.body()) {
                    if (res.id == null) continue;

                    EventEntity e = new EventEntity();
                    e.id = res.id;
                    e.eventName = res.eventName;
                    e.eventDate = res.eventDate;
                    e.description = res.description;
                    e.duration = res.duration;
                    e.category = res.category;
                    e.eventLocal = res.eventLocal;
                    e.isSynced = true;
                    apiEvents.add(e);
                }

                // Apenas insere/atualiza. NÃO APAGA NADA.
                if (!apiEvents.isEmpty()) {
                    eventDao.upsertAll(apiEvents);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao baixar eventos (usando cache): " + e.getMessage());
            // Isso garante que se o Gson falhar na data, o app não crasha, só usa o cache
        }

        return eventDao.getAllEvents();
    }

    public List<EventEntity> searchEventsByName(String query) {
        return eventDao.searchEventsByName("%" + query + "%");
    }
}
