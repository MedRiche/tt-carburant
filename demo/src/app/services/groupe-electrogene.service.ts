// src/app/services/groupe-electrogene.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeElectrogene, GroupeElectrogeneRequest } from '../models/groupe-electrogene';
import { GestionCarburantGE, GestionCarburantGERequest, Semestre } from '../models/gestion-carburant-ge';

@Injectable({ providedIn: 'root' })
export class GroupeElectrogeneService {

  private baseUrl = 'http://localhost:8081/api/admin';
  private geUrl   = `${this.baseUrl}/groupes-electrogenes`;
  private carbUrl = `${this.baseUrl}/carburant-ge`;

  constructor(private http: HttpClient) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── Groupe électrogène CRUD ────────────────────────────────────

  getAllGroupes(): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(this.geUrl, { headers: this.authHeaders() });
  }

  getBySite(site: string): Observable<GroupeElectrogene> {
    return this.http.get<GroupeElectrogene>(`${this.geUrl}/${encodeURIComponent(site)}`,
      { headers: this.authHeaders() });
  }

  getByZone(zoneId: number): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(`${this.geUrl}/zone/${zoneId}`,
      { headers: this.authHeaders() });
  }

  creer(req: GroupeElectrogeneRequest): Observable<GroupeElectrogene> {
    return this.http.post<GroupeElectrogene>(this.geUrl, req, { headers: this.authHeaders() });
  }

  modifier(site: string, req: GroupeElectrogeneRequest): Observable<GroupeElectrogene> {
    return this.http.put<GroupeElectrogene>(
      `${this.geUrl}/${encodeURIComponent(site)}`, req, { headers: this.authHeaders() });
  }

  supprimer(site: string): Observable<any> {
    return this.http.delete(`${this.geUrl}/${encodeURIComponent(site)}`,
      { headers: this.authHeaders() });
  }

  // ── Import Excel ───────────────────────────────────────────────

  importerExcel(file: File, zoneNom?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    if (zoneNom) formData.append('zoneNom', zoneNom);
    return this.http.post(`${this.carbUrl}/import`, formData, {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${localStorage.getItem('token')}` })
    });
  }

  // ── Saisies carburant GE ───────────────────────────────────────

  getAllSaisies(): Observable<GestionCarburantGE[]> {
    return this.http.get<GestionCarburantGE[]>(this.carbUrl, { headers: this.authHeaders() });
  }

  getBySiteHistorique(site: string): Observable<GestionCarburantGE[]> {
    return this.http.get<GestionCarburantGE[]>(
      `${this.carbUrl}/site/${encodeURIComponent(site)}`, { headers: this.authHeaders() });
  }

  getByPeriode(annee: number, semestre: Semestre): Observable<GestionCarburantGE[]> {
    const params = new HttpParams().set('annee', annee).set('semestre', semestre);
    return this.http.get<GestionCarburantGE[]>(`${this.carbUrl}/periode`,
      { headers: this.authHeaders(), params });
  }

  getByZonePeriode(zoneId: number, annee: number, semestre: Semestre): Observable<GestionCarburantGE[]> {
    const params = new HttpParams().set('annee', annee).set('semestre', semestre);
    return this.http.get<GestionCarburantGE[]>(
      `${this.carbUrl}/zone/${zoneId}/periode`, { headers: this.authHeaders(), params });
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

  downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}