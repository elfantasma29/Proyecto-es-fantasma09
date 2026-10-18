package com.fantasma.lector;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TiendaBibliotecasActivity extends Activity {

    private static final String URL_TOTALES = GestorBibliotecas.URL_BASE_REPO + "bibliotecas_totales.json";
    private static final String URL_CATALOGO = GestorBibliotecas.URL_BASE_REPO + "bibliotecas_catalogo.json";

    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private final List<String> nombresCompletos = new ArrayList<>();
    private ArrayAdapter<String> adaptador;
    private TextView textoTotales;
    private ListView listaBibliotecas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tienda);

        textoTotales = findViewById(R.id.textoTotales);
        listaBibliotecas = findViewById(R.id.listaBibliotecas);
        EditText campoBusqueda = findViewById(R.id.campoBusqueda);
        Button btnBuscar = findViewById(R.id.btnBuscar);

        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listaBibliotecas.setAdapter(adaptador);

        listaBibliotecas.setOnItemClickListener((parent, view, position, id) ->
                confirmarInstalacion((String) parent.getItemAtPosition(position)));

        btnBuscar.setOnClickListener(v -> filtrar(campoBusqueda.getText().toString()));

        cargarCatalogo();
    }

    private void cargarCatalogo() {
        new Thread(() -> {
            try {
                int totales = leerTotales();
                List<String> nombres = leerNombresCatalogo();

                hiloPrincipal.post(() -> {
                    textoTotales.setText("Bibliotecas totales: " + totales);
                    nombresCompletos.clear();
                    nombresCompletos.addAll(nombres);
                    actualizarListaMostrada(nombres);
                });
            } catch (Exception e) {
                hiloPrincipal.post(() -> {
                    textoTotales.setText("No se pudo cargar el catálogo.");
                    new AlertDialog.Builder(this)
                            .setTitle("Error al cargar la tienda")
                            .setMessage(String.valueOf(e))
                            .setPositiveButton("Cerrar", null)
                            .show();
                });
            }
        }).start();
    }

    private int leerTotales() throws Exception {
        String texto = descargarTexto(URL_TOTALES);
        JSONObject json = new JSONObject(texto);
        return json.getInt("bibliotecas_totales");
    }

    private List<String> leerNombresCatalogo() throws Exception {
        String texto = descargarTexto(URL_CATALOGO);
        JSONObject json = new JSONObject(texto);
        JSONArray arreglo = json.getJSONArray("bibliotecas");
        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < arreglo.length(); i++) {
            nombres.add(arreglo.getJSONObject(i).getString("nombre"));
        }
        return nombres;
    }

    private String descargarTexto(String url) throws Exception {
        HttpURLConnection conexion = (HttpURLConnection) new URL(url).openConnection();
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(20000);
        int codigo = conexion.getResponseCode();
        if (codigo != 200) {
            throw new Exception("El servidor respondió " + codigo + " al pedir " + url);
        }
        StringBuilder resultado = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(conexion.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                resultado.append(linea).append('\n');
            }
        } finally {
            conexion.disconnect();
        }
        return resultado.toString();
    }

    private void filtrar(String consulta) {
        String consultaNormalizada = consulta.trim().toLowerCase(Locale.ROOT);
        if (consultaNormalizada.isEmpty()) {
            actualizarListaMostrada(nombresCompletos);
            return;
        }
        List<String> filtradas = new ArrayList<>();
        for (String nombre : nombresCompletos) {
            if (nombre.toLowerCase(Locale.ROOT).contains(consultaNormalizada)) {
                filtradas.add(nombre);
            }
        }
        actualizarListaMostrada(filtradas);
    }

    private void actualizarListaMostrada(List<String> nombres) {
        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres);
        listaBibliotecas.setAdapter(adaptador);
    }

    private void confirmarInstalacion(String nombreBiblioteca) {
        boolean yaInstalada = GestorBibliotecas.listaInstaladas(this).contains(nombreBiblioteca);
        String mensaje = yaInstalada
                ? "'" + nombreBiblioteca + "' ya está instalada. ¿Descargarla de nuevo?"
                : "¿Desea descargar e instalar esta biblioteca?";

        new AlertDialog.Builder(this)
                .setTitle(nombreBiblioteca)
                .setMessage(mensaje)
                .setPositiveButton("Instalar", (dialogo, cual) -> instalar(nombreBiblioteca))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void instalar(String nombreBiblioteca) {
        AlertDialog dialogoDescargando = new AlertDialog.Builder(this)
                .setTitle("Descargando")
                .setMessage("Descargando e instalando '" + nombreBiblioteca + "'...")
                .setCancelable(false)
                .show();

        new Thread(() -> {
            try {
                GestorBibliotecas.descargarEInstalar(this, nombreBiblioteca);
                hiloPrincipal.post(() -> {
                    dialogoDescargando.dismiss();
                    new AlertDialog.Builder(this)
                            .setTitle("Listo")
                            .setMessage("'" + nombreBiblioteca + "' se instaló correctamente. Ya puedes usarla en tu código.")
                            .setPositiveButton("Aceptar", (dialogo, cual) -> finish())
                            .show();
                });
            } catch (Exception e) {
                hiloPrincipal.post(() -> {
                    dialogoDescargando.dismiss();
                    new AlertDialog.Builder(this)
                            .setTitle("No se pudo instalar")
                            .setMessage(String.valueOf(e))
                            .setPositiveButton("Cerrar", null)
                            .show();
                });
            }
        }).start();
    }
}
