import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard-admin',
  standalone: false,
  templateUrl: './dashboard-admin.component.html',
  styleUrl: './dashboard-admin.component.css'
})
export class DashboardAdminComponent implements OnInit {
  currentUser: any = null;
  isAdmin = false;
  isTechnicien = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // S'abonner aux changements de l'utilisateur connecté
    this.authService.currentUser$.subscribe((user: any) => {
      this.currentUser = user;
      this.isAdmin = this.authService.isAdmin();
      this.isTechnicien = this.authService.isTechnicien();
    });
  }

  logout(): void {
    if (confirm('Êtes-vous sûr de vouloir vous déconnecter ?')) {
      this.authService.logout();
    }
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }
}


