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
        int count = 0; // 1. Iniciamos o contador
        List<EnrollmentEntity> pending = enrollmentDao.getUnsyncedEnrollments();

        if (pending.isEmpty()) {
            Log.d(TAG, "Nenhuma inscrição pendente para sincronizar.");
            return 0;
        }

        for (EnrollmentEntity e : pending) {
            Log.d(TAG, "Sincronizando item: " + e.id);
            boolean itemSuccess = false; // Flag para saber se DESTE item deu certo

            try {
                // --- PASSO 1: CRIAR INSCRIÇÃO (Se for nova/offline) ---
                // Se isSynced for false e o ID parecer temporário (ou confiamos na flag isSynced)
                boolean isNewRegistration = !e.isSynced && e.status != null && !e.status.equals("CANCELED");

                // Nota: Se estiver CANCELADO, nem tentamos criar de novo, vamos direto pro cancelamento no passo 2

                // Mas precisamos diferenciar: "Nunca foi pro servidor" vs "Já foi pro servidor mas editei localmente"
                // Como seu app cria IDs UUID longos, difícil distinguir pelo ID.
                // Vamos assumir: Se não tem status "CONFIRMED" do servidor e isSynced=false, é nova.
                // Para simplificar, vou manter sua lógica: se !isSynced tenta criar, a menos que seja só update.
                // O ideal é tentar criar se ela não existir no servidor.
                // Se o servidor retornar 409 (Conflict) ou 200 com ID, aceitamos.

                // Simplificação segura: Se a inscrição foi criada offline, ela precisa subir.
                if (!e.isSynced) {
                    // Verificação extra para não recriar inscrições que só tiveram check-in pendente
                    // (O ideal seria ter uma flag "isCreatedOffline", mas vamos tentar criar)

                    // Se já tiver um check-in mas sem ID real, é criação nova com check-in.
                    // Vamos tentar criar.

                    try {
                        CreateEnrollmentRequest req = new CreateEnrollmentRequest(e.usersId, e.eventsId);
                        // Chamada síncrona
                        Response<EnrollmentResponse> res = apiService.createEnrollment(req).execute();

                        if (res.isSuccessful() && res.body() != null) {
                            String realId = res.body().id; // ID que veio do servidor

                            // Remove a inscrição temporária antiga
                            enrollmentDao.delete(e);

                            // Cria o objeto novo e limpo para salvar
                            EnrollmentEntity synced = new EnrollmentEntity();
                            synced.id = realId;

                            // CORREÇÃO DOS DADOS DO EVENTO (Para não ficar invisível)
                            if (res.body().eventsId != null) {
                                synced.eventsId = res.body().eventsId;
                            } else {
                                synced.eventsId = e.eventsId; // Mantém o nosso ID local
                            }

                            // CORREÇÃO DOS DADOS DO USUÁRIO
                            if (res.body().usersId != null) {
                                synced.usersId = res.body().usersId;
                            } else {
                                synced.usersId = e.usersId;
                            }

                            // Mantém os dados de estado
                            synced.status = res.body().status;

                            // Importante: Se tínhamos um check-in local, PRESERVAMOS ele no objeto novo
                            // para o Passo 2 poder enviar o check-in logo em seguida
                            if (e.checkIn != null) {
                                synced.checkIn = e.checkIn;
                            } else {
                                synced.checkIn = res.body().checkIn;
                            }

                            synced.isSynced = true; // Marcamos como sincronizado a CRIAÇÃO

                            enrollmentDao.upsert(synced);

                            // Atualiza a variável 'e' para apontar para o novo objeto com ID real
                            // Assim o Passo 2 (Check-in) usa o ID certo
                            e = synced;

                            itemSuccess = true; // Contamos como sucesso
                            Log.d(TAG, "Inscrição criada. ID Real: " + realId);
                        } else {
                            // Se deu erro 400/409, talvez já exista?
                            // Por segurança, logamos e se falhar a criação, não tentamos check-in
                            String msg = res.errorBody() != null ? res.errorBody().string() : "";
                            Log.e(TAG, "Falha ao criar (Code " + res.code() + "): " + msg);
                            // Se falhou criar, continue para o próximo item do loop principal
                            continue;
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Erro de conexão ao criar: " + ex.getMessage());
                        continue;
                    }
                }

                // --- PASSO 2: AÇÕES SECUNDÁRIAS (Check-in ou Cancelar) ---
                // Agora 'e' tem o ID real (seja porque veio do servidor agora ou já tinha)

                if ("CANCELED".equals(e.status)) {
                    Log.d(TAG, "Enviando cancelamento para: " + e.id);
                    Response<EnrollmentResponse> res = apiService.cancelRegistration(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.cancelEnrollmentStatus(e.id, true);
                        itemSuccess = true;
                    }
                }
                else if (e.checkIn != null) {
                    // Se tem data de check-in, tentamos confirmar no servidor
                    Log.d(TAG, "Enviando Check-in para: " + e.id);
                    Response<EnrollmentResponse> res = apiService.performCheckIn(e.id).execute();
                    if (res.isSuccessful()) {
                        enrollmentDao.updateCheckInStatus(e.id, e.checkIn, true);
                        itemSuccess = true;
                    } else {
                        Log.e(TAG, "Erro check-in: " + res.code());
                    }
                }

                // 3. Se fizemos alguma operação com sucesso, incrementa o contador geral
                if (itemSuccess) {
                    count++;
                }

            } catch (Exception ex) {
                Log.e(TAG, "EXCEÇÃO CRÍTICA no item " + e.id, ex);
            }
        }

        return count; // Retorna o total processado para o Toast
    }
}