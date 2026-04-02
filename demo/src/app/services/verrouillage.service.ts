// src/app/services/verrouillage.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VerrouillageDto, HistoriqueModificationDto } from '../models/verrouillage';

@Injectable({ providedIn: 'root' })
export class VerrouillageService {

  private apiVerr  = 'http://localhost:8081/api/admin/verrouillage-carburant';
  private apiHisto = 'http://localhost:8081/api/admin/historique-carburant';

  constructor(private http: HttpClient) {}

  private h(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── Verrouillage ───────────────────────────────────────────────

  getStatut(annee: number, mois: number, zoneId?: number): Observable<VerrouillageDto> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<VerrouillageDto>(`${this.apiVerr}/statut`, { headers: this.h(), params });
  }

  isVerrouille(annee: number, mois: number, zoneId?: number): Observable<{ verrouille: boolean }> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<{ verrouille: boolean }>(`${this.apiVerr}/check`, { headers: this.h(), params });
  }

  verrouiller(annee: number, mois: number, zoneId?: number): Observable<any> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.post(`${this.apiVerr}/verrouiller`, {}, { headers: this.h(), params });
  }

  deverrouiller(annee: number, mois: number, zoneId?: number): Observable<any> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.post(`${this.apiVerr}/deverrouiller`, {}, { headers: this.h(), params });
  }

  getVerrouxParAnnee(annee: number): Observable<VerrouillageDto[]> {
    return this.http.get<VerrouillageDto[]>(`${this.apiVerr}/annee/${annee}`, { headers: this.h() });
  }

  getTousActifs(): Observable<VerrouillageDto[]> {
    return this.http.get<VerrouillageDto[]>(`${this.apiVerr}/actifs`, { headers: this.h() });
  }

  // ── Historique ─────────────────────────────────────────────────

  getToutHistorique(): Observable<HistoriqueModificationDto[]> {
    return this.http.get<HistoriqueModificationDto[]>(this.apiHisto, { headers: this.h() });
  }

  getHistoriqueVehicule(matricule: string, annee?: number): Observable<HistoriqueModificationDto[]> {
    let params = new HttpParams();
    if (annee) params = params.set('annee', annee);
    return this.http.get<HistoriqueModificationDto[]>(
      `${this.apiHisto}/vehicule/${matricule}`, { headers: this.h(), params });
  }

  getHistoriquePeriode(annee: number, mois: number): Observable<HistoriqueModificationDto[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<HistoriqueModificationDto[]>(
      `${this.apiHisto}/periode`, { headers: this.h(), params });
  }

  getHistoriqueSaisie(id: number): Observable<HistoriqueModificationDto[]> {
    return this.http.get<HistoriqueModificationDto[]>(
      `${this.apiHisto}/saisie/${id}`, { headers: this.h() });
  }
}