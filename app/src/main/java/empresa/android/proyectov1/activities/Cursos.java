package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

import empresa.android.proyectov1.R;

public class Cursos extends AppCompatActivity {
    private ChipGroup cg;
    private String rol;
    private Button btnGuardar, btnCancelar;
    private String uid;
    private String nodo;
    private String[] lista = {"Cálculo I", "Cálculo II", "Java Core", "Algoritmos", "Física I", "Base de Datos"};
    private ColorStateList bgList, textList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rol = getIntent().getStringExtra("rol");

        if (rol != null && rol.equals("profesor")) {
            setContentView(R.layout.activity_especialidades);
            cg = findViewById(R.id.cgEspecialidades);
            btnGuardar = findViewById(R.id.btnGuardarEspecialidades);
            btnCancelar = findViewById(R.id.btnCancelarEspecialidades);
        } else {
            setContentView(R.layout.activity_intereses);
            cg = findViewById(R.id.cgIntereses);
            btnGuardar = findViewById(R.id.btnGuardarIntereses);
            btnCancelar = findViewById(R.id.btnCancelarIntereses);
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        nodo = (rol != null && rol.equals("profesor")) ? "especialidades" : "intereses";

        configurarSelectoresColor();
        obtenerSeleccionadosPreviamente();

        btnCancelar.setOnClickListener(v -> finish());

        btnGuardar.setOnClickListener(v -> {
            List<String> sel = new ArrayList<>();
            for (int i = 0; i < cg.getChildCount(); i++) {
                Chip c = (Chip) cg.getChildAt(i);
                if (c.isChecked()) {
                    sel.add(c.getText().toString());
                }
            }

            if (sel.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos uno", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).child(nodo).setValue(sel)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "¡Guardado con éxito!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, (rol != null && rol.equals("profesor")) ? HomeProfesor.class : HomeEstudiante.class);
                        startActivity(intent);
                        finish();
                    });
        });
    }

    private void configurarSelectoresColor() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked},
                new int[] {-android.R.attr.state_checked}
        };
        int[] colors = new int[] {
                ContextCompat.getColor(this, R.color.upn_yellow),
                ContextCompat.getColor(this, R.color.upn_black)
        };
        bgList = new ColorStateList(states, colors);

        int[] textColors = new int[] {
                ContextCompat.getColor(this, R.color.pure_black),
                ContextCompat.getColor(this, R.color.text_white)
        };
        textList = new ColorStateList(states, textColors);
    }

    private void obtenerSeleccionadosPreviamente() {
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).child(nodo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> preseleccionados = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String item = ds.getValue(String.class);
                                if (item != null) {
                                    preseleccionados.add(item);
                                }
                            }
                        }
                        pintentarChips(preseleccionados);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pintentarChips(new ArrayList<>());
                    }
                });
    }

    private void pintentarChips(List<String> preseleccionados) {
        cg.removeAllViews();
        for (String s : lista) {
            Chip c = new Chip(this);
            c.setText(s);
            c.setCheckable(true);
            c.setChipBackgroundColor(bgList);
            c.setTextColor(textList);
            c.setChipStrokeColorResource(R.color.upn_yellow);
            c.setChipStrokeWidth(3f);

            if (preseleccionados.contains(s)) {
                c.setChecked(true);
            }

            cg.addView(c);
        }
    }
}