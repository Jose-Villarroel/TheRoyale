import { Component, ElementRef, computed, input, model, output, viewChildren } from '@angular/core';

type Relleno = 'llena' | 'media' | 'vacia';

// Estrellas doradas reutilizables.
// - Modo lectura ([valor]): pinta la calificación; admite medias estrellas para los promedios.
// - Modo entrada ([(valor)] con modo="entrada"): grupo de radios accesible con teclado
//   (flechas, Inicio, Fin) donde cada estrella es un botón "Rate N of 5" con id resena-estrella-N.
@Component({
  selector: 'app-estrellas',
  standalone: true,
  templateUrl: './estrellas.component.html'
})
export class EstrellasComponent {
  valor = model<number | null>(null);
  modo = input<'lectura' | 'entrada'>('lectura');
  tamano = input<'sm' | 'md' | 'lg'>('md');
  // Texto del grupo en modo entrada (lo lee el lector de pantalla)
  etiqueta = input<string>('Rating');
  // Avisa al padre que el usuario ya interactuó, para marcar el control como tocado
  tocado = output<void>();

  readonly posiciones = [1, 2, 3, 4, 5];
  private botones = viewChildren<ElementRef<HTMLButtonElement>>('estrella');

  // Redondeo a la media estrella más cercana: 4.3 -> 4.5, 4.2 -> 4
  readonly rellenos = computed<Relleno[]>(() => {
    const redondeado = Math.round((this.valor() ?? 0) * 2) / 2;
    return this.posiciones.map(n =>
      redondeado >= n ? 'llena' : redondeado >= n - 0.5 ? 'media' : 'vacia');
  });

  readonly textoLectura = computed(() => {
    const v = this.valor();
    return v === null || v === undefined ? 'Not rated yet' : `Rated ${v} out of 5`;
  });

  // Roving tabindex: solo la estrella elegida (o la primera) entra en el orden de tabulación
  indiceTab(n: number): number {
    const v = this.valor();
    return (v ? v === n : n === 1) ? 0 : -1;
  }

  elegir(n: number): void {
    this.valor.set(n);
    this.tocado.emit();
  }

  alPulsarTecla(evento: KeyboardEvent): void {
    const actual = this.valor() ?? 0;
    let siguiente: number | null = null;
    switch (evento.key) {
      case 'ArrowRight':
      case 'ArrowUp':
        siguiente = Math.min(5, actual + 1);
        break;
      case 'ArrowLeft':
      case 'ArrowDown':
        siguiente = Math.max(1, actual - 1);
        break;
      case 'Home':
        siguiente = 1;
        break;
      case 'End':
        siguiente = 5;
        break;
    }
    if (siguiente === null) {
      return;
    }
    evento.preventDefault();
    this.elegir(siguiente);
    this.botones()[siguiente - 1]?.nativeElement.focus();
  }
}
