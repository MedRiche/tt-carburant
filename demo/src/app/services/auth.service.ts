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
    // Charger l'utilisateur depuis le localStorage au démarrage
    this.loadUserFromStorage();
  }

  /**
   * Inscription d'un nouvel utilisateur
   */
  register(request: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, request);
  }

  /**
   * Connexion
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        if (response.token) {
          // Sauvegarder les informations dans localStorage
          localStorage.setItem('token', response.token);
          localStorage.setItem('userId', response.userId.toString());
          localStorage.setItem('nom', response.nom);
          localStorage.setItem('email', response.email);
          localStorage.setItem('role', response.role);
          localStorage.setItem('statutCompte', response.statutCompte);

          // Mettre à jour le BehaviorSubject
          this.currentUserSubject.next(response);
        }
      })
    );
  }

  /**
   * Déconnexion
   */
  logout(): void {
    // Supprimer toutes les données du localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('nom');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
    localStorage.removeItem('statutCompte');

    // Réinitialiser le BehaviorSubject
    this.currentUserSubject.next(null);

    // Rediriger vers la page de login
    this.router.navigate(['/login']);
  }

  /**
   * Charger l'utilisateur depuis le localStorage
   */
  private loadUserFromStorage(): void {
    const token = localStorage.getItem('token');
    if (token) {
      const user = {
        token,
        userId: localStorage.getItem('userId'),
        nom: localStorage.getItem('nom'),
        email: localStorage.getItem('email'),
        role: localStorage.getItem('role'),
        statutCompte: localStorage.getItem('statutCompte')
      };
      this.currentUserSubject.next(user);
    }
  }

  /**
   * Vérifier si l'utilisateur est connecté
   */
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  /**
   * Obtenir le token
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Obtenir le rôle de l'utilisateur
   */
  getUserRole(): string | null {
    return localStorage.getItem('role');
  }

  /**
   * Vérifier si l'utilisateur est admin
   */
  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  /**
   * Vérifier si l'utilisateur est technicien
   */
  isTechnicien(): boolean {
    return this.getUserRole() === 'TECHNICIEN';
  }

  /**
   * Obtenir les informations de l'utilisateur connecté
   */
  getCurrentUser() {
    return this.currentUserSubject.value;
  }
}