// src/app/services/technicien-carburant.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VehiculeCarburantResume {
  matricule: string;
  marqueModele: string;
  typeVehicule: string;
  typeCarburant: string;
  prixCarburant: number;
  coutDuMois: number;
  kilometrageTotal: number;
  nomConducteur: string;
  prenomConducteur: string;
  zoneId?: number;
  zoneNom?: string;
  saisieExistante: boolean;
  distanceMois?: number;
  consoMoisLitres?: number;
  budgetDepasse?: boolean;
  depassementMontant?: number;
  gestionId?: number;
}

export interface HistoriqueCarburant {
  id: number;
  annee: number;
  mois: number;
  periodeLabel: string;
  indexDemarrageMois: number;
  indexFinMois: number;
  montantRestantMoisPrecedent: number;
  ravitaillementMoisPrecedent: number;
  ravitaillementMois: number;
  totalRavitaillementLitres: number;
  quantiteRestanteReservoir: number;
  distanceParcourue: number;
  pourcentageConsommation: number;
  carburantDemandeDinars: number;
  montantRestantReservoirFin: number;
  prixCarburant: number;
  coutDuMois: number;
  budgetDepasse: boolean;
  depassementMontant: number;
  tauxBudget: number;
  consoLitres: number;
  coutReel: number;
  dateCreation: string;
}

export interface CarburantStats {
  matricule: string;
  marqueModele: string;
  annee: number;
  nbMoisSaisis: number;
  totalKm: number;
  totalLitres: number;
  totalCout: number;
  totalBudget: number;
  tauxBudget: number;
  rendementMoyen: number;
  nbBudgetsDepasses: number;
  evolution: EvolutionMois[];
}

export interface EvolutionMois {
  mois: number;
  label: string;
  km: number;
  litres: number;
  cout: number;
  rendement: number;
  budgetDepasse: boolean;
  budget: number;
  id?: number | null; // id de la saisie (null si pas de saisie ce mois)
}

export interface DashboardTechnicien {
  totalVehicules: number;
  totalKmAnnee: number;
  totalLitresAnnee: number;
  totalCoutAnnee: number;
  nbBudgetsDepasses: number;
  annee: number;
  vehicules: any[];
  evolutionMensuelle: any[];
}

export interface SaisieCarburantRequest {
  vehiculeMatricule: string;
  annee: number;
  mois: number;
  indexDemarrageMois: number;
  indexFinMois: number;
  montantRestantMoisPrecedent: number;
  ravitaillementMoisPrecedent: number;
  ravitaillementMois: number;
}

export interface PrefillData {
  vehiculeMatricule: string;
  annee: number;
  mois: number;
  prixCarburant: number;
  coutDuMois: number;
  indexDemarrageMois: number;
  montantRestantMoisPrecedent: number;
  ravitaillementMoisPrecedent: number;
}

@Injectable({ providedIn: 'root' })
export class TechnicienCarburantService {

  private api = 'http://localhost:8081/api/technicien/carburant';

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

  // ── Lecture ──────────────────────────────────────────────────

  getMesVehicules(): Observable<VehiculeCarburantResume[]> {
    return this.http.get<VehiculeCarburantResume[]>(`${this.api}/mes-vehicules`, { headers: this.h() });
  }

  getHistorique(matricule: string, annee?: number): Observable<HistoriqueCarburant[]> {
    let params = new HttpParams();
    if (annee) params = params.set('annee', annee);
    return this.http.get<HistoriqueCarburant[]>(`${this.api}/historique/${matricule}`, { headers: this.h(), params });
  }

  getPrefill(matricule: string, annee: number, mois: number): Observable<PrefillData> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<PrefillData>(`${this.api}/prefill/${matricule}`, { headers: this.h(), params });
  }

  getStats(matricule: string, annee?: number): Observable<CarburantStats> {
    let params = new HttpParams();
    if (annee) params = params.set('annee', annee);
    return this.http.get<CarburantStats>(`${this.api}/stats/${matricule}`, { headers: this.h(), params });
  }

  getDashboard(): Observable<DashboardTechnicien> {
    return this.http.get<DashboardTechnicien>(`${this.api}/dashboard`, { headers: this.h() });
  }

  // ── CRUD ─────────────────────────────────────────────────────

  saisir(req: SaisieCarburantRequest): Observable<any> {
    return this.http.post(`${this.api}/saisir`, req, { headers: this.h() });
  }

  modifier(id: number, req: SaisieCarburantRequest): Observable<any> {
    return this.http.put(`${this.api}/modifier/${id}`, req, { headers: this.h() });
  }

  supprimer(id: number): Observable<any> {
    return this.http.delete(`${this.api}/supprimer/${id}`, { headers: this.h() });
  }

  // ── Export Excel ─────────────────────────────────────────────

  /** Export Excel pour un véhicule (toute l'année ou un mois) */
  exportExcelVehicule(matricule: string, annee: number, mois?: number): Observable<Blob> {
    let params = new HttpParams().set('annee', annee);
    if (mois) params = params.set('mois', mois);
    return this.http.get(`${this.api}/export/excel/${matricule}`, {
      headers: this.hBlob(),
      params,
      responseType: 'blob'
    });
  }

  /** Export Excel tous les véhicules — un mois */
  exportExcelPeriode(annee: number, mois: number): Observable<Blob> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get(`${this.api}/export/excel/periode`, {
      headers: this.hBlob(),
      params,
      responseType: 'blob'
    });
  }

  /** Déclenche le téléchargement navigateur */
  downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a   = document.createElement('a');
    a.href    = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}