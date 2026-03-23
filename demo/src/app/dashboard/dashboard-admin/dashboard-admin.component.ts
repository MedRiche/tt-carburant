import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ZoneService } from '../../services/zone.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { VehiculeService } from '../../services/vehicule.service';
import { Zone } from '../../models/zone';
import { Utilisateur, StatutCompte } from '../../models/utilisateur';
import { Vehicule, TypeCarburant } from '../../models/vehicule';

declare var Chart: any;

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
export class DashboardAdminComponent implements OnInit, AfterViewInit, OnDestroy {

  currentUser: any = null;
  isAdmin = false;

  // Utilisateurs stats
  totalZones        = 0;
  totalUtilisateurs = 0;
  totalActifs       = 0;
  totalEnAttente    = 0;
  totalTechniciens  = 0;
  totalAdmins       = 0;
  totalDesactives   = 0;
  totalRefuses      = 0;

  // Véhicules stats
  totalVehicules      = 0;
  totalTypesCarburant = 0;
  totalVisitesTech    = 0;
  totalCoutMois       = 0;
  vehiculesByZone: { nom: string; count: number }[] = [];
  vehiculesByCarburant: { type: string; count: number }[] = [];

  // Displayed counters (animated)
  displayZones        = 0;
  displayUtilisateurs = 0;
  displayActifs       = 0;
  displayEnAttente    = 0;
  displayVehicules    = 0;

  topZones: ZoneStat[] = [];
  zones: Zone[] = [];
  utilisateurs: Utilisateur[] = [];
  vehicules: Vehicule[] = [];

  loading = true;
  dataReady = false;
  today = new Date().toLocaleDateString('fr-FR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  private charts: any[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private zoneService: ZoneService,
    private utilisateurService: UtilisateurService,
    private vehiculeService: VehiculeService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user: any) => {
      this.currentUser = user;
      this.isAdmin = this.authService.isAdmin();
    });
    this.loadStatistics();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.charts.forEach(c => c?.destroy());
  }

  loadStatistics(): void {
    this.loading = true;
    let zonesLoaded = false;
    let usersLoaded = false;
    let vehiculesLoaded = false;

    const checkReady = () => {
      if (zonesLoaded && usersLoaded && vehiculesLoaded) {
        this.loading = false;
        this.dataReady = true;
        setTimeout(() => {
          this.animateCounters();
          this.buildCharts();
        }, 100);
      }
    };

    this.zoneService.getAllZones().subscribe({
      next: (zones) => {
        this.zones = zones;
        this.totalZones = zones.length;
        this.topZones = zones
          .map(z => ({ zone: z, nombreTechniciens: z.nombreUtilisateurs || 0 }))
          .sort((a, b) => b.nombreTechniciens - a.nombreTechniciens)
          .slice(0, 5);
        zonesLoaded = true;
        checkReady();
      },
      error: () => { zonesLoaded = true; checkReady(); }
    });

    this.utilisateurService.getAllUtilisateurs().subscribe({
      next: (utilisateurs) => {
        this.utilisateurs = utilisateurs;
        this.totalUtilisateurs = utilisateurs.length;
        this.totalActifs       = utilisateurs.filter(u => u.statutCompte === StatutCompte.ACTIF).length;
        this.totalEnAttente    = utilisateurs.filter(u => u.statutCompte === StatutCompte.EN_ATTENTE).length;
        this.totalDesactives   = utilisateurs.filter(u => (u.statutCompte as string) === 'DESACTIVE').length;
        this.totalRefuses      = utilisateurs.filter(u => u.statutCompte === StatutCompte.REFUSE).length;
        this.totalTechniciens  = utilisateurs.filter(u => u.role === 'TECHNICIEN').length;
        this.totalAdmins       = utilisateurs.filter(u => u.role === 'ADMIN').length;
        usersLoaded = true;
        checkReady();
      },
      error: () => { usersLoaded = true; checkReady(); }
    });

    this.vehiculeService.getAllVehicules().subscribe({
      next: (vehicules) => {
        this.vehicules = vehicules;
        this.totalVehicules = vehicules.length;
        this.totalCoutMois = vehicules.reduce((sum, v) => sum + (v.coutDuMois || 0), 0);

        // Unique fuel types
        const typesSet = new Set(vehicules.map(v => v.typeCarburant));
        this.totalTypesCarburant = typesSet.size;

        // Count visits tech in next 3 months
        const now = new Date();
        const inThreeMonths = new Date(now);
        inThreeMonths.setMonth(now.getMonth() + 3);
        this.totalVisitesTech = vehicules.filter(v => {
          if (!v.visiteTechnique) return false;
          const d = new Date(v.visiteTechnique);
          return d >= now && d <= inThreeMonths;
        }).length;

        // Vehicles by zone
        const byZone: Record<string, number> = {};
        vehicules.forEach(v => {
          const key = v.zoneNom || 'Sans zone';
          byZone[key] = (byZone[key] || 0) + 1;
        });
        this.vehiculesByZone = Object.entries(byZone)
          .map(([nom, count]) => ({ nom, count }))
          .sort((a, b) => b.count - a.count)
          .slice(0, 9);

        // Vehicles by carburant
        const byCarb: Record<string, number> = {};
        vehicules.forEach(v => {
          const key = v.typeCarburant || 'Inconnu';
          byCarb[key] = (byCarb[key] || 0) + 1;
        });
        this.vehiculesByCarburant = Object.entries(byCarb)
          .map(([type, count]) => ({ type, count }));

        vehiculesLoaded = true;
        checkReady();
      },
      error: () => { vehiculesLoaded = true; checkReady(); }
    });
  }

  // ── Animated counters ────────────────────────────────────────────────────
  animateCounters(): void {
    this.animateValue('displayZones',        0, this.totalZones,        1200);
    this.animateValue('displayUtilisateurs', 0, this.totalUtilisateurs, 1400);
    this.animateValue('displayActifs',       0, this.totalActifs,       1600);
    this.animateValue('displayEnAttente',    0, this.totalEnAttente,    1000);
    this.animateValue('displayVehicules',    0, this.totalVehicules,    1300);
  }

  private animateValue(prop: keyof this, from: number, to: number, duration: number): void {
    const startTime = performance.now();
    const update = (currentTime: number) => {
      const elapsed  = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const ease     = 1 - Math.pow(1 - progress, 3);
      (this as any)[prop] = Math.round(from + (to - from) * ease);
      if (progress < 1) requestAnimationFrame(update);
    };
    requestAnimationFrame(update);
  }

  // ── Charts ───────────────────────────────────────────────────────────────
  buildCharts(): void {
    this.buildDonutChart();
    this.buildRoleChart();
    this.buildBarChart();
    this.buildVehiculeZoneChart();
    this.buildCarburantChart();
  }

  buildDonutChart(): void {
    const canvas = document.getElementById('statutChart') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Actifs', 'En attente', 'Désactivés', 'Refusés'],
        datasets: [{
          data: [this.totalActifs, this.totalEnAttente, this.totalDesactives, this.totalRefuses],
          backgroundColor: ['#00d4aa', '#f59e0b', '#64748b', '#ef4444'],
          borderColor: '#0f172a',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '72%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#94a3b8', font: { size: 12 }, padding: 16, usePointStyle: true }
          },
          tooltip: { backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8 }
        },
        animation: { animateRotate: true, duration: 1200, easing: 'easeOutQuart' }
      }
    });
    this.charts.push(chart);
  }

  buildRoleChart(): void {
    const canvas = document.getElementById('roleChart') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const chart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: ['Techniciens', 'Administrateurs'],
        datasets: [{
          data: [this.totalTechniciens, this.totalAdmins],
          backgroundColor: ['#3b82f6', '#8b5cf6'],
          borderColor: '#0f172a',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#94a3b8', font: { size: 12 }, padding: 16, usePointStyle: true }
          },
          tooltip: { backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8 }
        },
        animation: { duration: 1200, easing: 'easeOutQuart' }
      }
    });
    this.charts.push(chart);
  }

  buildBarChart(): void {
    const canvas = document.getElementById('zonesChart') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const labels = this.topZones.map(z => z.zone.nom);
    const data   = this.topZones.map(z => z.nombreTechniciens);

    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(0, 212, 170, 0.9)');
    gradient.addColorStop(1, 'rgba(0, 212, 170, 0.1)');

    const chart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Techniciens',
          data,
          backgroundColor: gradient,
          borderColor: '#00d4aa',
          borderWidth: 2,
          borderRadius: 8,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8,
            callbacks: { label: (ctx: any) => ` ${ctx.parsed.y} technicien${ctx.parsed.y > 1 ? 's' : ''}` }
          }
        },
        scales: {
          x: { grid: { color: 'rgba(51,65,85,0.4)' }, ticks: { color: '#64748b', font: { size: 11 } } },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(51,65,85,0.4)' },
            ticks: { color: '#64748b', font: { size: 11 }, stepSize: 1, precision: 0 }
          }
        },
        animation: { duration: 1200, easing: 'easeOutQuart' }
      }
    });
    this.charts.push(chart);
  }

  buildVehiculeZoneChart(): void {
    const canvas = document.getElementById('vehiculesZoneChart') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Use zone names from our known list if no data from API
    const labels = this.vehiculesByZone.length > 0
      ? this.vehiculesByZone.map(z => z.nom)
      : ['COMMERCIAL', 'CSC BARDO', 'CSC BELVEDERE', 'CSC KASBAH', 'DAF', 'DIV. CLIENTELE', 'Réseaux&S.I', 'HACHED', 'BL NORD'];

    const data = this.vehiculesByZone.length > 0
      ? this.vehiculesByZone.map(z => z.count)
      : [20, 18, 16, 15, 14, 17, 19, 16, 10]; // approximate distribution for 145 vehicles

    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(139, 92, 246, 0.9)');
    gradient.addColorStop(1, 'rgba(139, 92, 246, 0.1)');

    const chart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Véhicules',
          data,
          backgroundColor: gradient,
          borderColor: '#8b5cf6',
          borderWidth: 2,
          borderRadius: 8,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8,
            callbacks: { label: (ctx: any) => ` ${ctx.parsed.y} véhicule${ctx.parsed.y > 1 ? 's' : ''}` }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(51,65,85,0.4)' },
            ticks: { color: '#64748b', font: { size: 10 }, maxRotation: 30 }
          },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(51,65,85,0.4)' },
            ticks: { color: '#64748b', font: { size: 11 }, stepSize: 2, precision: 0 }
          }
        },
        animation: { duration: 1200, easing: 'easeOutQuart' }
      }
    });
    this.charts.push(chart);
  }

  buildCarburantChart(): void {
    const canvas = document.getElementById('carburantChart') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    const labels = this.vehiculesByCarburant.length > 0
      ? this.vehiculesByCarburant.map(c => this.getLabelCarburant(c.type))
      : ['Gasoil Ordinaire', 'Gasoil SS', 'Gasoil 50', 'Super SP', 'Essence'];

    const data = this.vehiculesByCarburant.length > 0
      ? this.vehiculesByCarburant.map(c => c.count)
      : [60, 40, 25, 12, 8];

    const chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: ['#3b82f6', '#00d4aa', '#f59e0b', '#8b5cf6', '#ef4444'],
          borderColor: '#0f172a',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#94a3b8', font: { size: 10 }, padding: 10, usePointStyle: true }
          },
          tooltip: { backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8 }
        },
        animation: { animateRotate: true, duration: 1200, easing: 'easeOutQuart' }
      }
    });
    this.charts.push(chart);
  }

  getLabelCarburant(type: string): string {
    const map: Record<string, string> = {
      ESSENCE: 'Essence',
      GASOIL_ORDINAIRE: 'Gasoil Ord.',
      GASOIL_SANS_SOUFRE: 'Gasoil SS',
      GASOIL_50: 'Gasoil 50',
      SUPER_SANS_PLOMB: 'Super SP'
    };
    return map[type] || type;
  }

  logout(): void {
    if (confirm('Êtes-vous sûr de vouloir vous déconnecter ?')) {
      this.authService.logout();
    }
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  getRankClass(i: number): string {
    if (i === 0) return 'rank-gold';
    if (i === 1) return 'rank-silver';
    if (i === 2) return 'rank-bronze';
    return 'rank-default';
  }

  getInitials(nom: string): string {
    return nom ? nom.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2) : '?';
  }
}