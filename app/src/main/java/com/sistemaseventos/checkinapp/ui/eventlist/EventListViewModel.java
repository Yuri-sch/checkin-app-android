package com.sistemaseventos.checkinapp.ui.eventlist;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.EventRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventListViewModel extends AndroidViewModel {

    private EventRepository eventRepository;
    private EnrollmentRepository enrollmentRepository;
    private MutableLiveData<List<EventEntity>> _events = new MutableLiveData<>();
    public LiveData<List<EventEntity>> events = _events;
    private MutableLiveData<Boolean> _enrollSuccess = new MutableLiveData<>();
    public LiveData<Boolean> enrollSuccess = _enrollSuccess;
    public MutableLiveData<Map<String, String>> enrollmentStatusMap = new MutableLiveData<>();
    private EnrollmentDao enrollmentDao;
    public EventListViewModel(Application application) {
        super(application);
        eventRepository = new EventRepository(application);
        enrollmentRepository = new EnrollmentRepository(application);
        this.enrollmentDao = AppDatabase.getInstance(application).enrollmentDao();
    }

    public void search(String query) {
        new Thread(() -> {
            List<EventEntity> list;
            if (query.isEmpty()) list = eventRepository.getAllEvents();
            else list = eventRepository.searchEventsByName(query);
            _events.postValue(list);
        }).start();
    }

    public void enroll(String userId, String eventId) {
        new Thread(() -> {
            boolean success = enrollmentRepository.createEnrollment(userId, eventId);
            _enrollSuccess.postValue(success);
        }).start();
    }

    public void loadUserEnrollments(String userId) {
        new Thread(() -> {
            List<EnrollmentWithEvent> userEnrollments = enrollmentDao.getEnrollmentsWithEvents(userId);

            Map<String, String> statusMap = new HashMap<>();

            for (EnrollmentWithEvent e : userEnrollments) {
                statusMap.put(e.event.id, e.enrollment.status);
            }

            enrollmentStatusMap.postValue(statusMap);
        }).start();
    }
}
