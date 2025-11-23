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
            // Tenta buscar na API (Online)
            Response<List<EnrollmentResponse>> response = apiService.getEnrollments(userId).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<EnrollmentEntity> apiList = new ArrayList<>();
                for(EnrollmentResponse res : response.body()) {
                    EnrollmentEntity e = new EnrollmentEntity();
                    e.id = res.id;
                    e.eventsId = res.eventsId;
                    e.usersId = res.usersId;
                    e.status = res.status;
                    e.checkIn = res.checkIn;
                    e.isSynced = true;
                    apiList.add(e);
                }

                // SÓ LIMPA SE A API TROUXER DADOS (Para evitar apagar dados offline por erro)
                // Melhoria: Em vez de apagar tudo, fazemos um 'merge'.
                // Mas para simplificar e garantir que o que está no servidor é a verdade:
                if (!apiList.isEmpty()) {
                    // Apaga apenas os que já estão sincronizados para não perder os pendentes
                    // Como não temos deleteBySynced, vamos confiar no upsert.
                    // O upsert substitui se o ID bater. Se a API trouxer IDs novos, eles são adicionados.
                    // O problema de "limpar" é remover inscrições que foram deletadas no servidor.
                    // Vamos manter a limpeza total por enquanto, MAS salvando os pendentes antes.

                    // 1. Salva os pendentes em memória
                    List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();

                    // 2. Limpa o banco para esse usuário
                    enrollmentDao.deleteEnrollmentsForUser(userId);

                    // 3. Reinsere os pendentes (que não existem no servidor ainda)
                    enrollmentDao.upsertAll(pending);

                    // 4. Insere os novos da API
                    enrollmentDao.upsertAll(apiList);

                    return enrollmentDao.getEnrollmentsForUser(userId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: buscando dados locais", e);
        }
        // Retorna o que tem no banco local (seja cache antigo ou dados offline)
        return enrollmentDao.getEnrollmentsForUser(userId);
    }

    public boolean createEnrollment(String userId, String eventId) {
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
            Log.e(TAG, "Erro API ao inscrever. Salvando Offline.", e);
        }

        // Fallback Offline
        EnrollmentEntity local = new EnrollmentEntity();
        local.id = java.util.UUID.randomUUID().toString();
        local.usersId = userId;
        local.eventsId = eventId;
        local.status = "PENDENTE";
        local.isSynced = false; // Importante para não ser apagado na sincronização
        enrollmentDao.upsert(local);
        return true;
    }

    public boolean performCheckIn(String enrollmentId) {
        boolean successOnline = false;
        try {
            Response<Void> res = apiService.performCheckIn(enrollmentId).execute();
            if (res.isSuccessful()) {
                successOnline = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro API Checkin", e);
        }

        // Atualiza localmente (com a data atual)
        Date now = new Date();
        // Se sucesso online, isSynced = true. Se offline, false.
        enrollmentDao.updateCheckInStatus(enrollmentId, now, successOnline);

        return true;
    }

    public void syncPendingEnrollments() {
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();
        for (EnrollmentEntity e : pending) {
            try {
                if (e.checkIn != null) {
                    Response<Void> res = apiService.performCheckIn(e.id).execute();
                    if (res.isSuccessful()) {
                        // Atualiza status de sync
                        enrollmentDao.updateCheckInStatus(e.id, e.checkIn, true);
                    }
                } else {
                    CreateEnrollmentRequest req = new CreateEnrollmentRequest(e.usersId, e.eventsId);
                    Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();
                    if (res.isSuccessful() && res.body() != null) {
                        // Remove o registro temporário e insere o oficial (com ID do servidor)
                        // Para simplificar e evitar erro de chave, vamos apenas atualizar o ID
                        // Mas o Room não deixa atualizar PK.
                        // Então: Deletamos o antigo e inserimos o novo.
                        enrollmentDao.delete(e); // Você precisará adicionar este método na DAO

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