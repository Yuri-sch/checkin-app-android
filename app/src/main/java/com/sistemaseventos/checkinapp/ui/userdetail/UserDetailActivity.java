package com.sistemaseventos.checkinapp.ui.userdetail;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import com.sistemaseventos.checkinapp.ui.BaseActivity;
import com.sistemaseventos.checkinapp.ui.eventlist.EventListActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDetailActivity extends BaseActivity implements EnrollmentAdapter.OnInteractionListener {

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

        // Recebe a data de nascimento da Intent (passada como Long ou String)
        // Supondo que CheckinActivity passe como long (timestamp)
        long birthDateMillis = getIntent().getLongExtra("USER_BIRTHDATE", 0);

        viewModel = new ViewModelProvider(this).get(UserDetailViewModel.class);

        TextView textName = findViewById(R.id.text_user_name);
        TextView textCpf = findViewById(R.id.text_user_cpf);
        TextView textEmail = findViewById(R.id.text_user_email);
        TextView textBirth = findViewById(R.id.text_user_birth); // Novo campo

        textName.setText(userName != null ? userName : "Nome não disponível");
        textCpf.setText(userCpf != null ? "CPF: " + userCpf : "");
        textEmail.setText(userEmail != null ? userEmail : "");

        if (birthDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textBirth.setText("Nascimento: " + sdf.format(new Date(birthDateMillis)));
            textBirth.setVisibility(View.VISIBLE);
        } else {
            textBirth.setVisibility(View.GONE);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_enrollments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EnrollmentAdapter(this);
        recyclerView.setAdapter(adapter);

        Button btnNewEnrollment = findViewById(R.id.btn_new_enrollment);
        btnNewEnrollment.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        viewModel.enrollments.observe(this, list -> adapter.setList(list));

        viewModel.actionSuccess.observe(this, success -> {
            if (success) {
                if (userId != null) viewModel.loadEnrollments(userId);
            } else {
                Toast.makeText(this, "Ação pendente (offline) ou erro.", Toast.LENGTH_SHORT).show();
                // Recarrega mesmo assim para mostrar estado offline se tiver mudado
                if (userId != null) viewModel.loadEnrollments(userId);
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

    @Override
    protected void onSyncFinished() {
        super.onSyncFinished();

        // CORREÇÃO: O ID do usuário pode ter mudado durante a sincronização (de Temp para Real).
        // Usamos o CPF (que é constante) para descobrir o ID atual antes de recarregar a lista.
        String userCpf = getIntent().getStringExtra("USER_CPF");

        if (userCpf != null) {
            // Busca o usuário atualizado
            viewModel.getUserByCpf(userCpf).observe(this, user -> {
                if (user != null) {
                    // Atualiza a variável local userId com o ID oficial do banco/API
                    this.userId = user.id;

                    // Agora sim carrega as inscrições com o ID certo
                    viewModel.loadEnrollments(this.userId);
                }
            });
        } else {
            // Fallback caso não tenha CPF (raro)
            if (userId != null) viewModel.loadEnrollments(userId);
        }
    }

    @Override
    public void onCheckInClick(EnrollmentWithEvent item) {
        if (item.enrollment.checkIn != null) {
            Toast.makeText(this, "Check-in já realizado.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("CANCELED".equals(item.enrollment.status)) {
            Toast.makeText(this, "Inscrição cancelada.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Presença")
                .setMessage("Evento: " + (item.event != null ? item.event.eventName : "Desconhecido"))
                .setPositiveButton("Confirmar", (dialog, which) -> viewModel.performCheckIn(item.enrollment.id))
                .setNegativeButton("Voltar", null)
                .show();
    }
}