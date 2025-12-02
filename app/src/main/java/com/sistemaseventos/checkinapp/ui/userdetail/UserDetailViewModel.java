package com.sistemaseventos.checkinapp.ui.userdetail;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity; // Importe isso
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.UserRepository; // Importe isso
import java.util.List;

public class UserDetailViewModel extends AndroidViewModel {

    private EnrollmentRepository enrollmentRepository;
    private UserRepository userRepository; // Adicione o repo de usuário

    private MutableLiveData<List<EnrollmentWithEvent>> _enrollments = new MutableLiveData<>();
    public LiveData<List<EnrollmentWithEvent>> enrollments = _enrollments;

    private MutableLiveData<Boolean> _actionSuccess = new MutableLiveData<>();
    public LiveData<Boolean> actionSuccess = _actionSuccess;

    public UserDetailViewModel(@NonNull Application application) {
        super(application);
        enrollmentRepository = new EnrollmentRepository(application);
        userRepository = new UserRepository(application); // Inicialize aqui
    }

    public void loadEnrollments(String userId) {
        new Thread(() -> {
            List<EnrollmentWithEvent> list = enrollmentRepository.getEnrollmentsWithEventsForUser(userId);
            _enrollments.postValue(list);
        }).start();
    }

    // --- NOVO MÉTODO PARA RECUPERAR ID APÓS SYNC ---
    public LiveData<UserEntity> getUserByCpf(String cpf) {
        MutableLiveData<UserEntity> result = new MutableLiveData<>();
        new Thread(() -> {
            // Busca no repositório (que vai consultar API ou Banco Local)
            UserEntity user = userRepository.findUserByCpf(cpf);
            result.postValue(user);
        }).start();
        return result;
    }
    // -----------------------------------------------

    public void performCheckIn(String enrollmentId) {
        new Thread(() -> {
            boolean success = enrollmentRepository.performCheckIn(enrollmentId);
            _actionSuccess.postValue(success);
        }).start();
    }

    // Método de cancelamento removido anteriormente (se ainda existir, pode manter ou remover)
    public void cancelEnrollment(String enrollmentId) {
        new Thread(() -> {
            boolean success = enrollmentRepository.cancelEnrollment(enrollmentId);
            _actionSuccess.postValue(success);
        }).start();
    }
}