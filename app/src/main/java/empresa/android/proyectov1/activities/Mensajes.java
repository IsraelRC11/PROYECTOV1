package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import empresa.android.proyectov1.R;
import empresa.android.proyectov1.adapters.ChatAdapter;
import empresa.android.proyectov1.models.ChatModel;

public class Mensajes extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private RecyclerView rvChats;
    private EditText etBuscar;
    private ChatAdapter chatAdapter;
    private List<ChatModel> listaChats;
    private List<ChatModel> listaChatsFiltrada;

    private DatabaseReference mDatabase;
    private String uidLogueado, rolUsuario;
    private ValueEventListener chatsValueListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensajes);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        uidLogueado = FirebaseAuth.getInstance().getUid();

        initViews();
        obtenerRolYContenido();
        configurarNavegacion();
        configurarBuscador();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        etBuscar = findViewById(R.id.etBuscarConversacion);

        rvChats = findViewById(R.id.rvConversaciones);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        listaChats = new ArrayList<>();
        listaChatsFiltrada = new ArrayList<>();

        chatAdapter = new ChatAdapter(listaChatsFiltrada, rolUsuario);
        rvChats.setAdapter(chatAdapter);
    }

    private void obtenerRolYContenido() {
        if (uidLogueado == null) return;

        mDatabase.child("Usuarios").child(uidLogueado).child("role")
                .get().addOnCompleteListener(task -> {
                    // Intento dinámico secundario de respaldo por compatibilidad
                    if(task.isSuccessful() && task.getResult().exists()){
                        rolUsuario = task.getResult().getValue(String.class);
                        chatAdapter = new ChatAdapter(listaChatsFiltrada, rolUsuario);
                        rvChats.setAdapter(chatAdapter);
                        cargarListaChats();
                        return;
                    }

                    // Listener por defecto
                    mDatabase.child("Usuarios").child(uidLogueado).child("rol")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    rolUsuario = snapshot.getValue(String.class);
                                    chatAdapter = new ChatAdapter(listaChatsFiltrada, rolUsuario);
                                    rvChats.setAdapter(chatAdapter);
                                    cargarListaChats();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                });
    }

    private void cargarListaChats() {
        chatsValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaChats.clear();
                if (!snapshot.exists()) {
                    listaChatsFiltrada.clear();
                    chatAdapter.notifyDataSetChanged();
                    return;
                }

                final long totalChats = snapshot.getChildrenCount();
                final int[] chatsProcesados = {0};

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String idSala = ds.getKey();

                    if (idSala != null && idSala.contains(uidLogueado)) {
                        String idReceptor = idSala.replace(uidLogueado, "").replace("_", "");

                        ChatModel chat = ds.getValue(ChatModel.class);
                        if (chat != null) {
                            chat.setIdChat(idSala);
                            chat.setIdReceptor(idReceptor);

                            mDatabase.child("Usuarios").child(idReceptor)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                            if (userSnapshot.exists()) {
                                                String nombre = userSnapshot.child("nombre").getValue(String.class);
                                                String apellido = userSnapshot.child("apellido").getValue(String.class);
                                                chat.setNombreReceptor(nombre + " " + apellido);
                                                chat.setFotoReceptor(userSnapshot.child("fotoUrl").getValue(String.class));

                                                mDatabase.child("Mensajes").child(idSala)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot mensajesSnapshot) {
                                                                int contadorPendientes = 0;
                                                                for (DataSnapshot msgSnap : mensajesSnapshot.getChildren()) {
                                                                    String emisorMsg = msgSnap.child("emisorUid").getValue(String.class);
                                                                    Boolean leido = msgSnap.child("leido").getValue(Boolean.class);

                                                                    if (emisorMsg != null && !emisorMsg.equals(uidLogueado)) {
                                                                        if (leido == null || !leido) {
                                                                            contadorPendientes++;
                                                                        }
                                                                    }
                                                                }

                                                                chat.setMensajesNoLeidos(contadorPendientes);

                                                                listaChats.remove(chat);
                                                                listaChats.add(chat);

                                                                chatsProcesados[0]++;
                                                                if (chatsProcesados[0] <= totalChats) {
                                                                    realizarFiltradoYOrdenamiento(etBuscar.getText().toString());
                                                                }
                                                            }
                                                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                                                        });
                                            } else {
                                                chatsProcesados[0]++;
                                            }
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) { chatsProcesados[0]++; }
                                    });
                        } else {
                            chatsProcesados[0]++;
                        }
                    } else {
                        chatsProcesados[0]++;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Chats").addValueEventListener(chatsValueListener);
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                realizarFiltradoYOrdenamiento(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void realizarFiltradoYOrdenamiento(String textoABuscar) {
        listaChatsFiltrada.clear();
        String query = textoABuscar.toLowerCase().trim();

        for (ChatModel chat : listaChats) {
            if (query.isEmpty() || (chat.getNombreReceptor() != null && chat.getNombreReceptor().toLowerCase().contains(query))) {
                listaChatsFiltrada.add(chat);
            }
        }

        Collections.sort(listaChatsFiltrada, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
        chatAdapter.notifyDataSetChanged();
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_chats);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                redireccionarAlHomeCorrecto();
                return true;
            } else if (id == R.id.nav_perfil) {
                redireccionarAlPerfilCorrecto();
                return true;
            }
            return id == R.id.nav_chats;
        });
    }

    private void redireccionarAlHomeCorrecto() {
        Intent intent = (rolUsuario != null && rolUsuario.equals("profesor"))
                ? new Intent(this, HomeProfesor.class)
                : new Intent(this, HomeEstudiante.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void redireccionarAlPerfilCorrecto() {
        Intent intent = (rolUsuario != null && rolUsuario.equals("profesor"))
                ? new Intent(this, PerfilProfesor.class)
                : new Intent(this, PerfilEstudiante.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsValueListener != null) {
            mDatabase.child("Chats").removeEventListener(chatsValueListener);
        }
    }
}