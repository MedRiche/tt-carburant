// src/app/services/maintenance.service.ts
// VERSION CORRIGÉE - Compatible avec le backend existant
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Maintenance, MaintenanceRequest, GlobalVehicleListItem,
  MaintenanceDashboard, DetailMaintenance, StatutMaintenance, TypeIntervention
} from '../models/maintenance';

@Injectable({ providedIn: 'root' })
export class MaintenanceService {

  private api = 'http://localhost:8081/api/admin/maintenances';

  constructor(private http: HttpClient) {}

  private h(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── CRUD ──────────────────────────────────────────────────────────────

  creer(req: MaintenanceRequest): Observable<any> {
    return this.http.post(this.api, req, { headers: this.h() });
  }

  getAll(): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(this.api, { headers: this.h() });
  }

  getById(id: number): Observable<Maintenance> {
    return this.http.get<Maintenance>(`${this.api}/${id}`, { headers: this.h() });
  }

  modifier(id: number, req: MaintenanceRequest): Observable<any> {
    return this.http.put(`${this.api}/${id}`, req, { headers: this.h() });
  }

  supprimer(id: number): Observable<any> {
    return this.http.delete(`${this.api}/${id}`, { headers: this.h() });
  }

  // ── Filtres ───────────────────────────────────────────────────────────

  getByVehicule(matricule: string): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/vehicule/${matricule}`, { headers: this.h() });
  }

  getByZone(zoneId: number): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/zone/${zoneId}`, { headers: this.h() });
  }

  getByStatut(statut: StatutMaintenance): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/statut/${statut}`, { headers: this.h() });
  }

  getByType(type: TypeIntervention): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/type/${type}`, { headers: this.h() });
  }

  search(q: string): Observable<Maintenance[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Maintenance[]>(`${this.api}/search`, { headers: this.h(), params });
  }

  // ── Global Vehicle List ───────────────────────────────────────────────
  // Le backend retourne List<Map<String, Object>> — on utilise any[]
  getGlobalVehicleList(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/global-list`, { headers: this.h() });
  }

  // ── Détails ───────────────────────────────────────────────────────────

  ajouterDetail(maintenanceId: number, detail: DetailMaintenance): Observable<any> {
    return this.http.post(`${this.api}/${maintenanceId}/details`, detail, { headers: this.h() });
  }

  supprimerDetail(maintenanceId: number, detailId: number): Observable<any> {
    return this.http.delete(`${this.api}/${maintenanceId}/details/${detailId}`, { headers: this.h() });
  }

  // ── Analytics ─────────────────────────────────────────────────────────

  getDashboard(): Observable<MaintenanceDashboard> {
    return this.http.get<MaintenanceDashboard>(`${this.api}/analytics/dashboard`, { headers: this.h() });
  }

  // ── Import / Export ───────────────────────────────────────────────────

  importerDataset(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.api}/import`, formData, {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${localStorage.getItem('token')}` })
    });
  }

  exporterExcel(zoneId?: number): Observable<Blob> {
    let params = new HttpParams();
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get(`${this.api}/export/excel`, {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${localStorage.getItem('token')}` }),
      params,
      responseType: 'blob'
    });
  }

  downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    window.URL.revokeObjectURL(url);
  }
}