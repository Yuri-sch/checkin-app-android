package com.sistemaseventos.checkinapp.ui.userdetail;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import java.util.List;

public class UserDetailViewModel extends AndroidViewModel {

    private EnrollmentRepository repository;
    private MutableLiveData<List<EnrollmentEntity>> _enrollments = new MutableLiveData<>();
    public LiveData<List<EnrollmentEntity>> enrollments = _enrollments;
    private MutableLiveData<Boolean> _checkinSuccess = new MutableLiveData<>();
    public LiveData<Boolean> checkinSuccess = _checkinSuccess;

    public UserDetailViewModel(Application application) {
        super(application);
        repository = new EnrollmentRepository(application);
    }

    public void loadEnrollments(String userId) {
        new Thread(() -> {
            List<EnrollmentEntity> list = repository.getEnrollmentsForUser(userId);
            _enrollments.postValue(list);
        }).start();
    }

    public void performCheckIn(String enrollmentId) {
        new Thread(() -> {
            boolean success = repository.performCheckIn(enrollmentId);
            _checkinSuccess.postValue(success);
        }).start();
    }
}
