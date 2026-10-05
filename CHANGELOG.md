# Novedades de Pixelados

Cada build de la app lleva un nombre de letra griega que corresponde al `versionCode`
(`1 = alfa`, `2 = beta`, …, `22 = chi`). La pantalla de inicio muestra esa etiqueta, por ejemplo
*"Ver 1.0 build chi"*, así siempre se sabe qué versión está instalada.

Las versiones firmadas para instalar se publican en
[Releases](https://github.com/lgonzalh/pixelados/releases).

---

## 1.0 — build chi (versionCode 22)

Primera entrega firmada para distribución: **APK de release con R8, 2,2 MB** (la build de depuración pesaba
21,7 MB). Incluye la llave de firma propia del proyecto y la tarea `publicarRelease`.

### Añadido

- **Lienzo exacto**: cada cuadro de la cuadrícula es un píxel relleno por completo, sin suavizado. Tamaños de
  16×16, 32×32, 64×64 y 100×100, más tamaño personalizado entre 8 y 128.
- **Herramientas de dibujo**: pincel de un píxel, borrador, relleno por zona, cuentagotas y selección
  rectangular con opción de borrar la zona marcada.
- **Color**: paleta de 288 colores en nueve familias, fila de colores recientes, color personalizado con dos
  deslizadores y reemplazo de un color en todo el dibujo.
- **Galería de 149 símbolos**: 109 geométricos, de naturaleza, animales, objetos y fantasía, más 40 Pokémon,
  en tres estilos: relleno, contorno o color original de la figura.
- **Simetría** horizontal y vertical, cuadrícula conmutable y capa de referencia para calcar una foto con
  opacidad ajustable.
- **Conversión de foto a pixel art** con nivel de detalle ajustable.
- **Exportación PNG a 1×, 2× y 4×** sin suavizado, con transparencia conservada, guardado en galería y
  hoja de compartir.
- **Apariencia**: seis paletas de color y modo claro/oscuro, aplicados a toda la app.
- **Guardado automático** con miniaturas y galería de "Mis lienzos".
- **Ayuda dentro del editor** en ventana emergente.

### Corregido

- **Deshacer al abrir un dibujo guardado ya no lo borra.** El historial empezaba en el lienzo vacío con el
  que nace el editor, por lo que deshacer podía vaciar el dibujo completo del usuario.
- Los símbolos se colocan **al soltar el dedo**, con vista previa centrada que sigue al dedo al arrastrar.
- La galería de símbolos ya no se cierra ni falla al cambiar de categoría o de estilo (existían dos figuras
  con el mismo nombre y eso rompía la lista).
- Los iconos de la barra superior ya no se ven sobredimensionados: se normalizó el peso óptico de los iconos
  propios respecto a los del sistema.
- Se eliminaron los textos de guía que se cortaban en la barra inferior.

### Notas técnicas

- `minSdk` 26 (Android 8.0) y `targetSdk` 36; sin librerías nativas, funciona en cualquier procesador.
- Firmado con la llave de release del proyecto (fuera del repositorio por seguridad).
- 26 pruebas unitarias y 16 instrumentadas en verde, verificadas en un moto g23 (Android 14) y en una
  tablet W30 (Android 16).

---

## Historial de desarrollo (builds internas alfa → phi)

Antes de esta entrega, el proyecto se desarrolló y probó en builds de depuración identificadas con las letras
anteriores. Resumen por tema:

- **Lienzo y editor**: vista de píxeles con cuadrícula exacta, trazo continuo interpolado, zoom con dos dedos,
  candado de vista, deshacer por trazo (no píxel a píxel) y autoguardado.
- **Interfaz**: filas de herramientas y acciones de altura fija (nada se desplaza al dibujar), franja de
  contexto por herramienta, ayuda en ventana emergente y textos en español.
- **Apariencia**: sistema de paletas con seis combinaciones, modo claro/oscuro persistente, tipografía
  redondeada Quicksand y set de iconos propio del proyecto.
- **Símbolos**: catálogo generado por código (109 figuras) y categoría Pokémon creada a partir de referencias
  pixel art, con los tres estilos de estampado.
- **Fotografía**: conversión de foto a pixel art con reducción por promedio de área y cuantización a la paleta.
- **Estabilidad**: pruebas unitarias del modelo, pruebas instrumentadas de la interfaz en dispositivo, y
  correcciones de cierres inesperados y de contraste en modo claro/oscuro.

---

## Cómo se publica una versión

1. `.\gradlew assembleRelease` — compila, firma y copia el APK a `apk/pixelados-<versión>-<código>-<letra>.apk`.
2. Crear la etiqueta: `git tag -a v1.0-<letra> -m "Pixelados 1.0 - build <letra>"` y `git push origin v1.0-<letra>`.
3. En GitHub → *Releases* → *Draft a new release*: elegir la etiqueta, pegar estas notas y adjuntar el APK.
