package com.upiiz.ble_sipi.Repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.upiiz.ble_sipi.Models.Usuario;

import java.util.HashMap;
import java.util.Map;

public class UsuarioRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COL_USUARIOS = "usuarios";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // Registrar nuevo usuario
    public void registrar(String email, String password,
                          Usuario usuario, Callback<Void> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    usuario.uid = uid;
                    usuario.email = email;
                    usuario.rol = "medico";
                    usuario.creadoEn = System.currentTimeMillis();

                    Map<String, Object> data = usuarioToMap(usuario);
                    db.collection(COL_USUARIOS).document(uid)
                            .set(data)
                            .addOnSuccessListener(v -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // Login
    public void login(String email, String password, Callback<Void> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // Cerrar sesión
    public void logout() {
        auth.signOut();
    }

    // Obtener usuario actual
    public void obtenerUsuarioActual(Callback<Usuario> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new Exception("No hay sesión activa"));
            return;
        }

        db.collection(COL_USUARIOS).document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(mapToUsuario(doc.getId(), doc.getData()));
                    } else {
                        callback.onError(new Exception("Usuario no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // Verificar si hay sesión activa
    public boolean haySesionActiva() {
        return auth.getCurrentUser() != null;
    }

    // ================= MAPEO =================

    private Map<String, Object> usuarioToMap(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre",      u.nombre);
        map.put("apellidos",   u.apellidos);
        map.put("email",       u.email);
        map.put("institucion", u.institucion);
        map.put("rol",         u.rol);
        map.put("creadoEn",    u.creadoEn);
        return map;
    }

    private Usuario mapToUsuario(String uid, Map<String, Object> map) {
        Usuario u = new Usuario();
        u.uid         = uid;
        u.nombre      = (String) map.get("nombre");
        u.apellidos   = (String) map.get("apellidos");
        u.email       = (String) map.get("email");
        u.institucion = (String) map.get("institucion");
        u.rol         = (String) map.get("rol");
        Object creadoEn = map.get("creadoEn");
        u.creadoEn = creadoEn instanceof Long ? (Long) creadoEn : 0L;
        return u;
    }
}
