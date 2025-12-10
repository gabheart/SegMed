package com.example.segsaude;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TelaLogin extends AppCompatActivity {

    private TextView textoTelaCadastro;
    private EditText editEmail, editSenha;
    private Button btEntrar;
    private ProgressBar barraDeProgresso;

    String[] mensagem = {"Preencha todos os campos"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        IniciarComponentes();

        // ir para tela de cadastro
        textoTelaCadastro.setOnClickListener(v -> {
            Intent intencao = new Intent(TelaLogin.this, TelaCadastro.class);
            startActivity(intencao);
        });

        // botão Entrar
        btEntrar.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String senha = editSenha.getText().toString().trim();

            if (email.isEmpty() || senha.isEmpty()) {
                CentralizarTextoSnackbar(
                        Snackbar.make(v, mensagem[0], Snackbar.LENGTH_SHORT)
                );
            } else {
                AutenticarUsuario(v, email, senha);
            }
        });
    }

    // 1) faz login no FirebaseAuth
    private void AutenticarUsuario(View v, String emailDigitado, String senha) {

        barraDeProgresso.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(emailDigitado, senha)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            FirebaseUser usuarioAtual =
                                    FirebaseAuth.getInstance().getCurrentUser();

                            if (usuarioAtual != null) {
                                String uid = usuarioAtual.getUid();
                                // 2) depois do login, verifica os dados no Firestore
                                verificarUsuarioNoFirestore(v, uid, emailDigitado);
                            } else {
                                barraDeProgresso.setVisibility(View.INVISIBLE);
                                CentralizarTextoSnackbar(Snackbar.make(
                                        v,
                                        "Erro: usuário autenticado é nulo",
                                        Snackbar.LENGTH_SHORT
                                ));
                            }

                        } else {
                            barraDeProgresso.setVisibility(View.INVISIBLE);

                            String erro = "Erro ao logar o usuário";
                            if (task.getException() != null) {
                                erro += ": " + task.getException().getMessage();
                            }

                            CentralizarTextoSnackbar(Snackbar.make(
                                    v,
                                    erro,
                                    Snackbar.LENGTH_LONG
                            ));
                        }
                    }
                });
    }

    // 2) verifica se existe documento em "usuarios" com esse UID
    // e se o campo "email" bate com o email digitado
    private void verificarUsuarioNoFirestore(View v, String uid, String emailDigitado) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener((DocumentSnapshot documentSnapshot) -> {

                    barraDeProgresso.setVisibility(View.INVISIBLE);

                    if (documentSnapshot.exists()) {

                        String emailBanco = documentSnapshot.getString("email");

                        if (emailBanco != null &&
                                emailBanco.trim().equalsIgnoreCase(emailDigitado.trim())) {

                            // e-mail confere, agora vê o tipo
                            String tipo = documentSnapshot.getString("tipo");
                            if (tipo != null) tipo = tipo.toLowerCase().trim();

                            if ("consultor".equals(tipo)) {
                                // tela do consultor
                                Intent intent = new Intent(
                                        TelaLogin.this,
                                        TelaPrincipalConsultor.class
                                );
                                startActivity(intent);

                            } else if ("administrador".equals(tipo)) {
                                // tela do administrador
                                Intent intent = new Intent(
                                        TelaLogin.this,
                                        TelaPrincipalAdm.class
                                );
                                startActivity(intent);

                            } else {
                                CentralizarTextoSnackbar(Snackbar.make(
                                        v,
                                        "Tipo de usuário inválido ou não definido",
                                        Snackbar.LENGTH_LONG
                                ));
                                FirebaseAuth.getInstance().signOut();
                                return;
                            }

                            finish(); // fecha tela de login

                        } else {
                            FirebaseAuth.getInstance().signOut();
                            CentralizarTextoSnackbar(Snackbar.make(
                                    v,
                                    "E-mail do login não confere com o cadastrado",
                                    Snackbar.LENGTH_LONG
                            ));
                        }

                    } else {
                        FirebaseAuth.getInstance().signOut();
                        CentralizarTextoSnackbar(Snackbar.make(
                                v,
                                "Usuário não encontrado na coleção 'usuarios'",
                                Snackbar.LENGTH_LONG
                        ));
                    }
                })
                .addOnFailureListener(e -> {
                    barraDeProgresso.setVisibility(View.INVISIBLE);
                    CentralizarTextoSnackbar(Snackbar.make(
                            v,
                            "Erro ao acessar dados do usuário: " + e.getMessage(),
                            Snackbar.LENGTH_LONG
                    ));
                });
    }

    private void CentralizarTextoSnackbar(Snackbar snackbar) {
        snackbar.setBackgroundTint(android.graphics.Color.WHITE);
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.verde));

        TextView textoSnackbar =
                snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        textoSnackbar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textoSnackbar.setGravity(Gravity.CENTER_HORIZONTAL);

        snackbar.show();
    }

    private void IniciarComponentes() {
        textoTelaCadastro = findViewById(R.id.texto_tela_de_cadastro);
        editEmail        = findViewById(R.id.edit_email);
        editSenha        = findViewById(R.id.edit_senha);
        btEntrar         = findViewById(R.id.bt_entrar);
        barraDeProgresso = findViewById(R.id.barra_progresso);
    }
}
