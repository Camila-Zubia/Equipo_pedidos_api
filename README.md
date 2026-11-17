# UC3 Práctica 1 — Seguridad en APIs

Materia: Ciberseguridad Aplicada · Ingeniería en Software · ITSON
Unidad de Competencia 3 · Práctica 1

Análisis de seguridad de una API REST de pedidos construida con Java 17 y Spring Boot. El repositorio contiene la versión vulnerable, la versión corregida, las pruebas de seguridad automatizadas, los tres reportes y las evidencias capturadas durante la práctica.

---

## Integrantes

- Manuel Romo López
- Camila Zubia Higuera
- Yuri Germán García López
- Alejandro Valdez Hermosillo

---

## Estructura del repositorio

```
.
├── codigo-original/
│   └── pedidos-api-vulnerable/     API con las fallas intencionales
│       ├── src/
│       └── postman/Coleccion_Explotacion_P1.json
├── codigo-modificado/
│   └── pedidos-api-seguro/         API con las correcciones aplicadas
│       ├── src/                    incluye las pruebas de seguridad
│       └── postman/Coleccion_Validacion_P1.json
├── reportes/
│   ├── reporte_vulnerabilidades.pdf
│   ├── reporte_correcciones.pdf
│   └── reporte_auditoria.pdf
├── evidencias/
│   ├── p1-001/ ... p1-007/         capturas de la explotación
│   └── retest/                     capturas de la verificación
└── README.md
```

---

## Requisitos

- Java 17 o superior
- Maven, incluido en Apache NetBeans
- Postman, versión de escritorio o web

---

## Cómo ejecutar

Los dos proyectos usan el puerto 8080, así que no deben correr al mismo tiempo. Primero se ejecuta el vulnerable, se detiene y después se ejecuta el seguro.

### API vulnerable

```bash
cd codigo-original/pedidos-api-vulnerable
mvn spring-boot:run
```

### API segura

```bash
cd codigo-modificado/pedidos-api-seguro
mvn spring-boot:run
```

Desde NetBeans se abre el proyecto con Open Project, se esperan las dependencias de Maven y se ejecuta con clic derecho y Run.

La API arrancó correctamente cuando en la consola aparecen los mensajes `Started PedidosApiApplication` y `Tomcat started on port 8080`. Para comprobarlo se puede abrir `http://localhost:8080/api/pedidos`, que responde con los tres pedidos iniciales.

La base de datos es H2 en memoria, por lo que los datos se reinician cada vez que se detiene la aplicación. La consola está en `http://localhost:8080/h2-console` con la URL JDBC `jdbc:h2:mem:pedidosdb`, usuario `sa` y contraseña vacía.

### Colecciones de Postman

Se importan desde File, Import y se selecciona el archivo .json. En las variables de la colección se define `baseUrl` con el valor `http://localhost:8080`.

- `Coleccion_Explotacion_P1.json` reproduce los ataques sobre la API vulnerable.
- `Coleccion_Validacion_P1.json` repite los mismos ataques sobre la API segura y verifica que ahora se bloqueen.

### Pruebas de seguridad

```bash
cd codigo-modificado/pedidos-api-seguro
mvn test
```

Desde NetBeans se ejecutan con clic derecho en el proyecto y Test Project. Las pruebas son `PruebaBolaTest`, `PruebaMassAssignmentTest` y `PruebaValidacionTest`.

---

## Resumen de hallazgos

| ID | Título | Severidad | Referencia OWASP |
|---|---|---|---|
| P1-001 | BOPLA de lectura por exposición de contraseñas en `GET /api/usuarios` | Crítica | API3:2023 |
| P1-002 | BOLA de lectura en `GET /api/pedidos` | Crítica | API1:2023 |
| P1-003 | BOLA de lectura en `GET /api/pedidos/{id}` | Alta | API1:2023 |
| P1-004 | BOLA de lectura en `GET /api/usuarios/{id}` | Alta | API1:2023 |
| P1-005 | BOLA de escritura en `POST /api/pedidos` | Crítica | API1:2023 |
| P1-006 | BOPLA de escritura por asignación masiva del rol en `POST /api/usuarios` | Crítica | API3:2023 |
| P1-007 | Validación inadecuada de datos de entrada en `POST /api/usuarios` y `POST /api/pedidos` | Alta | API8:2023 |

Total de hallazgos: 7. Críticos: 4. Altos: 3. Medios: 0. Bajos: 0.

El detalle de cada hallazgo, con su descripción, evidencia, impacto y recomendación, está en `reportes/reporte_vulnerabilidades.docx`. La correspondencia entre cada hallazgo y su corrección está en `reportes/reporte_correcciones.docx`.

---

## Controles aplicados en la versión segura

- Verificación de propiedad del recurso en el servidor, que responde 403 cuando el objeto pertenece a otro usuario y 401 cuando falta el encabezado `X-User-Id`.
- DTOs de entrada que solo aceptan los campos permitidos, por lo que el rol y el dueño del pedido los fija el servidor.
- DTOs de salida y `@JsonIgnore` en el campo `password`, para que la contraseña no aparezca en ninguna respuesta.
- Esquema estricto de Jackson con `fail-on-unknown-properties=true`, que rechaza con 400 cualquier campo desconocido.
- Validación con `@Valid` y anotaciones por campo, con manejo centralizado de errores en `GlobalExceptionHandler`.
