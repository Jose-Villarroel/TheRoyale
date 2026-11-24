"""
Entrena el modelo de pronóstico de ocupación de The Royale y lo exporta a
src/main/resources/modelo/modelo-ocupacion.json, que el backend (ModeloOcupacion.java) lee al arrancar.

Qué predice: la ocupación final (0 a 1) de un tipo de habitación en una fecha, sabiendo cuánto
hay reservado L días antes. Las variables son las mismas que arma el backend:
- reservada: fracción de habitaciones con reserva hecha L días antes (incluye las que luego se
  cancelan, porque en ese momento seguían en el libro)
- anticipacion: L / 90, y la interacción reservada x anticipacion
- ocupacionAnioAnterior: ocupación final del mismo día de la semana un año antes (fecha - 364)
- feriado, día de la semana, mes y tipo de habitación (one-hot)

Modelo: Ridge (regresión lineal regularizada), para que Java solo haga un producto punto.
Se compara contra un modelo ingenuo (repetir la ocupación del año anterior) en los últimos 90 días.
Uso:  python -I Docs/modelo/entrenar_modelo.py   (después de generar_historico.py)
"""
from __future__ import annotations

import json
import sys
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generar_historico import FECHA_REFERENCIA, DIAS_HISTORIA, TIPOS, feriados  # noqa: E402

RAIZ = Path(__file__).resolve().parents[2]
HISTORICO = RAIZ / "src" / "main" / "resources" / "modelo" / "reservas-historicas.csv"
SALIDA = RAIZ / "src" / "main" / "resources" / "modelo" / "modelo-ocupacion.json"
ANTICIPACIONES = [0, 7, 14, 30, 60, 90]
DIAS_PRUEBA = 90


def nombres_variables() -> list[str]:
    return (["reservada", "anticipacion", "reservadaPorAnticipacion", "ocupacionAnioAnterior", "feriado"]
            + [f"dia_{d}" for d in range(1, 8)]
            + [f"mes_{m}" for m in range(1, 13)]
            + [f"tipo_{t}" for t in TIPOS])


def ocupacion_por_noche(reservas: pd.DataFrame) -> dict[tuple[date, str], int]:
    """Habitaciones ocupadas por noche y tipo, solo con reservas no canceladas."""
    ocupadas: dict[tuple[date, str], int] = {}
    for fila in reservas[~reservas["cancelada"]].itertuples():
        for k in range(fila.noches):
            clave = (fila.llegada + timedelta(days=k), fila.tipo)
            ocupadas[clave] = ocupadas.get(clave, 0) + 1
    return ocupadas


def reservada_a(reservas_tipo: pd.DataFrame, noche: date, corte: date) -> int:
    """Reservas (canceladas o no) creadas hasta 'corte' que cubren 'noche'."""
    r = reservas_tipo
    return int(((r["fechaCreacion"] <= corte) & (r["llegada"] <= noche)
                & (r["salida"] > noche)).sum())


def fila_variables(reservada: float, anticipacion: int, anio_anterior: float, noche: date,
                   tipo: str, altos: set[date]) -> dict[str, float]:
    v = {n: 0.0 for n in nombres_variables()}
    v["reservada"] = reservada
    v["anticipacion"] = anticipacion / 90
    v["reservadaPorAnticipacion"] = reservada * anticipacion / 90
    v["ocupacionAnioAnterior"] = anio_anterior
    v["feriado"] = 1.0 if noche in altos else 0.0
    v[f"dia_{noche.isoweekday()}"] = 1.0
    v[f"mes_{noche.month}"] = 1.0
    v[f"tipo_{tipo}"] = 1.0
    return v


def main() -> None:
    reservas = pd.read_csv(HISTORICO, parse_dates=["fechaCreacion", "llegada"])
    reservas["fechaCreacion"] = reservas["fechaCreacion"].dt.date
    reservas["llegada"] = reservas["llegada"].dt.date
    reservas["salida"] = [l + timedelta(days=int(n)) for l, n in zip(reservas["llegada"], reservas["noches"])]
    reservas["cancelada"] = reservas["cancelada"].astype(str).str.lower() == "true"

    ocupadas = ocupacion_por_noche(reservas)
    inicio = FECHA_REFERENCIA - timedelta(days=DIAS_HISTORIA)
    altos = set().union(*(feriados(a) for a in range(inicio.year, FECHA_REFERENCIA.year + 2)))
    # Solo noches con un año completo detrás (y sin el arranque vacío de la simulación)
    primera = inicio + timedelta(days=364 + 30)
    ultima = FECHA_REFERENCIA - timedelta(days=1)
    corte_prueba = ultima - timedelta(days=DIAS_PRUEBA)

    filas, objetivo, ingenuo, es_prueba, anticip = [], [], [], [], []
    for tipo, (habitaciones, _) in TIPOS.items():
        r_tipo = reservas[reservas["tipo"] == tipo]
        noche = primera
        while noche <= ultima:
            final = ocupadas.get((noche, tipo), 0) / habitaciones
            anterior = ocupadas.get((noche - timedelta(days=364), tipo), 0) / habitaciones
            for l in ANTICIPACIONES:
                reservada = reservada_a(r_tipo, noche, noche - timedelta(days=l)) / habitaciones
                filas.append(fila_variables(reservada, l, anterior, noche, tipo, altos))
                objetivo.append(final)
                ingenuo.append(anterior)
                es_prueba.append(noche > corte_prueba)
                anticip.append(l)
            noche += timedelta(days=1)

    nombres = nombres_variables()
    X = np.array([[f[n] for n in nombres] for f in filas])
    y = np.array(objetivo)
    prueba = np.array(es_prueba)
    anticip = np.array(anticip)

    modelo = Ridge(alpha=1.0).fit(X[~prueba], y[~prueba])
    pred = np.clip(modelo.predict(X[prueba]), 0, 1)
    mae_modelo = mean_absolute_error(y[prueba], pred)
    mae_ingenuo = mean_absolute_error(y[prueba], np.array(ingenuo)[prueba])
    mae_por_l = {str(l): round(float(mean_absolute_error(y[prueba][anticip[prueba] == l],
                                                          pred[anticip[prueba] == l])), 4)
                 for l in ANTICIPACIONES}

    # Se reentrena con todo el historial para el modelo que se exporta
    final = Ridge(alpha=1.0).fit(X, y)

    # Tres filas de ejemplo con su predicción: el test de Java comprueba que da lo mismo
    ejemplos = []
    for i in (0, len(filas) // 2, len(filas) - 1):
        ejemplos.append({"variables": {n: X[i][j] for j, n in enumerate(nombres) if X[i][j] != 0},
                         "prediccion": round(float(final.predict(X[i:i + 1])[0]), 6)})

    resultado = {
        "version": 1,
        "entrenadoEl": FECHA_REFERENCIA.isoformat(),
        # Se muestra tal cual en el panel del admin (en inglés, como el resto de la interfaz)
        "fuente": ("Trained on two years of simulated bookings for The Royale, calibrated with the public "
                   "Hotel Booking Demand dataset (City Hotel; Antonio, Almeida & Nunes, 2019) and with "
                   "approximate monthly hotel occupancy levels in New York City."),
        "algoritmo": "Ridge (alpha=1.0)",
        "anticipaciones": ANTICIPACIONES,
        "variables": nombres,
        "intercepto": round(float(final.intercept_), 8),
        "coeficientes": {n: round(float(c), 8) for n, c in zip(nombres, final.coef_)},
        "maeModelo": round(float(mae_modelo), 4),
        "maeIngenuo": round(float(mae_ingenuo), 4),
        "maePorAnticipacion": mae_por_l,
        "filasEntrenamiento": int((~prueba).sum()),
        "filasPrueba": int(prueba.sum()),
        "ejemplos": ejemplos,
    }
    SALIDA.write_text(json.dumps(resultado, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"Filas: {len(y)} (prueba: {prueba.sum()})")
    print(f"MAE modelo:  {mae_modelo:.4f}  ({mae_modelo * 100:.1f} puntos de ocupación)")
    print(f"MAE ingenuo: {mae_ingenuo:.4f}  ({mae_ingenuo * 100:.1f} puntos)")
    print("MAE por anticipación:", mae_por_l)
    print(f"Ocupación media real (prueba): {y[prueba].mean():.3f}")
    print(f"Archivo: {SALIDA}")


if __name__ == "__main__":
    main()
