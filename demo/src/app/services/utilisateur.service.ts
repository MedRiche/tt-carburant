import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Utilisateur, ValiderCompteRequest } from '../models/utilisateur';

@Injectable({
  providedIn: 'root'
})
export class UtilisateurService {
  private apiUrl = 'http://localhost:8081/api/admin/utilisateurs';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getAllUtilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(this.apiUrl, { headers: this.getHeaders() });
  }

  getUtilisateursEnAttente(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.apiUrl}/en-attente`, { headers: this.getHeaders() });
  }

  getUtilisateurById(id: number): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  validerCompteAvecZones(request: ValiderCompteRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/valider`, request, { headers: this.getHeaders() });
  }

  refuserCompte(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/refuser`, {}, { headers: this.getHeaders() });
  }

  /** TOGGLE: ACTIF→DESACTIVE, DESACTIVE→ACTIF, REFUSE→ACTIF */
  toggleActivation(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/toggle-activation`, {}, { headers: this.getHeaders() });
  }

  supprimerUtilisateur(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  ajouterZone(utilisateurId: number, zoneId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${utilisateurId}/zones/${zoneId}`, {}, { headers: this.getHeaders() });
  }

  retirerZone(utilisateurId: number, zoneId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${utilisateurId}/zones/${zoneId}`, { headers: this.getHeaders() });
  }
}