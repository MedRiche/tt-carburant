// src/app/services/technicien-equipement.service.ts
import { environment } from '../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Vehicule } from '../models/vehicule';
import { GroupeElectrogene } from '../models/groupe-electrogene';

export interface VehiculeStats {
  totalVehicules: number;
  parTypeCarburant: Record<string, number>;
  parTypeVehicule: Record<string, number>;
  visitesDepassees: number;
  kilometrageTotalCumul: number;
  coutTotalMois: number;
  estConducteur?: boolean;
}

export interface GEStats {
  totalGroupes: number;
  parTypeCarburant: Record<string, number>;
  puissanceTotaleKVA: number;
  cartesExpirees: number;
}

@Injectable({ providedIn: 'root' })
export class TechnicienEquipementService {

  private baseUrl = `${environment.apiUrl}/technicien`;

  constructor(private http: HttpClient) {}

  private h(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  /** Retourne true si l'utilisateur connecté est un conducteur */
  get estConducteur(): boolean {
    const specialite = localStorage.getItem('specialite') || '';
    return specialite.toLowerCase() === 'conducteur';
  }

  // ── Véhicules ────────────────────────────────────────────────
  // L'API /api/technicien/vehicules gère automatiquement le filtre
  // côté backend selon le rôle (conducteur vs technicien).

  getMesVehicules(): Observable<Vehicule[]> {
    return this.http.get<Vehicule[]>(`${this.baseUrl}/vehicules`, { headers: this.h() });
  }

  getVehiculesByZone(zoneId: number): Observable<Vehicule[]> {
    return this.http.get<Vehicule[]>(`${this.baseUrl}/vehicules/zone/${zoneId}`, { headers: this.h() });
  }

  getVehiculeDetail(matricule: string): Observable<Vehicule> {
    return this.http.get<Vehicule>(`${this.baseUrl}/vehicules/${matricule}`, { headers: this.h() });
  }

  getVehiculeStats(): Observable<VehiculeStats> {
    return this.http.get<VehiculeStats>(`${this.baseUrl}/vehicules/stats`, { headers: this.h() });
  }

  // ── Groupes Électrogènes ─────────────────────────────────────

  getMesGroupes(): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(`${this.baseUrl}/groupes-electrogenes`, { headers: this.h() });
  }

  getGroupesByZone(zoneId: number): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(`${this.baseUrl}/groupes-electrogenes/zone/${zoneId}`, { headers: this.h() });
  }

  getGroupeDetail(site: string): Observable<GroupeElectrogene> {
    return this.http.get<GroupeElectrogene>(
      `${this.baseUrl}/groupes-electrogenes/${encodeURIComponent(site)}`, { headers: this.h() });
  }

  getGEStats(): Observable<GEStats> {
    return this.http.get<GEStats>(`${this.baseUrl}/groupes-electrogenes/stats`, { headers: this.h() });
  }
}