                                                                                Table "public.choferes"
        Column         |           Type           | Collation | Nullable |          Default          | Storage  | Compression | Stats target |               Description
-----------------------+--------------------------+-----------+----------+---------------------------+----------+-------------+--------------+-----------------------------------------
 id                    | uuid                     |           | not null | gen_random_uuid()         | plain    |             |              |
 usuario_id            | uuid                     |           | not null |                           | plain    |             |              |
 licencia_numero       | character varying(50)    |           |          |                           | extended |             |              |
 licencia_foto_url     | character varying(255)   |           |          |                           | extended |             |              |
 estado                | estado_chofer            |           | not null | 'inactivo'::estado_chofer | plain    |             |              |
 calificacion_promedio | numeric(3,2)             |           | not null | 0.00                      | main     |             |              |
 total_viajes          | integer                  |           | not null | 0                         | plain    |             |              |
 provincia_base_id     | smallint                 |           |          |                           | plain    |             |              |
 municipio_base_id     | smallint                 |           |          |                           | plain    |             |              |
 opera_interprovincial | boolean                  |           | not null | false                     | plain    |             |              | TRUE si acepta viajes entre provincias.
 aprobado_por          | uuid                     |           |          |                           | plain    |             |              |
 aprobado_en           | timestamp with time zone |           |          |                           | plain    |             |              |
 visible_en_mapa       | boolean                  |           |          | false                     | plain    |             |              |
 ultima_lat            | double precision         |           |          |                           | plain    |             |              |
 ultima_lng            | double precision         |           |          |                           | plain    |             |              |
 ultima_ubicacion_en   | timestamp with time zone |           |          |                           | plain    |             |              |
 vehiculo_activo_id    | uuid                     |           |          |                           | plain    |             |              |
Indexes:
    "choferes_pkey" PRIMARY KEY, btree (id)
    "choferes_usuario_id_key" UNIQUE CONSTRAINT, btree (usuario_id)
    "idx_choferes_estado" btree (estado)
    "idx_choferes_municipio" btree (municipio_base_id)
    "idx_choferes_provincia" btree (provincia_base_id)
Foreign-key constraints:
    "choferes_aprobado_por_fkey" FOREIGN KEY (aprobado_por) REFERENCES usuarios(id) ON DELETE SET NULL
    "choferes_municipio_base_id_fkey" FOREIGN KEY (municipio_base_id) REFERENCES municipios(id) ON DELETE SET NULL
    "choferes_provincia_base_id_fkey" FOREIGN KEY (provincia_base_id) REFERENCES provincias(id) ON DELETE SET NULL
    "choferes_usuario_id_fkey" FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
    "choferes_vehiculo_activo_id_fkey" FOREIGN KEY (vehiculo_activo_id) REFERENCES vehiculos(id) ON DELETE SET NULL
Referenced by:
    TABLE "licencias_chofer" CONSTRAINT "licencias_chofer_chofer_id_fkey" FOREIGN KEY (chofer_id) REFERENCES choferes(id) ON DELETE CASCADE
    TABLE "respuestas_solicitud" CONSTRAINT "respuestas_solicitud_chofer_id_fkey" FOREIGN KEY (chofer_id) REFERENCES choferes(id) ON DELETE CASCADE
    TABLE "solicitudes" CONSTRAINT "solicitudes_chofer_seleccionado_id_fkey" FOREIGN KEY (chofer_seleccionado_id) REFERENCES choferes(id) ON DELETE SET NULL
    TABLE "valoraciones" CONSTRAINT "valoraciones_chofer_id_fkey" FOREIGN KEY (chofer_id) REFERENCES choferes(id) ON DELETE CASCADE
    TABLE "vehiculos" CONSTRAINT "vehiculos_chofer_id_fkey" FOREIGN KEY (chofer_id) REFERENCES choferes(id) ON DELETE CASCADE
Access method: heap




                                                                Table "public.licencias_chofer"
       Column       |           Type           | Collation | Nullable |              Default              | Storage  | Compression | Stats target | Description
--------------------+--------------------------+-----------+----------+-----------------------------------+----------+-------------+--------------+-------------
 chofer_id          | uuid                     |           | not null |                                   | plain    |             |              |
 estado             | character varying(20)    |           | not null | 'TRIAL_ACTIVO'::character varying | extended |             |              |
 trial_inicio       | timestamp with time zone |           | not null | now()                             | plain    |             |              |
 trial_fin          | timestamp with time zone |           | not null | now() + '45 days'::interval       | plain    |             |              |
 suscripcion_inicio | timestamp with time zone |           |          |                                   | plain    |             |              |
 suscripcion_fin    | timestamp with time zone |           |          |                                   | plain    |             |              |
 ultimo_pago        | timestamp with time zone |           |          |                                   | plain    |             |              |
 monto_mensual      | numeric(10,2)            |           |          | 0.00                              | main     |             |              |
 notas              | text                     |           |          |                                   | extended |             |              |
 creada_en          | timestamp with time zone |           |          | now()                             | plain    |             |              |
 actualizada_en     | timestamp with time zone |           |          | now()                             | plain    |             |              |
Indexes:
    "licencias_chofer_pkey" PRIMARY KEY, btree (chofer_id)
Foreign-key constraints:
    "licencias_chofer_chofer_id_fkey" FOREIGN KEY (chofer_id) REFERENCES choferes(id) ON DELETE CASCADE
Access method: heap



