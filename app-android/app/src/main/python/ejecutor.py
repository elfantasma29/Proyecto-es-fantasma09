"""
Mini intérprete de scripts. Recibe código Python escrito por el
usuario, lo ejecuta en un espacio de nombres aislado al que además
se le inyecta el Context de Android (como 'contexto'), para que el
script pueda abrir diálogos, vibrar, etc. Captura todo lo impreso
(stdout) y cualquier error con su traceback, como texto plano.
"""
import io
import sys
import traceback


def ejecutar(codigo, contexto=None):
    buffer_salida = io.StringIO()
    stdout_original = sys.stdout
    stderr_original = sys.stderr
    sys.stdout = buffer_salida
    sys.stderr = buffer_salida

    espacio_nombres = {
        "__name__": "__script_usuario__",
        "contexto": contexto,
    }

    try:
        compilado = compile(codigo, "<script_fantasma>", "exec")
        exec(compilado, espacio_nombres)
        salida = buffer_salida.getvalue()
        if not salida.strip():
            salida = "(el script se ejecutó sin errores, pero no imprimió nada en pantalla)"
        return "OK\n" + salida
    except Exception:
        error = traceback.format_exc()
        return "ERROR\n" + buffer_salida.getvalue() + error
    finally:
        sys.stdout = stdout_original
        sys.stderr = stderr_original
