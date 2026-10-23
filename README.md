# Core Banking – Microservicios

Prueba técnica Devsu (Java). Sistema bancario dividido en dos microservicios — **Clientes** y **Cuentas/Movimientos** — que se comunican de forma **asíncrona** mediante RabbitMQ. Arquitectura hexagonal, Spring Boot 4, PostgreSQL, pruebas unitarias, de integración y Karate, y despliegue completo con Docker Compose.

## Contenido

- [Arquitectura](#arquitectura)
- [Stack](#stack)
- [Despliegue con Docker](#despliegue-con-docker)
- [API](#api)
- [Pruebas](#pruebas)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Rendimiento, escalabilidad y resiliencia](#rendimiento-escalabilidad-y-resiliencia)
- [Seguridad](#seguridad)
- [Desarrollo local](#desarrollo-local)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Mejoras futuras](#mejoras-futuras)

## Arquitectura

```
                    ┌──────────────────────┐   RabbitMQ (exchange topic      ┌──────────────────────────┐
 /api/clientes ───► │  customers-service   │   "customers.events")           │     accounts-service     │ ◄─── /api/cuentas
                    │       :8081          │ ── customer.created ──────────► │          :8082           │      /api/movimientos
                    │                      │    customer.updated             │ copia local de clientes  │      /api/reportes
                    │                      │    customer.deactivated         │ (customer_ref)           │
                    └──────────┬───────────┘                                 └────────────┬─────────────┘
                               │                                                          │
                     PostgreSQL: customers_db                                   PostgreSQL: accounts_db
```

| Servicio | Responsabilidad | Base de datos |
|---|---|---|
| `customers-service` | Persona y Cliente (CRUD). Publica un evento por cada alta, modificación o baja. | `customers_db` |
| `accounts-service` | Cuentas, movimientos y reporte de estado de cuenta. Consume los eventos de clientes. | `accounts_db` |

- **Una base de datos por servicio**: ningún servicio lee la base del otro.
- **Comunicación asíncrona** (*event-carried state transfer*): cada evento lleva el estado del cliente que necesita `accounts-service` (id, nombre, estado). `accounts-service` mantiene una copia local (`customer_ref`), por lo que abrir cuentas, registrar movimientos o generar reportes **no requiere llamar a `customers-service`**: si este cae, el otro sigue operando.
- **Arquitectura hexagonal** en cada servicio:

```
domain/          Entidades y reglas de negocio puras, sin Spring ni JPA (Customer, Account.registerMovement, ...)
application/     Casos de uso y puertos (port/in = casos de uso, port/out = repositorios, eventos, cifrado)
infrastructure/  Adaptadores: REST (controllers, DTOs, manejo de errores), JPA, RabbitMQ, BCrypt, configuración
```

## Stack

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 (Spring Web MVC, Data JPA / Hibernate 7, Validation, AMQP, Actuator) |
| Base de datos | PostgreSQL 17 + Flyway |
| Mensajería | RabbitMQ 4 |
| Documentación de la API | springdoc-openapi (Swagger UI) |
| Pruebas | JUnit 5, Mockito, AssertJ, Testcontainers, Karate 1.5.2 |
| Contenedores | Docker, Docker Compose |

## Despliegue con Docker

**Requisito:** Docker (Docker Desktop en Windows/macOS). No hace falta tener Java ni Maven instalados: las imágenes se compilan dentro de Docker.

```bash
cp .env.example .env
docker compose up -d --build
```

La primera vez tarda unos minutos (descarga imágenes base y dependencias). Compose arranca PostgreSQL y RabbitMQ, espera a que estén *healthy* y luego levanta los dos servicios.

```bash
docker compose ps
```

Los cuatro contenedores deben quedar `healthy`.

| Recurso | URL |
|---|---|
| customers-service | http://localhost:8081 |
| accounts-service | http://localhost:8082 |
| Swagger customers | http://localhost:8081/swagger-ui.html |
| Swagger accounts | http://localhost:8082/swagger-ui.html |
| Health | http://localhost:8081/actuator/health · http://localhost:8082/actuator/health |
| Consola RabbitMQ | http://localhost:15672 (usuario y contraseña de `.env`) |
| PostgreSQL | `localhost:5434` (usuario y contraseña de `.env`) |

Los puertos publicados en el host se configuran en `.env` (`CUSTOMERS_PORT`, `ACCOUNTS_PORT`, `DB_PORT`, ...). Dentro de la red de Docker los servicios usan siempre `postgres:5432` y `rabbitmq:5672`.

Detener todo (`-v` borra también los datos):

```bash
docker compose down
```

### Base de datos

El esquema lo crea **Flyway** al arrancar cada servicio (`*/src/main/resources/db/migration`). El script consolidado con el esquema de ambas bases y los datos de los casos de uso está en [`BaseDatos.sql`](BaseDatos.sql): los 3 clientes del enunciado (contraseñas cifradas con BCrypt), las cuentas con sus números exactos (478758, 225487, 495878, 496825, 585545) y sus movimientos.

Para cargar los datos de ejemplo en el entorno de Docker:

```bash
docker exec -i core-banking-postgres psql -U devsu -d postgres < BaseDatos.sql
```

El script es idempotente: en un servidor vacío crea las bases, el esquema y los datos; sobre bases ya creadas por los servicios solo añade los datos que falten. Los servicios también arrancan sobre bases creadas por el script (Flyway toma ese esquema como *baseline*).

Con los datos cargados, el reporte del caso de uso 5 es:

```
GET http://localhost:8082/api/reportes?fecha=2022-02-01,2022-02-28&cliente=b1a5e5c0-1d2e-4f3a-8b4c-5d6e7f8a9b02
```

| Cliente | customerId |
|---|---|
| Jose Lema | `b1a5e5c0-1d2e-4f3a-8b4c-5d6e7f8a9b01` |
| Marianela Montalvo | `b1a5e5c0-1d2e-4f3a-8b4c-5d6e7f8a9b02` |
| Juan Osorio | `b1a5e5c0-1d2e-4f3a-8b4c-5d6e7f8a9b03` |

## API

Las rutas están en español porque así las exige el enunciado; el resto del contrato (JSON, código) está en inglés. Todos los errores siguen el formato **RFC 9457 (ProblemDetail)**.

### Clientes – `customers-service` (:8081)

| Verbo | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/clientes` | Crear cliente → `201` + `Location` |
| `GET` | `/api/clientes?page=0&size=20` | Listado paginado (`size` máx. 100) |
| `GET` | `/api/clientes/{customerId}` | Consultar cliente |
| `PATCH` | `/api/clientes/{customerId}` | Actualización parcial: `name`, `gender`, `address`, `phone`, `password`, `active` |
| `DELETE` | `/api/clientes/{customerId}` | Borrado lógico (`active = false`) → `204` |

```json
POST /api/clientes
{
  "name": "Jose Lema",
  "gender": "MALE",
  "birthDate": "1985-04-12",
  "identification": "1712345678",
  "address": "Otavalo sn y principal",
  "phone": "098254785",
  "password": "1234"
}
```

La respuesta incluye `customerId` (UUID generado por el servidor), `age` (calculada) y `active`; **nunca** la contraseña.

### Cuentas – `accounts-service` (:8082)

| Verbo | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/cuentas` | Abrir cuenta → `201`. El número de cuenta lo asigna el banco |
| `GET` | `/api/cuentas?page=0&size=20&customerId=` | Listado paginado, filtro opcional por cliente |
| `GET` | `/api/cuentas/{accountNumber}` | Consultar cuenta con su saldo disponible |
| `PATCH` | `/api/cuentas/{accountNumber}` | Activar / desactivar (`{ "active": false }`) |

```json
POST /api/cuentas
{ "accountType": "SAVINGS", "initialBalance": 2000, "customerId": "<customerId>" }
```

`accountType`: `SAVINGS` (ahorros) o `CHECKING` (corriente).

### Movimientos – `accounts-service` (:8082)

| Verbo | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/movimientos` | Registrar depósito o retiro → `201` |
| `GET` | `/api/movimientos?page=0&size=20&accountNumber=` | Listado paginado (más reciente primero), filtro opcional por cuenta |
| `GET` | `/api/movimientos/{movementId}` | Consultar movimiento |

```json
POST /api/movimientos
Idempotency-Key: 6f1c2a9e-0c43-4b0e-9a6e-3b2f0d8e1c55   (opcional)
{ "accountNumber": "100000", "amount": -575 }
```

- El **signo** define el tipo: positivo = `DEPOSIT`, negativo = `WITHDRAWAL`.
- La respuesta incluye `balance`: el saldo disponible tras el movimiento.
- Sin saldo suficiente responde **`422`** con `"detail": "Saldo no disponible"` (F3).
- Con `Idempotency-Key`, repetir la petición devuelve el movimiento original sin aplicarlo otra vez (cabecera `Idempotent-Replayed: true`).

### Reporte de estado de cuenta – `accounts-service` (:8082)

```
GET /api/reportes?fecha=2026-10-01,2026-10-31&cliente=<customerId>
```

- `fecha`: rango `desde,hasta` en formato `yyyy-MM-dd`, ambos días incluidos (máximo 366 días).
- `cliente`: `customerId` del cliente.

```json
{
  "customerId": "…",
  "customerName": "Marianela Montalvo",
  "from": "2026-10-01",
  "to": "2026-10-31",
  "accounts": [
    {
      "accountNumber": "100001",
      "accountType": "CHECKING",
      "initialBalance": 100.00,
      "availableBalance": 700.00,
      "active": true,
      "totalCredits": 600.00,
      "totalDebits": 0.00,
      "movements": [
        { "date": "2026-10-10", "occurredAt": "2026-10-10T15:00:00Z", "movementType": "DEPOSIT", "amount": 600.00, "balance": 700.00 }
      ]
    }
  ]
}
```

### Códigos de respuesta

| Código | Cuándo |
|---|---|
| `400` | Campos inválidos (con `errors` por campo), JSON mal formado o con campos desconocidos, parámetros inválidos |
| `404` | Recurso inexistente en la URL (cliente, cuenta, movimiento) |
| `405` | Verbo no soportado (p. ej. `PUT`); la cabecera `Allow` indica los permitidos |
| `409` | Identificación duplicada o modificación concurrente |
| `422` | Regla de negocio: **"Saldo no disponible"**, cliente o cuenta inexistente o inactivo, `Idempotency-Key` reutilizada |
| `500` | Error inesperado (mensaje genérico; el detalle solo queda en el log) |

### Colección Postman

[`postman/core-banking.postman_collection.json`](postman/core-banking.postman_collection.json): **Import** en Postman → clic derecho en la colección → **Run collection**.

- **Carpeta 1** reproduce los casos de uso del enunciado (Jose Lema, Marianela Montalvo, Juan Osorio, sus cuentas y movimientos, "Saldo no disponible" y el reporte) y **verifica cada saldo** con tests.
- Las carpetas 2 a 5 recorren todos los endpoints, incluidos los casos de error.
- Los ids generados se guardan solos en variables; las URLs base son variables (`customersBaseUrl`, `accountsBaseUrl`).
- Se puede ejecutar varias veces: cada ejecución genera identificaciones distintas.

## Pruebas

```bash
cd customers-service && ./mvnw test
cd accounts-service && ./mvnw test
```

**Requisito:** Docker en marcha (las pruebas de integración y Karate usan Testcontainers).

| Tipo | Qué cubre | Dónde |
|---|---|---|
| Unitarias – dominio (**F5**) | Reglas de `Customer` (mayoría de edad, datos inmutables, edad calculada) y de `Account` (saldo, "Saldo no disponible", estado) | `*/src/test/java/**/domain` |
| Unitarias – casos de uso | Servicios con Mockito | `*/src/test/java/**/application` |
| Capa web | Controladores con `@WebMvcTest`: validaciones, códigos HTTP, formato de errores | `*/src/test/java/**/infrastructure/adapter/in/rest` |
| Integración (**F6**) | `AccountsIntegrationTest`: aplicación completa con PostgreSQL y RabbitMQ reales. Evento de cliente → cuenta → movimientos → "Saldo no disponible" → reporte, y 20 retiros concurrentes que nunca dejan saldo negativo | `accounts-service/src/test/java/com/devsu/accounts` |
| Karate | Escenarios de API sobre la aplicación arrancada en un puerto aleatorio; se ejecutan junto al resto con `./mvnw test` | `*/src/test/resources/karate` |

## Decisiones de diseño

**Clientes**
- **Herencia JPA `JOINED`**: tabla `person` (con su PK) y tabla `customer`, cuya PK es a la vez FK a `person`. Una persona solo puede ser un cliente.
- **`customerId` UUID generado por el servidor**: identificador público. El id técnico numérico nunca se expone, así no se pueden recorrer clientes probando números.
- **`identification` es clave única** (no PK): evita registrar dos veces a la misma persona.
- **Fecha de nacimiento en lugar de edad**: la edad se calcula, así nunca queda desactualizada. Se exige ser **mayor de 18 años**.
- **Identificación y fecha de nacimiento son inmutables**: son datos de identidad verificados al registrar al cliente; su corrección sería un proceso administrativo auditado.
- **Sin `PUT`**: al ser inmutables los datos de identidad, un reemplazo completo obligaría a reenviarlos junto con la contraseña. Las actualizaciones son parciales (`PATCH`), que el enunciado deja a criterio.
- **Borrado lógico**: `DELETE` desactiva al cliente y conserva su historial.
- **Contraseña**: mínimo 4 caracteres para admitir los datos del enunciado (`1234`); máximo **72 bytes**, el límite de BCrypt. En producción se aplicaría la política del NIST SP 800-63B (8 o más caracteres, lista de contraseñas filtradas).

**Cuentas y movimientos**
- **El número de cuenta lo asigna el banco** mediante una secuencia de PostgreSQL: es único incluso con peticiones concurrentes. Las cuentas del enunciado (478758, 225487, …) se cargan con sus números exactos en `BaseDatos.sql`.
- **Solo el estado de una cuenta es modificable** (`PATCH`). El saldo cambia únicamente con movimientos; el tipo, el titular y el saldo inicial no cambian. No hay `DELETE` (el enunciado pide CRU): una cuenta desactivada conserva saldo e historial.
- **Saldo inicial vs. saldo disponible**: `initialBalance` no cambia; `availableBalance` se actualiza con cada movimiento; cada movimiento guarda el `balance` resultante.
- **Movimientos inmutables**: el historial contable no se edita (cambiar un movimiento pasado descuadraría todos los posteriores). Una corrección se registra como un movimiento de reverso; por eso no se expone actualización de movimientos aunque el enunciado indique CRU.
- **Montos** con `BigDecimal` / `NUMERIC(15,2)`; se rechazan los montos con más de 2 decimales en lugar de redondearlos.
- **"Saldo no disponible" → `422`**: la petición es válida pero viola una regla de negocio. La regla vive en el dominio (`Account.registerMovement`).

**Reporte**
- **`fecha=desde,hasta` en ISO-8601**: estándar y sin la ambigüedad de `10/2/2022` (¿10 de febrero o 2 de octubre?).
- **Respuesta agrupada por cuenta** en lugar de la lista plana del ejemplo: no repite los datos de la cuenta en cada fila y añade totales; cada movimiento conserva los datos del ejemplo (fecha, tipo, monto, saldo disponible).
- **Zona horaria del banco** (`America/Guayaquil`, configurable): "10 de octubre" significa ese día completo en hora local.

## Rendimiento, escalabilidad y resiliencia

**Rendimiento**
- Listados paginados (`size` máximo 100) con orden estable.
- Índices en `account(customer_id)` y `movement(account_id, occurred_at)`.
- El reporte se resuelve con **3 consultas** sin importar cuántas cuentas o movimientos haya (`JOIN FETCH`, sin N+1).
- Transacciones de solo lectura en las consultas; sin llamadas de red entre servicios en ninguna operación.

**Escalabilidad**
- Servicios sin estado: se pueden ejecutar varias instancias detrás de un balanceador.
- Varias instancias de `accounts-service` consumen la misma cola en paralelo.
- Cada servicio escala su base de datos por separado.

**Resiliencia**
- Comunicación asíncrona: si `customers-service` cae, las cuentas y movimientos siguen funcionando; si `accounts-service` cae, los eventos esperan en la cola y se aplican al volver.
- Colas y mensajes durables; *publisher confirms*.
- Reintentos con espera creciente (3 intentos); los mensajes que siguen fallando van a una **cola de mensajes muertos** (`accounts.customer-events.dlq`). Los mensajes que nunca podrán procesarse (mal formados) van directo, sin reintentos.
- **Consumidor idempotente y tolerante al desorden**: un *upsert* atómico ignora eventos más antiguos que el último aplicado.
- Los eventos se publican **después del commit**: un cambio revertido nunca se anuncia.
- *Health checks* de Docker sobre `/actuator/health/readiness`, arranque ordenado y reinicio automático.

**Concurrencia del saldo**
- Al registrar un movimiento, la cuenta se bloquea (`SELECT … FOR UPDATE`) hasta el fin de la transacción: los movimientos de una misma cuenta se aplican en fila. Probado con 20 retiros simultáneos de 100 sobre 1000: se aprueban exactamente 10.
- Respaldos adicionales: bloqueo optimista (`@Version`) y `CHECK (available_balance >= 0)` en la base.
- **Idempotencia** con la cabecera `Idempotency-Key`: un reintento (timeout, doble clic) no mueve el dinero dos veces, incluso si llegan varios a la vez.

## Seguridad

- Contraseñas con **BCrypt**; nunca se devuelven ni se registran en logs ni en eventos.
- Validación de toda entrada (Bean Validation) y **rechazo de campos desconocidos** (protección contra *mass assignment*).
- Errores sin trazas ni detalles internos.
- Consultas parametrizadas con JPA (sin SQL concatenado).
- Identificadores públicos UUID: no se pueden recorrer recursos probando números.
- Credenciales solo por variables de entorno (`.env`, excluido de git; `.env.example` como plantilla).
- Contenedores con imagen JRE mínima y **usuario sin privilegios**.

## Desarrollo local

Para ejecutar los servicios desde el IDE, levanta solo la infraestructura:

```bash
docker compose up -d postgres rabbitmq
```

Cada servicio lee el `.env` de la raíz automáticamente; ejecuta `CustomersServiceApplication` (8081) y `AccountsServiceApplication` (8082). Si los servicios están corriendo en Docker, detenlos antes para liberar los puertos:

```bash
docker compose stop customers-service accounts-service
```

> **Windows:** si el directorio temporal del usuario contiene espacios, Tomcat no arranca. Añade la opción de VM `-Djdk.net.unixdomain.tmpdir=C:\tmp` (cualquier carpeta existente sin espacios). Las pruebas ya la configuran en el `pom.xml`.

## Estructura del repositorio

```
├── customers-service/        Microservicio de clientes (Dockerfile, pom.xml, código y pruebas)
├── accounts-service/         Microservicio de cuentas, movimientos y reportes
├── docker/postgres/          Script que crea customers_db y accounts_db
├── postman/                  Colección Postman
├── BaseDatos.sql             Esquema y datos de ambas bases
├── docker-compose.yml        Despliegue completo
└── .env.example              Plantilla de configuración
```

## Mejoras futuras

- **Transactional Outbox**: guardar el evento en la misma transacción que el cambio, para no perderlo si RabbitMQ cae justo después del commit.
- **Autenticación** OAuth2/JWT: el cliente autenticado solo podría operar sus propias cuentas.
- **Transferencias** entre cuentas (dos movimientos en una transacción, bloqueando las cuentas siempre en el mismo orden para evitar *deadlocks*).
- **Movimientos de reverso** como endpoint para correcciones contables.
- **Endpoint de republicación de eventos** para sincronizar un consumidor nuevo con los clientes existentes.
- **Paginación por cursor** para listados muy grandes y **UUID v7** (ordenado por tiempo) para índices más eficientes.
- **Pipeline de CI** (GitHub Actions) que compile, ejecute todas las pruebas y construya las imágenes en cada push.
