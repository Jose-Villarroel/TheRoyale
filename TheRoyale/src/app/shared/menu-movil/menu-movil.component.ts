import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-menu-movil',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './menu-movil.component.html'
})
export class MenuMovilComponent {

  @Input() abierto = false;
  @Output() cerrar = new EventEmitter<void>();
}
