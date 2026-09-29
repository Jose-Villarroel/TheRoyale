import { Injectable } from '@angular/core';
import { EstadoHabitacion, Habitacion } from '../models/habitacion.model';
import { TipoHabitacion } from '../models/tipo-habitacion.model';
import { tipoHabitacionSemilla } from '../datos/tipos-habitacion.data';

@Injectable({ providedIn: 'root' })
export class HabitacionService {

  // ===== "Base de datos" quemada, calcada del DataLoader del backend (10 habitaciones por tipo) =====
  // Cada habitación guarda el OBJETO TipoHabitacion (no su id), igual que el @ManyToOne del backend.
  // tipoHabitacionSemilla: 1 Normal, 2 Executive, 3 VIP, 4 Luxury, 5 Presidential Suite
  private habitaciones: Habitacion[] = [
    // ----- Normal: piso 1-2, $120-$140 -----
    this.crearHabitacion(1, '101', tipoHabitacionSemilla(1), 120, 'DISPONIBLE'),
    this.crearHabitacion(2, '102', tipoHabitacionSemilla(1), 120, 'OCUPADA'),
    this.crearHabitacion(3, '103', tipoHabitacionSemilla(1), 125, 'DISPONIBLE'),
    this.crearHabitacion(4, '104', tipoHabitacionSemilla(1), 125, 'DISPONIBLE'),
    this.crearHabitacion(5, '105', tipoHabitacionSemilla(1), 130, 'MANTENIMIENTO'),
    this.crearHabitacion(6, '106', tipoHabitacionSemilla(1), 130, 'DISPONIBLE'),
    this.crearHabitacion(7, '107', tipoHabitacionSemilla(1), 135, 'OCUPADA'),
    this.crearHabitacion(8, '108', tipoHabitacionSemilla(1), 135, 'DISPONIBLE'),
    this.crearHabitacion(9, '109', tipoHabitacionSemilla(1), 140, 'DISPONIBLE'),
    this.crearHabitacion(10, '110', tipoHabitacionSemilla(1), 140, 'DISPONIBLE'),

    // ----- Executive: piso 3-4, $190-$220 -----
    this.crearHabitacion(11, '301', tipoHabitacionSemilla(2), 190, 'DISPONIBLE'),
    this.crearHabitacion(12, '302', tipoHabitacionSemilla(2), 190, 'OCUPADA'),
    this.crearHabitacion(13, '303', tipoHabitacionSemilla(2), 195, 'DISPONIBLE'),
    this.crearHabitacion(14, '304', tipoHabitacionSemilla(2), 195, 'DISPONIBLE'),
    this.crearHabitacion(15, '305', tipoHabitacionSemilla(2), 200, 'MANTENIMIENTO'),
    this.crearHabitacion(16, '306', tipoHabitacionSemilla(2), 200, 'DISPONIBLE'),
    this.crearHabitacion(17, '307', tipoHabitacionSemilla(2), 205, 'OCUPADA'),
    this.crearHabitacion(18, '308', tipoHabitacionSemilla(2), 210, 'DISPONIBLE'),
    this.crearHabitacion(19, '309', tipoHabitacionSemilla(2), 215, 'DISPONIBLE'),
    this.crearHabitacion(20, '310', tipoHabitacionSemilla(2), 220, 'DISPONIBLE'),

    // ----- VIP: piso 5-6, $280-$320 -----
    this.crearHabitacion(21, '501', tipoHabitacionSemilla(3), 280, 'DISPONIBLE'),
    this.crearHabitacion(22, '502', tipoHabitacionSemilla(3), 280, 'OCUPADA'),
    this.crearHabitacion(23, '503', tipoHabitacionSemilla(3), 285, 'DISPONIBLE'),
    this.crearHabitacion(24, '504', tipoHabitacionSemilla(3), 290, 'DISPONIBLE'),
    this.crearHabitacion(25, '505', tipoHabitacionSemilla(3), 295, 'DISPONIBLE'),
    this.crearHabitacion(26, '506', tipoHabitacionSemilla(3), 295, 'MANTENIMIENTO'),
    this.crearHabitacion(27, '507', tipoHabitacionSemilla(3), 300, 'DISPONIBLE'),
    this.crearHabitacion(28, '508', tipoHabitacionSemilla(3), 305, 'OCUPADA'),
    this.crearHabitacion(29, '509', tipoHabitacionSemilla(3), 310, 'DISPONIBLE'),
    this.crearHabitacion(30, '510', tipoHabitacionSemilla(3), 320, 'DISPONIBLE'),

    // ----- Luxury: piso 7-8, $350-$420 -----
    this.crearHabitacion(31, '701', tipoHabitacionSemilla(4), 350, 'OCUPADA'),
    this.crearHabitacion(32, '702', tipoHabitacionSemilla(4), 355, 'OCUPADA'),
    this.crearHabitacion(33, '703', tipoHabitacionSemilla(4), 360, 'DISPONIBLE'),
    this.crearHabitacion(34, '704', tipoHabitacionSemilla(4), 370, 'DISPONIBLE'),
    this.crearHabitacion(35, '705', tipoHabitacionSemilla(4), 380, 'DISPONIBLE'),
    this.crearHabitacion(36, '706', tipoHabitacionSemilla(4), 380, 'MANTENIMIENTO'),
    this.crearHabitacion(37, '707', tipoHabitacionSemilla(4), 390, 'DISPONIBLE'),
    this.crearHabitacion(38, '708', tipoHabitacionSemilla(4), 400, 'OCUPADA'),
    this.crearHabitacion(39, '709', tipoHabitacionSemilla(4), 410, 'DISPONIBLE'),
    this.crearHabitacion(40, '710', tipoHabitacionSemilla(4), 420, 'DISPONIBLE'),

    // ----- Presidential Suite: piso 9-10, $800-$1200 -----
    this.crearHabitacion(41, '901', tipoHabitacionSemilla(5), 800, 'OCUPADA'),
    this.crearHabitacion(42, '902', tipoHabitacionSemilla(5), 850, 'DISPONIBLE'),
    this.crearHabitacion(43, '903', tipoHabitacionSemilla(5), 900, 'OCUPADA'),
    this.crearHabitacion(44, '904', tipoHabitacionSemilla(5), 950, 'DISPONIBLE'),
    this.crearHabitacion(45, '905', tipoHabitacionSemilla(5), 1000, 'DISPONIBLE'),
    this.crearHabitacion(46, '906', tipoHabitacionSemilla(5), 1000, 'MANTENIMIENTO'),
    this.crearHabitacion(47, '907', tipoHabitacionSemilla(5), 1050, 'DISPONIBLE'),
    this.crearHabitacion(48, '908', tipoHabitacionSemilla(5), 1100, 'DISPONIBLE'),
    this.crearHabitacion(49, '909', tipoHabitacionSemilla(5), 1150, 'OCUPADA'),
    this.crearHabitacion(50, '910', tipoHabitacionSemilla(5), 1200, 'DISPONIBLE')
  ];

  obtenerTodos(): Habitacion[] {
    return this.habitaciones;
  }

  existePorTipoHabitacionId(tipoHabitacionId: number): boolean {
    return this.habitaciones.some(habitacion => habitacion.tipoHabitacion.id === tipoHabitacionId);
  }

  // ===== Método de ayuda para no repetir el objeto 50 veces =====
  private crearHabitacion(id: number, numero: string, tipoHabitacion: TipoHabitacion, precio: number, estado: EstadoHabitacion): Habitacion {
    return { id, numero, tipoHabitacion, precio, estado };
  }
}
