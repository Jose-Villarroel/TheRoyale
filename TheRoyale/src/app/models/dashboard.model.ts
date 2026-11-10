export interface OcupacionTipo {
  tipoId: number;
  tipoNombre: string;
  total: number;
  ocupadas: number;
  porcentaje: number;
}

export interface IngresoMensual {
  mes: string;
  anioMes: string;
  ingresosHospedaje: number;
  ingresosServicios: number;
  total: number;
}

export interface ActividadReciente {
  reservaId: number;
  clienteNombreCompleto: string;
  habitacionNumero: string;
  tipoHabitacion: string;
  fechaInicio: string;
  fechaFin: string;
  estado: string;
  montoTotal: number;
  fechaCreacion: string;
}

export interface PrediccionOcupacion {
  semana: string;
  rangoFechas: string;
  ocupacionProyectada: number;
  habitacionesEstimadas: number;
  factorTendencia: string;
  nivelDemanda: string;
  recomendacionTarifa: string;
}

export interface DashboardMetricas {
  porcentajeOcupacion: number;
  habitacionesOcupadas: number;
  habitacionesDisponibles: number;
  habitacionesMantenimiento: number;
  totalHabitaciones: number;
  llegadasHoy: number;
  salidasHoy: number;
  reservasActivas: number;
  totalClientes: number;
  ingresosMesActual: number;
  ingresosTotales: number;
  ocupacionPorTipo: OcupacionTipo[];
  ingresosMensuales: IngresoMensual[];
  habitacionesPorEstado: Record<string, number>;
  actividadReciente: ActividadReciente[];
  proyeccionOcupacion?: PrediccionOcupacion[];
}
