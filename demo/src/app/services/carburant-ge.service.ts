import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GestionCarburantGE, GestionCarburantGERequest, Semestre } from '../models/gestion-carburant-ge';

@Injectable({
  providedIn: 'root'
})
export class CarburantGeService {


  private carbUrl = 'http://localhost:8081/api/admin/carburant-ge';

  constructor(private http: HttpClient) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }

  // ── Saisies carburant GE ───────────────────────────────────────

  getAllSaisies(): Observable<GestionCarburantGE[]> {
    return this.http.get<GestionCarburantGE[]>(this.carbUrl, { headers: this.authHeaders() });
  }

  getBySiteHistorique(site: string): Observable<GestionCarburantGE[]> {
    return this.http.get<GestionCarburantGE[]>(
      `${this.carbUrl}/site/${encodeURIComponent(site)}`,
      { headers: this.authHeaders() }
    );
  }

  getByPeriode(annee: number, semestre: Semestre): Observable<GestionCarburantGE[]> {
    const params = new HttpParams().set('annee', annee).set('semestre', semestre);
    return this.http.get<GestionCarburantGE[]>(
      `${this.carbUrl}/periode`,
      { headers: this.authHeaders(), params }
    );
  }

  getByZonePeriode(zoneId: number, annee: number, semestre: Semestre): Observable<GestionCarburantGE[]> {
    const params = new HttpParams().set('annee', annee).set('semestre', semestre);
    return this.http.get<GestionCarburantGE[]>(
      `${this.carbUrl}/zone/${zoneId}/periode`,
      { headers: this.authHeaders(), params }
    );
  }

  saisir(req: GestionCarburantGERequest): Observable<any> {
    return this.http.post(this.carbUrl, req, { headers: this.authHeaders() });
  }

  modifierSaisie(id: number, req: GestionCarburantGERequest): Observable<any> {
    return this.http.put(`${this.carbUrl}/${id}`, req, { headers: this.authHeaders() });
  }

  supprimerSaisie(id: number): Observable<any> {
    return this.http.delete(`${this.carbUrl}/${id}`, { headers: this.authHeaders() });
  }
}