# Guía paso a paso: Maratón Doomsday 🍿

Esta guía asume que nunca has usado Firebase ni Android Studio. Hay 3 partes:

1. **Crear el proyecto de Firebase** (es gratis, ~10 minutos) → obtienes el archivo `google-services.json`.
2. **Convertir el proyecto en un APK** (el archivo instalable).
3. **Instalar el APK** en tu celular y en el de tu papá.

> ¿Por qué Firebase? Es el servicio de Google que guarda los datos "en la nube" para que los dos
> celulares vean lo mismo en tiempo real. El plan gratuito (Spark) alcanza de sobra: no pide tarjeta.

---

## Parte 1 · Crear el proyecto de Firebase

Hazlo desde una computadora (en el celular también se puede, pero es más incómodo).
Los nombres de los botones pueden variar un poco porque Google cambia la página seguido.

### 1.1 Crear el proyecto
1. Entra a **https://console.firebase.google.com** e inicia sesión con tu cuenta de Google (Gmail).
2. Presiona **"Crear un proyecto"** (o **"Comenzar con un proyecto de Firebase"**).
3. Nombre del proyecto: escribe `maraton-doomsday` → acepta las condiciones → **Continuar**.
4. Si pregunta por **Gemini** o **Google Analytics**: desactívalos (no se necesitan) → **Crear proyecto**.
5. Espera a que termine y presiona **Continuar**.

### 1.2 Registrar la app Android y descargar `google-services.json`
1. En la página principal del proyecto, presiona el ícono de **Android** (el robotcito verde)
   o **"Agregar app" → Android**.
2. **Nombre del paquete de Android**: escribe EXACTAMENTE esto (sin espacios):
   ```
   com.cami.maratondoomsday
   ```
3. Sobrenombre de la app: `Maratón Doomsday` (opcional). Deja vacío el "certificado SHA-1".
4. Presiona **Registrar app**.
5. Presiona **Descargar google-services.json**. ⭐ **Este es el archivo que necesito.**
6. Las siguientes pantallas ("Agregar el SDK de Firebase", etc.) ya están hechas en el proyecto:
   presiona **Siguiente** / **Ir a la consola** hasta salir.

### 1.3 Activar el inicio de sesión anónimo
La app no pide usuario ni contraseña, pero Firebase necesita saber que quien escribe es "la app".
1. En el menú de la izquierda: **Compilación (Build) → Authentication**.
2. Presiona **Comenzar**.
3. En la pestaña **Método de acceso (Sign-in method)**, elige **Anónimo**.
4. Activa el interruptor **Habilitar** → **Guardar**.

### 1.4 Crear la base de datos Firestore
1. En el menú de la izquierda: **Compilación (Build) → Firestore Database**.
2. Presiona **Crear base de datos**.
3. Si pregunta la edición, elige **Standard** (la normal).
4. **Ubicación**: elige `northamerica-south1 (Querétaro)` si aparece; si no, `nam5 (United States)`.
   ⚠️ Esto ya no se puede cambiar después, pero cualquiera funciona bien.
5. Elige **Iniciar en modo de producción** → **Crear**.
6. Cuando cargue, ve a la pestaña **Reglas (Rules)**, borra todo lo que hay y pega esto:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /marathons/{rest=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```
7. Presiona **Publicar**.

¡Listo! Firebase quedó configurado. 🎉

---

## Parte 2 · Convertir el proyecto en un APK

Elige **una** de estas opciones. La A es la más fácil.

### Opción A (la más fácil): mándame el archivo y yo lo compilo
1. Abre `google-services.json` con el Bloc de notas (o cualquier editor de texto).
2. Copia **todo** el contenido y pégalo en el chat con Claude.
   (No es una contraseña: es la "dirección" de tu proyecto de Firebase. Lo que protege
   tus datos son las reglas del paso 1.4 y el código de 6 caracteres.)
3. Yo compilo el APK y te lo regreso listo para instalar.

### Opción B: que GitHub lo compile solo (sin instalar nada)
El proyecto ya trae una "receta" (`.github/workflows/compilar-apk.yml`) que GitHub ejecuta automáticamente.
1. Entra a tu repositorio en GitHub → **Settings** (Configuración) → en el menú izquierdo
   **Secrets and variables → Actions** → botón verde **New repository secret**.
2. **Name**: `GOOGLE_SERVICES_JSON`
3. **Secret**: pega **todo** el contenido de `google-services.json`. → **Add secret**.
4. Ve a la pestaña **Actions** → elige **Compilar APK** → **Run workflow** → **Run workflow**.
   (Si te pide activar Actions, presiona el botón para activarlas.)
5. Espera ~5 minutos a que salga la palomita verde ✅.
6. El APK queda en la sección **Releases** del repositorio (columna derecha de la página
   principal) con el nombre `maraton-doomsday.apk`. También aparece abajo del resumen de
   la ejecución, en **Artifacts** (viene dentro de un .zip).

### Opción C: compilarlo tú en tu computadora con Android Studio
1. Descarga e instala **Android Studio** desde https://developer.android.com/studio
   (Siguiente → Siguiente → Instalar; acepta todas las opciones por defecto, incluido el
   "Android SDK"). La primera vez descarga varias cosas; tarda.
2. Descarga este proyecto: en GitHub, botón verde **Code → Download ZIP**, y descomprímelo.
3. Copia `google-services.json` dentro de la carpeta **`app`** del proyecto
   (queda junto al archivo `build.gradle.kts` de esa carpeta).
4. En Android Studio: **Open** → elige la carpeta del proyecto (la que tiene `settings.gradle.kts`).
   Espera a que termine de "sincronizar" (barra de progreso abajo a la derecha).
5. Menú **Build → Generate App Bundles or APKs → Generate APKs**
   (en algunas versiones: **Build → Build Bundle(s) / APK(s) → Build APK(s)**).
6. Cuando termine, aparece un aviso abajo a la derecha: presiona **locate**. El archivo es
   `app-debug.apk` (en `app/build/outputs/apk/debug/`).

   Si prefieres la terminal (dentro de la carpeta del proyecto):
   `./gradlew assembleRelease` (Mac/Linux) o `gradlew.bat assembleRelease` (Windows).
   El APK queda en `app/build/outputs/apk/release/app-release.apk`.

> Si la app abre y dice **"Falta configurar Firebase"**, el APK se compiló sin el
> `google-services.json`: revisa que esté en la carpeta `app` (opción C) o que el secreto
> exista (opción B), y vuelve a compilar.

---

## Parte 3 · Instalar el APK en los celulares

### 3.1 Pasar el archivo al celular (elige una)
- **WhatsApp**: mándate el APK a ti misma o a tu papá como **Documento** (📎 → Documento).
- **Google Drive**: súbelo a Drive y ábrelo desde la app de Drive en el celular.
- **GitHub Releases** (opción B): abre la página del repositorio en el navegador del celular
  → **Releases** → toca `maraton-doomsday.apk` para descargarlo.
- **Cable USB**: conecta el celular a la compu, elige "Transferir archivos" y copia el APK a la
  carpeta **Descargas**.

### 3.2 Instalarlo (permitir "apps de origen desconocido")
1. En el celular, toca el archivo APK (en WhatsApp, Drive, o en la app **Archivos → Descargas**).
2. Android dirá algo como: *"Por tu seguridad, tu teléfono no puede instalar apps desconocidas
   de esta fuente"*. Toca **Configuración**.
3. Activa **"Permitir de esta fuente"** (o "Permitir instalación de apps").
   Esto se activa solo para la app desde la que abriste el APK (WhatsApp, Drive, Archivos…).
4. Regresa con la flecha ← y toca **Instalar**.
5. Si aparece **Google Play Protect** diciendo que la app no es reconocida: toca
   **Más detalles → Instalar de todas formas**. Es normal en apps que no vienen de Play Store.
6. Toca **Abrir**. 🎬

> Los nombres cambian un poco según la marca (Samsung, Motorola, Xiaomi…). En Xiaomi a veces
> pide además iniciar sesión en la cuenta Mi o esperar unos segundos: es normal.

### 3.3 Primera vez que abren la app (con internet)
1. **En tu celular**: toca **Crear maratón**. Aparece un código de 6 caracteres (por ejemplo `K7M2QX`).
   Tócale **Compartir** para mandárselo a tu papá por WhatsApp.
2. Elige **"Soy Cami"**.
3. **En el celular de tu papá**: toca **Unirse con código**, escribe el código → **Unirme**.
   Elige **"Soy Papá"**.
4. Ya los dos ven lo mismo. Cuando uno marca una película o escribe una nota, al otro le
   aparece en segundos. Sin internet también funciona: se sincroniza cuando vuelva la señal.

### 3.4 Actualizar la app más adelante
Si hago cambios, solo instalen el APK nuevo encima del anterior (**Actualizar**). No se pierde
nada, porque los datos viven en Firebase y el APK siempre se firma con la misma llave
(`app/maraton.keystore`).

---

## Cómo funciona la agenda

- **Reglas** (se cambian en Ajustes): lunes, miércoles, jueves, viernes y sábado → 1;
  martes → 0 (D&D); domingo → 2.
- Las fechas se calculan desde la primera película pendiente, siguiendo esas reglas.
- Si un día pasa y no marcaron la película, **todo lo pendiente se recorre solo** al
  siguiente día disponible. Nunca se pierde una película.
- Si vieron una película otro día (por ejemplo, antes de instalar la app), ábranla en
  **Lista → (película) → "Cambiar la fecha en que la vimos"**, para que no ocupe el lugar de hoy.
- **"Mover al siguiente día disponible"** (en el detalle de cada película) la pospone; las que
  siguen en la lista también se recorren para respetar el orden.
- Si con el ritmo actual la última película queda después de la fecha del cine, la pantalla
  **Hoy** muestra: *"Vamos atrasados: nos faltan X películas y quedan Y días disponibles"*.
- En **Lista**: botón **Reordenar** (flechas ↑ ↓), **＋ Agregar película** al final, y para
  quitar una, ábrela → **Quitar del maratón**.

## Para curiosos: cómo está hecho

- Kotlin + Jetpack Compose + Material 3, Android 8.0 (minSdk 26) en adelante.
- Firebase Firestore con caché persistente (funciona sin internet) y autenticación anónima.
- Datos en Firestore:
  - `marathons/{CÓDIGO}` → fecha del cine, reglas, fecha de inicio
  - `marathons/{CÓDIGO}/movies/{id}` → películas
  - `marathons/{CÓDIGO}/notes/{id}` → notas (cada una con el id de su película)
- La lógica de la agenda está en `app/src/main/java/com/cami/maratondoomsday/domain/Scheduler.kt`
  y tiene pruebas en `app/src/test/.../SchedulerTest.kt` (`./gradlew testDebugUnitTest`).
