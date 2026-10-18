package com.fantasma.lector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends Activity {

    private static final int CODIGO_PERMISOS_COMUNES = 100;
    private static final String[] PERMISOS_COMUNES = {
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        GestorBibliotecas.restaurarEnSysPath(this);

        Python py = Python.getInstance();
        PyObject modulo = py.getModule("saludo");
        String versionPython = modulo.callAttr("version_python").toString();

        TextView tv = findViewById(R.id.resultado);
        tv.setText("Listo, todo cargado.\nVersión de Python: " + versionPython);

        Button btnAbrirEditor = findViewById(R.id.btnAbrirEditor);
        btnAbrirEditor.setOnClickListener(v ->
                startActivity(new Intent(this, ProyectosActivity.class)));

        pedirPermisosNecesarios();
    }

    private void pedirPermisosNecesarios() {
        requestPermissions(PERMISOS_COMUNES, CODIGO_PERMISOS_COMUNES);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this,
                        "Necesitas conceder acceso a todos los archivos para crear proyectos en almacenamiento compartido.",
                        Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
            }
        }
    }
}
