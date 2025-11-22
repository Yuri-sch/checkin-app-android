package com.sistemaseventos.checkinapp.ui.checkin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.ui.register.RegisterActivity;
import com.sistemaseventos.checkinapp.ui.userdetail.UserDetailActivity;

public class CheckinActivity extends AppCompatActivity {

    private CheckinViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        viewModel = new ViewModelProvider(this).get(CheckinViewModel.class);

        EditText cpfInput = findViewById(R.id.cpf_input);
        Button searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(v -> {
            String cpf = cpfInput.getText().toString();
            if (!cpf.isEmpty()) {
                viewModel.searchUser(cpf);
            }
        });

        // Se achou o usuário, vai para detalhes (Etapa 4)
        viewModel.userFound.observe(this, user -> {
            Intent intent = new Intent(this, UserDetailActivity.class);
            intent.putExtra("USER_ID", user.id);
            intent.putExtra("USER_NAME", user.fullname);
            startActivity(intent);
        });

        // Se não achou, vai para cadastro (Etapa 3)
        viewModel.notFoundError.observe(this, cpf -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("CPF_EXTRA", cpf);
            startActivity(intent);
        });
    }
}
