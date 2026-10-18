"""
Script de ejemplo para pegar en el editor de Lector Fantasma.
Muestra un dialogo con dos botones: uno vibra el telefono,
el otro solo cierra el dialogo. Usa 'contexto', que el
ejecutor ya tiene disponible automaticamente.
"""
from java import jclass

AlertDialogBuilder = jclass("android.app.AlertDialog$Builder")
Context = jclass("android.content.Context")
VibrationEffect = jclass("android.os.VibrationEffect")


def vibrar(contexto):
    vibrador = contexto.getSystemService(Context.VIBRATOR_SERVICE)
    if vibrador is not None and vibrador.hasVibrator():
        efecto = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrador.vibrate(efecto)
        print("Vibracion enviada.")
    else:
        print("Este dispositivo no tiene motor de vibracion, o no se detecto.")


def mostrar_dialogo():
    constructor = AlertDialogBuilder(contexto)
    constructor.setTitle("Prueba de dos botones")
    constructor.setMessage("Elige una opcion:")

    constructor.setPositiveButton(
        "Vibrar",
        lambda dialogo, cual: vibrar(contexto),
    )
    constructor.setNegativeButton(
        "Cerrar",
        lambda dialogo, cual: print("Boton 'Cerrar' presionado, no hace nada mas."),
    )

    dialogo = constructor.create()
    dialogo.show()
    print("Dialogo mostrado con 2 botones.")


mostrar_dialogo()
