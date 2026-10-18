package com.fantasma.lector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pantalla de gestión de proyectos. Cada proyecto es una carpeta en
 * almacenamiento compartido: /almacenamiento/LectorFantasma/<nombre>/
 * con un archivo principal <nombre>.py adentro.
 */
public class ProyectosActivity extends Activity {

    private ListView listaProyectos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proyectos);

        listaProyectos = findViewById(R.id.listaProyectos);

        Button btnNuevo = findViewById(R.id.btnNuevoProyecto);
        Button btnEditar = findViewById(R.id.btnEditarProyecto);
        Button btnEliminar = findViewById(R.id.btnEliminarProyecto);

        btnNuevo.setOnClickListener(v -> mostrarDialogoNuevoProyecto());
        btnEditar.setOnClickListener(v -> mostrarSelectorProyecto("Editar proyecto", this::abrirProyectoEnEditor));
        btnEliminar.setOnClickListener(v -> mostrarSelectorProyecto("Eliminar proyecto", this::confirmarEliminarProyecto));

        listaProyectos.setOnItemClickListener((parent, view, position, id) ->
                abrirProyectoEnEditor((String) parent.getItemAtPosition(position)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarLista();
    }

    public static File carpetaBase() {
        return new File(Environment.getExternalStorageDirectory(), "LectorFantasma");
    }

    private List<String> listarProyectos() {
        File base = carpetaBase();
        List<String> nombres = new ArrayList<>();
        File[] hijos = base.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                if (hijo.isDirectory()) {
                    nombres.add(hijo.getName());
                }
            }
        }
        nombres.sort(String::compareToIgnoreCase);
        return nombres;
    }

    private void actualizarLista() {
        List<String> nombres = listarProyectos();
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, nombres);
        listaProyectos.setAdapter(adaptador);
    }

    private void mostrarDialogoNuevoProyecto() {
        EditText campo = new EditText(this);
        campo.setHint("nombre_del_proyecto");
        campo.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Nuevo proyecto")
                .setView(campo)
                .setPositiveButton("Aceptar", (dialogo, cual) -> {
                    String nombre = sanitizarNombre(campo.getText().toString());
                    if (nombre.isEmpty()) {
                        Toast.makeText(this, "El nombre no puede quedar vacío.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    crearProyecto(nombre);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String sanitizarNombre(String nombre) {
        return nombre.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private void crearProyecto(String nombre) {
        File carpetaProyecto = new File(carpetaBase(), nombre);
        if (carpetaProyecto.exists()) {
            Toast.makeText(this, "Ya existe un proyecto con ese nombre.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!carpetaProyecto.mkdirs()) {
            Toast.makeText(this,
                    "No se pudo crear la carpeta. ¿Concediste el permiso de 'acceso a todos los archivos' en Ajustes?",
                    Toast.LENGTH_LONG).show();
            return;
        }

        File archivoPrincipal = new File(carpetaProyecto, nombre + ".py");
        try (java.io.FileWriter escritor = new java.io.FileWriter(archivoPrincipal)) {
            escritor.write("# " + nombre + ".py\n" +
                    "# Este es el archivo principal del proyecto '" + nombre + "'.\n" +
                    "# Aqui va la interfaz y la logica central de tu script.\n\n" +
                    "print('Hola desde " + nombre + "')\n");
        } catch (java.io.IOException e) {
            Toast.makeText(this, "No se pudo crear el archivo: " + e, Toast.LENGTH_LONG).show();
            return;
        }

        abrirProyectoEnEditor(nombre);
    }

    private void abrirProyectoEnEditor(String nombreProyecto) {
        File archivoPrincipal = new File(new File(carpetaBase(), nombreProyecto), nombreProyecto + ".py");
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_RUTA_ARCHIVO, archivoPrincipal.getAbsolutePath());
        intent.putExtra(EditorActivity.EXTRA_NOMBRE_PROYECTO, nombreProyecto);
        startActivity(intent);
    }

    private void confirmarEliminarProyecto(String nombreProyecto) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar proyecto")
                .setMessage("¿Seguro que quieres eliminar '" + nombreProyecto + "' y todo su contenido? Esto no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialogo, cual) -> {
                    borrarRecursivo(new File(carpetaBase(), nombreProyecto));
                    actualizarLista();
                    Toast.makeText(this, "Proyecto eliminado.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrarRecursivo(File archivo) {
        if (archivo.isDirectory()) {
            File[] hijos = archivo.listFiles();
            if (hijos != null) {
                for (File hijo : hijos) borrarRecursivo(hijo);
            }
        }
        archivo.delete();
    }

    private interface AccionConNombre {
        void ejecutar(String nombre);
    }

    private void mostrarSelectorProyecto(String titulo, AccionConNombre accion) {
        List<String> nombres = listarProyectos();
        if (nombres.isEmpty()) {
            Toast.makeText(this, "No hay proyectos todavía.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arreglo = nombres.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(arreglo, (dialogo, posicion) -> accion.ejecutar(arreglo[posicion]))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
