import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ZoneService } from '../../services/zone.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { Zone } from '../../models/zone';
import { Utilisateur, StatutCompte } from '../../models/utilisateur';

interface ZoneStat {
  zone: Zone;
  nombreTechniciens: number;
}

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

  // Statistiques
  totalZones = 0;
  totalUtilisateurs = 0;
  totalActifs = 0;
  totalEnAttente = 0;
  totalTechniciens = 0;
  totalAdmins = 0;

  topZones: ZoneStat[] = [];
  zones: Zone[] = [];
  utilisateurs: Utilisateur[] = [];

  loading = true;

  constructor(
    private authService: AuthService,
    private router: Router,
    private zoneService: ZoneService,
    private utilisateurService: UtilisateurService
  ) {}

  ngOnInit(): void {
    // S'abonner aux changements de l'utilisateur connecté
    this.authService.currentUser$.subscribe((user: any) => {
      this.currentUser = user;
      this.isAdmin = this.authService.isAdmin();
      this.isTechnicien = this.authService.isTechnicien();
    });

    this.loadStatistics();
  }

  loadStatistics(): void {
    this.loading = true;

    // Charger les zones
    this.zoneService.getAllZones().subscribe({
      next: (zones) => {
        this.zones = zones;
        this.totalZones = zones.length;

        // Trier les zones par nombre d'utilisateurs
        this.topZones = zones
          .map(zone => ({
            zone: zone,
            nombreTechniciens: zone.nombreUtilisateurs || 0
          }))
          .sort((a, b) => b.nombreTechniciens - a.nombreTechniciens)
          .slice(0, 5); // Top 5
      },
      error: (err) => console.error('Erreur chargement zones', err)
    });

    // Charger les utilisateurs
    this.utilisateurService.getAllUtilisateurs().subscribe({
      next: (utilisateurs) => {
        this.utilisateurs = utilisateurs;
        this.totalUtilisateurs = utilisateurs.length;

        // Compter par statut
        this.totalActifs = utilisateurs.filter(u => u.statutCompte === StatutCompte.ACTIF).length;
        this.totalEnAttente = utilisateurs.filter(u => u.statutCompte === StatutCompte.EN_ATTENTE).length;

        // Compter par rôle
        this.totalTechniciens = utilisateurs.filter(u => u.role === 'TECHNICIEN').length;
        this.totalAdmins = utilisateurs.filter(u => u.role === 'ADMIN').length;

        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement utilisateurs', err);
        this.loading = false;
      }
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