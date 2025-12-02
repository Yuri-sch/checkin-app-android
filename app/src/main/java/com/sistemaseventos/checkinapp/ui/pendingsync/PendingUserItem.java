package com.sistemaseventos.checkinapp.ui.pendingsync;

import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import java.util.List;

public class PendingUserItem {
    public UserEntity user;
    public List<EnrollmentWithEvent> enrollments;

    public PendingUserItem(UserEntity user, List<EnrollmentWithEvent> enrollments) {
        this.user = user;
        this.enrollments = enrollments;
    }
}
