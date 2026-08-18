# The Royale

> **Luxury Boutique Hotel — New York City**

Proyecto universitario desarrollado para la asignatura **Desarrollo Web** de la **Pontificia Universidad Javeriana – Sede Bogotá**.

## Descripción

**The Royale** es una aplicación web para la gestión y digitalización de los servicios de un hotel boutique de lujo ubicado en New York.

El sistema permite gestionar las reservas de habitaciones y los diferentes servicios ofrecidos por el hotel, tanto dentro como fuera de sus instalaciones. Los servicios contratados durante la estadía son asociados a la cuenta de la habitación para facilitar su gestión y posterior pago.

El proyecto busca representar una solución web completa para la interacción entre **clientes, operadores y administradores del hotel**.

---

## Equipo

| Integrantes                              |
| ---------------------------------------- |
| **Jose Alejandro Villarroel Marcano**    |
| **Juan José Ballesteros Suarez**         |
| **Juan Diego Rojas Osorio**              |
| **Daniel Matias Mendoza**                |

**Asignatura:** Desarrollo Web
**Universidad:** Pontificia Universidad Javeriana – Sede Bogotá

---

## Sobre The Royale

The Royale es un **hotel boutique de lujo** enfocado principalmente en:

* Viajeros de negocios.
* Turistas de alto poder adquisitivo.
* Huéspedes que buscan una experiencia premium en New York.

A diferencia de un resort todo incluido, los servicios adicionales del hotel son **opcionales** y se cobran individualmente a la cuenta de la habitación.

### Tipos de habitación

El hotel cuenta con cuatro categorías:

* **Normal** — Habitación estándar para 1-2 personas.
* **Executive** — Habitación orientada a viajeros de negocios, con espacio de trabajo.
* **VIP** — Junior suite con sala de estar independiente, con capacidad de hasta 3 personas.
* **Luxury** — Suite completa con jacuzzi, capacidad de hasta 4 personas y vista privilegiada.

### Servicios

Los servicios del hotel se dividen en diferentes categorías.

#### Dentro del hotel

* Spa
* Piscina
* Turco
* Sauna
* Gimnasio
* Room service
* Lavandería
* Restaurante y bar
* Minibar
* Servicios ejecutivos

#### Experiencias y servicios externos

* Tours guiados por New York
* Transporte al aeropuerto
* Transporte privado
* Alquiler de bicicletas
* Entradas a atracciones locales
* Experiencias y actividades en la ciudad

---

## Roles del sistema

### Cliente

El cliente puede:

* Registrarse e iniciar sesión.
* Consultar los servicios disponibles.
* Consultar los tipos de habitación.
* Realizar reservas.
* Consultar sus reservas activas.
* Consultar su historial de reservas.
* Cancelar reservas.
* Modificar reservas cuando exista disponibilidad.
* Consultar y actualizar su información personal.

### Operador

El operador puede:

* Consultar las reservas.
* Filtrar reservas activas.
* Consultar información de una reserva.
* Cancelar reservas.
* Consultar los huéspedes asociados a una habitación.
* Agregar servicios a la cuenta de una habitación.
* Eliminar servicios agregados por error.
* Consultar los servicios consumidos durante una estadía.
* Registrar pagos.
* Realizar el check-out del huésped.

### Administrador

El administrador es responsable de la configuración del sistema y puede:

* Gestionar operadores.
* Crear, consultar, actualizar y eliminar servicios.
* Crear y gestionar habitaciones.
* Asignar habitaciones a diferentes tipos.
* Deshabilitar temporalmente habitaciones que se encuentren en mantenimiento.

---

## Funcionalidades principales

### Landing Page

La página principal presenta:

* Información general del hotel.
* Tipos de habitaciones.
* Servicios disponibles.
* Experiencias en New York.
* Información de contacto.
* Acceso al inicio de sesión.
* Acceso al proceso de reserva.

### Sistema de reservas

El cliente puede seleccionar:

* Fecha de entrada.
* Fecha de salida.
* Tipo de habitación.
* Número de huéspedes.

El sistema verifica la disponibilidad y evita que una habitación sea reservada por más de un cliente durante el mismo periodo.

### Gestión de servicios

Los servicios pueden ser contratados durante la estadía y se agregan a la cuenta de la habitación.

La cuenta puede crecer durante toda la estadía hasta que el huésped realice el pago.

### Check-out

Para realizar el check-out se verifica que la cuenta de la habitación no tenga gastos pendientes o que estos hayan sido pagados.

---

## Identidad visual

La identidad de **The Royale** busca transmitir una estética elegante, moderna y minimalista.

### Paleta de colores

| Color       | Hexadecimal | Uso                   |
| ----------- | ----------- | --------------------- |
| Negro       | `#252525`   | Color principal       |
| Gris oscuro | `#545454`   | Elementos secundarios |
| Gris medio  | `#7D7D7D`   | Elementos de apoyo    |
| Gris claro  | `#CFCFCF`   | Fondos y detalles     |
| Dorado      | `#C6A15B`   | Color de acento       |

La interfaz está inspirada en la estética de hoteles boutique de lujo de Manhattan, utilizando una combinación de fotografía, tipografía elegante, espacios amplios y una composición minimalista.

---

## Estructura del proyecto

La estructura del repositorio se organizará siguiendo una separación clara de responsabilidades y una organización coherente de paquetes y componentes.

```text
The-Royale/
│
├── README.md
├── docs/
│   ├── uml/
│   ├── database/
│   └── mockups/
│
├── frontend/
│
├── backend/
│
├── database/
│
└── tests/
```

La estructura puede evolucionar durante el desarrollo del proyecto según las necesidades técnicas de cada Sprint.

---

## Ramas

El repositorio utiliza diferentes ramas para organizar el desarrollo:

```text
main / master
    │
    └── development
            │
            ├── feature/...
            ├── feature/...
            └── feature/...
```

### `main`

Contiene las versiones estables del proyecto.

### `development`

Rama principal de desarrollo donde se integran las funcionalidades antes de llegar a producción.

### `feature/*`

Ramas utilizadas para desarrollar funcionalidades específicas.

---

## Estado del proyecto

El proyecto se encuentra actualmente en desarrollo como parte del semestre académico de la asignatura **Desarrollo Web**.

Las funcionalidades serán implementadas progresivamente siguiendo los diferentes Features y Sprints definidos para el proyecto.

---

## Contexto académico

Este proyecto forma parte del trabajo semestral de la asignatura **Desarrollo Web** de Ingeniería de Sistemas.

Durante el desarrollo se abordarán aspectos de:

* Diseño de interfaces web.
* Arquitectura de aplicaciones web.
* Desarrollo frontend.
* Desarrollo backend.
* Bases de datos.
* Autenticación y autorización.
* Gestión de reservas.
* Gestión de servicios.
* Pruebas automatizadas.
* Control de versiones con Git y GitHub.
* Diseño de software.

---

<p align="center">
  <strong>THE ROYALE</strong><br>
  <em>Stay Above the Ordinary.</em>
</p>
