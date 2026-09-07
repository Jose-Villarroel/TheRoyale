-- ============================================================
-- TheRoyale - Datos iniciales para H2
-- 5 tipos de habitación, 10 clientes, 50 habitaciones
-- ============================================================

-- ===== 5 TIPOS DE HABITACIÓN =====
INSERT INTO tipo_habitacion (nombre, descripcion, imagen_url) VALUES
  ('Normal',             'Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.',              '/images/suite-3.webp'),
  ('Executive',          'Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.',      '/images/suite-1.webp'),
  ('VIP',                'A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.',      '/images/suite-2.webp'),
  ('Luxury',             'The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.',             '/images/suite-4.webp'),
  ('Presidential Suite', 'Unmatched exclusivity. Up to 6 guests, private terrace, butler service, panoramic city view, private dining.',  '/images/luxury 1.jpg');

-- ===== 10 CLIENTES =====
INSERT INTO cliente (nombre, apellido, email, password, telefono, fecha_registro) VALUES
  ('Carlos',    'Mendoza',    'carlos.mendoza@email.com',    'pass1234', '+1-555-0101', '2025-01-15'),
  ('Ana',       'García',     'ana.garcia@email.com',        'pass1234', '+1-555-0102', '2025-02-20'),
  ('Luis',      'Hernández',  'luis.hernandez@email.com',    'pass1234', '+1-555-0103', '2025-03-05'),
  ('Sofia',     'Torres',     'sofia.torres@email.com',      'pass1234', '+1-555-0104', '2025-03-18'),
  ('Miguel',    'Ramírez',    'miguel.ramirez@email.com',    'pass1234', '+1-555-0105', '2025-04-02'),
  ('Isabella',  'López',      'isabella.lopez@email.com',    'pass1234', '+1-555-0106', '2025-04-25'),
  ('Andrés',    'Martínez',   'andres.martinez@email.com',   'pass1234', '+1-555-0107', '2025-05-10'),
  ('Valentina', 'Rodríguez',  'valentina.rodriguez@email.com','pass1234', '+1-555-0108', '2025-06-01'),
  ('Jorge',     'Sánchez',    'jorge.sanchez@email.com',     'pass1234', '+1-555-0109', '2025-07-14'),
  ('Camila',    'Díaz',       'camila.diaz@email.com',       'pass1234', '+1-555-0110', '2025-08-22');

-- ===== 50 HABITACIONES =====
-- (10 Normal | tipo_id=1, 10 Executive | tipo_id=2, 10 VIP | tipo_id=3, 10 Luxury | tipo_id=4, 10 Presidential | tipo_id=5)

-- Normal (piso 1-2, $120-$140)
INSERT INTO habitacion (numero, tipo_habitacion_id, precio, estado) VALUES
  ('101', 1, 120.00, 'DISPONIBLE'),
  ('102', 1, 120.00, 'OCUPADA'),
  ('103', 1, 125.00, 'DISPONIBLE'),
  ('104', 1, 125.00, 'DISPONIBLE'),
  ('105', 1, 130.00, 'MANTENIMIENTO'),
  ('106', 1, 130.00, 'DISPONIBLE'),
  ('107', 1, 135.00, 'OCUPADA'),
  ('108', 1, 135.00, 'DISPONIBLE'),
  ('109', 1, 140.00, 'DISPONIBLE'),
  ('110', 1, 140.00, 'DISPONIBLE');

-- Executive (piso 3-4, $190-$220)
INSERT INTO habitacion (numero, tipo_habitacion_id, precio, estado) VALUES
  ('301', 2, 190.00, 'DISPONIBLE'),
  ('302', 2, 190.00, 'OCUPADA'),
  ('303', 2, 195.00, 'DISPONIBLE'),
  ('304', 2, 195.00, 'DISPONIBLE'),
  ('305', 2, 200.00, 'MANTENIMIENTO'),
  ('306', 2, 200.00, 'DISPONIBLE'),
  ('307', 2, 205.00, 'OCUPADA'),
  ('308', 2, 210.00, 'DISPONIBLE'),
  ('309', 2, 215.00, 'DISPONIBLE'),
  ('310', 2, 220.00, 'DISPONIBLE');

-- VIP (piso 5-6, $280-$320)
INSERT INTO habitacion (numero, tipo_habitacion_id, precio, estado) VALUES
  ('501', 3, 280.00, 'DISPONIBLE'),
  ('502', 3, 280.00, 'OCUPADA'),
  ('503', 3, 285.00, 'DISPONIBLE'),
  ('504', 3, 290.00, 'DISPONIBLE'),
  ('505', 3, 295.00, 'DISPONIBLE'),
  ('506', 3, 295.00, 'MANTENIMIENTO'),
  ('507', 3, 300.00, 'DISPONIBLE'),
  ('508', 3, 305.00, 'OCUPADA'),
  ('509', 3, 310.00, 'DISPONIBLE'),
  ('510', 3, 320.00, 'DISPONIBLE');

-- Luxury (piso 7-8, $350-$420)
INSERT INTO habitacion (numero, tipo_habitacion_id, precio, estado) VALUES
  ('701', 4, 350.00, 'DISPONIBLE'),
  ('702', 4, 355.00, 'OCUPADA'),
  ('703', 4, 360.00, 'DISPONIBLE'),
  ('704', 4, 370.00, 'DISPONIBLE'),
  ('705', 4, 380.00, 'DISPONIBLE'),
  ('706', 4, 380.00, 'MANTENIMIENTO'),
  ('707', 4, 390.00, 'DISPONIBLE'),
  ('708', 4, 400.00, 'OCUPADA'),
  ('709', 4, 410.00, 'DISPONIBLE'),
  ('710', 4, 420.00, 'DISPONIBLE');

-- Presidential Suite (piso 9-10, $800-$1200)
INSERT INTO habitacion (numero, tipo_habitacion_id, precio, estado) VALUES
  ('901', 5,  800.00, 'DISPONIBLE'),
  ('902', 5,  850.00, 'DISPONIBLE'),
  ('903', 5,  900.00, 'OCUPADA'),
  ('904', 5,  950.00, 'DISPONIBLE'),
  ('905', 5, 1000.00, 'DISPONIBLE'),
  ('906', 5, 1000.00, 'MANTENIMIENTO'),
  ('907', 5, 1050.00, 'DISPONIBLE'),
  ('908', 5, 1100.00, 'DISPONIBLE'),
  ('909', 5, 1150.00, 'OCUPADA'),
  ('910', 5, 1200.00, 'DISPONIBLE');
