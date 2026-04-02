// src/app/services/carburant-analytics.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HistoriqueRavitaillement, EvolutionData,
  Anomalie, ComparaisonData, DashboardCarburant
} from '../models/carburant-analytics';

@Injectable({ providedIn: 'root' })
export class CarburantAnalyticsService {

  private api = 'http://localhost:8081/api/admin/carburant-analytics';

  constructor(private http: HttpClient) {}

  private h(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── 1. Historique ──────────────────────────────────────────────────────────
  getHistorique(matricule: string, annee?: number): Observable<HistoriqueRavitaillement[]> {
    let params = new HttpParams();
    if (annee) params = params.set('annee', annee);
    return this.http.get<HistoriqueRavitaillement[]>(
      `${this.api}/historique/${matricule}`, { headers: this.h(), params });
  }

  getHistoriqueZone(zoneId: number, annee: number): Observable<HistoriqueRavitaillement[]> {
    const params = new HttpParams().set('annee', annee);
    return this.http.get<HistoriqueRavitaillement[]>(
      `${this.api}/historique/zone/${zoneId}`, { headers: this.h(), params });
  }

  // ── 2. Évolution ──────────────────────────────────────────────────────────
  getEvolution(matricule: string, annee: number): Observable<EvolutionData> {
    const params = new HttpParams().set('annee', annee);
    return this.http.get<EvolutionData>(
      `${this.api}/evolution/${matricule}`, { headers: this.h(), params });
  }

  getEvolutionZone(zoneId: number, annee: number): Observable<EvolutionData> {
    const params = new HttpParams().set('annee', annee);
    return this.http.get<EvolutionData>(
      `${this.api}/evolution/zone/${zoneId}`, { headers: this.h(), params });
  }

  // ── 3. Anomalies ──────────────────────────────────────────────────────────
  getAnomalies(annee: number, mois: number, zoneId?: number): Observable<Anomalie[]> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<Anomalie[]>(`${this.api}/anomalies`, { headers: this.h(), params });
  }

  getAnomaliesAnnee(annee: number, zoneId?: number): Observable<Anomalie[]> {
    let params = new HttpParams();
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<Anomalie[]>(
      `${this.api}/anomalies/annee/${annee}`, { headers: this.h(), params });
  }

  // ── 4. Comparaison ────────────────────────────────────────────────────────
  getComparaison(annee: number, mois: number, zoneId?: number): Observable<ComparaisonData> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<ComparaisonData>(`${this.api}/comparaison`, { headers: this.h(), params });
  }

  getComparaisonAnnuelle(annee: number, zoneId?: number): Observable<ComparaisonData> {
    let params = new HttpParams().set('annee', annee);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<ComparaisonData>(
      `${this.api}/comparaison/annuel`, { headers: this.h(), params });
  }

  // ── 5. Dashboard avancé ───────────────────────────────────────────────────
  getDashboard(annee: number, mois: number, zoneId?: number): Observable<DashboardCarburant> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<DashboardCarburant>(`${this.api}/dashboard`, { headers: this.h(), params });
  }

  getDashboardAnnuel(annee: number, zoneId?: number): Observable<DashboardCarburant> {
    let params = new HttpParams().set('annee', annee);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<DashboardCarburant>(
      `${this.api}/dashboard/annuel`, { headers: this.h(), params });
  }
}