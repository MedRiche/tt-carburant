import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Récupérer le token depuis le service d'authentification
    const token = this.authService.getToken();

    // Si le token existe, l'ajouter au header de la requête
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    // Passer la requête au prochain handler
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Gérer les erreurs HTTP
        if (error.status === 401) {
          // Token expiré ou invalide
          console.error('Token invalide ou expiré');
          this.authService.logout();
          alert('⚠️ Session expirée\n\nVeuillez vous reconnecter.');
        } else if (error.status === 403) {
          // Accès refusé
          console.error('Accès refusé');
          alert('❌ Accès refusé\n\nVous n\'avez pas les permissions nécessaires.');
          this.router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
  }
}