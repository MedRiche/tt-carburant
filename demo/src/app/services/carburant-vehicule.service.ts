// src/app/services/carburant-vehicule.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarburantVehicule, CarburantPrefill } from '../models/carburant-vehicule';

@Injectable({ providedIn: 'root' })
export class CarburantVehiculeService {

  private api = 'http://localhost:8081/api/admin/carburant-vehicules';

  constructor(private http: HttpClient) {}

  private h(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // ── Lecture ──────────────────────────────────────────────────

  getByVehicule(matricule: string): Observable<CarburantVehicule[]> {
    return this.http.get<CarburantVehicule[]>(`${this.api}/vehicule/${matricule}`, { headers: this.h() });
  }

  getByPeriode(annee: number, mois: number): Observable<CarburantVehicule[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<CarburantVehicule[]>(`${this.api}/periode`, { headers: this.h(), params });
  }

  getByZoneAndPeriode(zoneId: number, annee: number, mois: number): Observable<CarburantVehicule[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<CarburantVehicule[]>(`${this.api}/zone/${zoneId}/periode`, { headers: this.h(), params });
  }

  // ── NOUVEAU : pré-remplissage règles 6 & 7 ──────────────────

  getPrefill(matricule: string, annee: number, mois: number): Observable<CarburantPrefill> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<CarburantPrefill>(`${this.api}/prefill/${matricule}`, { headers: this.h(), params });
  }

  // ── NOUVEAU : récapitulatif annuel ───────────────────────────

  getRecapAnnuelVehicule(matricule: string, annee: number): Observable<CarburantVehicule[]> {
    const params = new HttpParams().set('annee', annee);
    return this.http.get<CarburantVehicule[]>(`${this.api}/recap/vehicule/${matricule}`, { headers: this.h(), params });
  }

  getRecapAnnuelZone(zoneId: number, annee: number): Observable<CarburantVehicule[]> {
    const params = new HttpParams().set('annee', annee);
    return this.http.get<CarburantVehicule[]>(`${this.api}/recap/zone/${zoneId}`, { headers: this.h(), params });
  }

  // ── NOUVEAU : alertes budget dépassé ─────────────────────────

  getBudgetDepasses(annee: number, mois: number): Observable<CarburantVehicule[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<CarburantVehicule[]>(`${this.api}/alertes/budget`, { headers: this.h(), params });
  }

  // ── NOUVEAU : export Excel ────────────────────────────────────

  exportExcelMensuel(annee: number, mois: number, zoneId?: number): Observable<Blob> {
    let params = new HttpParams().set('annee', annee).set('mois', mois);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get(`${this.api}/export/excel/mensuel`, {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${localStorage.getItem('token')}` }),
      params,
      responseType: 'blob'
    });
  }

  exportExcelAnnuel(annee: number, zoneId?: number): Observable<Blob> {
    let params = new HttpParams().set('annee', annee);
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get(`${this.api}/export/excel/annuel`, {
      headers: new HttpHeaders({ 'Authorization': `Bearer ${localStorage.getItem('token')}` }),
      params,
      responseType: 'blob'
    });
  }

  // ── CRUD ──────────────────────────────────────────────────────

  saisir(req: Partial<CarburantVehicule>): Observable<any> {
    return this.http.post(this.api, req, { headers: this.h() });
  }

  modifier(id: number, req: Partial<CarburantVehicule>): Observable<any> {
    return this.http.put(`${this.api}/${id}`, req, { headers: this.h() });
  }

  supprimer(id: number): Observable<any> {
    return this.http.delete(`${this.api}/${id}`, { headers: this.h() });
  }

  // ── Helper téléchargement ─────────────────────────────────────

  downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }


  // Dashboard
getDashboardKpi(annee: number, mois?: number): Observable<any> {
  const params = { annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any>(`${this.api}/analytics/dashboard`, { params });
}

getEvolutionMensuelleConsommation(annee: number): Observable<any> {
  return this.http.get<any>(`${this.api}/analytics/evolution-conso/${annee}`);
}

getEvolutionMensuelleCouts(annee: number): Observable<any> {
  return this.http.get<any>(`${this.api}/analytics/evolution-couts/${annee}`);
}

getRepartitionDepensesParZone(annee: number, mois?: number): Observable<any> {
  const params = { annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any>(`${this.api}/analytics/repartition-zones`, { params });
}

getRepartitionConsommationParTypeCarburant(annee: number, mois?: number): Observable<any> {
  const params = { annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any>(`${this.api}/analytics/repartition-carburant`, { params });
}

// Véhicules
getTopVehiculesConsommateurs(periode: 'mois'|'trimestre'|'annee', annee: number, mois?: number): Observable<any[]> {
  const params = { periode, annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any[]>(`${this.api}/analytics/top-consommateurs`, { params });
}

getTopVehiculesCouteux(periode: 'mois'|'trimestre'|'annee', annee: number, mois?: number): Observable<any[]> {
  const params = { periode, annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any[]>(`${this.api}/analytics/top-couteux`, { params });
}

getTopVehiculesRendement(periode: 'mois'|'trimestre'|'annee', annee: number, mois?: number): Observable<any[]> {
  const params = { periode, annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any[]>(`${this.api}/analytics/top-rendement`, { params });
}

getTopVehiculesKilometrage(periode: 'mois'|'trimestre'|'annee', annee: number, mois?: number): Observable<any[]> {
  const params = { periode, annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any[]>(`${this.api}/analytics/top-kilometrage`, { params });
}

getVehiculeAnalytics(matricule: string, annee: number): Observable<any> {
  return this.http.get<any>(`${this.api}/analytics/vehicule/${matricule}/${annee}`);
}

// Zones
getZoneStats(annee: number, mois?: number): Observable<any[]> {
  const params = { annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any[]>(`${this.api}/analytics/zone-stats`, { params });
}

getComparaisonZones(annee: number, mois?: number): Observable<any[]> {
  const params = { annee: annee.toString(), mois: mois?.toString() || '' };
  return this.http.get<any[]>(`${this.api}/analytics/comparaison-zones`, { params });
}
}