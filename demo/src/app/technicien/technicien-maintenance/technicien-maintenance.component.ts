// src/app/technicien/technicien-maintenance/technicien-maintenance.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TechnicienMaintenanceService, MaintenanceDashboardTech } from '../../services/technicien-maintenance.service';
import { TechnicienEquipementService } from '../../services/technicien-equipement.service';
import {
  Maintenance, MaintenanceRequest, DetailMaintenance,
  TypeIntervention, StatutMaintenance, TypeDetailMaintenance
} from '../../models/maintenance';
import { Vehicule } from '../../models/vehicule';

@Component({
  selector: 'app-technicien-maintenance',
  standalone: false,
  templateUrl: './technicien-maintenance.component.html',
  styleUrls: ['./technicien-maintenance.component.css']
})
export class TechnicienMaintenanceComponent implements OnInit {

  // ── UI State ─────────────────────────────────────────────────
  view: 'dashboard' | 'liste' | 'form' = 'dashboard';
  loading        = true;
  loadingListe   = false;
  submitting     = false;
  exportLoading  = false;
  deleteLoading  = false;

  // ── Auth ─────────────────────────────────────────────────────
  nomAffiche = localStorage.getItem('nom') || 'Technicien';

  get initiales(): string {
    return (this.nomAffiche || '?').split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2);
  }

  // ── Data ─────────────────────────────────────────────────────
  maintenances: Maintenance[]          = [];
  filtres: Maintenance[]               = [];
  vehicules: Vehicule[]                = [];
  dashboard: MaintenanceDashboardTech | null = null;
  selectedMaintenance: Maintenance | null   = null;

  // ── Analytics helpers ────────────────────────────────────────
  get topVehiculesArray(): { matricule: string; htva: number }[] {
    if (!this.dashboard?.topVehicules) return [];
    return Object.entries(this.dashboard.topVehicules).map(([matricule, htva]) => ({ matricule, htva }));
  }

  getParTypeArray(): { type: string; count: number }[] {
    if (!this.dashboard?.parType) return [];
    return Object.entries(this.dashboard.parType).map(([type, count]) => ({ type, count }));
  }

  // ── Filtres ──────────────────────────────────────────────────
  searchQ      = '';
  filtreStatut = '';
  filtreType   = '';

  // ── Totaux ──────────────────────────────────────────────────
  get totalHtva(): number {
    return this.filtres.reduce((s, m) => s + (m.coutTotalHtva || 0), 0);
  }

  // ── Formulaire ──────────────────────────────────────────────
  editMode = false;
  successMsg = '';
  errorMsg   = '';

  form: MaintenanceRequest = {
    numeroDossier: '',
    vehiculeMatricule: '',
    dateIntervention: undefined,
    typeIntervention: TypeIntervention.CORRECTIVE,
    statut: StatutMaintenance.EN_COURS,
    description: '',
    details: []
  };
  TypeDetailMaintenance = TypeDetailMaintenance;

  newDetail: Partial<DetailMaintenance> = {
    type: TypeDetailMaintenance.MAIN_D_OEUVRE,
    marque: '', numero: '', numeroPiece: '',
    designation: '', quantite: 1, montantUnitaire: 0
  };

  typesIntervention = Object.values(TypeIntervention);
  statuts           = Object.values(StatutMaintenance);

  get mainDoeuvreDetails(): DetailMaintenance[] {
    return (this.form.details || []).filter(d => d.type === TypeDetailMaintenance.MAIN_D_OEUVRE);
  }
  get piecesDetails(): DetailMaintenance[] {
    return (this.form.details || []).filter(d => d.type === TypeDetailMaintenance.PIECE);
  }
  get newDetailTotal(): number {
    return (this.newDetail.quantite || 0) * (this.newDetail.montantUnitaire || 0);
  }
  get totalHtvaForm(): number {
    return (this.form.details || []).reduce((s, d) => s + ((d.quantite || 0) * (d.montantUnitaire || 0)), 0);
  }

  // ── Suppression ──────────────────────────────────────────────
  showDeleteConfirm = false;
  deleteTarget: Maintenance | null = null;
  get deleteTargetLabel(): string {
    return this.deleteTarget ? `#${this.deleteTarget.numeroDossier}` : '';
  }

  constructor(
    private authService: AuthService,
    private maintenanceService: TechnicienMaintenanceService,
    private equipementService: TechnicienEquipementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  // ══════════════════════════════════════════════════════════════
  //  CHARGEMENT
  // ══════════════════════════════════════════════════════════════

  charger(): void {
    this.loading = true;
    this.maintenanceService.getAll().subscribe({
      next: (data) => {
        this.maintenances = data || [];
        this.filtres      = [...this.maintenances];
        this.loading      = false;
        this.chargerDashboard();
      },
      error: () => { this.loading = false; }
    });
    this.equipementService.getMesVehicules().subscribe({
      next: (v) => { this.vehicules = v || []; },
      error: () => {}
    });
  }

  chargerDashboard(): void {
    this.maintenanceService.getDashboard().subscribe({
      next: (d) => { this.dashboard = d; },
      error: () => {}
    });
  }

  // ══════════════════════════════════════════════════════════════
  //  FILTRES
  // ══════════════════════════════════════════════════════════════

  filtrer(): void {
    let list = [...this.maintenances];
    const q  = this.searchQ.toLowerCase().trim();
    if (q) {
      list = list.filter(m =>
        (m.numeroDossier || '').toLowerCase().includes(q) ||
        m.vehiculeMatricule.toLowerCase().includes(q) ||
        (m.vehiculeMarqueModele || '').toLowerCase().includes(q) ||
        (m.description || '').toLowerCase().includes(q)
      );
    }
    if (this.filtreStatut) list = list.filter(m => m.statut === this.filtreStatut);
    if (this.filtreType)   list = list.filter(m => m.typeIntervention === this.filtreType);
    this.filtres = list;
  }

  // ══════════════════════════════════════════════════════════════
  //  NAVIGATION DOSSIER
  // ══════════════════════════════════════════════════════════════

  voirDossier(m: Maintenance): void {
    if (!m.id) return;
    this.maintenanceService.getById(m.id).subscribe({
      next: (full) => { this.selectedMaintenance = full; },
      error: ()    => { this.selectedMaintenance = m;    }
    });
  }

  voirDossierById(id: number): void {
    this.maintenanceService.getById(id).subscribe({
      next: (full) => { this.selectedMaintenance = full; },
      error: () => {}
    });
  }

  fermerDetail(): void { this.selectedMaintenance = null; }

  // ══════════════════════════════════════════════════════════════
  //  FORMULAIRE
  // ══════════════════════════════════════════════════════════════

  ouvrirCreation(): void {
    this.editMode  = false;
    this.form      = {
      numeroDossier: '',
      vehiculeMatricule: '',
      dateIntervention: new Date().toISOString().split('T')[0],
      typeIntervention: TypeIntervention.CORRECTIVE,
      statut: StatutMaintenance.EN_COURS,
      description: '',
      details: []
    };
    this.resetNewDetail();
    this.successMsg = '';
    this.errorMsg   = '';
    this.view       = 'form';
  }

  editerDossier(m: Maintenance): void {
    const load = (full: Maintenance) => {
      this.editMode = true;
      this.form = {
        numeroDossier:    full.numeroDossier,
        vehiculeMatricule: full.vehiculeMatricule,
        dateIntervention: full.dateIntervention,
        typeIntervention: full.typeIntervention,
        statut:           full.statut,
        description:      full.description || '',
        details:          JSON.parse(JSON.stringify(full.details || []))
      };
      this.successMsg = '';
      this.errorMsg   = '';
      this.view       = 'form';
      this.selectedMaintenance = null;
    };

    if (m.id && (!m.details || m.details.length === 0)) {
      this.maintenanceService.getById(m.id).subscribe({
        next: load, error: () => load(m)
      });
    } else {
      load(m);
    }
  }

  annuler(): void {
    this.view = 'liste';
    this.editMode = false;
    this.successMsg = '';
    this.errorMsg   = '';
  }

  soumettre(): void {
    if (!this.form.vehiculeMatricule || !this.form.numeroDossier) return;
    this.submitting = true;
    this.successMsg = '';
    this.errorMsg   = '';

    const req: MaintenanceRequest = {
      ...this.form,
      details: (this.form.details || []).map(d => ({
        ...d,
        totalHtva: Math.round((d.quantite || 0) * (d.montantUnitaire || 0) * 1000) / 1000
      }))
    };

    const editId = this.editMode
      ? this.maintenances.find(m => m.numeroDossier === this.form.numeroDossier)?.id
      : undefined;

    const obs = (this.editMode && editId)
      ? this.maintenanceService.modifier(editId, req)
      : this.maintenanceService.creer(req);

    obs.subscribe({
      next: () => {
        this.submitting = false;
        this.successMsg = this.editMode ? 'Dossier modifié avec succès !' : 'Dossier créé avec succès !';
        setTimeout(() => {
          this.successMsg = '';
          this.charger();
          this.view = 'liste';
        }, 1800);
      },
      error: (err: any) => {
        this.submitting = false;
        this.errorMsg   = err.error?.message || 'Erreur lors de la sauvegarde';
      }
    });
  }

  // ── Gestion des détails ──────────────────────────────────────

  ajouterDetail(): void {
    if (!this.newDetail.designation?.trim()) {
      this.errorMsg = 'La désignation est obligatoire';
      return;
    }
    const d: DetailMaintenance = {
      type:           this.newDetail.type || TypeDetailMaintenance.MAIN_D_OEUVRE,
      numeroDossier:  this.form.numeroDossier,
      marque:         this.newDetail.marque   || undefined,
      numero:         this.newDetail.type === TypeDetailMaintenance.MAIN_D_OEUVRE ? (this.newDetail.numero || undefined) : undefined,
      numeroPiece:    this.newDetail.type === TypeDetailMaintenance.PIECE ? (this.newDetail.numeroPiece || undefined) : undefined,
      designation:    this.newDetail.designation!,
      quantite:       this.newDetail.quantite || 1,
      montantUnitaire: this.newDetail.montantUnitaire || 0,
      totalHtva:      this.newDetailTotal
    };
    if (!this.form.details) this.form.details = [];
    this.form.details.push(d);
    this.resetNewDetail();
    this.errorMsg = '';
  }

  supprimerDetail(index: number): void {
    this.form.details?.splice(index, 1);
  }

  recalcDetail(d: DetailMaintenance): void {
    d.totalHtva = Math.round((d.quantite || 0) * (d.montantUnitaire || 0) * 1000) / 1000;
  }

  getGlobalIndex(d: DetailMaintenance): number {
    return (this.form.details || []).indexOf(d);
  }

  resetNewDetail(): void {
    this.newDetail = {
      type: TypeDetailMaintenance.MAIN_D_OEUVRE,
      marque: '', numero: '', numeroPiece: '',
      designation: '', quantite: 1, montantUnitaire: 0
    };
  }

  // ══════════════════════════════════════════════════════════════
  //  SUPPRESSION
  // ══════════════════════════════════════════════════════════════

  demanderSuppression(m: Maintenance): void {
    this.deleteTarget      = m;
    this.showDeleteConfirm = true;
    this.errorMsg          = '';
  }

  annulerSuppression(): void {
    this.showDeleteConfirm = false;
    this.deleteTarget      = null;
  }

  confirmerSuppression(): void {
    if (!this.deleteTarget?.id) return;
    this.deleteLoading = true;
    this.maintenanceService.supprimer(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteLoading     = false;
        this.showDeleteConfirm = false;
        this.deleteTarget      = null;
        this.charger();
      },
      error: (err: any) => {
        this.deleteLoading = false;
        this.errorMsg      = err.error?.message || 'Erreur lors de la suppression';
        this.showDeleteConfirm = false;
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  //  EXPORT EXCEL
  // ══════════════════════════════════════════════════════════════

  exporterTout(): void {
    this.exportLoading = true;
    this.maintenanceService.exporterExcelGlobal().subscribe({
      next: (blob) => {
        this.exportLoading = false;
        this.maintenanceService.downloadBlob(blob, 'maintenances_zones.xlsx');
      },
      error: () => { this.exportLoading = false; }
    });
  }

  exporterDossier(m: Maintenance): void {
    if (!m.id) return;
    this.maintenanceService.exporterExcelDossier(m.id).subscribe({
      next: (blob) => {
        const fn = `maintenance_${m.numeroDossier.replace(/[^a-zA-Z0-9_-]/g, '_')}.xlsx`;
        this.maintenanceService.downloadBlob(blob, fn);
      },
      error: () => {}
    });
  }

  // ══════════════════════════════════════════════════════════════
  //  HELPERS DÉTAIL
  // ══════════════════════════════════════════════════════════════

  getMainDoeuvre(m: Maintenance): DetailMaintenance[] {
    return (m.details || []).filter(d => d.type === TypeDetailMaintenance.MAIN_D_OEUVRE);
  }

  getPieces(m: Maintenance): DetailMaintenance[] {
    return (m.details || []).filter(d => d.type === TypeDetailMaintenance.PIECE);
  }

  // ══════════════════════════════════════════════════════════════
  //  LABELS & CLASSES
  // ══════════════════════════════════════════════════════════════

  getTypeLabel(type: string): string {
    const m: Record<string, string> = {
      PREVENTIVE: 'Préventive', CORRECTIVE: 'Corrective',
      VISITE_TECHNIQUE: 'Visite technique', ACCIDENT: 'Accident'
    };
    return m[type] || type;
  }

  getStatutLabel(s: string): string {
    const m: Record<string, string> = {
      EN_COURS: 'En cours', TERMINEE: 'Terminée', ANNULEE: 'Annulée'
    };
    return m[s] || s;
  }

  getTypeClass(type: string): string {
    const m: Record<string, string> = {
      PREVENTIVE: 'chip-preventive', CORRECTIVE: 'chip-corrective',
      VISITE_TECHNIQUE: 'chip-visite', ACCIDENT: 'chip-accident'
    };
    return m[type] || '';
  }

  getStatutClass(s: string): string {
    const m: Record<string, string> = {
      EN_COURS: 'chip-en-cours', TERMINEE: 'chip-terminee', ANNULEE: 'chip-annulee'
    };
    return m[s] || '';
  }

  barPct(val: number, max: number): number {
    return max > 0 ? Math.round((val / max) * 100) : 0;
  }

  fmt3(v: number): string {
    return (v || 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }

  nav(r: string): void    { this.router.navigate([r]); }
  logout(): void           { this.authService.logout(); }
}