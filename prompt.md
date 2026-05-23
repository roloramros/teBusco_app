Contexto del proyecto:
Tengo una API REST en Node.js/Express. Cuando un pasajero crea una solicitud de viaje, el app Android envía los nombres de provincia y municipio del origen/destino tal como los devuelve la API de Google Geocoding. El problema es que Google a veces devuelve nombres en inglés ("Havana" en lugar de "La Habana") o sin tildes ("Cienaga de Zapata" en lugar de "Ciénaga de Zapata"), y el resolver del backend no los encuentra en la base de datos.
Archivo a modificar:
tebusco-api/src/controllers/solicitudController.js
Cambio 1 — Agregar función normalizarNombre
Justo antes de la función resolverUbicacion, agregar esta función:
javascriptconst normalizarNombre = (nombre) => {
  if (!nombre) return nombre;
  const mapa = {
    'havana': 'La Habana',
    'holguin': 'Holguín',
    'holguín': 'Holguín',
    'camaguey': 'Camagüey',
    'camagüey': 'Camagüey',
    'guantanamo': 'Guantánamo',
    'guantánamo': 'Guantánamo',
    'sancti spiritus': 'Sancti Spíritus',
    'sancti spíritus': 'Sancti Spíritus',
    'ciego de avila': 'Ciego de Ávila',
    'ciego de ávila': 'Ciego de Ávila',
    'pinar del rio': 'Pinar del Río',
    'pinar del río': 'Pinar del Río',
    'isla de la juventud': 'Isla de la Juventud',
    'isle of youth': 'Isla de la Juventud',
  };
  return mapa[nombre.toLowerCase().trim()] || nombre;
};
Cambio 2 — Aplicar normalizarNombre antes de llamar a resolverUbicacion
Actualmente el código llama al resolver así:
javascriptif (origen_municipio_nombre) {
  const { pId, mId } = await resolverUbicacion(origen_provincia_nombre, origen_municipio_nombre);
  resolved_origen_provincia_id = pId;
  resolved_origen_municipio_id = mId || resolved_origen_municipio_id;
}

if (destino_municipio_nombre) {
  const { pId, mId } = await resolverUbicacion(destino_provincia_nombre, destino_municipio_nombre);
  resolved_destino_provincia_id = pId;
  resolved_destino_municipio_id = mId;
}
Debe cambiarse para aplicar normalizarNombre a ambos argumentos antes de pasarlos:
javascriptif (origen_municipio_nombre) {
  const { pId, mId } = await resolverUbicacion(
    normalizarNombre(origen_provincia_nombre),
    normalizarNombre(origen_municipio_nombre)
  );
  resolved_origen_provincia_id = pId;
  resolved_origen_municipio_id = mId || resolved_origen_municipio_id;
}

if (destino_municipio_nombre) {
  const { pId, mId } = await resolverUbicacion(
    normalizarNombre(destino_provincia_nombre),
    normalizarNombre(destino_municipio_nombre)
  );
  resolved_destino_provincia_id = pId;
  resolved_destino_municipio_id = mId;
}
Restricciones:

No modificar ninguna otra parte del archivo.
No cambiar la lógica de resolverUbicacion.
Mantener el estilo de código existente.
La función normalizarNombre debe definirse dentro de la función createSolicitud, justo antes de resolverUbicacion, no como función global del módulo.