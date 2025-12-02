package com.sistemaseventos.checkinapp.ui.pendingsync;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class PendingSyncActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PendingSyncAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;

    private UserRepository userRepo;
    private EnrollmentRepository enrollmentRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vamos usar um layout simples temporário ou criar um novo
        setContentView(R.layout.activity_event_list); // Reutilizando layout de lista que já existe no seu projeto para facilitar

        // Ajuste de título se possível, ou ignore
        if(getSupportActionBar() != null) getSupportActionBar().setTitle("Pendentes de Envio");

        recyclerView = findViewById(R.id.recycler_events); // Reutilizando ID do layout event_list
        progressBar = findViewById(R.id.progress_loading); // Reutilizando ID
        // emptyView teria que ser adicionado no layout ou tratado via visibility do recycler

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingSyncAdapter();
        recyclerView.setAdapter(adapter);

        userRepo = new UserRepository(this);
        enrollmentRepo = new EnrollmentRepository(this);

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Busca usuários não sincronizados
            List<UserEntity> unsyncedUsers = userRepo.getUnsyncedUsersList();
            List<PendingUserItem> uiList = new ArrayList<>();

            // 2. Para cada usuário, busca suas inscrições (eventos)
            for (UserEntity user : unsyncedUsers) {
                // Aqui usamos o método que já existe no seu repo que faz o join
                // Nota: esse método getEnrollmentsWithEventsForUser tenta ir na API primeiro.
                // Como queremos ver o estado LOCAL, o ideal seria chamar direto o DAO ou garantir que o Repo trate o offline.
                // O seu Repo atual "Sempre retorna o local" no final, então vai funcionar.
                List<EnrollmentWithEvent> enrollments = enrollmentRepo.getEnrollmentsWithEventsForUser(user.id);

                uiList.add(new PendingUserItem(user, enrollments));
            }

            // 3. Atualiza UI
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(uiList);
                if (uiList.isEmpty()) {
                    // Se quiser mostrar mensagem de vazio
                    android.widget.Toast.makeText(this, "Nenhum usuário pendente!", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
