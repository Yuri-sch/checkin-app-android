package com.sistemaseventos.checkinapp.ui.register;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;

public class RegisterViewModel extends AndroidViewModel {

    private UserRepository userRepository;

    // CORREÇÃO: A variável que faltava
    private MutableLiveData<UserEntity> _registerSuccess = new MutableLiveData<>();
    public LiveData<UserEntity> registerSuccess = _registerSuccess;

    private MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    public void registerSimpleUser(String cpf, String email) {
        if (cpf.isEmpty() || email.isEmpty()) {
            _error.postValue("Preencha CPF e E-mail.");
            return;
        }

        new Thread(() -> {
            UserEntity newUser = userRepository.registerSimpleUser(cpf, email);

            if (newUser != null) {
                _registerSuccess.postValue(newUser);
            } else {
                _error.postValue("Erro ao realizar cadastro.");
            }
        }).start();
    }
}
