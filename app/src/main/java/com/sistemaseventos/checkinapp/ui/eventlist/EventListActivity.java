package com.sistemaseventos.checkinapp.ui.eventlist;

import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import com.sistemaseventos.checkinapp.ui.BaseActivity;

public class EventListActivity extends BaseActivity {

    private EventListViewModel viewModel;
    private EventListAdapter adapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        userId = getIntent().getStringExtra("USER_ID");
        viewModel = new ViewModelProvider(this).get(EventListViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recycler_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventListAdapter(this::confirmEnroll);
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.search(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.search(newText);
                return false;
            }
        });

        viewModel.events.observe(this, list -> adapter.setList(list));

        viewModel.enrollSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Inscrição realizada!", Toast.LENGTH_SHORT).show();
                finish(); // Volta para UserDetail
            }
        });

        viewModel.search(""); // Carrega tudo inicialmente
    }

    private void confirmEnroll(EventEntity event) {
        new AlertDialog.Builder(this)
                .setTitle("Inscrever")
                .setMessage("Inscrever usuário em " + event.eventName + "?")
                .setPositiveButton("Sim", (d, w) -> viewModel.enroll(userId, event.id))
                .setNegativeButton("Não", null)
                .show();
    }
}
