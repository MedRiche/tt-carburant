// src/app/carburant/carburant-analytics/carburant-analytics.component.ts
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CarburantAnalyticsService } from '../../services/carburant-analytics.service';
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

  // ── Navigation ────────────────────────────────────────────────────────────
  activeTab: 'historique' | 'evolution' | 'anomalies' | 'comparaison' | 'dashboard' = 'dashboard';

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

  // ── 1. Historique ─────────────────────────────────────────────────────────
  historique: HistoriqueRavitaillement[] = [];
  historiqueLoading = false;
  historiqueSearch = '';

  // ── 2. Évolution ──────────────────────────────────────────────────────────
  evolutionData: EvolutionData | null = null;
  evolutionLoading = false;
  evolutionMode: 'vehicule' | 'zone' = 'zone';
  private chartEvoConso: any = null;
  private chartEvoCout:  any = null;
  private chartEvoKm:    any = null;

  // ── 3. Anomalies ──────────────────────────────────────────────────────────
  anomalies: Anomalie[] = [];
  anomaliesLoading = false;
  anomaliesMode: 'mensuel' | 'annuel' = 'mensuel';
  filtreTypeAnomalie = '';
  filtreSeverite = '';

  // ── 4. Comparaison ────────────────────────────────────────────────────────
  comparaisonData: ComparaisonData | null = null;  // Renommé (suppression du ç)
  comparaisonLoading = false;
  comparaisonMode: 'mensuel' | 'annuel' = 'mensuel';
  comparaisonOnglet: 'consommation' | 'km' | 'meilleur' | 'pire' | 'budget' = 'consommation';
  private chartComparaison: any = null;

  // ── 5. Dashboard avancé ───────────────────────────────────────────────────
  dashboard: DashboardCarburant | null = null;
  dashboardLoading = false;
  dashboardMode: 'mensuel' | 'annuel' = 'mensuel';
  private chartZones: any    = null;
  private chartCarburant: any = null;
  private chartBudget: any   = null;
  private chartEvoDash: any  = null;

  Math = Math;

  constructor(
    private analyticsService: CarburantAnalyticsService,
    private vehiculeService:  VehiculeService,
    private zoneService:      ZoneService,
    private router:           Router
  ) {}

  ngOnInit(): void {
    this.vehiculeService.getAllVehicules().subscribe(d => this.vehicules = d);
    this.zoneService.getAllZones().subscribe(d => { this.zones = d; });
    this.chargerDashboard();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    [this.chartEvoConso, this.chartEvoCout, this.chartEvoKm,
     this.chartComparaison, this.chartZones, this.chartCarburant,
     this.chartBudget, this.chartEvoDash]
      .forEach(c => c?.destroy());
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
    setTimeout(() => {
      if (tab === 'evolution')   this.chargerEvolution();
      if (tab === 'anomalies')   this.chargerAnomalies();
      if (tab === 'comparaison') this.chargerComparaison();
      if (tab === 'dashboard')   this.chargerDashboard();
      if (tab === 'historique')  this.chargerHistorique();
    }, 50);
  }

  // ── 1. HISTORIQUE ─────────────────────────────────────────────────────────
  chargerHistorique(): void {
    this.historiqueLoading = true;
    const obs = this.filtreMatricule
      ? this.analyticsService.getHistorique(this.filtreMatricule, this.annee)
      : this.filtreZoneId
          ? this.analyticsService.getHistoriqueZone(+this.filtreZoneId, this.annee)
          : this.analyticsService.getHistoriqueZone(0, this.annee);

    obs.subscribe({
      next: d => { this.historique = d; this.historiqueLoading = false; },
      error: () => { this.historiqueLoading = false; }
    });
  }

  // Getter sans accent
  get filteredHistorique(): HistoriqueRavitaillement[] {
    if (!this.historiqueSearch) return this.historique;
    const q = this.historiqueSearch.toLowerCase();
    return this.historique.filter(h =>
      h.vehiculeMatricule.toLowerCase().includes(q) ||
      h.vehiculeMarqueModele?.toLowerCase().includes(q) ||
      h.periodeLabel.toLowerCase().includes(q)
    );
  }

  getStatutClass(statut: string): string {
    const map: Record<string, string> = {
      NORMAL: 'stat-ok', ALERTE_BUDGET: 'stat-warn',
      ANOMALIE_CONSO: 'stat-danger', ANOMALIE_KM: 'stat-danger', CRITIQUE: 'stat-critical'
    };
    return map[statut] || 'stat-ok';
  }

  // ── 2. ÉVOLUTION ──────────────────────────────────────────────────────────
  chargerEvolution(): void {
    this.evolutionLoading = true;
    const obs = this.evolutionMode === 'vehicule' && this.filtreMatricule
      ? this.analyticsService.getEvolution(this.filtreMatricule, this.annee)
      : this.filtreZoneId
          ? this.analyticsService.getEvolutionZone(+this.filtreZoneId, this.annee)
          : null;

    if (!obs) { this.evolutionLoading = false; return; }

    obs.subscribe({
      next: d => {
        this.evolutionData = d;
        this.evolutionLoading = false;
        setTimeout(() => this.buildEvolutionCharts(), 100);
      },
      error: () => { this.evolutionLoading = false; }
    });
  }

  private buildEvolutionCharts(): void {
    if (!this.evolutionData) return;
    this.chartEvoConso?.destroy();
    this.chartEvoCout?.destroy();
    this.chartEvoKm?.destroy();

    const d = this.evolutionData;

    this.chartEvoConso = this.buildLineChart('chartEvoConso', d.labels, [
      { label: 'Consommation (L)', data: d.consommationLitres, color: '#00d4aa' }
    ]);
    this.chartEvoCout = this.buildLineChart('chartEvoCout', d.labels, [
      { label: 'Coût réel (DT)', data: d.coutDinars, color: '#3b82f6' },
      { label: 'Budget (DT)',     data: d.budgetMensuel, color: '#f59e0b', dashed: true }
    ]);
    this.chartEvoKm = this.buildBarChart('chartEvoKm', d.labels,
      d.kmParcourus, '#8b5cf6', 'Distance (km)');
  }

  private buildLineChart(id: string, labels: string[], datasets: any[]): any {
    const canvas = document.getElementById(id) as HTMLCanvasElement;
    if (!canvas) return null;
    return new Chart(canvas.getContext('2d'), {
      type: 'line',
      data: {
        labels,
        datasets: datasets.map(ds => ({
          label: ds.label,
          data: ds.data,
          borderColor: ds.color,
          backgroundColor: ds.color + '1a',
          borderWidth: 2,
          pointRadius: 4,
          tension: 0.4,
          fill: true,
          borderDash: ds.dashed ? [6, 3] : undefined
        }))
      },
      options: this.baseChartOptions()
    });
  }

  private buildBarChart(id: string, labels: string[], data: number[], color: string, label: string): any {
    const canvas = document.getElementById(id) as HTMLCanvasElement;
    if (!canvas) return null;
    const ctx = canvas.getContext('2d');
    const grad = ctx!.createLinearGradient(0, 0, 0, 200);
    grad.addColorStop(0, color + 'cc');
    grad.addColorStop(1, color + '22');
    return new Chart(ctx, {
      type: 'bar',
      data: { labels, datasets: [{ label, data, backgroundColor: grad, borderColor: color, borderWidth: 1.5, borderRadius: 6 }] },
      options: this.baseChartOptions()
    });
  }

  private baseChartOptions(): any {
    return {
      responsive: true, maintainAspectRatio: false,
      plugins: {
        legend: { labels: { color: '#94a3b8', font: { size: 11 } } },
        tooltip: { backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8 }
      },
      scales: {
        x: { grid: { color: 'rgba(51,65,85,0.4)' }, ticks: { color: '#64748b', font: { size: 11 } } },
        y: { grid: { color: 'rgba(51,65,85,0.4)' }, ticks: { color: '#64748b', font: { size: 11 } }, beginAtZero: true }
      },
      animation: { duration: 800, easing: 'easeOutQuart' }
    };
  }

  // ── 3. ANOMALIES ──────────────────────────────────────────────────────────
  chargerAnomalies(): void {
    this.anomaliesLoading = true;
    const zId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    const obs = this.anomaliesMode === 'mensuel'
      ? this.analyticsService.getAnomalies(this.annee, this.mois, zId)
      : this.analyticsService.getAnomaliesAnnee(this.annee, zId);

    obs.subscribe({
      next: d => { this.anomalies = d; this.anomaliesLoading = false; },
      error: () => { this.anomaliesLoading = false; }
    });
  }

  // Getter sans accent
  get filteredAnomalies(): Anomalie[] {
    return this.anomalies.filter(a =>
      (!this.filtreTypeAnomalie || a.typeAnomalie === this.filtreTypeAnomalie) &&
      (!this.filtreSeverite     || a.severite === this.filtreSeverite)
    );
  }

  getSeveriteClass(s: string): string {
    return { CRITIQUE: 'sev-critical', ELEVEE: 'sev-high', MOYENNE: 'sev-medium', FAIBLE: 'sev-low' }[s] || '';
  }
  getTypeAnomLabel(t: string): string {
    return {
      BUDGET_DEPASSE: '💸 Budget dépassé',
      CONSO_ANORMALE: '⛽ Conso. anormale',
      KM_INCOHERENT:  '🔍 Km incohérent',
      KM_ELEVE:       '📍 Km excessifs'
    }[t] || t;
  }

  // ── 4. COMPARAISON ────────────────────────────────────────────────────────
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
        setTimeout(() => this.buildComparaisonChart(), 100);
      },
      error: () => { this.comparaisonLoading = false; }
    });
  }

  // Getter pour les données du tableau (sans accent)
  get comparaisonItems(): VehiculeRank[] {
    if (!this.comparaisonData) return [];
    const map: Record<string, VehiculeRank[]> = {
      consommation: this.comparaisonData.top5Consommation,
      km:           this.comparaisonData.top5KmParcourus,
      meilleur:     this.comparaisonData.meilleursRendements,
      pire:         this.comparaisonData.piresRendements,
      budget:       this.comparaisonData.plusGrandsBudgetDepasses
    };
    return map[this.comparaisonOnglet] || [];
  }

// Remplacer la déclaration privée par une méthode publique
public buildComparaisonChart(): void {
  if (!this.comparaisonData) return;
  this.chartComparaison?.destroy();
  const data = this.comparaisonItems;
  if (!data.length) return;

  const canvas = document.getElementById('chartComparaison') as HTMLCanvasElement;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const labels = data.map(r => r.matricule);
  const values = data.map(r =>
    this.comparaisonOnglet === 'km'       ? r.totalKm :
    this.comparaisonOnglet === 'meilleur' || this.comparaisonOnglet === 'pire'
                                          ? r.rendementLPour100km :
    this.comparaisonOnglet === 'budget'   ? r.tauxBudgetMoyen :
                                            r.totalCout
  );
  const colors = ['#00d4aa','#3b82f6','#8b5cf6','#f59e0b','#ef4444'];
  this.chartComparaison = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: labels.map((_, i) => colors[i % colors.length] + 'bb'),
        borderColor:     labels.map((_, i) => colors[i % colors.length]),
        borderWidth: 2, borderRadius: 8
      }]
    },
    options: { ...this.baseChartOptions(), plugins: { ...this.baseChartOptions().plugins, legend: { display: false } } }
  });
}

  // ── 5. DASHBOARD AVANCÉ ───────────────────────────────────────────────────
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
        setTimeout(() => this.buildDashboardCharts(), 100);
      },
      error: () => { this.dashboardLoading = false; }
    });
  }

  private buildDashboardCharts(): void {
    if (!this.dashboard) return;
    [this.chartZones, this.chartCarburant, this.chartBudget, this.chartEvoDash]
      .forEach(c => c?.destroy());

    const d = this.dashboard;

    const czCanvas = document.getElementById('chartZones') as HTMLCanvasElement;
    if (czCanvas && d.statsParZone.length) {
      const zLabels = d.statsParZone.map(z => z.zoneNom);
      const zData   = d.statsParZone.map(z => z.totalCout);
      this.chartZones = new Chart(czCanvas.getContext('2d'), {
        type: 'bar',
        data: {
          labels: zLabels,
          datasets: [{ label: 'Coût (DT)', data: zData,
            backgroundColor: zLabels.map((_, i) => ['#00d4aa','#3b82f6','#8b5cf6','#f59e0b','#ef4444','#14b8a6','#f97316','#22c55e','#e879f9'][i % 9] + 'aa'),
            borderWidth: 1.5, borderRadius: 6 }]
        },
        options: { indexAxis: 'y' as const, ...this.baseChartOptions() }
      });
    }

    const ccCanvas = document.getElementById('chartCarburantTypes') as HTMLCanvasElement;
    if (ccCanvas) {
      const types  = Object.keys(d.consommationParTypeCarburant);
      const values = Object.values(d.consommationParTypeCarburant);
      this.chartCarburant = new Chart(ccCanvas.getContext('2d'), {
        type: 'doughnut',
        data: {
          labels: types.map(t => this.labelCarburant(t)),
          datasets: [{ data: values, backgroundColor: ['#3b82f6','#00d4aa','#f59e0b','#8b5cf6','#ef4444'],
                       borderColor: '#0f172a', borderWidth: 3, hoverOffset: 8 }]
        },
        options: { responsive: true, maintainAspectRatio: false, cutout: '65%',
          plugins: { legend: { position: 'bottom' as const, labels: { color: '#94a3b8', font: { size: 10 } } },
                     tooltip: { backgroundColor: '#1e293b', titleColor: '#e2e8f0', bodyColor: '#94a3b8', cornerRadius: 8 } }
        }
      });
    }

    const cbCanvas = document.getElementById('chartBudgetZones') as HTMLCanvasElement;
    if (cbCanvas && d.statsParZone.length) {
      const zLabels = d.statsParZone.slice(0, 9).map(z => z.zoneNom);
      this.chartBudget = new Chart(cbCanvas.getContext('2d'), {
        type: 'bar',
        data: {
          labels: zLabels,
          datasets: [
            { label: 'Coût réel',  data: d.statsParZone.slice(0, 9).map(z => z.totalCout),   backgroundColor: '#3b82f6aa', borderColor: '#3b82f6', borderWidth: 1, borderRadius: 4 },
            { label: 'Budget',     data: d.statsParZone.slice(0, 9).map(z => z.totalBudget), backgroundColor: '#f59e0b22', borderColor: '#f59e0b', borderWidth: 1.5, borderRadius: 4, borderDash: [4, 4] }
          ]
        },
        options: this.baseChartOptions()
      });
    }

    const ceCanvas = document.getElementById('chartEvoDash') as HTMLCanvasElement;
    if (ceCanvas && d.evolutionMensuelle.length) {
      const labels = d.evolutionMensuelle.map(m => m.label.substring(0, 3));
      this.chartEvoDash = new Chart(ceCanvas.getContext('2d'), {
        type: 'line',
        data: {
          labels,
          datasets: [
            { label: 'Coût (DT)', data: d.evolutionMensuelle.map(m => m.totalCout), borderColor: '#00d4aa', backgroundColor: '#00d4aa1a', borderWidth: 2, tension: 0.4, fill: true },
            { label: 'Budget',    data: d.evolutionMensuelle.map(m => m.totalBudget), borderColor: '#f59e0b', backgroundColor: 'transparent', borderWidth: 1.5, borderDash: [6, 3], tension: 0.4 }
          ]
        },
        options: this.baseChartOptions()
      });
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  labelCarburant(t: string): string {
    return { ESSENCE: 'Essence', GASOIL_ORDINAIRE: 'Gasoil Ord.', GASOIL_SANS_SOUFRE: 'Gasoil SS',
             GASOIL_50: 'Gasoil 50', SUPER_SANS_PLOMB: 'Super SP' }[t] || t;
  }
  fmt3(v?: number): string {
    return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }
  fmt1(v?: number): string {
    return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  }
  fmt0(v?: number): string {
    return Math.round(v ?? 0).toLocaleString('fr-TN');
  }

  navigateTo(r: string): void { this.router.navigate([r]); }
}