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

    // Sync Manual
    public void syncPendingEnrollments() throws Exception {
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();

        if (pending.isEmpty()) {
            Log.d(TAG, "Nenhuma inscrição pendente para sincronizar.");
            return;
        }

        for (EnrollmentEntity e : pending) {
            Log.d(TAG, "Sincronizando item: " + e.id + " | User: " + e.usersId + " | Event: " + e.eventsId);

            try {
                // PASSO 1: Se a inscrição nunca foi enviada ao servidor (ID Temporário), cria ela primeiro
                // Verificamos isso checando se o ID é longo (UUID) e isSynced é falso,
                // ou simplesmente confiando na flag isSynced = false que definimos na criação offline
                boolean isNewRegistration = !e.isSynced;

                if (isNewRegistration) {
                    Log.d(TAG, "Criando inscrição nova no servidor...");
                    CreateEnrollmentRequest req = new CreateEnrollmentRequest(e.usersId, e.eventsId);
                    Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();

                    if (res.isSuccessful() && res.body() != null) {
                        // Importante: O servidor retornou o ID REAL
                        String realId = res.body().id;

                        // Atualizamos o objeto local
                        // Removemos o antigo (ID temporário) e inserimos o novo
                        enrollmentDao.delete(e);

                        EnrollmentEntity synced = new EnrollmentEntity();
                        synced.id = realId;

                        // Se o servidor devolver nulo (por erro de nome JSON), mantemos o ID local que sabemos que está certo
                        if (res.body().eventsId != null) {
                            synced.eventsId = res.body().eventsId;
                        } else {
                            synced.eventsId = e.eventsId; // Mantém o ID original do evento
                        }

                        // Faz o mesmo para o usersId por segurança
                        if (res.body().usersId != null) {
                            synced.usersId = res.body().usersId;
                        } else {
                            synced.usersId = e.usersId;
                        }

                        e.id = realId; // Agora 'e' tem o ID real
                        e.isSynced = true;
                        e.status = res.body().status; // Geralmente volta CONFIRMED

                        enrollmentDao.upsert(e);
                        Log.d(TAG, "Inscrição criada com sucesso. Novo ID: " + realId);
                    } else {
                        String errorMsg = res.errorBody() != null ? res.errorBody().string() : "Sem detalhes";
                        Log.e(TAG, "Falha ao criar inscrição (Erro " + res.code() + "): " + errorMsg);
                        continue; // Pula para o próximo item se falhar a criação
                    }
                }

                // PASSO 2: Agora que garantimos que a inscrição existe no servidor (ou já existia),
                // verificamos se precisa de Check-in ou Cancelamento.

                // Se estiver cancelado localmente
                if ("CANCELED".equals(e.status)) {
                    Log.d(TAG, "Enviando cancelamento...");
                    Response<EnrollmentResponse> res = apiService.cancelRegistration(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.cancelEnrollmentStatus(e.id, true);
                    } else {
                        Log.e(TAG, "Erro ao cancelar no servidor: " + res.code());
                    }
                }
                // Se tiver data de check-in local, mas o status de sync do check-in ainda não foi confirmado?
                // Nota: Seu banco não tem uma flag específica "isCheckInSynced",
                // então assumimos que se tem data e estamos no loop de pendentes, tentamos enviar.
                else if (e.checkIn != null) {
                    Log.d(TAG, "Enviando Check-in...");
                    Response<EnrollmentResponse> res = apiService.performCheckIn(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.updateCheckInStatus(e.id, e.checkIn, true);
                        Log.d(TAG, "Check-in sincronizado!");
                    } else {
                        // Pode dar 400 se já tiver feito check-in, tudo bem ignorar ou tratar
                        String msg = res.errorBody() != null ? res.errorBody().string() : "";
                        Log.e(TAG, "Erro ao enviar check-in (" + res.code() + "): " + msg);
                    }
                }

            } catch (Exception ex) {
                // LOG COMPLETO DA EXCEÇÃO
                Log.e(TAG, "EXCEÇÃO CRÍTICA ao sincronizar inscrição " + e.id, ex);
            }
        }
    }
}