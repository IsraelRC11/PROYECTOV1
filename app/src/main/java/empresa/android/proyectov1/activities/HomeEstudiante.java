package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;
import empresa.android.proyectov1.R;
import empresa.android.proyectov1.models.UsuarioModel;

public class HomeEstudiante extends AppCompatActivity {

    private TextView tvSaludo, tvNombreProf, tvEspecialidades, tvHorarios;
    private ShapeableImageView imgProfesor;
    private RatingBar ratingBar;
    private FloatingActionButton btnDescartar, btnCitar;
    private BottomNavigationView bottomNav;

    private DatabaseReference mDatabase;
    private String uidLogueado;
    private List<UsuarioModel> listaProfesores;
    private int indexActual = 0;
    private ValueEventListener badgeValueListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_estudiante);

        uidLogueado = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        listaProfesores = new ArrayList<>();

        initViews();
        obtenerDatosEstudiante();
        cargarProfesores();
        configurarBadgeMensajesGlobal();

        btnDescartar.setOnClickListener(v -> mostrarSiguienteProfesor());

        btnCitar.setOnClickListener(v -> {
            if (!listaProfesores.isEmpty()) {
                UsuarioModel p = listaProfesores.get(indexActual);
                Intent intent = new Intent(HomeEstudiante.this, Chat.class);
                intent.putExtra("idReceptor", p.getUid());
                intent.putExtra("nombreReceptor", p.getNombre() + " " + p.getApellido());
                intent.putExtra("fotoReceptor", p.getFotoUrl());
                intent.putExtra("rol", "estudiante");
                startActivity(intent);
            }
        });

        configurarNavegacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verifica si hay pendientes únicamente al volver a enfocar la pantalla principal
        verificarCalificacionesPendientes();
    }

    private void initViews() {
        tvSaludo = findViewById(R.id.tvSaludoHome);
        tvNombreProf = findViewById(R.id.tvNombreProfesor);
        tvEspecialidades = findViewById(R.id.tvEspecialidades);
        tvHorarios = findViewById(R.id.tvHorariosProfesor);
        imgProfesor = findViewById(R.id.imgFotoProfesor);
        ratingBar = findViewById(R.id.ratingProfesor);
        btnDescartar = findViewById(R.id.btnDescartar);
        btnCitar = findViewById(R.id.btnCitar);
        bottomNav = findViewById(R.id.bottomNavigation);
    }

    private void configurarBadgeMensajesGlobal() {
        if (uidLogueado == null) return;

        badgeValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int salasConMensajesNuevos = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String idSala = ds.getKey();
                    if (idSala != null && idSala.contains(uidLogueado)) {
                        Long misPendientes = ds.child("noLeidos_" + uidLogueado).getValue(Long.class);
                        if (misPendientes != null && misPendientes > 0) {
                            salasConMensajesNuevos++;
                        }
                    }
                }

                if (bottomNav != null) {
                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chats);
                    if (salasConMensajesNuevos > 0) {
                        badge.setVisible(true);
                        badge.setNumber(salasConMensajesNuevos);
                        badge.setBackgroundColor(getResources().getColor(R.color.upn_yellow));
                        badge.setBadgeTextColor(getResources().getColor(R.color.upn_black));
                    } else {
                        bottomNav.removeBadge(R.id.nav_chats);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Chats").addValueEventListener(badgeValueListener);
    }

    private void verificarCalificacionesPendientes() {
        if (uidLogueado == null) return;

        // CORREGIDO: addListenerForSingleValueEvent para evitar ejecuciones fantasmas por caché local
        mDatabase.child("CalificacionesPendientes").child(uidLogueado)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            for (DataSnapshot asesoriaSnap : snapshot.getChildren()) {
                                String idAsesoriaUnica = asesoriaSnap.getKey();
                                String idProf = asesoriaSnap.child("idProfesor").getValue(String.class);

                                if (idProf != null && idAsesoriaUnica != null) {
                                    Intent i = new Intent(HomeEstudiante.this, Calificar.class);
                                    i.putExtra("idProfesor", idProf);
                                    i.putExtra("idChatAsesoria", idAsesoriaUnica);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(i);
                                    break; // Procesa estrictamente de uno en uno
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDatabase.child("Usuarios").child(uidLogueado).child("estado").setValue("online");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabase.child("Usuarios").child(uidLogueado).child("estado").setValue("offline");
    }

    private void obtenerDatosEstudiante() {
        mDatabase.child("Usuarios").child(uidLogueado)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String nombre = snapshot.child("nombre").getValue(String.class);
                            if (nombre != null) tvSaludo.setText("HOLA, " + nombre.toUpperCase() + "!");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void cargarProfesores() {
        mDatabase.child("Usuarios").orderByChild("rol").equalTo("profesor")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaProfesores.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            UsuarioModel p = ds.getValue(UsuarioModel.class);
                            if (p != null) {
                                p.setUid(ds.getKey());
                                if (!p.getUid().equals(uidLogueado)) listaProfesores.add(p);
                            }
                        }
                        if (!listaProfesores.isEmpty()) {
                            actualizarInterfazTarjeta();
                        } else {
                            tvNombreProf.setText("No hay asesores disponibles");
                            tvEspecialidades.setText("-");
                            tvHorarios.setText("Intenta más tarde");
                            ratingBar.setRating(0.0f);
                            imgProfesor.setImageResource(R.drawable.usuario);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void mostrarSiguienteProfesor() {
        if (listaProfesores.size() > 1) {
            indexActual = (indexActual + 1) % listaProfesores.size();
            actualizarInterfazTarjeta();
        }
    }

    private void actualizarInterfazTarjeta() {
        UsuarioModel p = listaProfesores.get(indexActual);
        tvNombreProf.setText("Ing. " + p.getNombre() + " " + p.getApellido());

        if (p.getFotoUrl() != null && !p.getFotoUrl().isEmpty()) {
            Glide.with(this).load(p.getFotoUrl()).placeholder(R.drawable.usuario).into(imgProfesor);
        } else {
            imgProfesor.setImageResource(R.drawable.usuario);
        }

        if (p.getEspecialidades() != null && !p.getEspecialidades().isEmpty()) {
            tvEspecialidades.setText(String.join(" • ", p.getEspecialidades()));
        } else {
            tvEspecialidades.setText("General");
        }

        if (p.getEstadisticas() != null) {
            long completadas = p.getEstadisticas().getCitasCompletadas();
            float sumaCalificaciones = p.getEstadisticas().getSumaCalificaciones();
            float promedio = completadas > 0 ? (sumaCalificaciones / completadas) : 0f;
            ratingBar.setRating(promedio);
        } else {
            ratingBar.setRating(0.0f);
        }

        mDatabase.child("Usuarios").child(p.getUid()).child("disponibilidad")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            StringBuilder sb = new StringBuilder();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                sb.append("• ").append(ds.getKey()).append(": ").append(ds.getValue(String.class)).append("\n");
                            }
                            tvHorarios.setText(sb.toString().trim());
                        } else {
                            tvHorarios.setText("• Horarios por coordinar en el chat interno");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chats) {
                startActivity(new Intent(this, Mensajes.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilEstudiante.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return id == R.id.nav_home;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (badgeValueListener != null) {
            mDatabase.child("Chats").removeEventListener(badgeValueListener);
        }
    }
}