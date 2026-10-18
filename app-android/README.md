# Lector Fantasma (APK de prueba)

Código fuente de la APK de prueba del intérprete Python embebido (Chaquopy) +
FFmpegKit + yt-dlp, con editor de proyectos y tienda de bibliotecas.

## Compilar

Necesitas el Android SDK instalado. Antes de compilar, crea/edita
`local.properties` en esta carpeta con tu propia ruta:

```
sdk.dir=/ruta/a/tu/android-sdk
```

Luego:

```
./gradlew assembleDebug
```

(o `gradle assembleDebug` si no hay wrapper ejecutable en tu entorno).

## Qué incluye

- Intérprete Python vía Chaquopy, con `yt-dlp` empaquetado directamente en
  el APK (tiene dependencias nativas: `brotli`, `pycryptodomex`).
- `FFmpegKit` (fork mantenido `dev.ffmpegkit-maintained`, ya que el
  original de `com.arthenica` fue retirado y sus artefactos ya no están
  en Maven Central).
- Gestión de proyectos en almacenamiento compartido (`/LectorFantasma/`).
- Editor con deshacer/rehacer, buscar y reemplazar, eliminar y ejecutar
  código.
- Tienda de bibliotecas que lee `../bibliotecas/bibliotecas_totales.json`
  y `../bibliotecas/bibliotecas_catalogo.json` de este mismo repositorio,
  descarga el `.zip` correspondiente y lo agrega a `sys.path`.

## Pendiente para fase 2

- Múltiples archivos por proyecto (nuevo archivo, lista de archivos,
  renombrar/eliminar archivo, ejecución combinando módulos).
