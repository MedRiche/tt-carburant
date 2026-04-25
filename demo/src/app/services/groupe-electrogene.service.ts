import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeElectrogene, GroupeElectrogeneRequest } from '../models/groupe-electrogene';

@Injectable({ providedIn: 'root' })
export class GroupeElectrogeneService {

  private baseUrl = 'http://localhost:8081/api/admin';
  private geUrl   = 'http://localhost:8081/api/admin/groupes-electrogenes';

  constructor(private http: HttpClient) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }

  private fileHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Authorization': `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }

  // ── Groupe électrogène CRUD ────────────────────────────────────

  getAllGroupes(): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(this.geUrl, { headers: this.authHeaders() });
  }

  getBySite(site: string): Observable<GroupeElectrogene> {
    return this.http.get<GroupeElectrogene>(
      `${this.geUrl}/${encodeURIComponent(site)}`,
      { headers: this.authHeaders() }
    );
  }

  getByZone(zoneId: number): Observable<GroupeElectrogene[]> {
    return this.http.get<GroupeElectrogene[]>(
      `${this.geUrl}/zone/${zoneId}`,
      { headers: this.authHeaders() }
    );
  }

  creer(req: GroupeElectrogeneRequest): Observable<GroupeElectrogene> {
    return this.http.post<GroupeElectrogene>(this.geUrl, req, { headers: this.authHeaders() });
  }

  modifier(site: string, req: GroupeElectrogeneRequest): Observable<GroupeElectrogene> {
    return this.http.put<GroupeElectrogene>(
      `${this.geUrl}/${encodeURIComponent(site)}`,
      req,
      { headers: this.authHeaders() }
    );
  }

  supprimer(site: string): Observable<any> {
    return this.http.delete(
      `${this.geUrl}/${encodeURIComponent(site)}`,
      { headers: this.authHeaders() }
    );
  }

  // ── Import Excel ───────────────────────────────────────────────

  importerExcel(file: File, zoneNom?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    if (zoneNom) formData.append('zoneNom', zoneNom);
    return this.http.post(`${this.geUrl}/import`, formData, { headers: this.fileHeaders() });
  }

  // ── Téléchargement de fichier (utilitaire) ─────────────────────

  downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a   = document.createElement('a');
    a.href     = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}