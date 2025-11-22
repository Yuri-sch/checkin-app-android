package com.sistemaseventos.checkinapp.ui.userdetail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.ui.checkin.CheckinActivity;
import com.sistemaseventos.checkinapp.ui.eventlist.EventListActivity;

public class UserDetailActivity extends AppCompatActivity {

    private UserDetailViewModel viewModel;
    private EnrollmentAdapter adapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        userId = getIntent().getStringExtra("USER_ID");
        String userName = getIntent().getStringExtra("USER_NAME");

        viewModel = new ViewModelProvider(this).get(UserDetailViewModel.class);

        TextView welcomeText = findViewById(R.id.welcome_text);
        welcomeText.setText("Participante: " + userName);

        RecyclerView recyclerView = findViewById(R.id.recycler_enrollments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EnrollmentAdapter(this::confirmCheckIn);
        recyclerView.setAdapter(adapter);

        Button btnNewEnrollment = findViewById(R.id.btn_new_enrollment);
        btnNewEnrollment.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        viewModel.enrollments.observe(this, list -> adapter.setList(list));

        viewModel.checkinSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Check-in confirmado!", Toast.LENGTH_LONG).show();
                // Volta para o início (Etapa 2)
                Intent intent = new Intent(this, CheckinActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) viewModel.loadEnrollments(userId);
    }

    private void confirmCheckIn(EnrollmentEntity item) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Check-in")
                .setMessage("Confirmar presença neste evento?")
                .setPositiveButton("Sim", (d, w) -> viewModel.performCheckIn(item.id))
                .setNegativeButton("Não", null)
                .show();
    }
}