package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import empresa.android.proyectov1.R;
import empresa.android.proyectov1.adapters.MensajeAdapter;
import empresa.android.proyectov1.models.MensajeModel;

public class Chat extends AppCompatActivity {
    private TextView tvNombre, tvEstado;
    private ImageView btnRegresar, ivFoto;
    private RecyclerView rvMensajes;
    private EditText etMensaje;
    private FloatingActionButton btnEnviar;
    private MaterialButton btnFinalizar;

    private DatabaseReference mDatabase;
    private String idChat, idReceptor, idEmisor, rolUsuario;

    private MensajeAdapter mensajeAdapter;
    private List<MensajeModel> listaMensajes;
    private ValueEventListener chatStatusListener;
    private ChildEventListener mensajesListener;

    // CONTROL DE CACHÉ: Evita que estados antiguos fuercen la pantalla de calificación al abrir el chat
    private boolean lecturaInicial = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        idEmisor = FirebaseAuth.getInstance().getUid();

        idReceptor = getIntent().getStringExtra("idReceptor");
        String nombreReceptor = getIntent().getStringExtra("nombreReceptor");
        String fotoReceptor = getIntent().getStringExtra("fotoReceptor");
        rolUsuario = getIntent().getStringExtra("rol");

        idChat = generarIdChat(idEmisor, idReceptor);

        initViews();

        tvNombre.setText(nombreReceptor != null ? nombreReceptor : "Usuario");

        if (fotoReceptor != null && !fotoReceptor.isEmpty()) {
            ivFoto.setPadding(0, 0, 0, 0);
            ivFoto.setImageTintList(null);
            Glide.with(this).load(fotoReceptor).placeholder(R.drawable.usuario).into(ivFoto);
        } else {
            ivFoto.setImageResource(R.drawable.usuario);
        }

        setupStatusAndHeader();
        setupChatStatusListener();
        cargarMensajes();
        marcarMensajesComoLeidos();

        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnRegresar.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvNombre = findViewById(R.id.tvChatNombreUsuario);
        tvEstado = findViewById(R.id.tvChatEstado);
        ivFoto = findViewById(R.id.ivChatFotoUsuario);
        btnRegresar = findViewById(R.id.btnRegresarChat);
        rvMensajes = findViewById(R.id.rvChatMensajes);
        etMensaje = findViewById(R.id.etMensajeTexto);
        btnEnviar = findViewById(R.id.btnEnviarMensaje);
        btnFinalizar = findViewById(R.id.btnFinalizarAsesoria);

        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes);
        rvMensajes.setLayoutManager(new LinearLayoutManager(this));
        rvMensajes.setAdapter(mensajeAdapter);
    }

    private void cargarMensajes() {
        mensajesListener = mDatabase.child("Mensajes").child(idChat).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                MensajeModel msg = snapshot.getValue(MensajeModel.class);
                if (msg != null) {
                    listaMensajes.add(msg);
                    mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                    rvMensajes.scrollToPosition(listaMensajes.size() - 1);

                    if (msg.getEmisorUid() != null && !msg.getEmisorUid().equals(idEmisor)) {
                        snapshot.getRef().child("leido").setValue(true);
                        mDatabase.child("Chats").child(idChat).child("noLeidos_" + idEmisor).setValue(0);
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void marcarMensajesComoLeidos() {
        mDatabase.child("Mensajes").child(idChat).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> actualizaciones = new HashMap<>();
                    for (DataSnapshot msgSnap : snapshot.getChildren()) {
                        String emisorMsg = msgSnap.child("emisorUid").getValue(String.class);
                        Boolean leido = msgSnap.child("leido").getValue(Boolean.class);

                        if (emisorMsg != null && !emisorMsg.equals(idEmisor)) {
                            if (leido == null || !leido) {
                                actualizaciones.put(msgSnap.getKey() + "/leido", true);
                            }
                        }
                    }
                    if (!updatesIsEmpty(actualizaciones)) {
                        mDatabase.child("Mensajes").child(idChat).updateChildren(actualizaciones);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("Chats").child(idChat).child("noLeidos_" + idEmisor).setValue(0);
    }

    private boolean updatesIsEmpty(Map<String, Object> map) {
        return map == null || map.isEmpty();
    }

    private void enviarMensaje() {
        String texto = etMensaje.getText().toString().trim();
        if (texto.isEmpty()) return;

        String idMensaje = mDatabase.child("Mensajes").child(idChat).push().getKey();
        long tiempo = System.currentTimeMillis();

        Map<String, Object> map = new HashMap<>();
        map.put("idMensaje", idMensaje);
        map.put("mensaje", texto);
        map.put("emisorUid", idEmisor);
        map.put("receptorUid", idReceptor);
        map.put("timestamp", tiempo);
        map.put("leido", false);

        if (idMensaje != null) {
            mDatabase.child("Mensajes").child(idChat).child(idMensaje).setValue(map)
                    .addOnSuccessListener(aVoid -> {
                        etMensaje.setText("");

                        mDatabase.child("Chats").child(idChat).child("noLeidos_" + idReceptor)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        long pendientesReceptor = 0;
                                        if (snapshot.exists() && snapshot.getValue() != null) {
                                            pendientesReceptor = (long) snapshot.getValue();
                                        }

                                        Map<String, Object> updateChat = new HashMap<>();
                                        updateChat.put("ultimoMensaje", texto);
                                        updateChat.put("timestamp", tiempo);
                                        updateChat.put("estado", "activo");
                                        updateChat.put("emisorUid", idEmisor);
                                        updateChat.put("noLeidos_" + idReceptor, pendientesReceptor + 1);

                                        mDatabase.child("Chats").child(idChat).updateChildren(updateChat);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    });
        }
    }

    private void setupStatusAndHeader() {
        mDatabase.child("Usuarios").child(idReceptor).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String estado = snapshot.child("estado").getValue(String.class);
                    if ("online".equals(estado)) {
                        tvEstado.setText("En línea");
                        tvEstado.setTextColor(getResources().getColor(R.color.upn_yellow));
                    } else {
                        tvEstado.setText("Desconectado");
                        tvEstado.setTextColor(getResources().getColor(R.color.text_gray));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        if ("profesor".equals(rolUsuario)) {
            btnFinalizar.setVisibility(View.VISIBLE);
            btnFinalizar.setOnClickListener(v -> finalizarAsesoria());
        } else {
            btnFinalizar.setVisibility(View.GONE);
        }
    }

    private void setupChatStatusListener() {
        if (!"estudiante".equals(rolUsuario)) return;

        DatabaseReference refEstado = mDatabase.child("Chats").child(idChat).child("estado");

        refEstado.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String estado = snapshot.getValue(String.class);
                    if ("finalizado".equals(estado)) {
                        // Si ya estaba finalizado, limpiamos el estado y enviamos a calificar
                        refEstado.setValue("leido").addOnSuccessListener(aVoid -> abrirCalificacion());
                        return;
                    }
                }

                // 2. Solo si el estado es normal, activamos el listener en tiempo real por si se finaliza mientras chateamos
                chatStatusListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String estado = snapshot.getValue(String.class);
                            if ("finalizado".equals(estado)) {
                                // Desenganchar inmediatamente antes de proceder para evitar ejecuciones fantasma
                                refEstado.removeEventListener(this);
                                chatStatusListener = null;

                                refEstado.setValue("leido").addOnSuccessListener(aVoid -> abrirCalificacion());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                };
                refEstado.addValueEventListener(chatStatusListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void finalizarAsesoria() {
        btnFinalizar.setEnabled(false);
        btnFinalizar.setText("PROCESANDO...");

        mDatabase.child("Chats").child(idChat).child("estado").setValue("finalizado")
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> calif = new HashMap<>();
                    calif.put("idProfesor", idEmisor);
                    calif.put("idChatAsesoria", idChat);

                    mDatabase.child("CalificacionesPendientes")
                            .child(idReceptor)
                            .child(idChat)
                            .setValue(calif)
                            .addOnSuccessListener(unused -> {
                                btnFinalizar.setText("ASESORÍA FINALIZADA");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnFinalizar.setEnabled(true);
                                btnFinalizar.setText("TERMINAR");
                            });
                })
                .addOnFailureListener(e -> {
                    btnFinalizar.setEnabled(true);
                    btnFinalizar.setText("TERMINAR");
                });
    }

    private void abrirCalificacion() {
        if (chatStatusListener != null) {
            mDatabase.child("Chats").child(idChat).child("estado").removeEventListener(chatStatusListener);
            chatStatusListener = null;
        }

        Intent i = new Intent(Chat.this, Calificar.class);
        i.putExtra("idProfesor", idReceptor);
        i.putExtra("idChatAsesoria", idChat);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatStatusListener != null) {
            mDatabase.child("Chats").child(idChat).child("estado").removeEventListener(chatStatusListener);
        }
        if (mensajesListener != null) {
            mDatabase.child("Mensajes").child(idChat).removeEventListener(mensajesListener);
        }
    }

    private String generarIdChat(String u1, String u2) {
        return u1.compareTo(u2) < 0 ? u1 + "_" + u2 : u2 + "_" + u1;
    }
}