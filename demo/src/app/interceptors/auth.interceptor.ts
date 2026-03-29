import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler,
  HttpEvent, HttpErrorResponse
} from '@angular/common/http';
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
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {

        if (error.status === 401) {
          // Token expiré ou invalide → déconnexion silencieuse puis redirection
          const message = error.error?.message || '';
          const isExpired = message.toLowerCase().includes('expir');

          this.authService.logout(); // nettoie localStorage

          if (isExpired) {
            alert('⚠️ Votre session a expiré.\n\nVeuillez vous reconnecter.');
          } else {
            alert('⚠️ Session invalide.\n\nVeuillez vous reconnecter.');
          }

          // logout() navigue déjà vers /login — on ne redirige pas deux fois

        } else if (error.status === 403) {
          alert('❌ Accès refusé.\n\nVous n\'avez pas les permissions nécessaires.');
          this.router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
  }
}