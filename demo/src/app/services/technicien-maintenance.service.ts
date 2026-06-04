// src/app/services/technicien-maintenance.service.ts
import { environment } from '../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Maintenance,
  MaintenanceRequest,
  DetailMaintenance,
  StatutMaintenance,
  TypeIntervention
} from '../models/maintenance';

export interface MaintenanceDashboardTech {
  nbDossiers: number;
  totalHtva: number;
  nbEnCours: number;
  nbTermines: number;
  nbAnnules: number;
  parType: Record<string, number>;
  topVehicules: Record<string, number>;
  derniersDossiers: {
    id: number;
    numeroDossier: string;
    vehicule: string;
    type: string;
    statut: string;
    htva: number;
    date: string;
  }[];
}

@Injectable({ providedIn: 'root' })
export class TechnicienMaintenanceService {

  private api = `${environment.apiUrl}/technicien/maintenances`;

  constructor(private http: HttpClient) {}

  private h(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  private hBlob(): HttpHeaders {
    return new HttpHeaders({
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── CRUD ──────────────────────────────────────────────────

  getAll(): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(this.api, { headers: this.h() });
  }

  getById(id: number): Observable<Maintenance> {
    return this.http.get<Maintenance>(`${this.api}/${id}`, { headers: this.h() });
  }

  getByVehicule(matricule: string): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/vehicule/${matricule}`, { headers: this.h() });
  }

  getByStatut(statut: StatutMaintenance): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/statut/${statut}`, { headers: this.h() });
  }

  getByType(type: TypeIntervention): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>(`${this.api}/type/${type}`, { headers: this.h() });
  }

  creer(req: MaintenanceRequest): Observable<any> {
    return this.http.post(this.api, req, { headers: this.h() });
  }

  modifier(id: number, req: MaintenanceRequest): Observable<any> {
    return this.http.put(`${this.api}/${id}`, req, { headers: this.h() });
  }

  supprimer(id: number): Observable<any> {
    return this.http.delete(`${this.api}/${id}`, { headers: this.h() });
  }

  ajouterDetail(maintenanceId: number, detail: Partial<DetailMaintenance>): Observable<any> {
    return this.http.post(`${this.api}/${maintenanceId}/details`, detail, { headers: this.h() });
  }

  supprimerDetail(maintenanceId: number, detailId: number): Observable<any> {
    return this.http.delete(`${this.api}/${maintenanceId}/details/${detailId}`, { headers: this.h() });
  }

  getDashboard(): Observable<MaintenanceDashboardTech> {
    return this.http.get<MaintenanceDashboardTech>(`${this.api}/dashboard`, { headers: this.h() });
  }

  // ── Export Excel ─────────────────────────────────────────

  exporterExcelGlobal(zoneId?: number, statut?: string): Observable<Blob> {
    let params = new HttpParams();
    if (zoneId) params = params.set('zoneId', zoneId);
    if (statut) params = params.set('statut', statut);
    return this.http.get(`${this.api}/export/excel`, {
      headers: this.hBlob(),
      params,
      responseType: 'blob'
    });
  }

  exporterExcelDossier(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/${id}/export/excel`, {
      headers: this.hBlob(),
      responseType: 'blob'
    });
  }

  downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}