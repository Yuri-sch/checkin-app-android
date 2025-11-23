package com.sistemaseventos.checkinapp.ui.checkin;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;

public class CheckinViewModel extends AndroidViewModel {

    private UserRepository userRepository;

    // LiveData para quando o usuário é encontrado com sucesso
    private MutableLiveData<UserEntity> _userFound = new MutableLiveData<>();
    public LiveData<UserEntity> userFound = _userFound;

    // LiveData para quando o usuário NÃO é encontrado (envia o CPF digitado para pré-preencher o cadastro)
    private MutableLiveData<String> _userNotFound = new MutableLiveData<>();
    public LiveData<String> userNotFound = _userNotFound;

    // LiveData para mensagens de erro (ex: CPF inválido, sem conexão crítica)
    private MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    // LiveData para loading (opcional, para mostrar spinner enquanto busca)
    private MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public CheckinViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    /**
     * Busca o usuário pelo CPF.
     * A lógica de decidir se busca na API ou no Banco Local está dentro do Repositório.
     */
    public void searchUser(String cpf) {
        if (cpf == null || cpf.isEmpty()) {
            _error.postValue("Por favor, digite um CPF.");
            return;
        }

        // Remove caracteres não numéricos para garantir busca limpa
        String cleanCpf = cpf.replaceAll("[^0-9]", "");

        if (cleanCpf.length() != 11) {
            _error.postValue("CPF deve ter 11 dígitos.");
            return;
        }

        _isLoading.postValue(true);

        new Thread(() -> {
            // O repositório já trata a lógica: Tenta Online -> Se falhar, Tenta Offline -> Retorna null se não achar
            UserEntity user = userRepository.findUserByCpf(cleanCpf);

            // PostValue é usado porque estamos em uma thread de fundo e queremos atualizar a UI
            _isLoading.postValue(false);

            if (user != null) {
                _userFound.postValue(user);
            } else {
                // Se não achou nem online nem offline, avisa para ir ao cadastro
                _userNotFound.postValue(cleanCpf);
            }
        }).start();
    }
}
