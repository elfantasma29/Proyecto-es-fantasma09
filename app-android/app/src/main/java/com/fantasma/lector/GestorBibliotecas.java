package com.fantasma.lector;

import android.content.Context;
import android.content.SharedPreferences;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Maneja las bibliotecas de Python puro descargadas desde la tienda:
 * las descarga desde el repositorio de GitHub, las descomprime en
 * almacenamiento interno de la app, las agrega a sys.path, y recuerda
 * cuáles ya están instaladas para volver a agregarlas al arrancar la
 * app (sys.path no persiste solo entre ejecuciones).
 */
public class GestorBibliotecas {

    private static final String PREFS = "bibliotecas_instaladas";
    private static final String CLAVE_SET = "nombres";

    public static final String URL_BASE_REPO =
            "https://raw.githubusercontent.com/elfantasma29/Proyecto-es-fantasma09/main/bibliotecas/";

    private static File carpetaBiblioteca(Context contexto, String nombre) {
        return new File(new File(contexto.getFilesDir(), "bibliotecas_descargadas"), nombre);
    }

    public static Set<String> listaInstaladas(Context contexto) {
        SharedPreferences prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(CLAVE_SET, new HashSet<>()));
    }

    private static void marcarInstalada(Context contexto, String nombre) {
        SharedPreferences prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> actuales = new HashSet<>(prefs.getStringSet(CLAVE_SET, new HashSet<>()));
        actuales.add(nombre);
        prefs.edit().putStringSet(CLAVE_SET, actuales).apply();
    }

    private static void agregarASysPath(String ruta) {
        Python py = Python.getInstance();
        PyObject sys = py.getModule("sys");
        PyObject path = sys.get("path");
        path.callAttr("insert", 0, ruta);
    }

    /** Se llama al arrancar la app: vuelve a agregar a sys.path todas las
     * bibliotecas que ya se habían instalado en sesiones anteriores. */
    public static void restaurarEnSysPath(Context contexto) {
        for (String nombre : listaInstaladas(contexto)) {
            File carpeta = carpetaBiblioteca(contexto, nombre);
            if (carpeta.isDirectory()) {
                agregarASysPath(carpeta.getAbsolutePath());
            }
        }
    }

    /** Descarga el .zip de la biblioteca desde el repositorio, la
     * descomprime y la deja lista para usarse. Debe llamarse en un hilo
     * de fondo (no en el hilo principal). Lanza excepción con el detalle
     * si algo falla, para mostrarlo tal cual al usuario. */
    public static void descargarEInstalar(Context contexto, String nombreBiblioteca) throws Exception {
        String urlZip = URL_BASE_REPO + nombreBiblioteca + ".zip";
        File carpetaDestino = carpetaBiblioteca(contexto, nombreBiblioteca);
        carpetaDestino.mkdirs();

        File zipTemporal = new File(contexto.getCacheDir(), nombreBiblioteca + "_descarga.zip");

        HttpURLConnection conexion = (HttpURLConnection) new URL(urlZip).openConnection();
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(30000);
        int codigo = conexion.getResponseCode();
        if (codigo != 200) {
            throw new IOException("El servidor respondió " + codigo + " al pedir " + urlZip
                    + " (¿el nombre de la biblioteca es exacto?)");
        }

        try (InputStream entrada = conexion.getInputStream();
             OutputStream salida = new FileOutputStream(zipTemporal)) {
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }
        } finally {
            conexion.disconnect();
        }

        try (ZipInputStream zip = new ZipInputStream(
                new java.io.FileInputStream(zipTemporal))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                File archivoSalida = new File(carpetaDestino, entrada.getName());
                if (entrada.isDirectory()) {
                    archivoSalida.mkdirs();
                } else {
                    File padre = archivoSalida.getParentFile();
                    if (padre != null) padre.mkdirs();
                    try (OutputStream salida = new FileOutputStream(archivoSalida)) {
                        byte[] buffer = new byte[8192];
                        int leidos;
                        while ((leidos = zip.read(buffer)) != -1) {
                            salida.write(buffer, 0, leidos);
                        }
                    }
                }
            }
        }
        zipTemporal.delete();

        agregarASysPath(carpetaDestino.getAbsolutePath());
        marcarInstalada(contexto, nombreBiblioteca);
    }
}
