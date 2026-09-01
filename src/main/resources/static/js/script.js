// ===== Referencias generales del DOM =====
const envoltorioCabecera = document.getElementById("envoltorioCabecera");
const heroMedia = document.getElementById("heroMedia");
const experienciasMedia = document.getElementById("experienciasMedia");
const menuToggle = document.getElementById("menuToggle");
const menuMovil = document.getElementById("menuMovil");
const menuCerrar = document.getElementById("menuCerrar");

// ===== Función: transición del header al hacer scroll (aparece sólido) =====
function actualizarCabeceraEnScroll() {
  const y = window.scrollY || 0;

  if (envoltorioCabecera) {
    if (y > 90) {
      envoltorioCabecera.classList.add("cabecera-solida");
    } else {
      envoltorioCabecera.classList.remove("cabecera-solida");
    }
  }

  // Efecto parallax suave en la imagen del hero (solo existe en la landing)
  if (heroMedia) {
    const desplazamiento = Math.min(y * 0.13, 120);
    heroMedia.style.transform = "translateY(" + desplazamiento + "px)";
  }

  // Efecto parallax suave en el fondo de experiencias (solo existe en la landing)
  if (experienciasMedia && experienciasMedia.parentElement) {
    const rect = experienciasMedia.parentElement.getBoundingClientRect();
    experienciasMedia.style.transform = "translateY(" + (-rect.top * 0.045) + "px)";
  }
}

// ===== Función: abrir/cerrar el menú móvil =====
function abrirMenuMovil() {
  if (menuMovil) {
    menuMovil.classList.add("abierto");
  }
}

function cerrarMenuMovil() {
  if (menuMovil) {
    menuMovil.classList.remove("abierto");
  }
}

// ===== Función: animación de aparición al hacer scroll (IntersectionObserver) =====
function activarAnimacionesAparicion() {
  const elementos = document.querySelectorAll(".aparecer");

  if (elementos.length === 0) {
    return;
  }

  const observador = new IntersectionObserver(
    function (entradas) {
      entradas.forEach(function (entrada) {
        if (entrada.isIntersecting) {
          entrada.target.classList.add("visible");
          observador.unobserve(entrada.target);
        }
      });
    },
    { rootMargin: "0px 0px -8% 0px", threshold: 0.08 }
  );

  elementos.forEach(function (elemento) {
    observador.observe(elemento);
  });
}

// ===== Carrusel de habitaciones (solo existe en la landing) =====
const carruselTrack = document.getElementById("carruselTrack");
const carruselProgreso = document.getElementById("carruselProgreso");
const carruselContador = document.getElementById("carruselContador");
const botonCarruselAnterior = document.getElementById("carruselAnterior");
const botonCarruselSiguiente = document.getElementById("carruselSiguiente");
const totalHabitaciones = document.querySelectorAll(".habitacion-slide").length;
let habitacionActual = 0;

function actualizarCarrusel() {
  if (!carruselTrack || !carruselProgreso || !carruselContador) {
    return;
  }

  carruselTrack.style.transform = "translateX(-" + (habitacionActual * 100) + "%)";
  carruselProgreso.style.transform = "translateX(" + (habitacionActual * 100) + "%)";

  const numeroActual = String(habitacionActual + 1).padStart(2, "0");
  const numeroTotal = String(totalHabitaciones).padStart(2, "0");
  carruselContador.textContent = numeroActual + " / " + numeroTotal;
}

function irHabitacionAnterior() {
  habitacionActual = (habitacionActual - 1 + totalHabitaciones) % totalHabitaciones;
  actualizarCarrusel();
}

function irHabitacionSiguiente() {
  habitacionActual = (habitacionActual + 1) % totalHabitaciones;
  actualizarCarrusel();
}

// ===== Función: manejar el envío del formulario de reserva (solo en la landing) =====
function manejarFormularioReserva(evento) {
  evento.preventDefault();
  window.location.href = "/login";
}

// ===== Función: manejar el envío del formulario de newsletter (en todas las páginas con footer) =====
function manejarFormularioNewsletter(evento) {
  evento.preventDefault();
  alert("Thank you for subscribing to The Royale newsletter.");
  evento.target.reset();
}

// ===== Event listeners (todos con validación de existencia) =====
window.addEventListener("scroll", actualizarCabeceraEnScroll, { passive: true });

if (menuToggle) {
  menuToggle.addEventListener("click", abrirMenuMovil);
}

if (menuCerrar) {
  menuCerrar.addEventListener("click", cerrarMenuMovil);
}

if (botonCarruselAnterior) {
  botonCarruselAnterior.addEventListener("click", irHabitacionAnterior);
}

if (botonCarruselSiguiente) {
  botonCarruselSiguiente.addEventListener("click", irHabitacionSiguiente);
}

const formularioReserva = document.getElementById("formularioReserva");
if (formularioReserva) {
  formularioReserva.addEventListener("submit", manejarFormularioReserva);
}

const formularioNewsletter = document.getElementById("formularioNewsletter");
if (formularioNewsletter) {
  formularioNewsletter.addEventListener("submit", manejarFormularioNewsletter);
}

document.querySelectorAll(".enlace-menu-movil, .menu-movil-pie a").forEach(function (enlace) {
  enlace.addEventListener("click", cerrarMenuMovil);
});

document.addEventListener("keydown", function (evento) {
  if (evento.key === "Escape") {
    cerrarMenuMovil();
  }
});

// ===== Inicialización =====
actualizarCabeceraEnScroll();
activarAnimacionesAparicion();
actualizarCarrusel();
