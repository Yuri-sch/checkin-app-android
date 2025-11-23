package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent; // Importante
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.CreateEnrollmentRequest;
import com.sistemaseventos.checkinapp.data.network.dto.EnrollmentResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;

public class EnrollmentRepository {
    private EnrollmentDao enrollmentDao;
    private ApiService apiService;
    private static final String TAG = "APP_DEBUG";

    public EnrollmentRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.enrollmentDao = db.enrollmentDao();
        this.eventDao = db.eventDao(); // Inicialize o DAO de eventos
        this.apiService = RetrofitClient.getApiService(context);
    }

    // Agora retorna EnrollmentWithEvent para termos os nomes!
    public List<EnrollmentWithEvent> getEnrollmentsWithEventsForUser(String userId) {
        try {
            Response<List<EnrollmentResponse>> response = apiService.getEnrollments(userId).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<EnrollmentEntity> apiList = new ArrayList<>();
                List<EventEntity> eventsToSave = new ArrayList<>(); // 1. Nova lista para eventos

                for(EnrollmentResponse res : response.body()) {
                    // Mapeia a Inscrição (como você já fazia)
                    EnrollmentEntity e = new EnrollmentEntity();
                    e.id = (res.id != null) ? res.id : UUID.randomUUID().toString();
                    e.eventsId = res.eventsId;
                    e.usersId = res.usersId;
                    e.status = res.status;
                    e.checkIn = res.checkIn;
                    e.isSynced = true;
                    apiList.add(e);

                    // 2. Mapeia o Evento (Correção do Bug)
                    if (res.event != null) {
                        EventEntity evt = new EventEntity();
                        evt.id = res.event.id;
                        evt.eventName = res.event.eventName; // Certifique-se que EventEntity tem esses campos
                        evt.eventDate = res.event.eventDate;
                        evt.eventLocal = res.event.eventLocal;
                        evt.category = res.event.category;
                        evt.description = res.event.description;
                        // Adicione à lista de eventos para salvar
                        eventsToSave.add(evt);
                    }
                }

                // 3. Salva os EVENTOS no banco local primeiro
                if (!eventsToSave.isEmpty()) {
                    // Você precisará expor o eventDao aqui ou usar AppDatabase.getInstance(context).eventDao()
                    AppDatabase.getInstance(apiService.getContextOrPassContextHere).eventDao().upsertAll(eventsToSave);
                }

                // 4. Salva as INSCRIÇÕES depois
                if (!apiList.isEmpty()) {
                    enrollmentDao.deleteSyncedEnrollmentsForUser(userId);
                    enrollmentDao.upsertAll(apiList);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: mantendo inscrições locais", e);
        }
        // Retorna a lista COMBINADA (Inscrição + Evento)
        return enrollmentDao.getEnrollmentsWithEvents(userId);
    }

    public boolean createEnrollment(String userId, String eventId) {
        String tempId = UUID.randomUUID().toString();
        try {
            CreateEnrollmentRequest req = new CreateEnrollmentRequest(userId, eventId);
            Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();

            if (res.isSuccessful() && res.body() != null) {
                EnrollmentEntity e = new EnrollmentEntity();
                e.id = res.body().id;
                e.eventsId = res.body().eventsId;
                e.usersId = res.body().usersId;
                e.status = res.body().status;
                e.checkIn = res.body().checkIn;
                e.isSynced = true;
                enrollmentDao.upsert(e);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: salvando localmente", e);
        }

        EnrollmentEntity local = new EnrollmentEntity();
        local.id = tempId;
        local.usersId = userId;
        local.eventsId = eventId;
        local.status = "PENDENTE";
        local.isSynced = false;
        enrollmentDao.upsert(local);
        return true;
    }

    public boolean performCheckIn(String enrollmentId) {
        boolean successOnline = false;
        Date checkInDate = new Date();

        try {
            Response<EnrollmentResponse> res = apiService.performCheckIn(enrollmentId).execute();
            if (res.isSuccessful()) {
                successOnline = true;
                if (res.body().checkIn != null) checkInDate = res.body().checkIn;
            }
        } catch (Exception e) {
            Log.e(TAG, "Check-in Offline", e);
        }

        enrollmentDao.updateCheckInStatus(enrollmentId, checkInDate, successOnline);
        return true;
    }

    // Novo método de Cancelamento
    public boolean cancelEnrollment(String enrollmentId) {
        boolean successOnline = false;
        try {
            Response<EnrollmentResponse> res = apiService.cancelRegistration(enrollmentId).execute();
            if (res.isSuccessful()) {
                successOnline = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cancelamento Offline", e);
        }

        // Atualiza localmente para CANCELED
        enrollmentDao.cancelEnrollmentStatus(enrollmentId, successOnline);
        return true;
    }

    public void syncPendingEnrollments() {
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();
        for (EnrollmentEntity e : pending) {
            try {
                // Se estiver cancelado, tenta cancelar no servidor
                if ("CANCELED".equals(e.status)) {
                    // Só tenta cancelar se tiver ID real (se for temp, deleta local)
                    // Como simplificação, vamos tentar o PATCH
                    Response<EnrollmentResponse> res = apiService.cancelRegistration(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.cancelEnrollmentStatus(e.id, true);
                    }
                }
                else if (e.checkIn != null) {
                    Response<EnrollmentResponse> res = apiService.performCheckIn(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.updateCheckInStatus(e.id, e.checkIn, true);
                    }
                } else {
                    CreateEnrollmentRequest req = new CreateEnrollmentRequest(e.usersId, e.eventsId);
                    Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();

                    if (res.isSuccessful() && res.body() != null) {
                        enrollmentDao.delete(e);
                        EnrollmentEntity synced = new EnrollmentEntity();
                        synced.id = res.body().id;
                        synced.eventsId = res.body().eventsId;
                        synced.usersId = res.body().usersId;
                        synced.status = res.body().status;
                        synced.checkIn = res.body().checkIn;
                        synced.isSynced = true;
                        enrollmentDao.upsert(synced);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Erro sync: " + e.id);
            }
        }
    }
}