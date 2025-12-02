package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.dao.UserDao;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.SimpleUserRequest;
import com.sistemaseventos.checkinapp.data.network.dto.UserResponse;
import com.sistemaseventos.checkinapp.data.network.dto.UserSyncDTO; // Importe o DTO correto
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class UserRepository {
    private UserDao userDao;
    private EnrollmentDao enrollmentDao;
    private ApiService apiService;
    private static final String TAG = "UserRepository";

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.enrollmentDao = db.enrollmentDao();
        this.apiService = RetrofitClient.getApiService(context);
    }

    public UserEntity findUserByCpf(String cpf) {
        try {
            Response<UserResponse> response = apiService.findUserByCpf(cpf).execute();
            if (response.isSuccessful() && response.body() != null) {
                UserEntity user = mapResponseToEntity(response.body());
                user.isSynced = true;
                userDao.upsert(user);
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline ou Erro API na busca: " + e.getMessage());
        }
        return userDao.findByCpf(cpf);
    }

    public UserEntity registerSimpleUser(String cpf, String email) {
        // Definimos um nome padrão pois o UserSyncDTO exige, mas o cadastro simples não pergunta
        String defaultName = "Novo Usuário";

        try {
            // MUDANÇA: Agora usamos o DTO de Sincronização e a rota syncUser
            // Isso evita o erro 400 (falta de senha) do /auth/register
            UserSyncDTO syncDto = new UserSyncDTO(cpf, email, defaultName);

            Response<UserResponse> response = apiService.syncUser(syncDto).execute();

            if (response.isSuccessful() && response.body() != null) {
                UserEntity user = mapResponseToEntity(response.body());
                user.isSynced = true;
                userDao.upsert(user);
                return user;
            } else {
                // Log de erro para debug (caso o sync falhe por outro motivo)
                String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Sem detalhes";
                Log.e(TAG, "Falha no cadastro via Sync (Code " + response.code() + "): " + errorMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline ou Erro de Conexão: salvando localmente", e);
        }

        // Se falhar a API ou estiver offline, salva localmente para sincronizar depois
        UserEntity offlineUser = new UserEntity();
        offlineUser.id = UUID.randomUUID().toString(); // ID Temporário
        offlineUser.cpf = cpf;
        offlineUser.email = email;
        offlineUser.fullname = defaultName;
        offlineUser.complete = false;
        offlineUser.isSynced = false; // Importante: marca como pendente

        userDao.upsert(offlineUser);
        return offlineUser;
    }

    // --- ATUALIZAÇÃO AQUI ---
    public int syncPendingUsers() throws Exception {
        int count = 0;
        List<UserEntity> pending = userDao.getUnsyncedUsers();

        if (pending.isEmpty()) return 0;

        for (UserEntity u : pending) {
            String oldTempId = u.id;
            Log.d(TAG, "Tentando sincronizar usuário: " + u.cpf);

            try {
                UserSyncDTO syncDto = new UserSyncDTO(u.cpf, u.email, u.fullname);
                Response<UserResponse> res = apiService.syncUser(syncDto).execute();

                // Lógica para tratar Sucesso (200/201) OU Conflito (409)
                if (res.isSuccessful() || res.code() == 409) {
                    UserResponse body = res.body();

                    // SE FOR 409: O body vem nulo ou com erro, então buscamos o usuário real
                    if (res.code() == 409) {
                        Log.d(TAG, "Usuário " + u.cpf + " já existe (409). Buscando ID real...");
                        Response<UserResponse> fetchRes = apiService.findUserByCpf(u.cpf).execute();
                        if (fetchRes.isSuccessful() && fetchRes.body() != null) {
                            body = fetchRes.body();
                        } else {
                            Log.e(TAG, "Falha ao recuperar usuário existente: " + fetchRes.code());
                            continue; // Pula para o próximo se não conseguir recuperar
                        }
                    }

                    if (body != null) {
                        String realId = body.id;

                        // 1. Atualiza dados do objeto (mas calma na hora de salvar!)
                        u.isSynced = true;
                        u.fullname = body.fullname != null ? body.fullname : u.fullname;

                        // 2. Migra inscrições do ID temporário para o real
                        // (Isso conserta o erro 500 das inscrições)
                        enrollmentDao.migrateUserEnrollments(oldTempId, realId);

                        // 3. A CORREÇÃO MÁGICA:
                        if (!oldTempId.equals(realId)) {
                            // Deleta explicitamente o registro com ID ANTIGO
                            userDao.deleteById(oldTempId);
                        }

                        // 4. Agora sim, atualizamos o ID no objeto e salvamos o NOVO registro
                        u.id = realId;
                        userDao.upsert(u);

                        Log.d(TAG, "SUCESSO: Usuário sincronizado/recuperado. ID: " + realId);
                        count++;
                    }

                } else {
                    String errorBody = res.errorBody() != null ? res.errorBody().string() : "Sem detalhes";
                    Log.e(TAG, "ERRO API (" + res.code() + ") ao sincronizar " + u.cpf + ": " + errorBody);
                }
            } catch (Exception e) {
                Log.e(TAG, "EXCEÇÃO ao sincronizar " + u.cpf, e);
            }
        }
        return count;
    }
    // ------------------------

    private UserEntity mapResponseToEntity(UserResponse response) {
        UserEntity entity = new UserEntity();
        entity.id = response.id;
        entity.cpf = response.cpf;
        entity.fullname = response.fullname;
        entity.email = response.email;
        entity.birthDate = response.birthDate;
        entity.complete = response.complete;
        return entity;
    }

    public List<UserEntity> getUnsyncedUsersList() {
        return userDao.getUnsyncedUsers();
    }
}
