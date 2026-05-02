// src/app/services/auth.service.ts
// MISE À JOUR : stockage de `specialite` dans localStorage pour détecter côté frontend
// si l'utilisateur est un conducteur ou un technicien.
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface RegisterRequest {
  nom: string;
  email: string;
  motDePasse: string;
  role: 'ADMIN' | 'TECHNICIEN';
  specialite?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: number;
  nom: string;
  email: string;
  role: string;
  statutCompte: string;
  specialite?: string;   // ← NOUVEAU : retourné par le backend au login
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadUserFromStorage();
  }

  register(request: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('token',        response.token);
          localStorage.setItem('userId',       response.userId.toString());
          localStorage.setItem('nom',          response.nom);
          localStorage.setItem('email',        response.email);
          localStorage.setItem('role',         response.role);
          localStorage.setItem('statutCompte', response.statutCompte);
          // ← NOUVEAU : stocker la spécialité pour détecter conducteur côté frontend
          localStorage.setItem('specialite',   response.specialite || '');

          this.currentUserSubject.next(response);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('nom');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
    localStorage.removeItem('statutCompte');
    localStorage.removeItem('specialite'); // ← NOUVEAU

    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  private loadUserFromStorage(): void {
    const token = localStorage.getItem('token');
    if (token) {
      const user = {
        token,
        userId:       localStorage.getItem('userId'),
        nom:          localStorage.getItem('nom'),
        email:        localStorage.getItem('email'),
        role:         localStorage.getItem('role'),
        statutCompte: localStorage.getItem('statutCompte'),
        specialite:   localStorage.getItem('specialite'), // ← NOUVEAU
      };
      this.currentUserSubject.next(user);
    }
  }

  isLoggedIn(): boolean { return !!localStorage.getItem('token'); }
  getToken(): string | null { return localStorage.getItem('token'); }
  getUserRole(): string | null { return localStorage.getItem('role'); }
  isAdmin(): boolean { return this.getUserRole() === 'ADMIN'; }
  isTechnicien(): boolean { return this.getUserRole() === 'TECHNICIEN'; }

  /** Retourne true si l'utilisateur connecté est un conducteur de véhicule */
  isConducteur(): boolean {
    return (localStorage.getItem('specialite') || '').toLowerCase() === 'conducteur';
  }

  getCurrentUser() { return this.currentUserSubject.value; }
}