# RestApp 🌙

App Android **nativa** (Kotlin + Jetpack Compose) para reducir el uso excesivo de redes sociales y mejorar el sueño en adolescentes.

- Estadísticas **reales** con `UsageStatsManager` (sin datos simulados)
- Modo descanso: recordatorios, brillo progresivo, límites voluntarios y pausa consciente
- Datos 100 % privados en el dispositivo; la app **no declara permiso de Internet**
- Paleta azul oscuro / morado / blanco. `minSdk 26` (Android 8+), `targetSdk 35`

## Cómo obtener el APK

### Opción A — Android Studio (la más fácil)
1. Instala [Android Studio](https://developer.android.com/studio).
2. `File > Open` y elige la carpeta `RestApp`. Espera a que termine la sincronización de Gradle.
3. Menú `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Opción B — Línea de comandos
Requisitos: JDK 17 y Android SDK (plataforma 35 y build-tools). Define `ANDROID_HOME` o crea `local.properties` con `sdk.dir=/ruta/al/sdk`.

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Opción C — Sin instalar nada (GitHub Actions)
1. Crea un repositorio en GitHub y sube todo el contenido de esta carpeta.
2. Ve a la pestaña **Actions > Build RestApp APK > Run workflow** (también corre solo al hacer push).
3. Al terminar, descarga el artefacto **RestApp-APK** (contiene `app-debug.apk`).

## Instalar en el celular
- **Por cable (adb):** activa *Depuración USB* y ejecuta `adb install app-debug.apk`.
- **Manual:** copia el APK al teléfono, ábrelo y permite *Instalar apps desconocidas* para tu gestor de archivos.

## Primer uso
La app muestra una pantalla de permisos; cada uno se puede **permitir o rechazar**:
1. **Acceso de uso** → Android abre una lista; activa *RestApp*. Sin esto no hay estadísticas.
2. **Notificaciones** (Android 13+) → diálogo del sistema.
3. **Modificar ajustes del sistema** → solo para bajar el brillo; es opcional.

## Cómo funciona (y límites técnicos)
| Función | Implementación | Limitación / alternativa |
|---|---|---|
| Tiempo por app | `queryEvents` (pares RESUMED/PAUSED), solo apps con icono de lanzador | Android conserva eventos detallados ~7-10 días; RestApp guarda un resumen diario (total y redes) en almacenamiento privado para no perderlo |
| Redes sociales | Lista de paquetes conocidos (TikTok, Instagram, Facebook, WhatsApp, Snapchat, X, Telegram, Discord, Reddit, YouTube…) + categoría `CATEGORY_SOCIAL` | Una red nueva sin categoría declarada no se marca como red |
| Recordatorios | `AlarmManager.setAndAllowWhileIdle` | Son alarmas inexactas (pueden retrasarse algunos minutos con ahorro de batería); así no se pide el permiso de alarmas exactas |
| Brillo progresivo | `Settings.System` con permiso `WRITE_SETTINGS`, 6 pasos hasta la hora de dormir, se restaura a la hora de despertar | Algunos fabricantes ignoran cambios si el brillo adaptativo está activo; se cambia a modo manual mientras dura |
| Anti-scroll | Servicio en primer plano opcional: avisos tras N min seguidos, límites diarios por app y alerta después de la hora de dormir | **Android no permite cerrar o bloquear otra app sin Accesibilidad/Overlay (invasivos).** Por eso los límites son voluntarios: notificaciones + pausa de 60 s |
| Reinicio del celular | `BootReceiver` reprograma alarmas y servicio | Algunos fabricantes (Xiaomi, Huawei…) exigen permitir *inicio automático* manualmente |

## Estructura
```
app/src/main/java/com/restapp/app/
  MainActivity.kt   navegación, permisos, carga de datos
  Screens.kt        Inicio, Estadísticas, Apps, Descanso, Horarios, Permisos/Privacidad
  Ui.kt             tema, gráficos de barras y anillo, botones grandes
  UsageRepo.kt      lectura real de UsageStatsManager
  Scheduler.kt / Receivers.kt / Dimmer.kt / Notifier.kt / MonitorService.kt
  Prefs.kt          almacenamiento privado (SharedPreferences)
```
