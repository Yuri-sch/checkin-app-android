package com.sistemaseventos.checkinapp.ui.userdetail;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import java.util.List;

public class UserDetailViewModel extends AndroidViewModel {

    private EnrollmentRepository enrollmentRepository;

    private MutableLiveData<List<EnrollmentEntity>> _enrollments = new MutableLiveData<>();
    public LiveData<List<EnrollmentEntity>> enrollments = _enrollments;

    // A VARIÁVEL QUE FALTAVA:
    private MutableLiveData<Boolean> _checkInSuccess = new MutableLiveData<>();
    public LiveData<Boolean> checkInSuccess = _checkInSuccess;

    public UserDetailViewModel(@NonNull Application application) {
        super(application);
        enrollmentRepository = new EnrollmentRepository(application);
    }

    public void loadEnrollments(String userId) {
        new Thread(() -> {
            List<EnrollmentEntity> list = enrollmentRepository.getEnrollmentsForUser(userId);
            _enrollments.postValue(list);
        }).start();
    }

    public void performCheckIn(String enrollmentId) {
        new Thread(() -> {
            boolean success = enrollmentRepository.performCheckIn(enrollmentId);
            _checkInSuccess.postValue(success);
        }).start();
    }
}
