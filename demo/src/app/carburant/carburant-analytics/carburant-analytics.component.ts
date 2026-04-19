// src/app/carburant/carburant-analytics/carburant-analytics.component.ts
// MODIFIÉ : suppression des filtres "meilleur" et "budget" + nouveau graphique Top 3 barres groupées
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CarburantAnalyticsService } from '../../services/carburant-analytics.service';
import { CarburantVehiculeService } from '../../services/carburant-vehicule.service';
import { VehiculeService } from '../../services/vehicule.service';
import { ZoneService } from '../../services/zone.service';
import { Vehicule } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { MOIS_LABELS } from '../../models/carburant-vehicule';
import {
  HistoriqueRavitaillement, EvolutionData, Anomalie,
  ComparaisonData, DashboardCarburant, VehiculeRank
} from '../../models/carburant-analytics';

declare var Chart: any;

@Component({
  selector: 'app-carburant-analytics',
  standalone: false,
  templateUrl: './carburant-analytics.component.html',
  styleUrls: ['./carburant-analytics.component.css']
})
export class CarburantAnalyticsComponent implements OnInit, AfterViewInit, OnDestroy {

  // ── Onglet principal ───────────────────────────────────────────────────────
  activeMainTab: 'dashboard' | 'vehicule' | 'zone' = 'dashboard';

  // ── Filtres communs ───────────────────────────────────────────────────────
  annee = new Date().getFullYear();
  mois  = new Date().getMonth() + 1;
  filtreZoneId = '';
  filtreMatricule = '';
  annees      = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i);
  moisOptions = Array.from({ length: 12 }, (_, i) => i + 1);
  moisLabels  = MOIS_LABELS;

  vehicules: Vehicule[] = [];
  zones: Zone[] = [];

  // ── Dashboard ─────────────────────────────────────────────────────────────
  dashboard: DashboardCarburant | null = null;
  dashboardLoading = false;
  dashboardMode: 'mensuel' | 'annuel' = 'mensuel';

  // ── Véhicule ──────────────────────────────────────────────────────────────
  vehiculeTab: 'top' | 'detail' | 'historique' = 'top';
  comparaisonData: ComparaisonData | null = null;
  comparaisonLoading = false;
  comparaisonMode: 'mensuel' | 'annuel' = 'mensuel';

  // ✅ MODIFIÉ : seulement 3 options (suppression 'meilleur' et 'budget')
  comparaisonOnglet: 'consommation' | 'km' | 'pire' = 'consommation';

  evolutionData: EvolutionData | null = null;
  evolutionLoading = false;
  selectedVehiculeDetail = '';

  historique: HistoriqueRavitaillement[] = [];
  historiqueLoading = false;
  historiqueSearch = '';

  anomalies: Anomalie[] = [];

  // ── Zone ──────────────────────────────────────────────────────────────────
  zoneTab: 'comparaison' | 'evolution' | 'carte' = 'comparaison';
  zoneEvolutionData: EvolutionData | null = null;
  zoneEvolutionLoading = false;
  selectedZoneDetail = '';
  zoneComparaisonData: any = null;

  Math = Math;

  private charts: any[] = [];

  constructor(
    private analyticsService: CarburantAnalyticsService,
    private carburantService: CarburantVehiculeService,
    private vehiculeService:  VehiculeService,
    private zoneService:      ZoneService,
    private router:           Router
  ) {}

  ngOnInit(): void {
    this.vehiculeService.getAllVehicules().subscribe(d => this.vehicules = d);
    this.zoneService.getAllZones().subscribe(d => this.zones = d);
    this.chargerDashboard();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.charts.forEach(c => { try { c?.destroy(); } catch(e) {} });
  }

  // ── Navigation principale ─────────────────────────────────────────────────
  setMainTab(tab: 'dashboard' | 'vehicule' | 'zone'): void {
    this.activeMainTab = tab;
    this.destroyCharts();
    setTimeout(() => {
      if (tab === 'dashboard') this.chargerDashboard();
      if (tab === 'vehicule')  this.chargerComparaison();
      if (tab === 'zone')      this.chargerZoneComparaison();
    }, 50);
  }

  destroyCharts(): void {
    this.charts.forEach(c => { try { c?.destroy(); } catch(e) {} });
    this.charts = [];
  }

  // ── DASHBOARD ─────────────────────────────────────────────────────────────
  chargerDashboard(): void {
    this.dashboardLoading = true;
    const zId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    const obs = this.dashboardMode === 'mensuel'
      ? this.analyticsService.getDashboard(this.annee, this.mois, zId)
      : this.analyticsService.getDashboardAnnuel(this.annee, zId);
    obs.subscribe({
      next: d => {
        this.dashboard = d;
        this.dashboardLoading = false;
        setTimeout(() => this.buildDashboardCharts(), 200);
      },
      error: () => { this.dashboardLoading = false; }
    });
  }

  private buildDashboardCharts(): void {
    if (!this.dashboard) return;
    this.destroyCharts();
    const d = this.dashboard;

    const c1 = document.getElementById('chartDashEvo') as HTMLCanvasElement;
    if (c1 && d.evolutionMensuelle?.length) {
      const labels = d.evolutionMensuelle.map(m => (MOIS_LABELS as any)[m.mois]?.substring(0, 3) || m.mois);
      this.charts.push(new Chart(c1.getContext('2d'), {
        type: 'line',
        data: {
          labels,
          datasets: [
            { label: 'Coût réel (DT)', data: d.evolutionMensuelle.map(m => m.totalCout), borderColor: '#3b82f6', backgroundColor: '#3b82f622', borderWidth: 2, tension: 0.4, fill: true },
            { label: 'Budget (DT)', data: d.evolutionMensuelle.map(m => m.totalBudget), borderColor: '#f59e0b', borderWidth: 1.5, borderDash: [6,3], tension: 0.4, fill: false }
          ]
        },
        options: this.baseOpts('Évolution mensuelle')
      }));
    }

    const c2 = document.getElementById('chartDashZones') as HTMLCanvasElement;
    if (c2 && d.statsParZone?.length) {
      const zones = d.statsParZone.slice(0, 8);
      this.charts.push(new Chart(c2.getContext('2d'), {
        type: 'bar',
        data: {
          labels: zones.map(z => z.zoneNom),
          datasets: [
            { label: 'Coût DT', data: zones.map(z => z.totalCout), backgroundColor: '#3b82f6aa', borderColor: '#3b82f6', borderWidth: 1, borderRadius: 4 },
            { label: 'Budget DT', data: zones.map(z => z.totalBudget), backgroundColor: '#f59e0b22', borderColor: '#f59e0b', borderWidth: 1, borderRadius: 4 }
          ]
        },
        options: this.baseOpts('Coûts par zone')
      }));
    }

    const c3 = document.getElementById('chartDashCarburant') as HTMLCanvasElement;
    if (c3 && d.consommationParTypeCarburant) {
      const types = Object.keys(d.consommationParTypeCarburant);
      const vals  = Object.values(d.consommationParTypeCarburant);
      this.charts.push(new Chart(c3.getContext('2d'), {
        type: 'doughnut',
        data: {
          labels: types.map(t => this.labelCarb(t)),
          datasets: [{ data: vals, backgroundColor: ['#3b82f6','#00d4aa','#f59e0b','#8b5cf6','#ef4444'], borderColor: '#0f172a', borderWidth: 3 }]
        },
        options: { responsive: true, maintainAspectRatio: false, cutout: '65%', plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8', font: { size: 10 } } }, tooltip: this.tooltipOpts() } }
      }));
    }

    const c4 = document.getElementById('chartDashConso') as HTMLCanvasElement;
    if (c4 && d.evolutionMensuelle?.length) {
      const labels = d.evolutionMensuelle.map(m => (MOIS_LABELS as any)[m.mois]?.substring(0, 3) || m.mois);
      this.charts.push(new Chart(c4.getContext('2d'), {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'Litres', data: d.evolutionMensuelle.map(m => m.totalLitres), backgroundColor: '#00d4aa88', borderColor: '#00d4aa', borderWidth: 1, borderRadius: 4 }
          ]
        },
        options: this.baseOpts('Consommation mensuelle (L)')
      }));
    }
  }

  // ── VÉHICULE ──────────────────────────────────────────────────────────────
  setVehiculeTab(tab: 'top' | 'detail' | 'historique'): void {
    this.vehiculeTab = tab;
    this.destroyCharts();
    setTimeout(() => {
      if (tab === 'top')        this.chargerComparaison();
      if (tab === 'detail')     this.chargerEvolutionVehicule();
      if (tab === 'historique') this.chargerHistorique();
    }, 50);
  }

  chargerComparaison(): void {
    this.comparaisonLoading = true;
    const zId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    const obs = this.comparaisonMode === 'mensuel'
      ? this.analyticsService.getComparaison(this.annee, this.mois, zId)
      : this.analyticsService.getComparaisonAnnuelle(this.annee, zId);
    obs.subscribe({
      next: d => {
        this.comparaisonData = d;
        this.comparaisonLoading = false;
        setTimeout(() => this.buildComparaisonCharts(), 150);
      },
      error: () => { this.comparaisonLoading = false; }
    });
  }

  buildComparaisonCharts(): void {
    if (!this.comparaisonData) return;
    this.destroyCharts();
    const data = this.comparaisonItems;
    if (!data.length) return;

    // Chart 1 : Classement véhicules (barres horizontales)
    const c1 = document.getElementById('chartTopVehicules') as HTMLCanvasElement;
    if (c1) {
      const colors = ['#3b82f6','#00d4aa','#8b5cf6','#f59e0b','#ef4444'];
      const labelKey = this.comparaisonOnglet === 'km'
        ? 'Km parcourus'
        : this.comparaisonOnglet === 'pire'
        ? 'L/100km'
        : 'Coût (DT)';

      this.charts.push(new Chart(c1.getContext('2d'), {
        type: 'bar',
        data: {
          labels: data.map(r => r.matricule),
          datasets: [{
            label: labelKey,
            data: data.map(r =>
              this.comparaisonOnglet === 'km'   ? r.totalKm :
              this.comparaisonOnglet === 'pire' ? r.rendementLPour100km :
              r.totalCout),
            backgroundColor: colors.map(c => c + 'aa'),
            borderColor: colors,
            borderWidth: 2,
            borderRadius: 8
          }]
        },
        options: { ...this.baseOpts('Top véhicules'), plugins: { legend: { display: false }, tooltip: this.tooltipOpts() } }
      }));
    }

    // ✅ Chart 2 NOUVEAU : Barres groupées multi-indicateurs pour les Top 3
    this.buildTop3MultiBarChart();
  }

  /**
   * ✅ NOUVEAU : Graphique barres groupées comparant les Top 3 véhicules
   * sur 4 indicateurs normalisés : Coût DT, Km parcourus, L/100km, Nb anomalies
   */
  private buildTop3MultiBarChart(): void {
    if (!this.comparaisonData) return;
    const c2 = document.getElementById('chartTop3MultiBar') as HTMLCanvasElement;
    if (!c2) return;

    const top3 = (this.comparaisonData.top5Consommation || []).slice(0, 3);
    if (!top3.length) return;

    const colors = ['#3b82f6', '#00d4aa', '#f59e0b'];
    const labels = ['Coût DT', 'Km parcourus', 'L / 100km', 'Anomalies'];

    // Normaliser chaque indicateur sur 100 par rapport au max des 3
    const maxCout  = Math.max(...top3.map(r => r.totalCout), 1);
    const maxKm    = Math.max(...top3.map(r => r.totalKm), 1);
    const maxL100  = Math.max(...top3.map(r => r.rendementLPour100km), 1);
    const maxAnom  = Math.max(...top3.map(r => r.nbAnomalies), 1);

    const datasets = top3.map((r, i) => ({
      label: r.matricule,
      data: [
        Math.round(r.totalCout  / maxCout  * 100),
        Math.round(r.totalKm   / maxKm   * 100),
        Math.round(r.rendementLPour100km / maxL100 * 100),
        Math.round(r.nbAnomalies / maxAnom * 100)
      ],
      backgroundColor: colors[i] + '99',
      borderColor: colors[i],
      borderWidth: 2,
      borderRadius: 6
    }));

    this.charts.push(new Chart(c2.getContext('2d'), {
      type: 'bar',
      data: { labels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false  // Légende custom au-dessus dans le HTML
          },
          tooltip: {
            ...this.tooltipOpts(),
            callbacks: {
              label: (ctx: any) => {
                const r = top3[ctx.datasetIndex];
                const raw = [
                  `Coût : ${this.fmt3(r.totalCout)} DT`,
                  `Km : ${this.fmt0(r.totalKm)} km`,
                  `Rend. : ${this.fmt3(r.rendementLPour100km)} L/100km`,
                  `Anomalies : ${r.nbAnomalies}`
                ][ctx.dataIndex];
                return ` ${r.matricule} — ${raw}`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(51,65,85,0.4)' },
            ticks: { color: '#64748b', font: { size: 11 } }
          },
          y: {
            grid: { color: 'rgba(51,65,85,0.4)' },
            ticks: {
              color: '#64748b',
              font: { size: 10 },
              callback: (v: number) => v + '%'
            },
            beginAtZero: true,
            max: 100
          }
        },
        animation: { duration: 700, easing: 'easeOutQuart' }
      }
    }));
  }

  chargerEvolutionVehicule(): void {
    if (!this.selectedVehiculeDetail && !this.filtreMatricule) return;
    const mat = this.selectedVehiculeDetail || this.filtreMatricule;
    this.evolutionLoading = true;
    this.analyticsService.getEvolution(mat, this.annee).subscribe({
      next: d => {
        this.evolutionData = d;
        this.evolutionLoading = false;
        setTimeout(() => this.buildEvolutionCharts(), 150);
      },
      error: () => { this.evolutionLoading = false; }
    });
  }

  buildEvolutionCharts(): void {
    if (!this.evolutionData) return;
    this.destroyCharts();
    const d = this.evolutionData;

    const makeChart = (id: string, datasets: any[]) => {
      const el = document.getElementById(id) as HTMLCanvasElement;
      if (!el) return;
      this.charts.push(new Chart(el.getContext('2d'), {
        type: 'line',
        data: { labels: d.labels, datasets },
        options: this.baseOpts('')
      }));
    };

    makeChart('chartEvoConsoV', [{ label: 'Litres', data: d.consommationLitres, borderColor: '#00d4aa', backgroundColor: '#00d4aa1a', borderWidth: 2, tension: 0.4, fill: true }]);
    makeChart('chartEvoCoutV',  [
      { label: 'Coût réel (DT)', data: d.coutDinars, borderColor: '#3b82f6', backgroundColor: '#3b82f622', borderWidth: 2, tension: 0.4, fill: true },
      { label: 'Budget', data: d.budgetMensuel, borderColor: '#f59e0b', borderWidth: 1.5, borderDash: [6,3], tension: 0.4 }
    ]);

    const el = document.getElementById('chartEvoKmV') as HTMLCanvasElement;
    if (el) {
      const ctx = el.getContext('2d')!;
      const g = ctx.createLinearGradient(0, 0, 0, 200);
      g.addColorStop(0, '#8b5cf6cc'); g.addColorStop(1, '#8b5cf622');
      this.charts.push(new Chart(ctx, {
        type: 'bar',
        data: { labels: d.labels, datasets: [{ label: 'Km', data: d.kmParcourus, backgroundColor: g, borderColor: '#8b5cf6', borderWidth: 1, borderRadius: 6 }] },
        options: this.baseOpts('')
      }));
    }
  }

  chargerHistorique(): void {
    this.historiqueLoading = true;
    const mat = this.filtreMatricule;
    const obs = mat
      ? this.analyticsService.getHistorique(mat, this.annee)
      : this.analyticsService.getHistoriqueZone(this.filtreZoneId ? +this.filtreZoneId : 0, this.annee);
    obs.subscribe({
      next: d => { this.historique = d; this.historiqueLoading = false; },
      error: () => { this.historiqueLoading = false; }
    });
  }

  get filteredHistorique(): HistoriqueRavitaillement[] {
    if (!this.historiqueSearch) return this.historique;
    const q = this.historiqueSearch.toLowerCase();
    return this.historique.filter(h =>
      h.vehiculeMatricule.toLowerCase().includes(q) ||
      (h.vehiculeMarqueModele || '').toLowerCase().includes(q) ||
      h.periodeLabel.toLowerCase().includes(q)
    );
  }

  // ── ZONE ──────────────────────────────────────────────────────────────────
  setZoneTab(tab: 'comparaison' | 'evolution' | 'carte'): void {
    this.zoneTab = tab;
    this.destroyCharts();
    setTimeout(() => {
      if (tab === 'comparaison') this.chargerZoneComparaison();
      if (tab === 'evolution')   this.chargerEvolutionZone();
      if (tab === 'carte')       this.buildCarteZones();
    }, 50);
  }

  chargerZoneComparaison(): void {
    const zId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    this.analyticsService.getDashboardAnnuel(this.annee, zId).subscribe({
      next: d => {
        this.zoneComparaisonData = d;
        setTimeout(() => this.buildZoneCharts(), 150);
      }
    });
  }

  buildZoneCharts(): void {
    if (!this.zoneComparaisonData) return;
    this.destroyCharts();
    const d = this.zoneComparaisonData;

    const c1 = document.getElementById('chartZonesConso') as HTMLCanvasElement;
    if (c1 && d.statsParZone?.length) {
      const zones = d.statsParZone.slice(0, 9);
      this.charts.push(new Chart(c1.getContext('2d'), {
        type: 'bar',
        data: {
          labels: zones.map((z: any) => z.zoneNom),
          datasets: [
            { label: 'Coût total DT', data: zones.map((z: any) => z.totalCout), backgroundColor: '#3b82f6aa', borderColor: '#3b82f6', borderWidth: 1, borderRadius: 4 }
          ]
        },
        options: { ...this.baseOpts('Coût par zone'), indexAxis: 'y' as const }
      }));
    }

    const c2 = document.getElementById('chartZonesRendement') as HTMLCanvasElement;
    if (c2 && d.statsParZone?.length) {
      const zones = d.statsParZone.slice(0, 9);
      this.charts.push(new Chart(c2.getContext('2d'), {
        type: 'bar',
        data: {
          labels: zones.map((z: any) => z.zoneNom),
          datasets: [
            { label: 'L/100km', data: zones.map((z: any) => z.rendementMoyen), backgroundColor: '#00d4aa88', borderColor: '#00d4aa', borderWidth: 1, borderRadius: 4 }
          ]
        },
        options: { ...this.baseOpts('Rendement moyen par zone'), indexAxis: 'y' as const }
      }));
    }

    const c3 = document.getElementById('chartZonesBudget') as HTMLCanvasElement;
    if (c3 && d.statsParZone?.length) {
      const zones = d.statsParZone.slice(0, 9);
      this.charts.push(new Chart(c3.getContext('2d'), {
        type: 'bar',
        data: {
          labels: zones.map((z: any) => z.zoneNom),
          datasets: [
            { label: '% Budget utilisé', data: zones.map((z: any) => z.tauxBudget), backgroundColor: zones.map((z: any) => z.tauxBudget > 100 ? '#ef4444aa' : z.tauxBudget > 80 ? '#f59e0baa' : '#22c55eaa'), borderWidth: 1, borderRadius: 4 }
          ]
        },
        options: { ...this.baseOpts('% Budget par zone'), indexAxis: 'y' as const }
      }));
    }

    const c4 = document.getElementById('chartZonesVehicules') as HTMLCanvasElement;
    if (c4 && d.statsParZone?.length) {
      const zones = d.statsParZone.slice(0, 9);
      this.charts.push(new Chart(c4.getContext('2d'), {
        type: 'doughnut',
        data: {
          labels: zones.map((z: any) => z.zoneNom),
          datasets: [{ data: zones.map((z: any) => z.nbVehicules), backgroundColor: ['#3b82f6','#00d4aa','#8b5cf6','#f59e0b','#ef4444','#14b8a6','#f97316','#22c55e','#e879f9'], borderColor: '#0f172a', borderWidth: 2 }]
        },
        options: { responsive: true, maintainAspectRatio: false, cutout: '60%', plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8', font: { size: 10 } } }, tooltip: this.tooltipOpts() } }
      }));
    }
  }

  chargerEvolutionZone(): void {
    if (!this.selectedZoneDetail && !this.filtreZoneId) return;
    const zId = +(this.selectedZoneDetail || this.filtreZoneId);
    this.zoneEvolutionLoading = true;
    this.analyticsService.getEvolutionZone(zId, this.annee).subscribe({
      next: d => {
        this.zoneEvolutionData = d;
        this.zoneEvolutionLoading = false;
        setTimeout(() => this.buildZoneEvolutionCharts(), 150);
      },
      error: () => { this.zoneEvolutionLoading = false; }
    });
  }

  buildZoneEvolutionCharts(): void {
    if (!this.zoneEvolutionData) return;
    this.destroyCharts();
    const d = this.zoneEvolutionData;

    const makeChart = (id: string, datasets: any[]) => {
      const el = document.getElementById(id) as HTMLCanvasElement;
      if (!el) return;
      this.charts.push(new Chart(el.getContext('2d'), { type: 'line', data: { labels: d.labels, datasets }, options: this.baseOpts('') }));
    };

    makeChart('chartZoneEvoConso', [{ label: 'Litres', data: d.consommationLitres, borderColor: '#00d4aa', backgroundColor: '#00d4aa1a', borderWidth: 2, tension: 0.4, fill: true }]);
    makeChart('chartZoneEvoCout', [
      { label: 'Coût DT', data: d.coutDinars, borderColor: '#3b82f6', backgroundColor: '#3b82f622', borderWidth: 2, tension: 0.4, fill: true },
      { label: 'Budget', data: d.budgetMensuel, borderColor: '#f59e0b', borderWidth: 1.5, borderDash: [6,3], tension: 0.4 }
    ]);
    makeChart('chartZoneEvoKm', [{ label: 'Km', data: d.kmParcourus, borderColor: '#8b5cf6', backgroundColor: '#8b5cf622', borderWidth: 2, tension: 0.4, fill: true }]);
  }

  buildCarteZones(): void {
    setTimeout(() => {
      const el = document.getElementById('carteZonesTunisie');
      if (!el || !this.zoneComparaisonData?.statsParZone) return;
      const zones = this.zoneComparaisonData.statsParZone;
      const maxCout = Math.max(...zones.map((z: any) => z.totalCout));
      let html = `<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;padding:16px">`;
      for (const z of zones) {
        const ratio = maxCout > 0 ? z.totalCout / maxCout : 0;
        const color = ratio > 0.7 ? '#ef4444' : ratio > 0.4 ? '#f59e0b' : '#22c55e';
        html += `<div style="background:${color}22;border:1px solid ${color}44;border-radius:8px;padding:12px">
          <div style="font-size:12px;font-weight:600;color:${color};margin-bottom:4px">${z.zoneNom}</div>
          <div style="font-size:11px;color:#94a3b8">${z.nbVehicules} véh. · ${this.fmt3(z.totalCout)} DT</div>
          <div style="margin-top:6px;height:6px;border-radius:3px;background:#1e2d47"><div style="height:100%;border-radius:3px;background:${color};width:${Math.round(ratio*100)}%"></div></div>
        </div>`;
      }
      html += `</div>`;
      el.innerHTML = html;
    }, 100);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get comparaisonItems(): VehiculeRank[] {
    if (!this.comparaisonData) return [];
    const map: Record<string, VehiculeRank[]> = {
      consommation: this.comparaisonData.top5Consommation,
      km:           this.comparaisonData.top5KmParcourus,
      // ✅ Plus de 'meilleur' ni 'budget'
      pire:         this.comparaisonData.piresRendements
    };
    return map[this.comparaisonOnglet] || [];
  }

  baseOpts(title: string): any {
    return {
      responsive: true, maintainAspectRatio: false,
      plugins: {
        legend: { labels: { color: '#94a3b8', font: { size: 11 } } },
        title: title ? { display: true, text: title, color: '#e2e8f0', font: { size: 13, weight: '500' } } : { display: false },
        tooltip: this.tooltipOpts()
      },
      scales: {
        x: { grid: { color: 'rgba(51,65,85,0.4)' }, ticks: { color: '#64748b', font: { size: 11 } } },
        y: { grid: { color: 'rgba(51,65,85,0.4)' }, ticks: { color: '#64748b', font: { size: 11 } }, beginAtZero: true }
      },
      animation: { duration: 700, easing: 'easeOutQuart' }
    };
  }

  tooltipOpts(): any {
    return { backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8 };
  }

  labelCarb(t: string): string {
    return { ESSENCE: 'Essence', GASOIL_ORDINAIRE: 'Gasoil Ord.', GASOIL_SANS_SOUFRE: 'Gasoil SS', GASOIL_50: 'Gasoil 50', SUPER_SANS_PLOMB: 'Super SP' }[t] || t;
  }

  getSeveriteClass(s: string): string {
    return { CRITIQUE: 'sev-critical', ELEVEE: 'sev-high', MOYENNE: 'sev-medium', FAIBLE: 'sev-low' }[s] || '';
  }

  getTypeAnomLabel(t: string): string {
    return { BUDGET_DEPASSE: 'Budget dépassé', CONSO_ANORMALE: 'Conso. anormale', KM_INCOHERENT: 'Km incohérent', KM_ELEVE: 'Km excessifs' }[t] || t;
  }

  getStatutClass(s: string): string {
    return { NORMAL: 'stat-ok', ALERTE_BUDGET: 'stat-warn', ANOMALIE_CONSO: 'stat-danger', ANOMALIE_KM: 'stat-danger', CRITIQUE: 'stat-critical' }[s] || 'stat-ok';
  }

  fmt3(v?: number): string { return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 }); }
  fmt1(v?: number): string { return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 1, maximumFractionDigits: 1 }); }
  fmt0(v?: number): string { return Math.round(v ?? 0).toLocaleString('fr-TN'); }

  // Ajoutez cette propriété getter
get hasTop3(): boolean {
  return !!(this.comparaisonData?.top5Consommation?.length && this.comparaisonData.top5Consommation.length >= 1);
}

  navigateTo(r: string): void { this.router.navigate([r]); }
  navigateToCarburant(): void { this.router.navigate(['/admin/carburant']); }
}