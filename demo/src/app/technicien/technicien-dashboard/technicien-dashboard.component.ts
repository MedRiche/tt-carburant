// src/app/technicien/technicien-dashboard/technicien-dashboard.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
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
export class TechnicienDashboardComponent implements OnInit, OnDestroy {

  profil: Utilisateur | null = null;
  zones: Zone[] = [];
  loading = true;
  error: string | null = null;
  currentTime = new Date();
  private timerInterval: any;

  constructor(
    private authService: AuthService,
    private technicienService: TechnicienService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProfil();
    this.timerInterval = setInterval(() => {
      this.currentTime = new Date();
    }, 60000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  loadProfil(): void {
    this.loading = true;
    this.error = null;

    // On appelle les deux endpoints en parallèle pour fiabilité
    this.technicienService.getProfil().subscribe({
      next: (data) => {
        this.profil = data;
        // Zones viennent du profil
        if (data.zones && data.zones.length > 0) {
          this.zones = data.zones;
          this.loading = false;
        } else {
          // Fallback : appel direct /mes-zones
          this.loadZonesFallback();
        }
      },
      error: (err) => {
        console.error('Erreur getProfil:', err);
        this.error = 'Impossible de charger le profil.';
        this.loading = false;
      }
    });
  }

  /** Fallback si le profil ne retourne pas les zones */
  private loadZonesFallback(): void {
    this.technicienService.getMesZones().subscribe({
      next: (zones) => {
        this.zones = zones || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur getMesZones:', err);
        this.zones = [];
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

  get nbZones(): number {
    return this.zones.length;
  }
}