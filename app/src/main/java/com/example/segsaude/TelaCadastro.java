package com.example.segsaude;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TelaCadastro extends AppCompatActivity {

    private EditText editNome, editEmail, editSenha;
    private Button btCadastar;

    // mensagens usadas no Snackbar
    String[] mensagem = {"Preencha todos os campos", "Cadastro realizado com sucesso"};
    String usuarioID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_cadastro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        IniciarComponentes();

        btCadastar.setOnClickListener(v -> {

            String nome  = editNome.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String senha = editSenha.getText().toString().trim();

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
                Snackbar mensagemErro = Snackbar.make(v, mensagem[0], Snackbar.LENGTH_SHORT);
                CentralizarTextoSnackbar(mensagemErro);
                return;
            }

            // >>> DESCOBRIR TIPO PELO E-MAIL <<<
            String tipoUsuario = obterTipoPorEmail(email);
            if (tipoUsuario == null) {
                CentralizarTextoSnackbar(Snackbar.make(
                        v,
                        "Domínio de e-mail inválido. Use @cliente.com ou @admin.com",
                        Snackbar.LENGTH_LONG
                ));
                return; // não tenta cadastrar no Firebase
            }

            // tudo ok -> cadastrar
            CadastrarUsuario(v, nome, email, senha, tipoUsuario);
        });
    }

    // retorna "consultor", "administrador" ou null se o domínio for inválido
    private String obterTipoPorEmail(String email) {
        String lower = email.toLowerCase();

        if (lower.endsWith("@consultor.com")) {
            return "consultor";
        } else if (lower.endsWith("@admin.com")) {
            return "administrador";
        } else {
            return null;
        }
    }

    // centraliza e estiliza qualquer Snackbar
    private void CentralizarTextoSnackbar(Snackbar snackbar) {
        snackbar.setBackgroundTint(Color.WHITE);
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.verde));

        TextView textoSnackbar =
                snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        textoSnackbar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textoSnackbar.setGravity(Gravity.CENTER_HORIZONTAL);

        snackbar.show();
    }

    // cria o usuário no Firebase Authentication
    private void CadastrarUsuario(View v,
                                  String nome,
                                  String email,
                                  String senha,
                                  String tipoUsuario) {

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            // se criou no Auth, salva os dados na coleção "usuarios"
                            SalvarDadosUsuario(nome, email, tipoUsuario);

                            Snackbar mensagemSucesso =
                                    Snackbar.make(v, mensagem[1], Snackbar.LENGTH_SHORT);
                            CentralizarTextoSnackbar(mensagemSucesso);

                            // >>> DEPOIS DE CADASTRAR, VOLTA PRA TELA DE LOGIN
                            Intent i = new Intent(TelaCadastro.this, TelaLogin.class);
                            startActivity(i);
                            finish();

                        } else {

                            String erro;
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                erro = "Digite uma senha com no mínimo 6 caracteres";
                            } catch (FirebaseAuthUserCollisionException e) {
                                erro = "Esta conta já foi cadastrada";
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                erro = "E-mail inválido";
                            } catch (Exception e) {
                                erro = "Erro ao cadastrar usuário: " + e.getMessage();
                                Log.e("auth_erro", "Erro ao cadastrar", e);
                            }

                            Snackbar mensagemErro =
                                    Snackbar.make(v, erro, Snackbar.LENGTH_SHORT);
                            CentralizarTextoSnackbar(mensagemErro);
                        }
                    }
                });
    }

    // salva os dados do usuário na COLEÇÃO "usuarios"
    private void SalvarDadosUsuario(String nome, String email, String tipoUsuario) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Mapa com os campos que vão para a "tabela"
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nome", nome);
        usuario.put("email", email);
        usuario.put("tipo", tipoUsuario);  // "cliente" ou "administrador"

        // UID do usuário recém-criado no Auth
        usuarioID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference documentReference =
                db.collection("usuarios").document(usuarioID);

        documentReference.set(usuario)
                .addOnSuccessListener(unused ->
                        Log.d("db", "Sucesso ao salvar os dados"))
                .addOnFailureListener(e ->
                        Log.d("db_erro", "Erro ao salvar os dados: " + e));
    }

    private void IniciarComponentes() {
        editNome   = findViewById(R.id.edit_nome);
        editEmail  = findViewById(R.id.edit_email);
        editSenha  = findViewById(R.id.edit_senha);
        btCadastar = findViewById(R.id.bt_cadastrar);
    }
}
