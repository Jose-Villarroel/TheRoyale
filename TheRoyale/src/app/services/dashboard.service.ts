import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardMetricas } from '../models/dashboard.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly url = `${environment.apiUrl}/admin/dashboard`;

  constructor(private http: HttpClient) {}

  obtenerMetricas(): Observable<DashboardMetricas> {
    return this.http.get<DashboardMetricas>(this.url);
  }
}
