// src/app/services/vehicule.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Vehicule, VehiculeRequest } from '../models/vehicule';

@Injectable({ providedIn: 'root' })
export class VehiculeService {
  private apiUrl = 'http://localhost:8081/api/admin/vehicules';

  constructor(private http: HttpClient) {}

  private headers(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  getAllVehicules(): Observable<Vehicule[]> {
    return this.http.get<Vehicule[]>(this.apiUrl, { headers: this.headers() });
  }

  getVehiculesByZone(zoneId: number): Observable<Vehicule[]> {
    return this.http.get<Vehicule[]>(`${this.apiUrl}/zone/${zoneId}`, { headers: this.headers() });
  }

  getVehiculeById(matricule: string): Observable<Vehicule> {
    return this.http.get<Vehicule>(`${this.apiUrl}/${matricule}`, { headers: this.headers() });
  }

  creerVehicule(req: VehiculeRequest): Observable<any> {
    return this.http.post(this.apiUrl, req, { headers: this.headers() });
  }

  modifierVehicule(matricule: string, req: VehiculeRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/${matricule}`, req, { headers: this.headers() });
  }

  supprimerVehicule(matricule: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${matricule}`, { headers: this.headers() });
  }

  affecterZone(matricule: string, zoneId: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${matricule}/zone/${zoneId}`, {}, { headers: this.headers() });
  }

   importerExcel(file: File, zoneNom?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    if (zoneNom) {
      formData.append('zoneNom', zoneNom);
    }
    return this.http.post(`${this.apiUrl}/import`, formData, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${localStorage.getItem('token')}`
        // Ne pas mettre Content-Type pour FormData (le browser le fait automatiquement)
      })
    });
  }
}