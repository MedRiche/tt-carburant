import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ZoneService } from '../../services/zone.service';
import { UtilisateurService } from '../../services/utilisateur.service';
import { Zone } from '../../models/zone';
import { Utilisateur, StatutCompte } from '../../models/utilisateur';

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

  totalZones        = 0;
  totalUtilisateurs = 0;
  totalActifs       = 0;
  totalEnAttente    = 0;
  totalTechniciens  = 0;
  totalAdmins       = 0;
  totalDesactives   = 0;
  totalRefuses      = 0;

  // Displayed counters (animated)
  displayZones        = 0;
  displayUtilisateurs = 0;
  displayActifs       = 0;
  displayEnAttente    = 0;

  topZones: ZoneStat[] = [];
  zones: Zone[] = [];
  utilisateurs: Utilisateur[] = [];

  loading = true;
  dataReady = false;
  today = new Date().toLocaleDateString('fr-FR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  private charts: any[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private zoneService: ZoneService,
    private utilisateurService: UtilisateurService
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

    const checkReady = () => {
      if (zonesLoaded && usersLoaded) {
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
  }

  // ── Animated counters ────────────────────────────────────────────────────
  animateCounters(): void {
    this.animateValue('displayZones',        0, this.totalZones,        1200);
    this.animateValue('displayUtilisateurs', 0, this.totalUtilisateurs, 1400);
    this.animateValue('displayActifs',       0, this.totalActifs,       1600);
    this.animateValue('displayEnAttente',    0, this.totalEnAttente,    1000);
  }

  private animateValue(prop: keyof this, from: number, to: number, duration: number): void {
    const startTime = performance.now();
    const update = (currentTime: number) => {
      const elapsed  = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const ease     = 1 - Math.pow(1 - progress, 3); // cubic ease-out
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
            labels: {
              color: '#94a3b8',
              font: { size: 12, family: "'DM Sans', sans-serif" },
              padding: 16,
              usePointStyle: true,
              pointStyleWidth: 8
            }
          },
          tooltip: {
            backgroundColor: '#1e293b',
            titleColor: '#e2e8f0',
            bodyColor: '#94a3b8',
            borderColor: '#334155',
            borderWidth: 1,
            padding: 12,
            cornerRadius: 8
          }
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
            labels: {
              color: '#94a3b8',
              font: { size: 12, family: "'DM Sans', sans-serif" },
              padding: 16,
              usePointStyle: true,
              pointStyleWidth: 8
            }
          },
          tooltip: {
            backgroundColor: '#1e293b',
            titleColor: '#e2e8f0',
            bodyColor: '#94a3b8',
            borderColor: '#334155',
            borderWidth: 1,
            padding: 12,
            cornerRadius: 8
          }
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

    // gradient fill
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0,   'rgba(0, 212, 170, 0.9)');
    gradient.addColorStop(1,   'rgba(0, 212, 170, 0.1)');

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
            backgroundColor: '#1e293b',
            titleColor: '#e2e8f0',
            bodyColor: '#94a3b8',
            borderColor: '#334155',
            borderWidth: 1,
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (ctx: any) => ` ${ctx.parsed.y} technicien${ctx.parsed.y > 1 ? 's' : ''}`
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(51,65,85,0.4)', drawBorder: false },
            ticks: { color: '#64748b', font: { size: 11, family: "'DM Sans', sans-serif" } }
          },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(51,65,85,0.4)', drawBorder: false },
            ticks: {
              color: '#64748b',
              font: { size: 11, family: "'DM Sans', sans-serif" },
              stepSize: 1,
              precision: 0
            }
          }
        },
        animation: { duration: 1200, easing: 'easeOutQuart' }
      }
    });
    this.charts.push(chart);
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