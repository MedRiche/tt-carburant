import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Zone, ZoneRequest } from '../models/zone';

@Injectable({
  providedIn: 'root'
})
export class ZoneService {
  private apiUrl = 'http://localhost:8081/api/admin/zones';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getAllZones(): Observable<Zone[]> {
    return this.http.get<Zone[]>(this.apiUrl, { headers: this.getHeaders() });
  }

  getZoneById(id: number): Observable<Zone> {
    return this.http.get<Zone>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  creerZone(zone: ZoneRequest): Observable<any> {
    return this.http.post(this.apiUrl, zone, { headers: this.getHeaders() });
  }

  modifierZone(id: number, zone: ZoneRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, zone, { headers: this.getHeaders() });
  }

  supprimerZone(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }
}