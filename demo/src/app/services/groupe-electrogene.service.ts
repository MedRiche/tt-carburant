// src/app/services/groupe-electrogene.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeElectrogene, GroupeElectrogeneRequest } from '../models/groupe-electrogene';
import { GestionCarburantGE, GestionCarburantGERequest, Semestre } from '../models/gestion-carburant-ge';

@Injectable({ providedIn: 'root' })
export class GroupeElectrogeneService {

  private baseUrl = 'http://localhost:8081/api/admin';
  private geUrl = `${this.baseUrl}/groupes-electrogenes`;
  private carbUrl = `${this.baseUrl}/carburant-ge`;

  constructor(private http: HttpClient) {}

  private headers(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── Groupe électrogène ─────────────────────────────────────────
  getAllGroupes(): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(this.geUrl, { headers: this.headers() });
  }

  getByZone(zoneId: number): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(`${this.geUrl}/zone/${zoneId}`, { headers: this.headers() });
  }

  getBySite(site: string): Observable<GroupeElectrogene> {
    return this.http.get<GroupeElectrogene>(`${this.geUrl}/${site}`, { headers: this.headers() });
  }

  creer(req: GroupeElectrogeneRequest): Observable<any> {
    return this.http.post(this.geUrl, req, { headers: this.headers() });
  }

  modifier(site: string, req: GroupeElectrogeneRequest): Observable<any> {
    return this.http.put(`${this.geUrl}/${site}`, req, { headers: this.headers() });
  }

  supprimer(site: string): Observable<any> {
    return this.http.delete(`${this.geUrl}/${site}`, { headers: this.headers() });
  }

  // ── Gestion carburant GE ───────────────────────────────────────
  getAllSaisies(): Observable<GestionCarburantGE[]> {
    return this.http.get<GestionCarburantGE[]>(this.carbUrl, { headers: this.headers() });
  }

  getBySiteHistorique(site: string): Observable<GestionCarburantGE[]> {
    return this.http.get<GestionCarburantGE[]>(`${this.carbUrl}/site/${site}`, { headers: this.headers() });
  }

  getByPeriode(annee: number, semestre: Semestre): Observable<GestionCarburantGE[]> {
    const params = new HttpParams().set('annee', annee).set('semestre', semestre);
    return this.http.get<GestionCarburantGE[]>(`${this.carbUrl}/periode`, { headers: this.headers(), params });
  }

  getByZonePeriode(zoneId: number, annee: number, semestre: Semestre): Observable<GestionCarburantGE[]> {
    const params = new HttpParams().set('annee', annee).set('semestre', semestre);
    return this.http.get<GestionCarburantGE[]>(`${this.carbUrl}/zone/${zoneId}/periode`, { headers: this.headers(), params });
  }

  saisir(req: GestionCarburantGERequest): Observable<any> {
    return this.http.post(this.carbUrl, req, { headers: this.headers() });
  }

  modifierSaisie(id: number, req: GestionCarburantGERequest): Observable<any> {
    return this.http.put(`${this.carbUrl}/${id}`, req, { headers: this.headers() });
  }

  supprimerSaisie(id: number): Observable<any> {
    return this.http.delete(`${this.carbUrl}/${id}`, { headers: this.headers() });
  }

  // Import Excel
  importerExcel(file: File, zoneNom?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    if (zoneNom) formData.append('zoneNom', zoneNom);
    return this.http.post(`${this.geUrl}/import`, formData, {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${localStorage.getItem('token')}` })
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