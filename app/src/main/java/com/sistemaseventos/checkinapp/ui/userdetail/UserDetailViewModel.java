package com.sistemaseventos.checkinapp.ui.userdetail;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent; // Novo import
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import java.util.List;

public class UserDetailViewModel extends AndroidViewModel {

    private EnrollmentRepository enrollmentRepository;

    // Agora lista EnrollmentWithEvent
    private MutableLiveData<List<EnrollmentWithEvent>> _enrollments = new MutableLiveData<>();
    public LiveData<List<EnrollmentWithEvent>> enrollments = _enrollments;

    private MutableLiveData<Boolean> _actionSuccess = new MutableLiveData<>();
    public LiveData<Boolean> actionSuccess = _actionSuccess;

    public UserDetailViewModel(@NonNull Application application) {
        super(application);
        enrollmentRepository = new EnrollmentRepository(application);
    }

    public void loadEnrollments(String userId) {
        new Thread(() -> {
            // Busca com JOIN
            List<EnrollmentWithEvent> list = enrollmentRepository.getEnrollmentsWithEventsForUser(userId);
            _enrollments.postValue(list);
        }).start();
    }

    public void performCheckIn(String enrollmentId) {
        new Thread(() -> {
            boolean success = enrollmentRepository.performCheckIn(enrollmentId);
            _actionSuccess.postValue(success);
        }).start();
    }

    public void cancelEnrollment(String enrollmentId) {
        new Thread(() -> {
            boolean success = enrollmentRepository.cancelEnrollment(enrollmentId);
            _actionSuccess.postValue(success);
        }).start();
    }
}