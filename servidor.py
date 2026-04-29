from flask import Flask, request, jsonify
import subprocess
import os

app = Flask(__name__)

# Ruta de descargas en el almacenamiento interno de Android
PATH_FINAL = "/sdcard/Download/%(title)s.%(ext)s"

# Presets de calidad para mantener compatibilidad con tu código anterior
QUALITY_PRESETS_AUDIO = {
    "best": "320K",
    "320k": "320K",
    "256k": "256K",
    "192k": "192K",
    "128k": "128K"
}

@app.route('/')
def root():
    """Endpoint de información similar al de Render"""
    return jsonify({
        "api": "Termux Omni API Pura",
        "version": "2.0.1",
        "status": "operational",
        "endpoints": {
            "buscar": "/buscar?q={termino}",
            "descargar": "/descargar?url={url}&format={mp3/mp4}&quality={valor}"
        }
    })

@app.route('/buscar')
def buscar():
    """Busca 10 resultados y devuelve JSON puro"""
    query = request.args.get('q')
    if not query:
        return jsonify({"error": "Falta el parámetro q"}), 400

    # Buscamos 10 resultados ignorando errores de videos privados/caídos
    comando = [
        "yt-dlp", f"ytsearch10:{query}", 
        "--get-title", "--get-id", "--flat-playlist", 
        "--ignore-errors"
    ]
    
    try:
        salida = subprocess.check_output(comando).decode("utf-8").split("\n")
        busqueda = []
        # yt-dlp devuelve Título e ID en líneas alternas
        for i in range(0, len(salida)-1, 2):
            if salida[i] and salida[i+1]:
                busqueda.append({
                    "id": (i // 2) + 1,
                    "titulo": salida[i],
                    "url": f"https://www.youtube.com/watch?v={salida[i+1]}"
                })
        return jsonify(busqueda)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/descargar')
def descargar():
    """Inicia la descarga según parámetros exactos"""
    url = request.args.get('url')
    formato = request.args.get('format', 'mp3')
    calidad = request.args.get('quality', '192k')

    if not url:
        return jsonify({"error": "Falta el parámetro url"}), 400

    comando = ["yt-dlp", "-o", PATH_FINAL, "--no-playlist", "--ignore-errors"]

    if formato == "mp3":
        # Traducir preset a valor K o usar el valor directo
        q_value = QUALITY_PRESETS_AUDIO.get(calidad.lower(), "192K")
        comando += ["-x", "--audio-format", "mp3", "--audio-quality", q_value]
    else:
        # Para video, el parámetro 'quality' se usa como altura (ej. 480, 720)
        res = calidad.replace('p', '') # Quitar la 'p' si viene como 480p
        comando += ["-f", f"bestvideo[height<={res}]+bestaudio/best", "--merge-output-format", "mp4"]

    try:
        # Ejecución en segundo plano
        subprocess.Popen(comando + [url])
        return jsonify({
            "status": "success",
            "mensaje": f"Descarga de {formato} iniciada",
            "info": {"url": url, "formato": formato, "calidad": calidad}
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    # Puerto 5000 para acceso local
    app.run(host='0.0.0.0', port=5000)

