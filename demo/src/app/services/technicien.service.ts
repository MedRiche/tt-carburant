// src/app/services/technicien.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Utilisateur } from '../models/utilisateur';
import { Zone } from '../models/zone';

@Injectable({
  providedIn: 'root'
})
export class TechnicienService {

  private apiUrl = 'http://localhost:8081/api/technicien';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Récupère le profil complet du technicien connecté (avec ses zones)
   */
  getProfil(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.apiUrl}/profil`, {
      headers: this.getHeaders()
    });
  }

  /**
   * Récupère uniquement les zones affectées au technicien
   */
  getMesZones(): Observable<Zone[]> {
    return this.http.get<Zone[]>(`${this.apiUrl}/mes-zones`, {
      headers: this.getHeaders()
    });
  }
}