package com.sistemaseventos.checkinapp.ui.register;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;

public class RegisterViewModel extends AndroidViewModel {

    private UserRepository repository;
    private MutableLiveData<UserEntity> _success = new MutableLiveData<>();
    public LiveData<UserEntity> success = _success;

    public RegisterViewModel(Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public void register(String cpf, String email) {
        new Thread(() -> {
            UserEntity user = repository.registerSimpleUser(cpf, email);
            if (user != null) {
                _success.postValue(user);
            }
        }).start();
    }
}
