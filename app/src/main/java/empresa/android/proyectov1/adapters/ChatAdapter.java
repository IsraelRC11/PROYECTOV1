package empresa.android.proyectov1.adapters;

import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import empresa.android.proyectov1.R;
import empresa.android.proyectov1.activities.Chat;
import empresa.android.proyectov1.models.ChatModel;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatModel> chatList;
    private String rolUsuarioActual; // Guarda el rol del usuario logueado

    // Constructor corregido para recibir el rol dinámico
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

        holder.tvNombre.setText(chat.getNombreReceptor());
        holder.tvUltimoMsg.setText(chat.getUltimoMensaje());

        if (chat.getTimestamp() > 0) {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(chat.getTimestamp());
            String horaFormateada = DateFormat.format("hh:mm a", cal).toString();
            holder.tvHora.setText(horaFormateada);
        } else {
            holder.tvHora.setText("");
        }

        if (chat.getMensajesNoLeidos() > 0) {
            holder.tvContador.setVisibility(View.VISIBLE);
            holder.tvContador.setText(String.valueOf(chat.getMensajesNoLeidos()));
        } else {
            holder.tvContador.setVisibility(View.GONE);
        }

        holder.ivFoto.setPadding(0, 0, 0, 0);
        holder.ivFoto.setImageTintList(null);

        if (chat.getFotoReceptor() != null && !chat.getFotoReceptor().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chat.getFotoReceptor())
                    .placeholder(R.drawable.usuario)
                    .into(holder.ivFoto);
        } else {
            holder.ivFoto.setImageResource(R.drawable.usuario);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), Chat.class);
            intent.putExtra("idReceptor", chat.getIdReceptor());
            intent.putExtra("nombreReceptor", chat.getNombreReceptor());
            intent.putExtra("fotoReceptor", chat.getFotoReceptor());

            // CORREGIDO: Manda el rol dinámico real a la pantalla de Chat
            intent.putExtra("rol", rolUsuarioActual);
            v.getContext().startActivity(intent);
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