Eres un desarrollador Android senior experto en Java. Vas a refactorizar RetrofitClient.java para corregir tres problemas: el singleton no sincronizado, la creación repetida de ApiService por reflection en cada llamada, y el contexto variable que puede causar memory leaks. No modifiques ninguna lógica existente fuera de este archivo salvo lo indicado explícitamente.

Contexto

RetrofitClient.java tiene un singleton lazy private static Retrofit retrofit = null no sincronizado.
getService(Context context) recibe un Context en cada llamada y llama a retrofit.create(ApiService.class) en cada invocación, creando un nuevo proxy por reflection cada vez.
El token JWT se lee dinámicamente en cada request a través de un Interceptor que accede a SessionManager. Esa lógica es correcta y no debe tocarse.
Todas las llamadas a RetrofitClient.getService(context) en el proyecto pasan this o getContext() desde una Activity o Fragment.


Cambios a implementar
1. Convertir el singleton a inicialización con Application context
Añadir un método estático de inicialización init(Context context) que debe llamarse una sola vez desde Application.onCreate():
java// NUEVO
public static void init(Context context) {
    if (appContext == null) {
        appContext = context.getApplicationContext();
    }
}
Añadir el campo estático privado:
java// NUEVO
private static Context appContext = null;
Esto garantiza que siempre se use Application context — nunca un Activity context — eliminando el riesgo de memory leak.
2. Aplicar double-checked locking con volatile
Reemplazar:
javaprivate static Retrofit retrofit = null;
Por:
java// MODIFICADO — volatile garantiza visibilidad entre hilos
private static volatile Retrofit retrofit = null;
private static volatile ApiService apiService = null;
3. Crear ApiService una sola vez junto al singleton
Reemplazar el método getService(Context context) completo por:
java// MODIFICADO
public static ApiService getService() {
    if (apiService == null) {
        synchronized (RetrofitClient.class) {
            if (apiService == null) {
                if (appContext == null) {
                    throw new IllegalStateException(
                        "RetrofitClient no inicializado. Llama a RetrofitClient.init(context) en Application.onCreate()"
                    );
                }
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

                OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        // El token se lee en cada request — correcto, no cachear
                        SessionManager session = new SessionManager(appContext);
                        String token = session.getToken();
                        okhttp3.Request request = chain.request().newBuilder()
                            .addHeader("Authorization", token != null ? "Bearer " + token : "")
                            .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

                retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

                apiService = retrofit.create(ApiService.class);
            }
        }
    }
    return apiService;
}
4. Crear la clase Application si no existe
Si el proyecto no tiene una clase que extienda Application, crear TeBuscoApp.java en el paquete raíz:
java// NUEVO
public class TeBuscoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}
Y registrarla en AndroidManifest.xml dentro del tag <application>:
xml<!-- NUEVO -->
android:name=".TeBuscoApp"
Si ya existe una clase Application en el proyecto, añadir RetrofitClient.init(this) al inicio de su onCreate() en lugar de crear una nueva.
5. Actualizar todas las llamadas a getService
Buscar en todo el proyecto todas las ocurrencias de:
javaRetrofitClient.getService(context)
RetrofitClient.getService(this)
RetrofitClient.getService(getContext())
RetrofitClient.getService(getActivity())
Y reemplazarlas todas por:
javaRetrofitClient.getService() // MODIFICADO
Sin ningún parámetro. Esta búsqueda debe hacerse en todos los archivos .java del proyecto, no solo en los que conozcas de antemano.

Restricciones estrictas

El token JWT debe seguir leyéndose dinámicamente en cada request dentro del interceptor usando new SessionManager(appContext). Nunca cachear el token en un campo estático — debe leerse fresco en cada llamada para reflejar cambios de sesión.
No modificar SessionManager.java ni ApiService.java.
No modificar la BASE_URL ni la configuración de GsonConverterFactory.
El IllegalStateException en getService() actúa como red de seguridad para detectar en desarrollo si init() no fue llamado. No eliminarlo.
Si el proyecto ya tiene una clase Application registrada en el Manifest, no crear TeBuscoApp.java — solo añadir RetrofitClient.init(this) a la existente.
Marcar cada cambio con // NUEVO o // MODIFICADO según corresponda.