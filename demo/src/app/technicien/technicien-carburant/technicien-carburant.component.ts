// src/app/technicien/technicien-carburant/technicien-carburant.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {
  TechnicienCarburantService,
  VehiculeCarburantResume,
  HistoriqueCarburant,
  CarburantStats,
  DashboardTechnicien,
  SaisieCarburantRequest
} from '../../services/technicien-carburant.service';

export const MOIS_LABELS: Record<number, string> = {
  1:'Janvier',2:'Février',3:'Mars',4:'Avril',5:'Mai',6:'Juin',
  7:'Juillet',8:'Août',9:'Septembre',10:'Octobre',11:'Novembre',12:'Décembre'
};

@Component({
  selector: 'app-technicien-carburant',
  standalone: false,
  templateUrl: './technicien-carburant.component.html',
  styleUrls: ['./technicien-carburant.component.css']
})
export class TechnicienCarburantComponent implements OnInit {

  // ── State ────────────────────────────────────────────────────
  view: 'dashboard' | 'saisie' | 'historique' | 'analytics' = 'dashboard';
  loading = true;
  nomAffiche = localStorage.getItem('nom') || 'Technicien';
  estConducteur = false;

  vehicules: VehiculeCarburantResume[] = [];
  dashboard: DashboardTechnicien | null = null;

  // Saisie
  editMode = false;
  editId: number | null = null;
  submitting = false;
  prefillLoading = false;
  successMsg = '';
  budgetAlertMsg = '';
  errorMsg = '';

  form: SaisieCarburantRequest = {
    vehiculeMatricule: '',
    annee: new Date().getFullYear(),
    mois: new Date().getMonth() + 1,
    indexDemarrageMois: 0,
    indexFinMois: 0,
    montantRestantMoisPrecedent: 0,
    ravitaillementMoisPrecedent: 0,
    ravitaillementMois: 0
  };

  vehiculeSelectionne: VehiculeCarburantResume | null = null;

  // Historique
  historique: HistoriqueCarburant[] = [];
  historiqueLoading = false;
  historiqueMatricule = '';
  historiqueAnnee = new Date().getFullYear();

  // Analytics
  stats: CarburantStats | null = null;
  analyticsLoading = false;
  analyticsMatricule = '';
  analyticsAnnee = new Date().getFullYear();

  moisLabels = MOIS_LABELS;
  moisOptions = Array.from({ length: 12 }, (_, i) => i + 1);
  annees = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);

  Math = Math; // pour template

  constructor(
    private authService: AuthService,
    private carburantService: TechnicienCarburantService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.estConducteur = this.authService.isConducteur();
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.carburantService.getMesVehicules().subscribe({
      next: (data) => {
        this.vehicules = data || [];
        this.loading = false;
        this.loadDashboard();
      },
      error: () => { this.loading = false; }
    });
  }

  loadDashboard(): void {
    this.carburantService.getDashboard().subscribe({
      next: (d) => { this.dashboard = d; },
      error: () => {}
    });
  }

  // ── Saisie ───────────────────────────────────────────────────

  ouvrirSaisie(v: VehiculeCarburantResume): void {
    this.resetForm();
    this.form.vehiculeMatricule = v.matricule;
    this.form.annee = new Date().getFullYear();
    this.form.mois = new Date().getMonth() + 1;
    this.vehiculeSelectionne = v;
    this.view = 'saisie';
    this.chargerPrefill();
  }

  onVehiculeChange(): void {
    this.vehiculeSelectionne = this.vehicules.find(v => v.matricule === this.form.vehiculeMatricule) || null;
    if (this.vehiculeSelectionne) this.chargerPrefill();
  }

  onPeriodeChange(): void {
    if (this.form.vehiculeMatricule) this.chargerPrefill();
  }

  chargerPrefill(): void {
    if (!this.form.vehiculeMatricule) return;
    this.prefillLoading = true;
    this.carburantService.getPrefill(this.form.vehiculeMatricule, this.form.annee, this.form.mois).subscribe({
      next: (p) => {
        this.form.indexDemarrageMois = p.indexDemarrageMois;
        this.form.montantRestantMoisPrecedent = p.montantRestantMoisPrecedent;
        this.form.ravitaillementMoisPrecedent = p.ravitaillementMoisPrecedent;
        this.prefillLoading = false;
      },
      error: () => { this.prefillLoading = false; }
    });
  }

  soumettre(): void {
    if (!this.form.vehiculeMatricule) return;
    this.submitting = true;
    this.successMsg = '';
    this.budgetAlertMsg = '';
    this.errorMsg = '';

    const obs = this.editMode && this.editId
      ? this.carburantService.modifier(this.editId, this.form)
      : this.carburantService.saisir(this.form);

    obs.subscribe({
      next: (res: any) => {
        this.submitting = false;
        if (res?.alert || res?.data?.budgetDepasse) {
          this.budgetAlertMsg = res.message || 'Saisie enregistrée — Budget dépassé !';
          setTimeout(() => {
            this.budgetAlertMsg = '';
            this.resetForm();
            this.charger();
            this.view = 'dashboard';
          }, 3000);
        } else {
          this.successMsg = res.message || 'Ravitaillement enregistré avec succès !';
          setTimeout(() => {
            this.successMsg = '';
            this.resetForm();
            this.charger();
            this.view = 'dashboard';
          }, 2000);
        }
      },
      error: (err: any) => {
        this.submitting = false;
        this.errorMsg = err.error?.message || 'Erreur lors de la sauvegarde';
      }
    });
  }

  resetForm(): void {
    this.editMode = false;
    this.editId = null;
    this.vehiculeSelectionne = null;
    this.successMsg = '';
    this.budgetAlertMsg = '';
    this.errorMsg = '';
    this.form = {
      vehiculeMatricule: '',
      annee: new Date().getFullYear(),
      mois: new Date().getMonth() + 1,
      indexDemarrageMois: 0,
      indexFinMois: 0,
      montantRestantMoisPrecedent: 0,
      ravitaillementMoisPrecedent: 0,
      ravitaillementMois: 0
    };
  }

  // ── Historique ───────────────────────────────────────────────

  ouvrirHistorique(v: VehiculeCarburantResume): void {
    this.historiqueMatricule = v.matricule;
    this.view = 'historique';
    this.chargerHistorique();
  }

  chargerHistorique(): void {
    if (!this.historiqueMatricule) return;
    this.historiqueLoading = true;
    this.carburantService.getHistorique(this.historiqueMatricule, this.historiqueAnnee).subscribe({
      next: (d) => { this.historique = d || []; this.historiqueLoading = false; },
      error: () => { this.historiqueLoading = false; }
    });
  }

  // ── Analytics ─────────────────────────────────────────────────

  ouvrirAnalytics(v: VehiculeCarburantResume): void {
    this.analyticsMatricule = v.matricule;
    this.analyticsAnnee = new Date().getFullYear();
    this.view = 'analytics';
    this.chargerAnalytics();
  }

  chargerAnalytics(): void {
    if (!this.analyticsMatricule) return;
    this.analyticsLoading = true;
    this.carburantService.getStats(this.analyticsMatricule, this.analyticsAnnee).subscribe({
      next: (s) => { this.stats = s; this.analyticsLoading = false; },
      error: () => { this.analyticsLoading = false; }
    });
  }

  // ── Calculs temps réel ─────────────────────────────────────────

  get prix(): number { return this.vehiculeSelectionne?.prixCarburant || 0; }
  get cout(): number { return this.vehiculeSelectionne?.coutDuMois || 0; }

  get totalRavitaillementLitres(): number {
    if (!this.prix) return 0;
    return (+this.form.ravitaillementMoisPrecedent + +this.form.montantRestantMoisPrecedent) / this.prix;
  }

  get quantiteRestanteReservoir(): number {
    if (!this.prix) return 0;
    return +this.form.montantRestantMoisPrecedent / this.prix;
  }

  get distanceParcourue(): number {
    return +this.form.indexFinMois - +this.form.indexDemarrageMois;
  }

  get pourcentageConsommation(): number {
    if (!this.distanceParcourue) return 0;
    return (this.totalRavitaillementLitres - this.quantiteRestanteReservoir) / this.distanceParcourue;
  }

  get carburantDemandeDinars(): number {
    return this.cout - +this.form.montantRestantMoisPrecedent;
  }

  get consommationReelleDT(): number {
    return (this.totalRavitaillementLitres - this.quantiteRestanteReservoir) * this.prix;
  }

  get budgetDepasseReel(): boolean {
    return this.cout > 0 && this.consommationReelleDT > this.cout;
  }

  get depassementMontant(): number {
    return Math.max(0, this.consommationReelleDT - this.cout);
  }

  // ── Totaux historique ─────────────────────────────────────────

  get totalKmHisto(): number {
    return this.historique.reduce((s, h) => s + h.distanceParcourue, 0);
  }

  get totalLitresHisto(): number {
    return this.historique.reduce((s, h) => s + h.consoLitres, 0);
  }

  get totalCoutHisto(): number {
    return this.historique.reduce((s, h) => s + h.coutReel, 0);
  }

  // ── Helpers UI ────────────────────────────────────────────────

  getCarburantColor(type: string | undefined): string {
    const colors: Record<string, string> = {
      ESSENCE: '#ef4444',
      GASOIL_ORDINAIRE: '#3b82f6',
      GASOIL_SANS_SOUFRE: '#8b5cf6',
      GASOIL_50: '#f59e0b',
      SUPER_SANS_PLOMB: '#22c55e'
    };
    return colors[type || ''] || '#64748b';
  }

  getCarburantLabel(type: string | undefined): string {
    const l: Record<string, string> = {
      ESSENCE: 'Essence', GASOIL_ORDINAIRE: 'Gasoil Ord.',
      GASOIL_SANS_SOUFRE: 'Gasoil SS', GASOIL_50: 'Gasoil 50',
      SUPER_SANS_PLOMB: 'Super SP'
    };
    return l[type || ''] || type || '—';
  }

  getBarHeight(val: number, data: any[], field = 'km'): number {
    const max = Math.max(...data.map((d: any) => d[field] || 0));
    return max > 0 ? (val / max) * 100 : 0;
  }

  get moisCourantLabel(): string {
    return MOIS_LABELS[new Date().getMonth() + 1] + ' ' + new Date().getFullYear();
  }

  get initiales(): string {
    return (this.nomAffiche || '?').split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2);
  }

  // ── Format helpers ────────────────────────────────────────────

  fmt0(n: number): string { return Math.round(n || 0).toLocaleString('fr-TN'); }
  fmt3(n: number): string {
    return (n || 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }
  fmt6(n: number): string {
    return (n || 0).toLocaleString('fr-TN', { minimumFractionDigits: 6, maximumFractionDigits: 6 });
  }

  nav(r: string): void { this.router.navigate([r]); }
  logout(): void { this.authService.logout(); }
}