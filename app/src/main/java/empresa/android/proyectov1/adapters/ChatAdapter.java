package empresa.android.proyectov1.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import empresa.android.proyectov1.R;
import empresa.android.proyectov1.activities.Chat;
import empresa.android.proyectov1.models.ChatModel;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatModel> chatList;
    private String rolUsuarioActual;

    public ChatAdapter(List<ChatModel> chatList, String rolUsuarioActual) {
        this.chatList = chatList;
        this.rolUsuarioActual = rolUsuarioActual;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvNombre.setText(chat.getNombreReceptor());

        String ultimoMsg = chat.getUltimoMensaje();
        String emisorUltimoMsg = chat.getEmisorUid();
        String miUid = FirebaseAuth.getInstance().getUid();

        // CONTROL REACTIVO DE ESTILOS (NEGRITA Y COLORES)
        if (ultimoMsg != null && !ultimoMsg.isEmpty()) {
            if (miUid != null && miUid.equals(emisorUltimoMsg)) {
                holder.tvUltimoMsg.setText("Tu: " + ultimoMsg);
                holder.tvUltimoMsg.setTypeface(null, Typeface.NORMAL);
                holder.tvUltimoMsg.setTextColor(context.getResources().getColor(R.color.text_gray));
            } else {
                holder.tvUltimoMsg.setText(ultimoMsg);

                // Si hay mensajes pendientes por leer, se resalta en Blanco y Negrita
                if (chat.getMensajesNoLeidos() > 0) {
                    holder.tvUltimoMsg.setTypeface(null, Typeface.BOLD);
                    holder.tvUltimoMsg.setTextColor(android.graphics.Color.WHITE);
                } else {
                    // Si ya los viste, la negrita se remueve y se atenúa a gris
                    holder.tvUltimoMsg.setTypeface(null, Typeface.NORMAL);
                    holder.tvUltimoMsg.setTextColor(context.getResources().getColor(R.color.text_gray));
                }
            }
        } else {
            holder.tvUltimoMsg.setText("Sin mensajes");
            holder.tvUltimoMsg.setTypeface(null, Typeface.NORMAL);
            holder.tvUltimoMsg.setTextColor(context.getResources().getColor(R.color.text_gray));
        }

        // Control de la fecha / hora
        if (chat.getTimestamp() > 0) {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(chat.getTimestamp());
            String horaFormateada = DateFormat.format("hh:mm a", cal).toString();
            holder.tvHora.setText(horaFormateada);
        } else {
            holder.tvHora.setText("");
        }

        // Control de visibilidad de la burbuja del contador
        if (chat.getMensajesNoLeidos() > 0) {
            holder.tvContador.setVisibility(View.VISIBLE);
            holder.tvContador.setText(String.valueOf(chat.getMensajesNoLeidos()));
        } else {
            holder.tvContador.setVisibility(View.GONE);
        }

        holder.ivFoto.setPadding(0, 0, 0, 0);
        holder.ivFoto.setImageTintList(null);

        if (chat.getFotoReceptor() != null && !chat.getFotoReceptor().isEmpty()) {
            Glide.with(context)
                    .load(chat.getFotoReceptor())
                    .placeholder(R.drawable.usuario)
                    .into(holder.ivFoto);
        } else {
            holder.ivFoto.setImageResource(R.drawable.usuario);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Chat.class);
            intent.putExtra("idReceptor", chat.getIdReceptor());
            intent.putExtra("nombreReceptor", chat.getNombreReceptor());
            intent.putExtra("fotoReceptor", chat.getFotoReceptor());
            intent.putExtra("rol", rolUsuarioActual);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvUltimoMsg, tvHora, tvContador;
        ShapeableImageView ivFoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivItemFoto);
            tvNombre = itemView.findViewById(R.id.tvItemNombre);
            tvUltimoMsg = itemView.findViewById(R.id.tvItemUltimoMensaje);
            tvHora = itemView.findViewById(R.id.tvItemHora);
            tvContador = itemView.findViewById(R.id.tvContadorNoLeidos);
        }
    }
}