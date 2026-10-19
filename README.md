# worldbank-backend

## Portada

**Integrante:**
Francisco Javier Silva Cadavid

**Asignatura:**
Arquitectura de Software

**Universidad de Antioquia**

**2025-1**


## Información

Backend en **arquitectura por capas** (Spring Boot + Gradle) que consume la
**World Bank Indicators API** y la **Frankfurter API** (ambas públicas, sin
autenticación, solo lectura) y expone una API REST propia con cuatro casos de
uso.

En lugar de hacer un simple *proxy* de las APIs externas, la aplicación agrega
valor de negocio sobre los datos crudos: limpia series temporales, **calcula**
un indicador que el Banco Mundial no expone directamente (PIB per cápita),
construye un ranking de países excluyendo agregados regionales (Mundo,
regiones, grupos de ingreso), y **enriquece** el PIB per cápita convirtiéndolo
a otra moneda con la tasa de cambio de referencia más reciente (Banco Central
Europeo, vía Frankfurter).

## Requisitos

- **JDK 21**
- **Gradle** (o un IDE como IntelliJ IDEA que lo gestione)

> Este proyecto incluye `gradle/wrapper/gradle-wrapper.properties` pero **no** el
> binario del wrapper. Para generar `gradlew` ejecuta una vez, con Gradle instalado:
>
> ```bash
> gradle wrapper
> ```
>
> A partir de ahí ya puedes usar `./gradlew`. Alternativamente, abre la carpeta en
> IntelliJ IDEA y ejecútalo desde el IDE.

## Ejecutar

```bash
gradle bootRun        # o ./gradlew bootRun tras generar el wrapper
```

La aplicación arranca en `http://localhost:8080`.

## Correr pruebas y cobertura

```bash
gradle test jacocoTestReport                # ejecuta las pruebas y genera el reporte
gradle test jacocoTestCoverageVerification  # falla el build si la cobertura baja del 100%
```

El proyecto mantiene **100% de cobertura** (instrucciones, líneas y ramas) verificada con Jacoco.

### Organización de las pruebas

Dentro de cada paquete de `src/test` (uno por cada carpeta de `src/main`:
`service`, `controller`, `client`, `client/dto`, `client/mapper`, `dto`,
`exception`, `model`, `config`) las pruebas se separan en dos subpaquetes:

```
<paquete>/
├── unit/          -> pruebas unitarias (comportamiento, con dobles/mocks)
└── performance/   -> pruebas de tiempo/carga (solo donde aplica)
```

Por ejemplo, `service/unit/RankingServiceTest.java` y
`service/performance/RankingServicePerformanceTest.java`. El subpaquete
`performance/` solo existe en `client`, `controller` y `service`, que son los
puntos donde hay procesamiento de datos o concurrencia real que vale la pena
medir.

### Pruebas de performance

Además de las pruebas unitarias, hay pruebas de **performance** (JUnit 5 +
Mockito, sin herramientas externas como JMeter/Gatling) que corren dentro de
la misma suite y validan presupuestos de tiempo con datasets/carga muy por
encima de lo real:

```bash
gradle test --tests "*.performance.*"    # solo las pruebas de performance
gradle test --tests "*.unit.*"           # solo las pruebas unitarias
```

| Clase | Qué valida |
|---|---|
| `RankingServicePerformanceTest` | `RankingService.ranking()` con 20 000 observaciones (~65× el universo real de países) resuelve en <300 ms; 1 000 llamadas secuenciales con un dataset moderado en <2 s |
| `GetSeriesServicePerformanceTest` | `GetSeriesService.getSeries()` con una serie de 50 000 puntos (filtrado + reorden cronológico) resuelve en <300 ms |
| `CountriesWorldBankClientPerformanceTest` | con el caché ya resuelto, 32 hilos × 2 000 lecturas concurrentes (64 000 llamadas) terminan en <3 s y **no** vuelven a golpear el `WorldBankApiExecutor` |
| `CalculateGdpPerCapitaServicePerformanceTest` | 5 000 llamadas secuenciales sin conversión en <1 s, con conversión de moneda en <1.5 s; 32 hilos × 500 cálculos concurrentes (mitad con conversión, mitad sin) en <3 s |
| `ExchangeRateServicePerformanceTest` | 5 000 llamadas secuenciales a `convert()` en <1 s; 32 hilos × 1 000 conversiones concurrentes en <3 s; 32 hilos × 1 000 conversiones de la misma moneda en <2 s y **sin** golpear la API externa |
| `IndicatorControllerPerformanceTest` | 50 hilos × 200 peticiones (10 000 en total) repartidas entre los cuatro endpoints, con los casos de uso mockeados, terminan en <5 s sin errores |

Los presupuestos de tiempo son deliberadamente holgados (varias veces el
tiempo real esperado) para que la suite no sea intermitente en máquinas más
lentas, mientras sigue detectando una regresión real de performance (por
ejemplo, un algoritmo O(n²) donde antes había O(n log n), o una pérdida del
caché).

## Endpoints (base: `/api/indicators`)

### 1. `GET /series` — serie temporal de un indicador para un país

Devuelve la serie limpia de un indicador: descarta los años sin dato reportado
y la ordena cronológicamente (la API del Banco Mundial la entrega del año más
nuevo al más viejo).

Parámetros: `country` (ISO3, obligatorio), `indicator` (código del Banco
Mundial, obligatorio), `from` / `to` (años, opcionales — si se omiten trae
toda la serie disponible).

```
GET /api/indicators/series?country=BRA&indicator=SP.POP.TOTL&from=2010&to=2020
GET /api/indicators/series?country=COL&indicator=NY.GDP.MKTP.CD
GET /api/indicators/series?country=USA&indicator=FP.CPI.TOTL.ZG&from=2015&to=2023
GET /api/indicators/series?country=ARG&indicator=SL.UEM.TOTL.ZS&from=2018&to=2022
```

### 2. `GET /gdp-per-capita` — PIB per cápita (indicador derivado)

Combina **dos** indicadores (PIB y población, dos llamadas a la API) y calcula
un valor que el Banco Mundial no expone directamente: `PIB / población`,
redondeado a 2 decimales.

Parámetros: `country` (ISO3, obligatorio), `year` (obligatorio, ≥ 1960),
`currency` (opcional, código ISO 4217 — si se indica, además convierte el PIB
per cápita a esa moneda con la tasa de cambio más reciente, vía la
[Frankfurter API](https://api.frankfurter.dev/)).

```
GET /api/indicators/gdp-per-capita?country=BRA&year=2022
GET /api/indicators/gdp-per-capita?country=COL&year=2020
GET /api/indicators/gdp-per-capita?country=DEU&year=2021
GET /api/indicators/gdp-per-capita?country=JPN&year=2019
GET /api/indicators/gdp-per-capita?country=COL&year=2022&currency=EUR
GET /api/indicators/gdp-per-capita?country=COL&year=2022&currency=MXN
```

### 3. `GET /ranking` — ranking de países por indicador

Pide el indicador para todos los países, descarta valores nulos y **agregados
regionales** (Mundo, regiones, grupos de ingreso), ordena de mayor a menor y
recorta al límite pedido.

Parámetros: `indicator` (obligatorio), `year` (obligatorio, ≥ 1960), `limit`
(opcional, por defecto 10, entre 1 y 100).

```
GET /api/indicators/ranking?indicator=NY.GDP.MKTP.CD&year=2022&limit=10
GET /api/indicators/ranking?indicator=SP.POP.TOTL&year=2023&limit=5
GET /api/indicators/ranking?indicator=NY.GDP.PCAP.CD&year=2021
GET /api/indicators/ranking?indicator=EN.ATM.CO2E.PC&year=2020&limit=20
```

### 4. `GET /exchange-rate` — conversión de moneda

Convierte un monto entre dos monedas con la tasa de cambio de referencia más
reciente del Banco Central Europeo, obtenida de la
[Frankfurter API](https://api.frankfurter.dev/) (pública, sin autenticación).

Parámetros: `from` (moneda de origen, ISO 4217, obligatorio), `to` (moneda de
destino, ISO 4217, obligatorio), `amount` (monto a convertir, opcional, por
defecto 1, debe ser positivo).

```
GET /api/indicators/exchange-rate?from=USD&to=EUR
GET /api/indicators/exchange-rate?from=USD&to=MXN&amount=1000
GET /api/indicators/exchange-rate?from=EUR&to=JPY&amount=50
```

Frankfurter solo cubre las ~30 monedas que reporta el Banco Central Europeo
(entre ellas EUR, GBP, JPY, BRL, MXN — **no** incluye COP, ARS ni otras
monedas latinoamericanas fuera de BRL/MXN); pedir una moneda no soportada
devuelve 404.

### Errores

Todos los endpoints devuelven errores como `ProblemDetail` (RFC 7807):

| Situación | Excepción | HTTP |
|---|---|---|
| No hay datos para lo pedido, o la moneda pedida no está soportada | `DataNotFoundException` | 404 |
| Falla la comunicación con la API del Banco Mundial | `WorldBankApiException` | 502 |
| Falla la comunicación con la API de tasas de cambio | `ExchangeRateApiException` | 502 |
| Parámetros inválidos (violan las validaciones del controlador) | `ConstraintViolationException` | 400 |

## Códigos útiles del Banco Mundial

| Indicador | Código |
|-----------|--------|
| Población total | `SP.POP.TOTL` |
| PIB (US$ actuales) | `NY.GDP.MKTP.CD` |
| PIB per cápita (US$) | `NY.GDP.PCAP.CD` |
| Inflación (% anual) | `FP.CPI.TOTL.ZG` |
| Esperanza de vida | `SP.DYN.LE00.IN` |
| Desempleo (% de la fuerza laboral) | `SL.UEM.TOTL.ZS` |
| Emisiones de CO2 per cápita | `EN.ATM.CO2E.PC` |

Los países se indican por código ISO3 (`BRA`, `USA`, `ARG`, `ESP`...).

## Arquitectura

Arquitectura **en capas**, con inversión de dependencias en el límite de cada
caso de uso (en el espíritu de *Clean Architecture* / *Ports & Adapters*, sin
toda su ceremonia):

```
Controller (web)
      │  depende de interfaces (UseCase), no de implementaciones
      ▼
UseCase interface  →  UseCase Service (implementación, 1 clase = 1 caso de uso)
      │  depende de los clientes, no del RestClient directamente
      ▼
Client layer (acceso a datos externos)
      │  WorldBankApiExecutor: infraestructura compartida (HTTP + parseo del sobre)
      ▼
World Bank API (externa)          Frankfurter API (externa, tasas de cambio)
```

`CalculateGdpPerCapitaService` combina ambas fuentes: pide PIB/población al
Banco Mundial y, si se pidió conversión, la tasa de cambio a Frankfurter — sin
que ninguna de las dos APIs externas sepa de la existencia de la otra.

Paquetes (`com.udea.worldbank.*`):

```
controller/       -> capa web: traduce HTTP <-> casos de uso (sin lógica de negocio)
service/          -> casos de uso: un interface + una clase Service por cada uno
model/            -> modelo de dominio propio (independiente de la API externa)
client/           -> acceso a datos: HTTP a las APIs externas (Banco Mundial, Frankfurter)
client/dto/       -> DTOs fieles al JSON crudo de cada API externa
client/mapper/    -> traduce los DTOs crudos al modelo de dominio
dto/              -> DTOs de la respuesta propia (lo que ve el cliente de esta API)
util/             -> helpers sin estado compartidos entre casos de uso (formateo de moneda)
config/           -> configuración (RestClient)
exception/        -> excepciones propias y manejador global de errores
```

El flujo es unidireccional: `controller -> service -> client -> API externa`.
La capa de servicio nunca reenvía el JSON crudo: filtra, ordena, combina y
transforma; el `client` traduce la estructura de cada API externa al dominio.

**Por qué esta arquitectura:**

- **Separación de responsabilidades**: si cambia el formato de una API
  externa, solo se toca su `client`/`client.dto`; el resto no se entera. Por
  eso sumar la Frankfurter API no tocó `controller`, `model` ni los otros
  `client`.
- **Testeable**: cada capa se prueba con dobles (mocks) de la capa inferior
  — el proyecto mantiene 100% de cobertura.
- **SOLID en los casos de uso**: cada uno vive en su propia clase (principio
  de responsabilidad única), expone una interfaz mínima de un solo método
  (segregación de interfaces), y el controlador depende de esa abstracción,
  no de la implementación concreta (inversión de dependencias) — así se
  podría cambiar la implementación de un caso de uso (por ejemplo, agregarle
  caché) sin tocar el controlador.

## Casos de uso en detalle

### 1. `GetSeriesUseCase` → `GetSeriesService`

```
SeriesResponse getSeries(String countryIso3, String indicator, Integer from, Integer to)
```

Obtiene la serie temporal "limpia" de un indicador para un país:

1. Llama a `ObservationsWorldBankClient.getObservations(...)`.
2. Filtra las observaciones con `value == null` (años sin reporte).
3. Reordena cronológicamente ascendente (la API devuelve del año más nuevo al
   más viejo).
4. Mapea cada observación a `SeriesPointResponse(year, value)`.
5. Si no queda ningún punto, lanza `DataNotFoundException`.
6. Toma el nombre del país y del indicador de la primera observación cruda
   para armar el `SeriesResponse`.

El método privado `getObservations(...)` aísla la llamada externa en un
`try/catch(WorldBankApiException)`, registra el contexto de negocio (país,
indicador, rango) que la capa de acceso a datos desconoce, y relanza la
excepción para que el `GlobalExceptionHandler` la traduzca a 502.

### 2. `CalculateGdpPerCapitaUseCase` → `CalculateGdpPerCapitaService`

```
GdpPerCapitaResponse calculateGdpPerCapita(String countryIso3, int year, String targetCurrency)
```

Calcula un indicador **derivado** que el Banco Mundial no expone: PIB per
cápita = PIB / población. Opcionalmente lo **enriquece** convirtiéndolo a otra
moneda.

1. Pide el valor puntual del indicador `NY.GDP.MKTP.CD` (PIB) para el
   país/año — una llamada a la API.
2. Pide el valor puntual de `SP.POP.TOTL` (población) — segunda llamada.
3. Si falta cualquiera de los dos, o la población es 0, lanza
   `DataNotFoundException`.
4. Calcula `pib / poblacion`, redondea a 2 decimales y arma el
   `GdpPerCapitaResponse`.
5. Si se pidió `targetCurrency` (no nulo/vacío), pide a `ExchangeRateClient`
   la tasa de cambio más reciente de USD a esa moneda, convierte el PIB per
   cápita y llena los campos `targetCurrency`, `gdpPerCapitaConverted`,
   `gdpPerCapitaConvertedFormatted`, `exchangeRate` y `exchangeRateDate`; si no
   se pidió, esos campos quedan en `null`.

El método privado `pointValue(iso3, indicador, año)` filtra las observaciones
sin valor y toma la primera con dato; también está envuelto en su propio
`try/catch` con logging de contexto — igual que `getLatestRate(...)` para la
llamada a Frankfurter.

### 3. `RankingUseCase` → `RankingService`

```
List<RankingItemResponse> ranking(String indicator, int year, int limit)
```

Arma un ranking de países según un indicador en un año dado:

1. Pide el indicador para `"all"` (todos los países y agregados).
2. Pide la lista de **códigos ISO3 de países reales**
   (`CountriesWorldBankClient`, cacheada tras la primera llamada porque es
   estable).
3. Filtra: descarta valores nulos y descarta cualquier código que no esté en
   la lista de países reales (así se excluyen "World", regiones, grupos de
   ingreso, etc.).
4. Ordena descendente por valor y recorta a `limit`.
5. Si no queda nada, lanza `DataNotFoundException`.
6. Asigna posición 1..N y arma cada `RankingItemResponse`.

Los métodos privados `getGlobalObservations(...)` y `getRealCountries()`
aíslan cada llamada externa en su propio `try/catch` con su propio mensaje de
log antes de relanzar.

### 4. `ExchangeRateUseCase` → `ExchangeRateService`

```
ExchangeRateResponse convert(String from, String to, BigDecimal amount)
```

Convierte un monto entre dos monedas con la tasa de cambio de referencia más
reciente (caso de uso nuevo, independiente de los indicadores del Banco
Mundial, sobre la Frankfurter API):

1. Normaliza ambos códigos de moneda a mayúsculas.
2. Si `from` y `to` son la misma moneda, no llama a la API externa: la tasa es
   `1` y el monto convertido es el mismo monto (redondeado).
3. Si no, pide a `ExchangeRateClient.getLatestRate(from, to)` la tasa más
   reciente.
4. Calcula `amount * tasa`, redondea al número de decimales oficial de la
   moneda destino y arma el `ExchangeRateResponse`.

## Piezas de soporte

No son casos de uso, pero los sostienen:

- **`WorldBankApiExecutor`**: infraestructura compartida. `execute(...)` hace
  la llamada HTTP (vía `RestClient`) y traduce `RestClientException` a
  `WorldBankApiException`. `extractData(...)` valida el sobre
  `[metadata, datos]` de la API y devuelve la posición `[1]` (o `null` si no
  hay datos, o lanza excepción si la metadata trae un `message` de error).
- **`ObservationsWorldBankClient`**: usa el ejecutor para pedir
  `/country/{paises}/indicator/{indicador}` y mapea el JSON crudo a
  `Observation` vía `ObservationMapper`.
- **`CountriesWorldBankClient`**: pide `/country`, filtra agregados
  (`WorldBankCountry.isAggregate()` — región nula o `"NA"`) y **cachea** el
  resultado (con doble verificación de bloqueo, porque la lista de países
  reales es estable).
- **`ObservationMapper`**: traduce `WorldBankObservation` (DTO fiel al JSON
  externo) a `Observation` (modelo de dominio), incluyendo el parseo del año
  (`"2020"` → `2020`; si no es un año de 4 dígitos, devuelve `-1` para que el
  caso de uso lo descarte).
- **`ExchangeRateClient`**: pide `/latest?base={from}&symbols={to}` a
  Frankfurter. A diferencia de la World Bank API, la respuesta no viene
  envuelta en un sobre `[metadata, datos]`, así que llama al `RestClient`
  directamente (sin un ejecutor compartido). Traduce un 4xx (moneda no
  soportada o par inválido, p.ej. `from == to`) o una respuesta sin la moneda
  pedida en `rates` a `DataNotFoundException`; cualquier otro fallo de
  comunicación, a `ExchangeRateApiException`.
- **`CurrencyFormatter`** (`util/`): helper sin estado, compartido por
  `CalculateGdpPerCapitaService` y `ExchangeRateService`, que redondea un
  monto al número de decimales oficial de una moneda ISO 4217 (`round`) y lo
  formatea con su símbolo (`format`) — evita duplicar esa lógica en los dos
  casos de uso que convierten montos.

## Particularidades de las APIs externas (resueltas en `client/`)

**World Bank Indicators API:**

- La respuesta es siempre un array de 2 posiciones: `[ metadata, [datos] ]`.
  Se extrae la posición `[1]` y se maneja el caso `null` (sin datos).
- `value` llega como `null` en los años que un país no reportó.
- El endpoint `all` mezcla países con agregados (Mundo, regiones, grupos de
  ingreso); estos traen `region.id == "NA"` y se descartan cruzando con `/country`.

**Frankfurter API:**

- Solo cubre las ~30 monedas que reporta el Banco Central Europeo (no incluye
  COP ni la mayoría de monedas latinoamericanas, salvo BRL y MXN).
- Devuelve 404 tanto si `base` o `symbols` no es una moneda soportada como si
  es un par inválido (p.ej. `base == symbols`); en ambos casos el `client` lo
  traduce a `DataNotFoundException`.


<!-- Prueba de conexión con GitHub realizada el 2026-10-19 por f.silvacadavid@gmail.com -->
