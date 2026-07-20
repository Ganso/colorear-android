# Colorear - Versión Android

`COLOREAR` es una versión para Android del juego original hecho por Jorge y Javi Prieto para MS-DOS en el año 1996.

El código fuente de la versión original de MS-DOS puede encontrarse en: [https://github.com/Ganso/Colorear](https://github.com/Ganso/Colorear)

Este proyecto es un port directo creado con la ayuda de la Inteligencia Artificial **Claude**, conservando su framebuffer indexado original de 320 x 200 píxeles y la semántica de la paleta VGA de 256 colores. La lógica original ha sido adaptada a Kotlin para funcionar de forma nativa en dispositivos Android modernos.

Infinitas gracias a **Migue McLeod** por conservar el código tras años creyéndolo perdido, y por hacer el port a Android.

## Abrir y ejecutar

1. Abrir este directorio con Android Studio.
2. Esperar a que finalice la sincronización de Gradle.
3. Ejecutar la configuración `app` en un dispositivo o emulador Android.

La actividad está bloqueada en apaisado y mantiene la relación de aspecto original (16:10).
Los assets originales necesarios se incluyen en `app/src/main/assets/dos`.

## Controles táctiles

- En el selector inicial, tocar el nombre de un dibujo.
- En el menú de dibujo, tocar un color para seleccionarlo.
- Tocar fuera de los controles para pasar al modo de coloreado.
- En el dibujo, apoyar y arrastrar un dedo para mover el lápiz visible por encima.
- Soltar el dedo para colorear la zona señalada por la punta del cursor.
- Hacer un toque breve con dos dedos para volver a abrir el menú.
- También se puede mantener pulsado para volver al menú.

Los ratones Android también pueden usar el botón secundario para alternar el menú.
La primera vez que se colorea una zona aparece un recordatorio de los controles.
