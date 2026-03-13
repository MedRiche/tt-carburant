import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (this.authService.isLoggedIn()) {
      // Vérifier si la route nécessite un rôle spécifique
      const requiredRole = route.data['role'];
      
      if (requiredRole) {
        const userRole = this.authService.getUserRole();
        
        if (userRole === requiredRole) {
          return true;
        } else {
          // L'utilisateur n'a pas le bon rôle
          alert('❌ Accès refusé\n\nVous n\'avez pas les permissions nécessaires pour accéder à cette page.');
          this.router.navigate(['/login']);
          return false;
        }
      }
      
      return true;
    }

    // L'utilisateur n'est pas connecté, rediriger vers la page de login
    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}

/**
 * Guard pour vérifier le rôle ADMIN
 */
@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    if (this.authService.isLoggedIn() && this.authService.isAdmin()) {
      return true;
    }

    alert('❌ Accès refusé\n\nCette page est réservée aux administrateurs.');
    this.router.navigate(['/login']);
    return false;
  }
}

/**
 * Guard pour vérifier le rôle TECHNICIEN
 */
@Injectable({
  providedIn: 'root'
})
export class TechnicienGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    if (this.authService.isLoggedIn() && this.authService.isTechnicien()) {
      return true;
    }

    alert('❌ Accès refusé\n\nCette page est réservée aux techniciens.');
    this.router.navigate(['/login']);
    return false;
  }
}