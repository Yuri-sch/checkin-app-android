package com.sistemaseventos.checkinapp.ui.checkin;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;

public class CheckinViewModel extends AndroidViewModel {

    private UserRepository repository;
    private MutableLiveData<UserEntity> _userFound = new MutableLiveData<>();
    public LiveData<UserEntity> userFound = _userFound;
    private MutableLiveData<String> _notFoundError = new MutableLiveData<>();
    public LiveData<String> notFoundError = _notFoundError;

    public CheckinViewModel(Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public void searchUser(String cpf) {
        new Thread(() -> {
            UserEntity user = repository.findUserByCpf(cpf);
            if (user != null) {
                _userFound.postValue(user);
            } else {
                _notFoundError.postValue(cpf); // Envia o CPF que falhou
            }
        }).start();
    }
}
