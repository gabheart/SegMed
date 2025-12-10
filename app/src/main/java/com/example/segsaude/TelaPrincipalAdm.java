package com.example.segsaude;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TelaPrincipalAdm extends AppCompatActivity {

    private LinearLayout containerTabela;
    private EditText editValorPlano;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_principal);

        // FIRESTORE
        db = FirebaseFirestore.getInstance();

        // COMPONENTES
        Spinner spinnerIdade      = findViewById(R.id.spinnerIdade);
        Spinner spinnerPlano      = findViewById(R.id.spinnerPlano);
        Spinner spinnerOperadora  = findViewById(R.id.spinnerOperadora);
        Spinner spinnerAcomodacao = findViewById(R.id.spinnerAcomodacao);
        CheckBox checkCopart      = findViewById(R.id.checkCopart);

        Button btDeslogar = findViewById(R.id.bt_deslogar);
        Button btnAplicar = findViewById(R.id.btnAplicarPlano);

        containerTabela  = findViewById(R.id.containerTabelaItens);
        editValorPlano   = findViewById(R.id.edit_valor_plano);

        // DADOS
        String[] idades = {
                "00 a 18", "19 a 23", "24 a 28", "29 a 33",
                "34 a 38", "39 a 43", "44 a 48", "49 a 53",
                "54 a 58", "59+"
        };

        String[] operadoras = {"Hapvida", "Unimed", "Amil"};
        String[] planos = {"Básico", "Intermediário", "Premium"};
        String[] acomodacoes = {"Enfermaria", "Apartamento", "Apartamento + Suíte"};

        // ADAPTERS
        spinnerIdade.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, idades)
        );

        spinnerOperadora.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, operadoras)
        );

        spinnerPlano.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, planos)
        );

        spinnerAcomodacao.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, acomodacoes)
        );

        // DESLOGAR
        btDeslogar.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(TelaPrincipalAdm.this, TelaLogin.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // APLICAR / SALVAR PLANO
        btnAplicar.setOnClickListener(v -> {

            String idade     = spinnerIdade.getSelectedItem().toString();
            String operadora = spinnerOperadora.getSelectedItem().toString();
            String plano     = spinnerPlano.getSelectedItem().toString();
            String acom      = spinnerAcomodacao.getSelectedItem().toString();
            String copart    = checkCopart.isChecked() ? "Com Copart." : "Sem Copart.";

            String valorStr  = editValorPlano.getText().toString().trim();

            if (valorStr.isEmpty()) {
                Toast.makeText(this, "Digite o valor do plano", Toast.LENGTH_SHORT).show();
                return;
            }

            double valor;
            try {
                // troca vírgula por ponto se o usuário digitar assim
                valor = Double.parseDouble(valorStr.replace(",", "."));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) Salva no Firestore
            salvarPlanoNoFirestore(idade, operadora, plano, acom, copart, valor);

            // 2) Mostra na tela (somente visual, não é o banco)
            adicionarLinhaNaTela(idade, operadora, plano, acom, copart, valor);

            // limpa o campo de valor
            editValorPlano.setText("");
        });
    }

    private void salvarPlanoNoFirestore(String idade,
                                        String operadora,
                                        String plano,
                                        String acom,
                                        String copart,
                                        double valor) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("idade", idade);
        dados.put("operadora", operadora);
        dados.put("plano", plano);
        dados.put("acomodacao", acom);
        dados.put("coparticipacao", copart);
        dados.put("valor", valor);

        db.collection("planos_saude")
                .add(dados)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Plano salvo!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void adicionarLinhaNaTela(String idade,
                                      String operadora,
                                      String plano,
                                      String acom,
                                      String copart,
                                      double valor) {

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.VERTICAL);
        linha.setPadding(16, 16, 16, 16);
        linha.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.white)
        );

        linha.addView(criarTexto("Idade: " + idade));
        linha.addView(criarTexto("Operadora: " + operadora));
        linha.addView(criarTexto("Plano: " + plano));
        linha.addView(criarTexto("Acomodação: " + acom));
        linha.addView(criarTexto("Coparticipação: " + copart));
        linha.addView(criarTexto(String.format("Valor: R$ %.2f", valor)));

        containerTabela.addView(linha);
    }

    private TextView criarTexto(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setPadding(8, 8, 8, 8);
        t.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        return t;
    }
}
