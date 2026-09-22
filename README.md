# Bank Inc - Backend Technical Test

API REST desarrollada como solución a la prueba técnica de Backend para la gestión de tarjetas y transacciones de Bank Inc.

## Tecnologías

- Java 17
- Spring Boot 3.5
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL
- H2 para pruebas
- JUnit 5
- Mockito
- JaCoCo
- OpenAPI / Swagger

## Arquitectura

La aplicación utiliza una arquitectura por capas:

`Controller -> Service -> Repository -> PostgreSQL`

- **Controller:** exposición de endpoints HTTP, validación de solicitudes y DTOs.
- **Service:** reglas de negocio y manejo transaccional.
- **Repository:** acceso y persistencia de datos mediante Spring Data JPA.
- **Entity:** representación del modelo relacional.
- **GlobalExceptionHandler:** manejo centralizado y uniforme de errores.

## Reglas de negocio implementadas

1. El número de tarjeta contiene 16 dígitos: 6 del producto y 10 generados aleatoriamente.
2. La fecha de vencimiento corresponde a la fecha de creación más 3 años.
3. Una tarjeta se crea inicialmente con estado `INACTIVE`.
4. Una tarjeta puede ser activada mediante enrollment.
5. Una tarjeta puede ser bloqueada.
6. Las recargas deben tener un valor positivo.
7. Una compra requiere una tarjeta activa, vigente y con saldo suficiente.
8. El saldo de una tarjeta no puede quedar negativo.
9. Cada compra genera un identificador único de transacción.
10. Una transacción únicamente puede anularse durante las primeras 24 horas.
11. Al anular una transacción, su valor es reintegrado al saldo de la tarjeta.
12. Una transacción no puede ser anulada más de una vez.
13. Se validan los datos de entrada de la API.
14. Los errores de negocio son manejados de forma centralizada.
15. Se utiliza bloqueo pesimista al modificar el saldo para controlar operaciones concurrentes.
16. JaCoCo verifica una cobertura mínima de pruebas del 80%.

## Ejecución local

### Requisitos

- JDK 17 o superior
- Maven 3.9 o superior
- PostgreSQL

### Base de datos

Crear una base de datos PostgreSQL:

```sql
CREATE DATABASE bankinc;
```

La aplicación utiliza las siguientes variables de entorno:

```text
DB_URL=jdbc:postgresql://localhost:5432/bankinc
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Si no se definen, se utilizan esos valores como configuración local predeterminada.

### Compilar y ejecutar pruebas

```bash
mvn clean verify
```

El build verifica automáticamente la cobertura configurada con JaCoCo.

El reporte HTML de cobertura se genera en:

```text
target/site/jacoco/index.html
```

### Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

## Endpoints

### Generar tarjeta

```http
GET /card/{productId}/number
```

Ejemplo:

```http
GET /card/102030/number
```

### Activar tarjeta

```http
POST /card/enroll
```

```json
{
  "cardId": "1020301234567890"
}
```

### Bloquear tarjeta

```http
DELETE /card/{cardId}
```

### Recargar saldo

```http
POST /card/balance
```

```json
{
  "cardId": "1020301234567890",
  "balance": 100000
}
```

### Consultar saldo

```http
GET /card/balance/{cardId}
```

### Realizar compra

```http
POST /transaction/purchase
```

```json
{
  "cardId": "1020301234567890",
  "price": 25000
}
```

### Consultar transacción

```http
GET /transaction/{transactionId}
```

### Anular transacción

```http
POST /transaction/anulation
```

```json
{
  "cardId": "1020301234567890",
  "transactionId": "96df47e8-5681-49b0-8700-857e4c92eb2f"
}
```

## Decisiones de diseño

### BigDecimal para valores monetarios

Los saldos y valores de las transacciones utilizan `BigDecimal` para evitar los problemas de precisión asociados con `float` y `double`.

### Manejo transaccional

Las operaciones que modifican información relacionada, como descontar saldo y registrar una compra, se ejecutan dentro de una transacción para mantener la consistencia de los datos.

### Bloqueo pesimista

Las operaciones que modifican el saldo obtienen la tarjeta con bloqueo pesimista. Esto evita que dos operaciones concurrentes utilicen simultáneamente el mismo saldo disponible.

### UUID para las transacciones

Cada compra utiliza un UUID como `transactionId`, proporcionando identificadores únicos sin depender de una secuencia pública.

## Pruebas

El proyecto incluye pruebas unitarias para servicios, controladores y manejo global de excepciones.

Para ejecutarlas junto con la validación de cobertura:

```bash
mvn clean verify
```

## Postman

La colección:

```text
postman/Bank-Inc.postman_collection.json
```

contiene solicitudes para probar el flujo de la API.

## Despliegue

La API se encuentra desplegada en Railway y utiliza PostgreSQL como base de datos.

### Swagger UI

La API desplegada puede consultarse y probarse desde:

https://bank-inc-backend-production.up.railway.app/swagger-ui/index.html

### OpenAPI

La especificación OpenAPI está disponible en:

https://bank-inc-backend-production.up.railway.app/v3/api-docs

La conexión a PostgreSQL se configura mediante las variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, evitando almacenar las credenciales de producción en el repositorio.