// src/app/services/conducteur.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConducteurImportResult {
  imported: number;
  updated: number;
  skipped: number;
  total: number;
  zone: string;
  errors: string[];
  conducteursCreated: number;
  conducteursExistants: number;
  conducteursDetails: { nomComplet: string; email: string; statut: string }[];
}

@Injectable({ providedIn: 'root' })
export class ConducteurService {
  private apiUrl = 'http://localhost:8081/api/admin/vehicules';

  constructor(private http: HttpClient) {}

  private headers(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${localStorage.getItem('token')}`
    });
  }

  importerExcelAvecConducteurs(file: File, zoneNom?: string): Observable<ConducteurImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    if (zoneNom) formData.append('zoneNom', zoneNom);
    return this.http.post<ConducteurImportResult>(`${this.apiUrl}/import`, formData, {
      headers: new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem('token')}` })
    });
  }
}