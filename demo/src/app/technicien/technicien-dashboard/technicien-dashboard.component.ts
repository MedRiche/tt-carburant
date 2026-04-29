// src/app/technicien/dashboard/technicien-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TechnicienService } from '../../services/technicien.service';
import { Utilisateur } from '../../models/utilisateur';
import { Zone } from '../../models/zone';

@Component({
  selector: 'app-technicien-dashboard',
  standalone: false,
  templateUrl: './technicien-dashboard.component.html',
  styleUrls: ['./technicien-dashboard.component.css']
})
export class TechnicienDashboardComponent implements OnInit {

  profil: Utilisateur | null = null;
  zones: Zone[] = [];
  loading = true;
  currentTime = new Date();

  constructor(
    private authService: AuthService,
    private technicienService: TechnicienService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProfil();
    // Met à jour l'heure chaque minute
    setInterval(() => { this.currentTime = new Date(); }, 60000);
  }

  loadProfil(): void {
    this.loading = true;
    this.technicienService.getProfil().subscribe({
      next: (data) => {
        this.profil = data;
        this.zones  = data.zones || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  getGreeting(): string {
    const h = this.currentTime.getHours();
    if (h < 12) return 'Bonjour';
    if (h < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  getInitials(nom: string): string {
    return (nom || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  get nomAffiche(): string {
    return this.profil?.nom || localStorage.getItem('nom') || 'Technicien';
  }

  get specialite(): string {
    return this.profil?.specialite || '—';
  }

  get isConducteur(): boolean {
    return this.profil?.specialite === 'Conducteur';
  }
}