package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import empresa.android.proyectov1.R;

public class Cursos extends AppCompatActivity {
    private LinearLayout contenedorCategorias; // Cambiado a LinearLayout para recibir los bloques verticales
    private String rol;
    private Button btnGuardar, btnCancelar;
    private String uid;
    private String nodo;

    // BANCO DE DATOS EQUILIBRADO PARA TODA LA UNIVERSIDAD
    private final Map<String, String[]> mapaCursos = new HashMap<String, String[]>() {{
        put("CIENCIAS Y MATEMÁTICA", new String[]{"Complemento de Matemática", "Matemática Básica", "Física General", "Química General", "Estadística General"});
        put("NEGOCIOS Y MARKETING", new String[]{"Economía General", "Administración para Negocios", "Fundamentos de Marketing", "Contabilidad Básica"});
        put("LETRAS Y HUMANIDADES", new String[]{"Comunicación I", "Comunicación II", "Metodología de la Investigación", "Realidad Nacional", "Introducción al Derecho"});
        put("TECNOLOGÍA Y HERRAMIENTAS", new String[]{"Herramientas Informáticas", "Introducción a la Programación", "Diseño Gráfico Básico"});
        put("SALUD Y BIENESTAR", new String[]{"Biología General", "Nutrición y Salud", "Psicología de la Felicidad", "Primeros Auxilios"});
    }};

    private ColorStateList bgList, textList;
    private final List<Chip> todosLosChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rol = getIntent().getStringExtra("rol");

        if (rol != null && rol.equals("profesor")) {
            setContentView(R.layout.activity_especialidades);
            contenedorCategorias = findViewById(R.id.cgEspecialidades); // Vincula al LinearLayout interno del ScrollView
            btnGuardar = findViewById(R.id.btnGuardarEspecialidades);
            btnCancelar = findViewById(R.id.btnCancelarEspecialidades);
        } else {
            setContentView(R.layout.activity_intereses);
            contenedorCategorias = findViewById(R.id.cgIntereses); // Vincula al LinearLayout interno del ScrollView
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
            for (Chip c : todosLosChips) {
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
                        pintarChipsCategorizados(preseleccionados);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pintarChipsCategorizados(new ArrayList<>());
                    }
                });
    }

    // CORREGIDO: Inyecta los títulos y sub-grupos de chips de manera limpia dentro del scroll nativo
    private void pintarChipsCategorizados(List<String> preseleccionados) {
        contenedorCategorias.removeAllViews();
        todosLosChips.clear();

        for (Map.Entry<String, String[]> area : mapaCursos.entrySet()) {

            // 1. Título de la categoría
            TextView tvCategoria = new TextView(this);
            tvCategoria.setText(area.getKey());
            tvCategoria.setTextSize(13f);
            tvCategoria.setTypeface(null, Typeface.BOLD);
            tvCategoria.setTextColor(ContextCompat.getColor(this, R.color.upn_yellow));
            tvCategoria.setPadding(10, 24, 0, 12);
            contenedorCategorias.addView(tvCategoria);

            // 2. Grupo de chips exclusivo para la categoría (Flexbox horizontal nativo)
            ChipGroup subGrupo = new ChipGroup(this);
            subGrupo.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            subGrupo.setChipSpacingHorizontal(16);
            subGrupo.setChipSpacingVertical(12);

            // 3. Inyección de chips individuales
            for (String curso : area.getValue()) {
                Chip c = new Chip(this);
                c.setText(curso);
                c.setCheckable(true);
                c.setChipBackgroundColor(bgList);
                c.setTextColor(textList);
                c.setChipStrokeColorResource(R.color.upn_yellow);
                c.setChipStrokeWidth(3f);

                if (preseleccionados.contains(curso)) {
                    c.setChecked(true);
                }

                subGrupo.addView(c);
                todosLosChips.add(c);
            }

            contenedorCategorias.addView(subGrupo);
        }
    }
}