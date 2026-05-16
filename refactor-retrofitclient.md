# Plan de Refactorización RetrofitClient

## Objective
Refactorizar `RetrofitClient.java` para solucionar el problema del singleton no sincronizado, evitar la creación repetida de `ApiService` y usar siempre el contexto de la aplicación para prevenir memory leaks.

## Key Files & Context
- `app/src/main/java/com/codram/terecojo/data/remote/RetrofitClient.java`
- `app/src/main/java/com/codram/terecojo/TeBuscoApp.java`
- `app/src/main/AndroidManifest.xml`
- Todos los archivos `.java` que invocan `RetrofitClient.getService(...)`

## Implementation Steps
1. **Crear `TeBuscoApp.java`**: Implementar la clase Application e inicializar `RetrofitClient`.
2. **Actualizar `AndroidManifest.xml`**: Registrar `TeBuscoApp`.
3. **Actualizar `RetrofitClient.java`**:
   - Añadir inicialización con `Application` context.
   - Implementar el patrón double-checked locking con `volatile`.
   - Inicializar el interceptor y `ApiService` dentro del singleton de forma thread-safe, manteniendo la lectura dinámica del token.
   - Quitar el parámetro `Context` del método `getService()`.
4. **Refactorizar referencias en todo el proyecto**: 
   - Reemplazar todas las apariciones de `RetrofitClient.getService(...)` por `RetrofitClient.getService() // MODIFICADO`.

## Verification & Testing
- Compilar el proyecto para verificar que no queden referencias con el método antiguo `getService(context)`.
- Asegurar que las llamadas a la API se siguen realizando correctamente con el token actualizado.
