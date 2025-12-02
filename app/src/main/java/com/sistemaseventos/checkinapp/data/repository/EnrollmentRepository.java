package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.CreateEnrollmentRequest;
import com.sistemaseventos.checkinapp.data.network.dto.EnrollmentResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class EnrollmentRepository {
    private EnrollmentDao enrollmentDao;
    private ApiService apiService;
    private static final String TAG = "EnrollmentRepo";

    public EnrollmentRepository(Context context) {
        this.enrollmentDao = AppDatabase.getInstance(context).enrollmentDao();
        this.apiService = RetrofitClient.getApiService(context);
    }

    public List<EnrollmentWithEvent> getEnrollmentsWithEventsForUser(String userId) {
        try {
            // Tenta API
            Response<List<EnrollmentResponse>> response = apiService.getEnrollments(userId).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<EnrollmentEntity> apiList = new ArrayList<>();
                for(EnrollmentResponse res : response.body()) {
                    EnrollmentEntity e = new EnrollmentEntity();
                    // Se vier ID nulo do back, gera um.
                    e.id = (res.id != null) ? res.id : UUID.randomUUID().toString();
                    e.eventsId = res.eventsId;
                    e.usersId = res.usersId;
                    e.status = res.status;
                    e.checkIn = res.checkIn;
                    e.isSynced = true;
                    apiList.add(e);
                }

                if (!apiList.isEmpty()) {
                    // Limpa cache sincronizado antigo e poe o novo
                    enrollmentDao.deleteSyncedEnrollmentsForUser(userId);
                    enrollmentDao.upsertAll(apiList);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: " + e.getMessage());
        }
        // Sempre retorna o local
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
            Log.e(TAG, "Offline: salvando local", e);
        }

        // Offline
        EnrollmentEntity local = new EnrollmentEntity();
        local.id = tempId;
        local.usersId = userId;
        local.eventsId = eventId;
        local.status = "PENDENTE"; // Backend vai confirmar como CONFIRMED
        local.isSynced = false;
        enrollmentDao.upsert(local);
        return true;
    }

    // ... performCheckIn e cancelEnrollment (Lógica similar: Tenta API -> Falha -> Salva Local)
    public boolean performCheckIn(String enrollmentId) {
        boolean successOnline = false;
        Date checkInDate = new Date();
        try {
            Response<EnrollmentResponse> res = apiService.performCheckIn(enrollmentId).execute();
            if (res.isSuccessful()) {
                successOnline = true;
                if (res.body().checkIn != null) checkInDate = res.body().checkIn;
            }
        } catch (Exception e) { Log.e(TAG, "Offline Checkin", e); }
        enrollmentDao.updateCheckInStatus(enrollmentId, checkInDate, successOnline);
        return true;
    }

    public boolean cancelEnrollment(String enrollmentId) {
        boolean successOnline = false;
        try {
            Response<EnrollmentResponse> res = apiService.cancelRegistration(enrollmentId).execute();
            if (res.isSuccessful()) successOnline = true;
        } catch (Exception e) { Log.e(TAG, "Offline Cancel", e); }
        enrollmentDao.cancelEnrollmentStatus(enrollmentId, successOnline);
        return true;
    }

    public int syncPendingEnrollments() throws Exception {
        int count = 0;
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();

        if (pending.isEmpty()) {
            Log.d(TAG, "Nada para sincronizar.");
            return 0;
        }

        for (EnrollmentEntity e : pending) {
            Log.d(TAG, "Processando item local (Temp ID?): " + e.id);
            boolean itemSuccess = false;

            try {
                String serverId = null;
                EnrollmentResponse serverData = null;

                try {
                    Response<List<EnrollmentResponse>> userEnrollmentsRes =
                            apiService.getEnrollments(e.usersId).execute();

                    if (userEnrollmentsRes.isSuccessful() && userEnrollmentsRes.body() != null) {
                        List<EnrollmentResponse> serverList = userEnrollmentsRes.body();

                        // Procura na lista do servidor se já existe inscrição para este Evento
                        for (EnrollmentResponse serverItem : serverList) {
                            // Compara se o ID do evento bate (assumindo que eventsId é String ou int)
                            if (serverItem.eventsId != null && serverItem.eventsId.equals(e.eventsId)) {
                                serverId = serverItem.id; // ACHAMOS O ID REAL!
                                serverData = serverItem;
                                Log.d(TAG, "Encontrado no servidor via lista do usuário. ID Real: " + serverId);
                                break;
                            }
                        }
                    }
                } catch (Exception searchEx) {
                    Log.w(TAG, "Falha ao buscar lista do usuário: " + searchEx.getMessage());
                }

                // ==============================================================================
                // PASSO 2: CRIAÇÃO (Se realmente não estava na lista do usuário)
                // ==============================================================================

                if (serverId == null) {
                    // Lógica igual a anterior: tenta criar
                    try {
                        CreateEnrollmentRequest req = new CreateEnrollmentRequest(e.usersId, e.eventsId);
                        Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();

                        if (res.isSuccessful() && res.body() != null) {
                            serverData = res.body();
                            serverId = serverData.id;
                        } else if (res.code() == 409) {
                            // Caso raríssimo onde a lista estava desatualizada ou concorrência
                            Log.e(TAG, "Conflito 409: Inscrição existe mas não veio na lista.");
                            continue;
                        } else {
                            Log.e(TAG, "Falha ao criar: " + res.code());
                            continue;
                        }
                    } catch (Exception createEx) {
                        Log.e(TAG, "Erro ao criar: " + createEx.getMessage());
                        continue;
                    }
                }

                // ==============================================================================
                // PASSO 3: UNIFICAÇÃO (Merge Local)
                // ==============================================================================

                // Se o ID local for diferente do ID do servidor (ex: UUID temporário vs ID numérico)
                if (serverId != null && !serverId.equals(e.id)) {
                    enrollmentDao.delete(e); // Remove o registro com ID provisório

                    e.id = serverId; // Assume o ID oficial

                    enrollmentDao.upsert(e); // Salva com o ID novo
                }

                // ==============================================================================
                // PASSO 4: AÇÕES FINAIS (Check-in / Cancel)
                // ==============================================================================

                if ("CANCELED".equals(e.status)) {
                    Response<EnrollmentResponse> res = apiService.cancelRegistration(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.cancelEnrollmentStatus(e.id, true);
                        itemSuccess = true;
                    }
                }
                else if (e.checkIn != null) {
                    // Verifica se o servidor já tem o checkin (pra não enviar repetido)
                    boolean alreadyCheckedInServer = (serverData.checkIn != null);

                    if (alreadyCheckedInServer) {
                        enrollmentDao.updateCheckInStatus(e.id, serverData.checkIn, true);
                        itemSuccess = true;
                    } else {
                        Response<EnrollmentResponse> res = apiService.performCheckIn(e.id).execute();
                        if (res.isSuccessful()) {
                            enrollmentDao.updateCheckInStatus(e.id, e.checkIn, true);
                            itemSuccess = true;
                        }
                    }
                } else {
                    // Apenas sincronizou a criação/existência
                    e.isSynced = true;
                    enrollmentDao.upsert(e);
                    itemSuccess = true;
                }

                if (itemSuccess) count++;

            } catch (Exception ex) {
                Log.e(TAG, "Erro no item " + e.id, ex);
            }
        }
        return count;
    }
}