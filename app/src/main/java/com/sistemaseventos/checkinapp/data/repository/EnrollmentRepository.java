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
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
            // Rota atualizada no ApiService
            Response<List<EnrollmentResponse>> response = apiService.getEnrollments(userId).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<EnrollmentEntity> apiList = new ArrayList<>();
                for(EnrollmentResponse res : response.body()) {
                    EnrollmentEntity e = new EnrollmentEntity();
                    e.id = (res.id != null) ? res.id : UUID.randomUUID().toString();
                    e.eventsId = res.eventsId;
                    e.usersId = res.usersId;
                    e.status = res.status;
                    e.checkIn = res.checkIn;
                    e.isSynced = true;
                    apiList.add(e);
                }

                if (!apiList.isEmpty()) {
                    List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();
                    enrollmentDao.deleteEnrollmentsForUser(userId);

                    if (pending != null && !pending.isEmpty()) {
                        for (EnrollmentEntity p : pending) {
                            if (p.usersId.equals(userId)) enrollmentDao.upsert(p);
                        }
                    }
                    enrollmentDao.upsertAll(apiList);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: carregando local", e);
        }
        return enrollmentDao.getEnrollmentsForUser(userId);
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
            Log.e(TAG, "Offline: salvando inscrição", e);
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
            // Agora chama o PATCH
            Response<EnrollmentResponse> res = apiService.performCheckIn(enrollmentId).execute();
            if (res.isSuccessful() && res.body() != null) {
                successOnline = true;
                if (res.body().checkIn != null) {
                    checkInDate = res.body().checkIn; // Usa a data do servidor
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline check-in", e);
        }

        enrollmentDao.updateCheckInStatus(enrollmentId, checkInDate, successOnline);
        return true;
    }

    public void syncPendingEnrollments() {
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();
        for (EnrollmentEntity e : pending) {
            try {
                if (e.checkIn != null) {
                    // Sync Check-in (PATCH)
                    Response<EnrollmentResponse> res = apiService.performCheckIn(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.updateCheckInStatus(e.id, e.checkIn, true);
                    }
                } else {
                    // Sync Inscrição (POST)
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