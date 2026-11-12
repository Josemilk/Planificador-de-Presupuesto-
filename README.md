# Ábaco

App Android nativa, 100% offline. Presupuesto, hábitos, códigos QR y de barras, y calculadora de efectivo. Los datos se quedan en el teléfono: no hay cuenta ni servidor.

Hecha con Kotlin y Jetpack Compose. El paquete es `app.abaco`.

## Qué incluye

- **Presupuesto.** Ingresos y gastos, categorías, meta de ahorro, saldo que se actualiza al momento, gráfico de gastos, notas del mes, historial y PDF.
- **Hábitos.** Marcas diarias, vista de semana o mes, racha y porcentaje del mes. Se pueden crear, editar y borrar, cada uno con su color.
- **QR.** Vista previa al escribir, colores, tamaño, nivel de corrección y descarga PNG. El escáner lee un QR (y lo abre si es un enlace) o un código de barras y muestra su contenido.
- **Efectivo.** Billetes y monedas con valor editable, cantidades, selección, borrado y conteos guardados. Plantillas USD, MXN y EUR.
- **Copia local.** Todo se escribe en la memoria privada de la app (`abaco.json`, con una caché de respaldo). Desde Presupuesto se puede exportar o restaurar un JSON.

La app no pide permiso de internet. La cámara solo se usa si pulsas escanear.

## Compilar en GitHub Actions

1. Crea un repositorio vacío.
2. Sube **el contenido de esta carpeta** como raíz (debe verse `settings.gradle.kts` y `.github/workflows/build-apk.yml` en la raíz, no dentro de otra carpeta).
3. En GitHub abre **Actions → Compilar APK → Run workflow**, o haz un push a `main`.
4. Al terminar, descarga el artefacto `abaco-apk`. El archivo se llama `app-release.apk`. GitHub lo conserva 15 días.

Ese APK está firmado con la clave de depuración para que se pueda instalar sin Play Store. En el teléfono: Ajustes → instalar apps desconocidas, y abre el archivo.

## Abrir en Android Studio

1. Android Studio con JDK 17.
2. **File → Open** y elige esta carpeta.
3. Deja que baje el SDK 35 si lo pide.
4. Run, o `./gradlew assembleRelease`.

El APK queda en `app/build/outputs/apk/release/`.

## Notas

- El escáner usa ZXing (Apache 2.0) y funciona sin Google Play Services.
- Abrir un enlace del QR sale de la app y usa el navegador del teléfono. Eso sí necesita red, y solo si tú lo pides.
- `local.properties` no se sube: cada máquina tiene su propia ruta del SDK.
