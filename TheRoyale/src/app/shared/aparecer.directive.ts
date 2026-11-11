import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

// Añade la clase "visible" cuando el elemento entra en pantalla, para la animación de aparición.
// Antes esto vivía en LandingComponent con un querySelectorAll global; ahora cada elemento se
// observa a sí mismo, así que funciona igual dentro de cualquier componente.
@Directive({
  selector: '[appAparecer]',
  standalone: true,
  host: { class: 'aparecer' }
})
export class AparecerDirective implements AfterViewInit, OnDestroy {

  private elemento = inject(ElementRef<HTMLElement>);
  private observador?: IntersectionObserver;

  ngAfterViewInit(): void {
    this.observador = new IntersectionObserver(
      entradas => {
        entradas.forEach(entrada => {
          if (entrada.isIntersecting) {
            entrada.target.classList.add('visible');
            this.observador?.unobserve(entrada.target);
          }
        });
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.08 }
    );

    this.observador.observe(this.elemento.nativeElement);
  }

  ngOnDestroy(): void {
    this.observador?.disconnect();
  }
}
