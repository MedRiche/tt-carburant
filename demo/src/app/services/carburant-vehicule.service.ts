// src/app/services/carburant-vehicule.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarburantVehicule } from '../models/carburant-vehicule';

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

  saisir(req: Partial<CarburantVehicule>): Observable<any> {
    return this.http.post(this.api, req, { headers: this.h() });
  }

  modifier(id: number, req: Partial<CarburantVehicule>): Observable<any> {
    return this.http.put(`${this.api}/${id}`, req, { headers: this.h() });
  }

  supprimer(id: number): Observable<any> {
    return this.http.delete(`${this.api}/${id}`, { headers: this.h() });
  }
}