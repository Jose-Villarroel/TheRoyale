// ===== Referencias generales del DOM =====
const envoltorioCabecera = document.getElementById("envoltorioCabecera");
const heroMedia = document.getElementById("heroMedia");
const experienciasMedia = document.getElementById("experienciasMedia");
const menuMovil = document.getElementById("menuMovil");

// Instancia Bootstrap Offcanvas para el menú móvil
let offcanvasMenu = null;
if (menuMovil && typeof bootstrap !== "undefined") {
  offcanvasMenu = bootstrap.Offcanvas.getOrCreateInstance(menuMovil);
}

// ===== Función: transición del header al hacer scroll (aparece sólido) =====
function actualizarCabeceraEnScroll() {
  const y = window.scrollY || 0;

  if (y > 90) {
    envoltorioCabecera.classList.add("cabecera-solida");
  } else {
    envoltorioCabecera.classList.remove("cabecera-solida");
  }

  // Efecto parallax suave en la imagen del hero
  if (heroMedia) {
    const desplazamiento = Math.min(y * 0.13, 120);
    heroMedia.style.transform = "translateY(" + desplazamiento + "px)";
  }

  // Efecto parallax suave en el fondo de la sección de experiencias
  if (experienciasMedia) {
    const rect = experienciasMedia.parentElement.getBoundingClientRect();
    experienciasMedia.style.transform = "translateY(" + (-rect.top * 0.045) + "px)";
  }
}

// ===== Función: cerrar el menú móvil (Bootstrap Offcanvas) =====
function cerrarMenuMovil() {
  if (offcanvasMenu) {
    offcanvasMenu.hide();
  }
}

// ===== Función: animación de aparición al hacer scroll (IntersectionObserver) =====
function activarAnimacionesAparicion() {
  const elementos = document.querySelectorAll(".aparecer");

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

// ===== Carrusel de habitaciones =====
const carruselTrack = document.getElementById("carruselTrack");
const carruselProgreso = document.getElementById("carruselProgreso");
const carruselContador = document.getElementById("carruselContador");
const totalHabitaciones = document.querySelectorAll(".habitacion-slide").length;
let habitacionActual = 0;

// ===== Función: mostrar la habitación según el índice actual =====
function actualizarCarrusel() {
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

// ===== Función: manejar el envío del formulario de reserva =====
function manejarFormularioReserva(evento) {
  evento.preventDefault();
  // La confirmación de la reserva requiere haber iniciado sesión
  window.location.href = "login.html";
}

// ===== Función: manejar el envío del formulario de newsletter =====
function manejarFormularioNewsletter(evento) {
  evento.preventDefault();
  alert("Thank you for subscribing to The Royale newsletter.");
  evento.target.reset();
}

// ===== Event listeners =====
window.addEventListener("scroll", actualizarCabeceraEnScroll, { passive: true });
document.getElementById("carruselAnterior").addEventListener("click", irHabitacionAnterior);
document.getElementById("carruselSiguiente").addEventListener("click", irHabitacionSiguiente);
document.getElementById("formularioReserva").addEventListener("submit", manejarFormularioReserva);
document.getElementById("formularioNewsletter").addEventListener("submit", manejarFormularioNewsletter);

// Cierra el menú móvil automáticamente al hacer clic en cualquiera de sus enlaces
document.querySelectorAll(".enlace-menu-movil, .menu-movil-pie a").forEach(function (enlace) {
  enlace.addEventListener("click", cerrarMenuMovil);
});

// ===== Inicialización =====
actualizarCabeceraEnScroll();
activarAnimacionesAparicion();
actualizarCarrusel();
