package com.fantasma.lector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EditorActivity extends Activity {

    public static final String EXTRA_RUTA_ARCHIVO = "ruta_archivo";
    public static final String EXTRA_NOMBRE_PROYECTO = "nombre_proyecto";

    private EditText cuadroEdicion;
    private String rutaArchivo;
    private String nombreProyecto;

    // Deshacer/rehacer: historial simple de snapshots de texto, con
    // debounce para no guardar uno por cada tecla presionada.
    private final List<String> historial = new ArrayList<>();
    private int indiceHistorial = -1;
    private boolean aplicandoDesdeHistorial = false;
    private final Handler manejadorDebounce = new Handler(Looper.getMainLooper());
    private Runnable tareaGuardarSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        rutaArchivo = getIntent().getStringExtra(EXTRA_RUTA_ARCHIVO);
        nombreProyecto = getIntent().getStringExtra(EXTRA_NOMBRE_PROYECTO);

        TextView textoRuta = findViewById(R.id.textoRuta);
        textoRuta.setText(nombreProyecto + " / " + new File(rutaArchivo).getName());

        cuadroEdicion = findViewById(R.id.cuadroEdicion);
        cargarArchivo();

        historial.add(cuadroEdicion.getText().toString());
        indiceHistorial = 0;

        cuadroEdicion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (aplicandoDesdeHistorial) return;
                if (tareaGuardarSnapshot != null) manejadorDebounce.removeCallbacks(tareaGuardarSnapshot);
                tareaGuardarSnapshot = () -> guardarSnapshot(s.toString());
                manejadorDebounce.postDelayed(tareaGuardarSnapshot, 600);
            }
        });

        findViewById(R.id.btnDeshacer).setOnClickListener(v -> deshacer());
        findViewById(R.id.btnRehacer).setOnClickListener(v -> rehacer());
        findViewById(R.id.btnBuscarReemplazar).setOnClickListener(v -> mostrarDialogoBuscarReemplazar());
        findViewById(R.id.btnEliminarCodigo).setOnClickListener(v -> confirmarEliminarCodigo());
        findViewById(R.id.btnEjecutar).setOnClickListener(v -> guardarYEjecutar());
        findViewById(R.id.btnBuscarBibliotecas).setOnClickListener(v ->
                startActivity(new Intent(this, TiendaBibliotecasActivity.class)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        guardarArchivo();
    }

    private void cargarArchivo() {
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            cuadroEdicion.setText("");
            return;
        }
        StringBuilder contenido = new StringBuilder();
        try (FileReader lector = new FileReader(archivo)) {
            char[] buffer = new char[4096];
            int leidos;
            while ((leidos = lector.read(buffer)) != -1) {
                contenido.append(buffer, 0, leidos);
            }
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo leer el archivo: " + e, Toast.LENGTH_LONG).show();
        }
        cuadroEdicion.setText(contenido.toString());
    }

    private void guardarArchivo() {
        try (FileWriter escritor = new FileWriter(rutaArchivo)) {
            escritor.write(cuadroEdicion.getText().toString());
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo guardar el archivo: " + e, Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Deshacer / rehacer ----------

    private void guardarSnapshot(String texto) {
        if (indiceHistorial >= 0 && historial.get(indiceHistorial).equals(texto)) return;
        while (historial.size() > indiceHistorial + 1) {
            historial.remove(historial.size() - 1);
        }
        historial.add(texto);
        indiceHistorial = historial.size() - 1;
    }

    private void deshacer() {
        if (indiceHistorial <= 0) {
            Toast.makeText(this, "No hay nada más que deshacer.", Toast.LENGTH_SHORT).show();
            return;
        }
        indiceHistorial--;
        aplicarSnapshot(historial.get(indiceHistorial));
    }

    private void rehacer() {
        if (indiceHistorial >= historial.size() - 1) {
            Toast.makeText(this, "No hay nada más que rehacer.", Toast.LENGTH_SHORT).show();
            return;
        }
        indiceHistorial++;
        aplicarSnapshot(historial.get(indiceHistorial));
    }

    private void aplicarSnapshot(String texto) {
        aplicandoDesdeHistorial = true;
        cuadroEdicion.setText(texto);
        cuadroEdicion.setSelection(texto.length());
        aplicandoDesdeHistorial = false;
    }

    // ---------- Buscar y reemplazar ----------

    private void mostrarDialogoBuscarReemplazar() {
        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setPadding(24, 16, 24, 0);

        EditText campoBuscar = new EditText(this);
        campoBuscar.setHint("Texto a buscar");
        EditText campoReemplazar = new EditText(this);
        campoReemplazar.setHint("Reemplazar con");

        contenedor.addView(campoBuscar);
        contenedor.addView(campoReemplazar);

        new AlertDialog.Builder(this)
                .setTitle("Buscar y reemplazar")
                .setView(contenedor)
                .setPositiveButton("Reemplazar todo", (dialogo, cual) -> {
                    String buscar = campoBuscar.getText().toString();
                    String reemplazar = campoReemplazar.getText().toString();
                    if (buscar.isEmpty()) return;
                    String actual = cuadroEdicion.getText().toString();
                    int ocurrencias = actual.split(java.util.regex.Pattern.quote(buscar), -1).length - 1;
                    cuadroEdicion.setText(actual.replace(buscar, reemplazar));
                    Toast.makeText(this, ocurrencias + " ocurrencia(s) reemplazada(s).", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- Eliminar / ejecutar ----------

    private void confirmarEliminarCodigo() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar código")
                .setMessage("¿Borrar todo el contenido de este archivo?")
                .setPositiveButton("Eliminar", (dialogo, cual) -> cuadroEdicion.setText(""))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarYEjecutar() {
        guardarArchivo();
        String codigo = cuadroEdicion.getText().toString();

        Python py = Python.getInstance();
        PyObject modulo = py.getModule("ejecutor");
        String resultado = modulo.callAttr("ejecutar", codigo, this).toString();

        if (resultado.contains("ModuleNotFoundError")) {
            Toast.makeText(this,
                    "Biblioteca no disponible. Verifica si está en la tienda de bibliotecas.",
                    Toast.LENGTH_LONG).show();
        }

        mostrarResultado(resultado);
    }

    private void mostrarResultado(String resultado) {
        TextView texto = new TextView(this);
        texto.setText(resultado);
        texto.setTypeface(android.graphics.Typeface.MONOSPACE);
        texto.setTextIsSelectable(true);
        texto.setPadding(24, 16, 24, 16);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(texto);

        new AlertDialog.Builder(this)
                .setTitle(resultado.startsWith("OK") ? "Ejecución terminada" : "Hubo un error")
                .setView(scroll)
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
