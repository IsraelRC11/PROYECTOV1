package empresa.android.proyectov1.models;

import java.util.Objects;

public class ChatModel {
    private String idChat;
    private String idReceptor;
    private String nombreReceptor;
    private String fotoReceptor;
    private String ultimoMensaje;
    private String estado; // "activo" o "finalizado"
    private long timestamp;
    private int mensajesNoLeidos;

    // Constructor vacío para Firebase
    public ChatModel() {
    }

    // Getters y Setters
    public String getIdChat() { return idChat; }
    public void setIdChat(String idChat) { this.idChat = idChat; }

    public String getIdReceptor() { return idReceptor; }
    public void setIdReceptor(String idReceptor) { this.idReceptor = idReceptor; }

    public String getNombreReceptor() { return nombreReceptor; }
    public void setNombreReceptor(String nombreReceptor) { this.nombreReceptor = nombreReceptor; }

    public String getFotoReceptor() { return fotoReceptor; }
    public void setFotoReceptor(String fotoReceptor) { this.fotoReceptor = fotoReceptor; }

    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getMensajesNoLeidos() { return mensajesNoLeidos; }
    public void setMensajesNoLeidos(int mensajesNoLeidos) { this.mensajesNoLeidos = mensajesNoLeidos; }

    // Equals e HashCode para el manejo correcto de colecciones dinámicas y ordenamiento
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatModel chatModel = (ChatModel) o;
        return Objects.equals(idChat, chatModel.idChat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idChat);
    }
}