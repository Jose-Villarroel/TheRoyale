# Pronóstico de ocupación y precios dinámicos

Funcionalidades 9 (pronóstico de ocupación en el panel del admin) y 10 (precio recomendado según temporada y ocupación, que el admin aprueba).

## De dónde salen los datos

The Royale es un hotel ficticio, así que no tiene historial real. El historial que usa el modelo es **simulado**, pero está calibrado con datos públicos:

| Qué | Fuente |
|---|---|
| Cómo reserva la gente: anticipación, noches, cancelaciones y día de llegada | Dataset **Hotel Booking Demand**, hotel urbano (*City Hotel*). Antonio, N., de Almeida, A. y Nunes, L. (2019). *Hotel booking demand datasets*. Data in Brief, 22, 41–49. Copia pública en el repo `rfordatascience/tidytuesday` (2020-02-11). |
| Cuánto se llena el hotel cada mes | Niveles **aproximados** de ocupación hotelera mensual en Nueva York (enero y febrero bajos; otoño y diciembre altos). No se usó una cifra oficial verificada: están en `OCUPACION_NYC_MES` de `generar_historico.py`. |
| Inventario | El del `DataLoader`: 5 tipos con 10 habitaciones cada uno. |

En el panel del admin se dice explícitamente que el historial es simulado.

## Archivos

| Archivo | Qué es |
|---|---|
| `generar_historico.py` | Genera `src/main/resources/modelo/reservas-historicas.csv`: 2 años de reservas y las ya hechas para los próximos 90 días, con semilla fija. |
| `entrenar_modelo.py` | Entrena una regresión Ridge y la compara contra un modelo ingenuo (repetir el año anterior). Exporta `src/main/resources/modelo/modelo-ocupacion.json`. |
| `datos/hotels.csv` | El dataset descargado. **No se sube a git**: es grande y de terceros. |

## Cómo regenerar

```bash
python -m pip install pandas scikit-learn
curl -L -o Docs/modelo/datos/hotels.csv https://raw.githubusercontent.com/rfordatascience/tidytuesday/master/data/2020/2020-02-11/hotels.csv
python -I Docs/modelo/generar_historico.py
python -I Docs/modelo/entrenar_modelo.py
```

La salida es la misma cada vez (semilla fija). `ModeloOcupacionTest` comprueba que Java predice igual que scikit-learn con 3 filas de ejemplo del JSON.

## Cómo funciona en el backend

- **Historial con fechas relativas.** Al arrancar, el `DataLoader` carga el CSV en `ReservaHistorica`, una tabla aparte de `Reserva` que no aparece en los listados. Corre todas las fechas para que el historial termine **ayer**: la demo siempre se ve actual.
- **Variables del modelo.** `ModeloOcupacion` lee el JSON. La predicción es intercepto + suma de coeficiente × variable, con estas variables:
  - lo reservado a la fecha;
  - días que faltan para la noche;
  - ocupación del mismo día un año antes;
  - feriado, día de la semana, mes y tipo de habitación.
- **Pronóstico.** `PronosticoOcupacionService` arma el de los próximos 7 a 90 días (`GET /api/admin/ocupacion`), por tipo o para el hotel completo.
- **Precio recomendado.** `RecomendacionPrecioService` propone un precio por tipo y semana para las próximas 8 semanas:
  - **Fórmula:** temporada (`Temporada`, la define el admin) × demanda (ocupación esperada), limitada entre 0,80× y 1,50×.
  - **Factor demanda:** por debajo de 40 % da 0,90; hasta 70 % 1,00; hasta 85 % 1,10; por encima 1,25.
  - **Cuándo se genera:** los lunes a las 06:00 (hora de NY) y con el botón "Recalculate now". Si al abrir el panel la última tiene más de 7 días, se genera en ese momento.
- **El precio solo cambia si el admin lo aprueba.** Aplicar una recomendación crea una `TarifaAplicada`, y `PrecioService` cobra base × factor, redondeado a $5. Las reservas ya hechas conservan su `precioNocheAcordado`.

## Despliegue

- **Frontend (Angular):** va en Vercel. Hay que:
  - apuntar `apiUrl` de `environment.prod.ts` al backend desplegado;
  - agregar un `vercel.json` con un rewrite a `index.html`;
  - autorizar el dominio en `CorsConfig` y en el client ID de Google.
- **Backend (Spring Boot):** **no** corre en Vercel, que no ejecuta Java. Va en Render, Railway o Fly.io, con Docker.
- **Python no se despliega:** solo se usa en local para regenerar el historial y el modelo. El backend lee el JSON.
- **Base de datos:** con H2 en memoria, las tarifas aprobadas se pierden al reiniciar. Para producción, Postgres (por ejemplo Neon).
- **Servidores que se duermen:** en planes gratuitos la tarea de los lunes puede no correr. Por eso existe el respaldo de "generar al abrir el panel".
