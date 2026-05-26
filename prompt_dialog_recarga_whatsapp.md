# PROMPT — Dialog "¿Cómo Recargar?" con imagen y envío por WhatsApp
**Proyecto:** Te Busco App (Android Java) · Rama: Desarrollo  
**Objetivo:** Reemplazar el dialog informativo simple de recarga por un flujo completo: mostrar número de tarjeta con botón copiar, permitir cargar una captura de pantalla de la transferencia, y enviarla por WhatsApp al administrador con los datos del chofer pre-llenados.

---

## PARTE 1 — `AndroidManifest.xml`

### 1A — Agregar permiso de lectura de imágenes

Agregar junto a los permisos existentes:

```xml
<!-- Para Android 13+ (API 33+) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### 1B — Registrar FileProvider dentro de `<application>`

Agregar junto a los demás `<provider>` o `<service>`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

---

## PARTE 2 — Nuevo archivo: `app/src/main/res/xml/file_paths.xml`

Si el directorio `res/xml/` no existe, crearlo. Crear el archivo:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="external_files" path="." />
    <cache-path name="cache_files" path="." />
    <external-cache-path name="external_cache" path="." />
</paths>
```

---

## PARTE 3 — Nuevo layout: `app/src/main/res/layout/dialog_recarga.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <!-- Título -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="¿Cómo Recargar tu Fondo?"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary" />

        <!-- Instrucción 1 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="Realiza una transferencia desde tu aplicación bancaria al siguiente número de tarjeta:"
            android:textSize="14sp"
            android:textColor="@color/text_secondary"
            android:lineSpacingMultiplier="1.4" />

        <!-- Card con número de tarjeta y botón copiar -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="0dp"
            app:cardBackgroundColor="#F0F4FF"
            app:strokeColor="@color/primary_blue"
            app:strokeWidth="1dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:padding="16dp">

                <!-- Número de tarjeta -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="NÚMERO DE TARJETA"
                        android:textSize="10sp"
                        android:textAllCaps="true"
                        android:letterSpacing="0.08"
                        android:textColor="@color/primary_blue"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/tvNumeroTarjeta"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:text="9226 3000 XXXX XXXX"
                        android:textSize="20sp"
                        android:textStyle="bold"
                        android:textColor="@color/text_primary"
                        android:letterSpacing="0.05"
                        android:fontFamily="monospace" />

                </LinearLayout>

                <!-- Botón copiar -->
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnCopiarTarjeta"
                    style="@style/Widget.Material3.Button.IconButton"
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:contentDescription="Copiar número de tarjeta"
                    app:icon="@android:drawable/ic_menu_share"
                    app:iconSize="20dp"
                    app:iconTint="@color/primary_blue" />

            </LinearLayout>

        </com.google.android.material.card.MaterialCardView>

        <!-- Instrucción 2 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="Una vez realizada la transferencia, toma una captura de pantalla con los detalles completos de la confirmación y envíala al administrador para su revisión."
            android:textSize="14sp"
            android:textColor="@color/text_secondary"
            android:lineSpacingMultiplier="1.4" />

        <!-- Área de preview de imagen seleccionada -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardPreviewImagen"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:visibility="gone"
            app:cardCornerRadius="12dp"
            app:cardElevation="2dp">

            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content">

                <ImageView
                    android:id="@+id/ivPreviewCaptura"
                    android:layout_width="match_parent"
                    android:layout_height="200dp"
                    android:scaleType="centerCrop"
                    android:contentDescription="Vista previa de la captura" />

                <!-- Botón para quitar la imagen -->
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnQuitarImagen"
                    style="@style/Widget.Material3.Button.IconButton"
                    android:layout_width="36dp"
                    android:layout_height="36dp"
                    android:layout_gravity="top|end"
                    android:layout_margin="8dp"
                    app:icon="@android:drawable/ic_menu_close_clear_cancel"
                    app:iconSize="16dp"
                    app:iconTint="@color/white"
                    app:backgroundTint="#88000000" />

            </FrameLayout>

        </com.google.android.material.card.MaterialCardView>

        <!-- Botón cargar captura -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnCargarCaptura"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="📷  Cargar captura de transferencia"
            android:textSize="14sp"
            app:cornerRadius="12dp"
            app:strokeColor="@color/primary_blue" />

        <!-- Botón enviar por WhatsApp -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnEnviarWhatsApp"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="Enviar por WhatsApp"
            android:textSize="15sp"
            android:enabled="false"
            app:cornerRadius="12dp"
            app:backgroundTint="#25D366"
            app:icon="@android:drawable/sym_action_chat"
            app:iconSize="18dp" />

        <!-- Nota informativa -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="⚠️ El saldo se acreditará una vez que el administrador verifique la transferencia."
            android:textSize="12sp"
            android:textColor="@color/text_secondary"
            android:lineSpacingMultiplier="1.3" />

    </LinearLayout>

</androidx.core.widget.NestedScrollView>
```

---

## PARTE 4 — Modificar `VerificationActivity.java`

### 4A — Nuevas variables de instancia

Agregar al inicio de la clase, junto a `ActivityVerificationBinding binding`:

```java
// Recarga dialog
private android.net.Uri capturaUri = null;
private android.app.Dialog dialogRecarga = null;

// Launcher moderno para selección de imagen (mismo patrón que AddVehicleDialogFragment)
private final androidx.activity.result.ActivityResultLauncher<Intent> pickImageLauncher =
    registerForActivityResult(
        new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK
                    && result.getData() != null
                    && result.getData().getData() != null) {
                capturaUri = result.getData().getData();
                // Actualizar preview en el dialog si sigue abierto
                actualizarPreviewCaptura();
            }
        }
    );
```

### 4B — Reemplazar el método `mostrarDialogRecarga()`

Reemplazar el método actual (el que solo muestra un `MaterialAlertDialogBuilder` simple) por este completo:

```java
private void mostrarDialogRecarga() {
    // Número de tarjeta del administrador — modificar cuando cambie
    final String NUMERO_TARJETA = "9226 3000 XXXX XXXX";
    final String WHATSAPP_NUMBER = "5350140609"; // Cuba +53, sin el +

    // Inflar el layout del dialog
    android.view.View dialogView = getLayoutInflater()
        .inflate(R.layout.dialog_recarga, null);

    // Referencias a las vistas del dialog
    android.widget.TextView tvNumeroTarjeta =
        dialogView.findViewById(R.id.tvNumeroTarjeta);
    com.google.android.material.button.MaterialButton btnCopiarTarjeta =
        dialogView.findViewById(R.id.btnCopiarTarjeta);
    com.google.android.material.card.MaterialCardView cardPreview =
        dialogView.findViewById(R.id.cardPreviewImagen);
    android.widget.ImageView ivPreview =
        dialogView.findViewById(R.id.ivPreviewCaptura);
    com.google.android.material.button.MaterialButton btnQuitarImagen =
        dialogView.findViewById(R.id.btnQuitarImagen);
    com.google.android.material.button.MaterialButton btnCargarCaptura =
        dialogView.findViewById(R.id.btnCargarCaptura);
    com.google.android.material.button.MaterialButton btnEnviarWhatsApp =
        dialogView.findViewById(R.id.btnEnviarWhatsApp);

    // Número de tarjeta
    tvNumeroTarjeta.setText(NUMERO_TARJETA);

    // Botón copiar tarjeta
    btnCopiarTarjeta.setOnClickListener(v -> {
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
            "Número de tarjeta", NUMERO_TARJETA.replace(" ", ""));
        clipboard.setPrimaryClip(clip);
        android.widget.Toast.makeText(this,
            "Número copiado al portapapeles", android.widget.Toast.LENGTH_SHORT).show();
    });

    // Botón cargar captura — abre la galería
    btnCargarCaptura.setOnClickListener(v -> {
        // Verificar permiso en Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 200);
                return;
            }
        }
        Intent pickIntent = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        pickImageLauncher.launch(pickIntent);
    });

    // Botón quitar imagen
    btnQuitarImagen.setOnClickListener(v -> {
        capturaUri = null;
        cardPreview.setVisibility(android.view.View.GONE);
        btnEnviarWhatsApp.setEnabled(false);
        btnCargarCaptura.setText("📷  Cargar captura de transferencia");
    });

    // Botón enviar por WhatsApp
    btnEnviarWhatsApp.setOnClickListener(v -> {
        if (capturaUri == null) {
            android.widget.Toast.makeText(this,
                "Primero carga la captura de la transferencia",
                android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        enviarPorWhatsApp(WHATSAPP_NUMBER, capturaUri);
    });

    // Guardar referencias para actualizarlas desde el launcher
    // Usamos tags en las vistas para acceder desde actualizarPreviewCaptura()
    dialogView.setTag(new Object[]{cardPreview, ivPreview, btnEnviarWhatsApp, btnCargarCaptura});

    // Construir y mostrar el dialog
    dialogRecarga = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setView(dialogView)
        .setNegativeButton("Cerrar", (d, w) -> {
            dialogRecarga = null;
        })
        .create();

    dialogRecarga.show();
}
```

### 4C — Nuevo método `actualizarPreviewCaptura()`

Este método es llamado por el `pickImageLauncher` cuando el usuario elige una imagen, y actualiza las vistas del dialog si sigue abierto:

```java
private void actualizarPreviewCaptura() {
    if (dialogRecarga == null || !dialogRecarga.isShowing() || capturaUri == null) return;

    // Recuperar referencias desde el tag del dialog
    android.view.View dialogView = dialogRecarga.findViewById(R.id.cardPreviewImagen);
    if (dialogView == null) return;

    // Buscar las vistas directamente desde la ventana del dialog
    com.google.android.material.card.MaterialCardView cardPreview =
        dialogRecarga.findViewById(R.id.cardPreviewImagen);
    android.widget.ImageView ivPreview =
        dialogRecarga.findViewById(R.id.ivPreviewCaptura);
    com.google.android.material.button.MaterialButton btnEnviar =
        dialogRecarga.findViewById(R.id.btnEnviarWhatsApp);
    com.google.android.material.button.MaterialButton btnCargar =
        dialogRecarga.findViewById(R.id.btnCargarCaptura);

    if (cardPreview == null || ivPreview == null) return;

    // Mostrar preview de la imagen seleccionada
    cardPreview.setVisibility(android.view.View.VISIBLE);
    com.bumptech.glide.Glide.with(this)
        .load(capturaUri)
        .centerCrop()
        .into(ivPreview);

    // Habilitar el botón de enviar
    if (btnEnviar != null) btnEnviar.setEnabled(true);

    // Cambiar texto del botón cargar
    if (btnCargar != null) btnCargar.setText("📷  Cambiar captura");
}
```

### 4D — Nuevo método `enviarPorWhatsApp()`

```java
private void enviarPorWhatsApp(String numeroWhatsApp, android.net.Uri imagenUri) {
    // Obtener datos del chofer de la sesión
    com.codram.terecojo.data.model.AuthResponse.User usuario =
        com.codram.terecojo.utils.SessionManager.getInstance(this).getUser();

    String nombre   = usuario != null ? usuario.getNombre()   : "Chofer";
    String username = usuario != null ? "@" + usuario.getUsername() : "";

    // Construir el mensaje de texto pre-llenado
    String mensaje = "📋 *Solicitud de Recarga de Fondo — Te Busco*\n\n"
        + "👤 Nombre: " + nombre + "\n"
        + "🆔 Usuario: " + username + "\n\n"
        + "Adjunto captura de la transferencia para su revisión.\n"
        + "Por favor confirmar la acreditación del saldo. ¡Gracias!";

    // Verificar si WhatsApp está instalado
    Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
    whatsappIntent.setType("image/*");
    whatsappIntent.setPackage("com.whatsapp");
    whatsappIntent.putExtra("jid", numeroWhatsApp + "@s.whatsapp.net");
    whatsappIntent.putExtra(Intent.EXTRA_TEXT, mensaje);
    whatsappIntent.putExtra(Intent.EXTRA_STREAM, imagenUri);
    whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    if (whatsappIntent.resolveActivity(getPackageManager()) != null) {
        startActivity(whatsappIntent);
    } else {
        // WhatsApp no instalado — abrir con el link wa.me como fallback
        try {
            String mensajeCodificado = android.net.Uri.encode(mensaje);
            android.net.Uri uri = android.net.Uri.parse(
                "https://wa.me/" + numeroWhatsApp + "?text=" + mensajeCodificado);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(browserIntent);
            android.widget.Toast.makeText(this,
                "La imagen deberá adjuntarse manualmente en WhatsApp Web.",
                android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this,
                "No se pudo abrir WhatsApp. Instala la aplicación e intenta de nuevo.",
                android.widget.Toast.LENGTH_LONG).show();
        }
    }
}
```

### 4E — Agregar `import Intent` si no está ya en `VerificationActivity.java`

```java
import android.content.Intent;
```

---

## PARTE 5 — Constante del número de tarjeta

El número de tarjeta está hardcodeado en `mostrarDialogRecarga()` como:

```java
final String NUMERO_TARJETA = "9226 3000 XXXX XXXX";
```

**Reemplaza `"9226 3000 XXXX XXXX"` por el número real de tu tarjeta.**

El formato con espacios cada 4 dígitos es intencional — hace más fácil la lectura y el botón copiar elimina los espacios automáticamente antes de copiar al portapapeles.

---

## RESUMEN DE ARCHIVOS

| Archivo | Tipo |
|---|---|
| `app/src/main/AndroidManifest.xml` | ✏️ Permiso `READ_MEDIA_IMAGES` + `FileProvider` |
| `app/src/main/res/xml/file_paths.xml` | ✅ Nuevo |
| `app/src/main/res/layout/dialog_recarga.xml` | ✅ Nuevo |
| `app/src/main/java/.../VerificationActivity.java` | ✏️ Variable `capturaUri` + `pickImageLauncher` + 3 métodos nuevos |

---

## FLUJO COMPLETO ESPERADO

```
Chofer toca "¿Cómo Recargar?"
    ↓
Aparece el dialog con:
  ┌─────────────────────────────────┐
  │ ¿Cómo Recargar tu Fondo?        │
  │                                 │
  │ Realiza una transferencia al    │
  │ siguiente número de tarjeta:    │
  │                                 │
  │ ┌─────────────────────────────┐ │
  │ │ NÚMERO DE TARJETA           │ │
  │ │ 9226 3000 XXXX XXXX  [📋]  │ │
  │ └─────────────────────────────┘ │
  │                                 │
  │ Toma una captura de los         │
  │ detalles de la confirmación...  │
  │                                 │
  │ [ 📷 Cargar captura ]           │
  │                                 │
  │ [ Enviar por WhatsApp ] (gris)  │
  └─────────────────────────────────┘

Toca [📋] → número copiado al portapapeles → Toast confirmación

Toca [📷 Cargar captura]
    → Abre galería del teléfono
    → Chofer selecciona la imagen
    → Preview de la imagen aparece en el dialog
    → Botón "Enviar por WhatsApp" se activa (verde)

Toca [Enviar por WhatsApp]
    → Abre WhatsApp con:
        • Chat: +53 50140609
        • Imagen adjunta
        • Texto:
          "📋 Solicitud de Recarga de Fondo — Te Busco
           👤 Nombre: Juan Pérez
           🆔 Usuario: @juanperez
           Adjunto captura..."
    → Chofer solo toca Enviar en WhatsApp
```
