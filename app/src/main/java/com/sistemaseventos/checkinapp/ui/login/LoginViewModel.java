package com.sistemaseventos.checkinapp.ui.login;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.sistemaseventos.checkinapp.data.manager.SessionManager;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.LoginRequest;
import com.sistemaseventos.checkinapp.sync.SyncWorker;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    private ApiService apiService;
    private SessionManager sessionManager;

    // LiveData para a Activity observar (Sucesso ou Erro)
    private MutableLiveData<Boolean> _loginSuccess = new MutableLiveData<>();
    public LiveData<Boolean> loginSuccess = _loginSuccess;

    private MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        // Inicializa as dependências passando o contexto da aplicação
        apiService = RetrofitClient.getApiService(application);
        sessionManager = new SessionManager(application);
    }

    public void login(String email, String password) {
        // 1. Validação básica de campos vazios
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            _error.postValue("Por favor, preencha e-mail e senha.");
            return;
        }

        // 2. Prepara a requisição
        LoginRequest request = new LoginRequest(email, password);

        // 3. Chama a API de forma assíncrona
        // Note que esperamos uma String como resposta (o Token)
        apiService.login(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // SUCESSO!
                    String token = response.body();

                    // a) Salva o token nas preferências
                    sessionManager.saveToken(token);

                    // b) Inicia o agendamento da sincronização em background
                    scheduleSync();

                    // c) Avisa a tela para navegar
                    _loginSuccess.postValue(true);
                } else {
                    // ERRO (Ex: 401 - Senha errada)
                    if (response.code() == 401 || response.code() == 403) {
                        _error.postValue("E-mail ou senha incorretos.");
                    } else {
                        _error.postValue("Falha no login. Código: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // ERRO DE REDE (Servidor fora ou sem internet)
                _error.postValue("Erro de conexão: " + t.getMessage());
            }
        });
    }

    private void scheduleSync() {
        // Configura o WorkManager para rodar periodicamente
        // Constraints: Só roda se tiver internet (NetworkType.CONNECTED)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Cria a requisição: Rodar SyncWorker a cada 15 minutos
        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag("sync_work")
                        .build();

        // Enfileira o trabalho no sistema Android
        WorkManager.getInstance(getApplication()).enqueue(syncRequest);
    }
}
