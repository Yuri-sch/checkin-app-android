package com.sistemaseventos.checkinapp.ui.userdetail;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent; // Novo import
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;

import java.util.Collections;
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

            list.sort((item1, item2) -> {
                int p1 = getPriority(item1);
                int p2 = getPriority(item2);

                if (p1 != p2) {
                    return Integer.compare(p1, p2);
                }

                if (item1.event != null && item2.event != null && item1.event.eventDate != null && item2.event.eventDate != null) {
                    return item1.event.eventDate.compareTo(item2.event.eventDate);
                }

                return 0;
            });
            _enrollments.postValue(list);
        }).start();
    }

    private int getPriority(EnrollmentWithEvent item) {
        // Se for cancelado, vai pro final (Peso 2)
        if ("CANCELED".equals(item.enrollment.status)) {
            return 2;
        }
        // Se já fez check-in, fica no meio (Peso 1)
        if (item.enrollment.checkIn != null) {
            return 1;
        }
        // Se está ativo e sem check-in, é prioridade total (Peso 0)
        return 0;
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