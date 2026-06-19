package empresa.android.proyectov1;

import java.util.List;

public class Estudiante {
    private String uid;
    private String nombre;
    private String apellido;
    private String codigo; // Cambiado de codigo_upn a codigo para coincidir con el Map de Registro
    private String rol;
    private String fotoUrl;
    private List<String> intereses;

    public Estudiante() {} // Constructor vacío obligatorio para Firebase

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public List<String> getIntereses() { return intereses; }
    public void setIntereses(List<String> intereses) { this.intereses = intereses; }
}