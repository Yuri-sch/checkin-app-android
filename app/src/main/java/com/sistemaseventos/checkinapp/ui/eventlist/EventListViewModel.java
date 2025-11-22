package com.sistemaseventos.checkinapp.ui.eventlist;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.EventRepository;
import java.util.List;

public class EventListViewModel extends AndroidViewModel {

    private EventRepository eventRepository;
    private EnrollmentRepository enrollmentRepository;
    private MutableLiveData<List<EventEntity>> _events = new MutableLiveData<>();
    public LiveData<List<EventEntity>> events = _events;
    private MutableLiveData<Boolean> _enrollSuccess = new MutableLiveData<>();
    public LiveData<Boolean> enrollSuccess = _enrollSuccess;

    public EventListViewModel(Application application) {
        super(application);
        eventRepository = new EventRepository(application);
        enrollmentRepository = new EnrollmentRepository(application);
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
}
