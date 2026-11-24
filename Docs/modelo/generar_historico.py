"""
Genera el historial simulado de reservas de The Royale (2 años + reservas ya hechas para los
próximos 90 días) y lo guarda en src/main/resources/modelo/reservas-historicas.csv.

De dónde sale cada cosa:
- CÓMO reserva la gente: del dataset público "Hotel Booking Demand" (Antonio, Almeida y Nunes,
  Data in Brief, 2019), hotel urbano ("City Hotel"): anticipación, noches por estadía, tasa de
  cancelación según anticipación y peso de cada día de la semana en las llegadas.
- CUÁNTO se llena el hotel cada mes: ocupación hotelera mensual típica de Nueva York. Son valores
  APROXIMADOS (ver OCUPACION_NYC_MES): no se encontró una cifra oficial verificable para usar aquí.
- QUÉ habitaciones hay: el inventario real del DataLoader (5 tipos x 10 habitaciones).

Todo usa una semilla fija: correrlo de nuevo produce exactamente el mismo archivo.
Uso:  python -I Docs/modelo/generar_historico.py
"""
from __future__ import annotations

import csv
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

SEMILLA = 20261123
RAIZ = Path(__file__).resolve().parents[2]
DATASET = RAIZ / "Docs" / "modelo" / "datos" / "hotels.csv"
SALIDA = RAIZ / "src" / "main" / "resources" / "modelo" / "reservas-historicas.csv"

# Fecha en que "se toma la foto": el historial termina el día anterior. El backend desplaza
# todas las fechas para que esta fecha coincida con el día en que arranca la aplicación.
FECHA_REFERENCIA = date(2026, 11, 23)
DIAS_HISTORIA = 730
DIAS_FUTURO = 90

# Inventario y nivel relativo de demanda por tipo (las suites se llenan menos que las estándar)
TIPOS = {
    "Normal": (10, 1.00),
    "Executive": (10, 0.97),
    "VIP": (10, 0.90),
    "Luxury": (10, 0.83),
    "Presidential Suite": (10, 0.68),
}

# Ocupación hotelera mensual típica de Nueva York (APROXIMADA): enero y febrero son los meses
# flojos; otoño y diciembre, los más llenos. The Royale apunta un poco por debajo del promedio
# de la ciudad por ser un hotel de lujo pequeño.
OCUPACION_NYC_MES = {1: 0.74, 2: 0.79, 3: 0.85, 4: 0.88, 5: 0.89, 6: 0.91,
                     7: 0.89, 8: 0.88, 9: 0.90, 10: 0.92, 11: 0.89, 12: 0.90}
AJUSTE_ROYALE = 0.93


def feriados(anio: int) -> set[date]:
    """Fechas de alta demanda en NY: Thanksgiving (+-2 días), fiestas de fin de año y 4 de julio."""
    noviembre = date(anio, 11, 1)
    primer_jueves = noviembre + timedelta(days=(3 - noviembre.weekday()) % 7)
    thanksgiving = primer_jueves + timedelta(weeks=3)
    dias = {thanksgiving + timedelta(days=k) for k in range(-2, 3)}
    dias |= {date(anio, 12, d) for d in range(20, 32)} | {date(anio, 1, 1)}
    dias |= {date(anio, 7, d) for d in (3, 4, 5)}
    return dias


def parametros_dataset() -> dict:
    """Extrae del City Hotel los patrones de reserva que usa la simulación."""
    df = pd.read_csv(DATASET)
    df = df[df["hotel"] == "City Hotel"].copy()
    df["noches"] = df["stays_in_weekend_nights"] + df["stays_in_week_nights"]
    df = df[(df["noches"] >= 1) & (df["noches"] <= 14) & (df["lead_time"] <= 365)]
    df["llegada"] = pd.to_datetime(df["arrival_date_year"].astype(str) + "-"
                                   + df["arrival_date_month"] + "-"
                                   + df["arrival_date_day_of_month"].astype(str), format="%Y-%B-%d")

    tramos = [0, 7, 30, 90, 180, 366]
    df["tramo"] = pd.cut(df["lead_time"], tramos, right=False)
    cancelacion = df.groupby("tramo", observed=True)["is_canceled"].mean()

    llegadas_dow = df["llegada"].dt.day_of_week.value_counts(normalize=True).sort_index()
    return {
        "anticipacion": df["lead_time"].to_numpy(),
        "noches": df["noches"].to_numpy(),
        "tramos": tramos,
        "cancelacion": cancelacion.to_numpy(),
        "peso_dow": (llegadas_dow / llegadas_dow.mean()).to_numpy(),
        "filas": len(df),
    }


def prob_cancelacion(anticipacion: int, p: dict) -> float:
    for i in range(len(p["tramos"]) - 1):
        if p["tramos"][i] <= anticipacion < p["tramos"][i + 1]:
            return float(p["cancelacion"][i])
    return float(p["cancelacion"][-1])


def main() -> None:
    rng = np.random.default_rng(SEMILLA)
    p = parametros_dataset()
    noches_media = p["noches"].mean()
    cancel_media = float(np.mean(p["cancelacion"]))

    inicio = FECHA_REFERENCIA - timedelta(days=DIAS_HISTORIA)
    fin = FECHA_REFERENCIA + timedelta(days=DIAS_FUTURO)
    altos = set().union(*(feriados(a) for a in range(inicio.year, fin.year + 1)))

    filas = []
    for tipo, (habitaciones, nivel) in TIPOS.items():
        ocupadas = {}  # fecha -> habitaciones ocupadas por reservas NO canceladas
        dia = inicio
        while dia < fin:
            objetivo = OCUPACION_NYC_MES[dia.month] * AJUSTE_ROYALE * nivel
            objetivo *= 0.92 + 0.08 * p["peso_dow"][dia.weekday()]
            if dia in altos:
                objetivo = min(0.99, objetivo * 1.12)
            objetivo = min(0.99, objetivo * rng.normal(1.0, 0.05))
            # Llegadas que faltan para llegar a la ocupación objetivo esa noche: lo que ya ocupan las
            # estadías que empezaron antes no se vuelve a pedir. Se suman las que luego se cancelan.
            faltan = max(0.0, objetivo * habitaciones - ocupadas.get(dia, 0))
            lam = faltan / (1 - cancel_media)
            for _ in range(rng.poisson(lam)):
                noches = int(rng.choice(p["noches"]))
                anticipacion = int(rng.choice(p["anticipacion"]))
                creada = dia - timedelta(days=anticipacion)
                if creada > FECHA_REFERENCIA:
                    continue  # todavía no existiría: es una reserva futura no hecha aún
                cancelada = rng.random() < prob_cancelacion(anticipacion, p)
                noches_estadia = [dia + timedelta(days=k) for k in range(noches)]
                if not cancelada:
                    if any(ocupadas.get(n, 0) >= habitaciones for n in noches_estadia):
                        continue  # hotel lleno para ese tipo: la reserva no se habría aceptado
                    for n in noches_estadia:
                        ocupadas[n] = ocupadas.get(n, 0) + 1
                filas.append((creada.isoformat(), dia.isoformat(), noches, tipo, str(cancelada).lower()))
            dia += timedelta(days=1)

    filas.sort(key=lambda f: (f[1], f[3], f[0]))
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    with SALIDA.open("w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo, lineterminator="\n")
        escritor.writerow(["fechaCreacion", "llegada", "noches", "tipo", "cancelada"])
        escritor.writerows(filas)

    canceladas = sum(1 for f in filas if f[4] == "true")
    print(f"City Hotel: {p['filas']} reservas usadas para calibrar")
    print(f"Generadas {len(filas)} reservas ({canceladas} canceladas) entre {inicio} y {fin}")
    print(f"Archivo: {SALIDA}")


if __name__ == "__main__":
    main()
