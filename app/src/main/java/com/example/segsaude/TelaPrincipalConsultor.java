package com.example.segsaude;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class TelaPrincipalConsultor extends AppCompatActivity {

    private Spinner spinnerIdadeCliente, spinnerOperadoraCliente,
            spinnerPlanoCliente, spinnerAcomodacaoCliente;
    private CheckBox checkCopartCliente;
    private Button btBuscar, btVoltar;
    private LinearLayout containerTabelaCliente;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_principal_cliente);

        db = FirebaseFirestore.getInstance();

        // COMPONENTES
        spinnerIdadeCliente       = findViewById(R.id.spinnerIdadeCliente);
        spinnerOperadoraCliente   = findViewById(R.id.spinnerOperadoraCliente);
        spinnerPlanoCliente       = findViewById(R.id.spinnerPlanoCliente);
        spinnerAcomodacaoCliente  = findViewById(R.id.spinnerAcomodacaoCliente);
        checkCopartCliente        = findViewById(R.id.checkCopartCliente);

        btBuscar                  = findViewById(R.id.bt_buscar_planos);
        btVoltar                  = findViewById(R.id.bt_voltar_cliente);
        containerTabelaCliente    = findViewById(R.id.containerTabelaCliente);

        // DADOS (mesmos do ADM)
        String[] idades = {
                "00 a 18", "19 a 23", "24 a 28", "29 a 33",
                "34 a 38", "39 a 43", "44 a 48", "49 a 53",
                "54 a 58", "59+"
        };

        String[] operadoras = {"Hapvida", "Unimed", "Amil"};
        String[] planos = {"Básico", "Intermediário", "Premium"};
        String[] acomodacoes = {"Enfermaria", "Apartamento", "Apartamento + Suíte"};

        // ADAPTERS
        spinnerIdadeCliente.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, idades)
        );

        spinnerOperadoraCliente.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, operadoras)
        );

        spinnerPlanoCliente.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, planos)
        );

        spinnerAcomodacaoCliente.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, acomodacoes)
        );

        // VOLTAR
        btVoltar.setOnClickListener(v -> {
            // Se quiser deslogar o usuário ao voltar:
            // FirebaseAuth.getInstance().signOut();

            Intent i = new Intent(TelaPrincipalConsultor.this, TelaLogin.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // BUSCAR PLANO
        btBuscar.setOnClickListener(v -> {

            String idade     = spinnerIdadeCliente.getSelectedItem().toString();
            String operadora = spinnerOperadoraCliente.getSelectedItem().toString();
            String plano     = spinnerPlanoCliente.getSelectedItem().toString();
            String acom      = spinnerAcomodacaoCliente.getSelectedItem().toString();
            String copart    = checkCopartCliente.isChecked() ? "Com Copart." : "Sem Copart.";

            buscarPlanoNoFirestore(idade, operadora, plano, acom, copart);
        });
    }

    private void buscarPlanoNoFirestore(String idade,
                                        String operadora,
                                        String plano,
                                        String acom,
                                        String copart) {

        containerTabelaCliente.removeAllViews();

        db.collection("planos_saude")
                .whereEqualTo("idade", idade)
                .whereEqualTo("operadora", operadora)
                .whereEqualTo("plano", plano)
                .whereEqualTo("acomodacao", acom)
                .whereEqualTo("coparticipacao", copart)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        adicionarTextoSimples("Nenhum plano encontrado para essa combinação.");
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {

                        String idadeDoc     = doc.getString("idade");
                        String operadoraDoc = doc.getString("operadora");
                        String planoDoc     = doc.getString("plano");
                        String acomDoc      = doc.getString("acomodacao");
                        String copartDoc    = doc.getString("coparticipacao");
                        Double valorDoc     = doc.getDouble("valor");

                        double valor = (valorDoc != null) ? valorDoc : 0.0;

                        // Monta um "card" igual ao do ADM, mas só leitura
                        LinearLayout linha = new LinearLayout(this);
                        linha.setOrientation(LinearLayout.VERTICAL);
                        linha.setPadding(16, 16, 16, 16);
                        linha.setBackgroundColor(
                                ContextCompat.getColor(this, android.R.color.white)
                        );

                        linha.addView(criarTexto("Idade: " + idadeDoc));
                        linha.addView(criarTexto("Operadora: " + operadoraDoc));
                        linha.addView(criarTexto("Plano: " + planoDoc));
                        linha.addView(criarTexto("Acomodação: " + acomDoc));
                        linha.addView(criarTexto("Coparticipação: " + copartDoc));
                        linha.addView(criarTexto(String.format("Valor: R$ %.2f", valor)));

                        containerTabelaCliente.addView(linha);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Erro ao buscar planos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void adicionarTextoSimples(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setPadding(16, 16, 16, 16);
        t.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        containerTabelaCliente.addView(t);
    }

    private TextView criarTexto(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setPadding(8, 8, 8, 8);
        t.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        return t;
    }
}
