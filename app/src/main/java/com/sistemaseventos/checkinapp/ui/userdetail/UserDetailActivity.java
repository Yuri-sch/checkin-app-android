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
        String userCpf = getIntent().getStringExtra("USER_CPF");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");

        viewModel = new ViewModelProvider(this).get(UserDetailViewModel.class);

        TextView textName = findViewById(R.id.text_user_name);
        TextView textCpf = findViewById(R.id.text_user_cpf);
        TextView textEmail = findViewById(R.id.text_user_email);

        textName.setText(userName != null ? userName : "Nome não disponível");
        textCpf.setText(userCpf != null ? "CPF: " + userCpf : "");
        textEmail.setText(userEmail != null ? userEmail : "");

        RecyclerView recyclerView = findViewById(R.id.recycler_enrollments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EnrollmentAdapter(this::showCheckInConfirmation);
        recyclerView.setAdapter(adapter);

        Button btnNewEnrollment = findViewById(R.id.btn_new_enrollment);
        btnNewEnrollment.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        viewModel.enrollments.observe(this, list -> adapter.setList(list));

        // Agora a variável checkInSuccess EXISTE no ViewModel
        viewModel.checkInSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Check-in realizado com sucesso!", Toast.LENGTH_LONG).show();
                if (userId != null) viewModel.loadEnrollments(userId);
            } else {
                Toast.makeText(this, "Erro ao realizar check-in.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            viewModel.loadEnrollments(userId);
        }
    }

    private void showCheckInConfirmation(EnrollmentEntity item) {
        if (item.checkIn != null) {
            Toast.makeText(this, "Check-in já foi realizado.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Presença")
                .setMessage("Realizar check-in para este evento?")
                .setPositiveButton("Confirmar", (dialog, which) -> viewModel.performCheckIn(item.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}