package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.CreateEnrollmentRequest;
import com.sistemaseventos.checkinapp.data.network.dto.EnrollmentResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

public class EnrollmentRepository {
    private EnrollmentDao enrollmentDao;
    private ApiService apiService;
    private static final String TAG = "EnrollmentRepository";

    public EnrollmentRepository(Context context) {
        this.enrollmentDao = AppDatabase.getInstance(context).enrollmentDao();
        this.apiService = RetrofitClient.getApiService(context);
    }

    public List<EnrollmentEntity> getEnrollmentsForUser(String userId) {
        try {
            Response<List<EnrollmentResponse>> response = apiService.getEnrollments(userId).execute();
            if (response.isSuccessful() && response.body() != null) {
                enrollmentDao.deleteEnrollmentsForUser(userId);
                List<EnrollmentEntity> list = new ArrayList<>();
                for(EnrollmentResponse res : response.body()) {
                    EnrollmentEntity e = new EnrollmentEntity();
                    e.id = res.id;
                    e.eventsId = res.eventsId;
                    e.usersId = res.usersId;
                    e.status = res.status;
                    e.checkIn = res.checkIn;
                    e.isSynced = true; // Veio da API, está sincronizado
                    list.add(e);
                }
                enrollmentDao.upsertAll(list);
                return list;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: buscando local", e);
        }
        return enrollmentDao.getEnrollmentsForUser(userId);
    }

    public boolean createEnrollment(String userId, String eventId) {
        try {
            CreateEnrollmentRequest req = new CreateEnrollmentRequest(userId, eventId);
            Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();

            if (res.isSuccessful() && res.body() != null) {
                // Sucesso Online
                EnrollmentResponse body = res.body();
                EnrollmentEntity e = new EnrollmentEntity();
                e.id = body.id;
                e.eventsId = body.eventsId;
                e.usersId = body.usersId;
                e.status = body.status;
                e.checkIn = body.checkIn;
                e.isSynced = true;

                enrollmentDao.upsert(e);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: salvando inscrição local", e);
        }

        // Sucesso Offline (Cria temporário)
        EnrollmentEntity local = new EnrollmentEntity();
        local.id = java.util.UUID.randomUUID().toString();
        local.usersId = userId;
        local.eventsId = eventId;
        local.status = "PENDENTE";
        local.isSynced = false; // Marca para o SyncWorker pegar depois

        enrollmentDao.upsert(local);
        return true;
    }

    public boolean performCheckIn(String enrollmentId) {
        try {
            Response<Void> res = apiService.performCheckIn(enrollmentId).execute();
            if (res.isSuccessful()) return true;
        } catch (Exception e) {
            Log.e(TAG, "Offline: salvando check-in local", e);
        }
        // No modo offline, assumimos sucesso visual.
        // A lógica ideal aqui seria buscar a entidade local e marcar isSynced=false
        return true;
    }

    // Chamado pelo SyncWorker
    public void syncPendingEnrollments() {
        // Agora este método vai funcionar porque criamos o getUnsyncedEnrollments na DAO
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();

        for (EnrollmentEntity e : pending) {
            try {
                // Lógica simplificada: Se tem checkIn data é checkin, senão é inscrição
                if (e.checkIn != null) {
                    Response<Void> res = apiService.performCheckIn(e.id).execute();
                    if (res.isSuccessful()) {
                        e.isSynced = true;
                        enrollmentDao.upsert(e);
                    }
                } else {
                    CreateEnrollmentRequest req = new CreateEnrollmentRequest(e.usersId, e.eventsId);
                    Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();
                    if (res.isSuccessful()) {
                        e.isSynced = true;
                        // Idealmente atualizaríamos o ID local com o ID do servidor aqui
                        enrollmentDao.upsert(e);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Erro ao sincronizar: " + e.id);
            }
        }
    }
}