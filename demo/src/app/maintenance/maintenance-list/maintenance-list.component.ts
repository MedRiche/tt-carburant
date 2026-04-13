// src/app/maintenance/maintenance-list/maintenance-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {
  Maintenance, MaintenanceRequest, GlobalVehicleListItem,
  MaintenanceDashboard, DetailMaintenance,
  StatutMaintenance, TypeIntervention, TypeDetailMaintenance,
  TYPE_INTERVENTION_LABELS, STATUT_LABELS
} from '../../models/maintenance';
import { Vehicule } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { MaintenanceService } from '../../services/maintenance.service';
import { VehiculeService } from '../../services/vehicule.service';
import { ZoneService } from '../../services/zone.service';

@Component({
  selector: 'app-maintenance-list',
  standalone: false,
  templateUrl: './maintenance-list.component.html',
  styleUrls: ['./maintenance-list.component.css']
})
export class MaintenanceListComponent implements OnInit {

  // ── Tabs ──────────────────────────────────────────────────────────────────
  activeTab: 'global' | 'dossiers' | 'analytics' = 'global';

  // ── Données ───────────────────────────────────────────────────────────────
  globalList: GlobalVehicleListItem[] = [];
  globalListFiltree: GlobalVehicleListItem[] = [];
  tousLesDossiers: Maintenance[] = [];
  dossiersFiltres: Maintenance[] = [];
  vehicules: Vehicule[] = [];
  zones: Zone[] = [];
  dashboard: MaintenanceDashboard | null = null;

  // ── Detail panel ──────────────────────────────────────────────────────────
  selectedGlobalItem: GlobalVehicleListItem | null = null;
  detailMaintenances: Maintenance[] = [];
  loadingDetail = false;

  // ── Form ──────────────────────────────────────────────────────────────────
  showForm = false;
  maintenanceSelectionnee: Maintenance | null = null;
  matriculeDefaut = '';

  // ── Loading ───────────────────────────────────────────────────────────────
  loadingGlobal = false;
  loadingDossiers = false;
  loadingDashboard = false;
  exportLoading = false;

  // ── Filtres global ────────────────────────────────────────────────────────
  searchGlobal = '';
  filtreZoneGlobal = '';
  filtrePrestataire = '';

  // ── Filtres dossiers ──────────────────────────────────────────────────────
  searchDossiers = '';
  filtreStatut = '';
  filtreType = '';

  // ── Computed ──────────────────────────────────────────────────────────────
  get totalHtvaGlobal(): number {
    return this.globalListFiltree.reduce((s, i) => s + (i.totalHtva || 0), 0);
  }

  get nbEnCours(): number {
    return this.tousLesDossiers.filter(d => d.statut === StatutMaintenance.EN_COURS).length;
  }

  constructor(
    private maintenanceService: MaintenanceService,
    private vehiculeService: VehiculeService,
    private zoneService: ZoneService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.chargerGlobalList();
    this.chargerDossiers();
    this.vehiculeService.getAllVehicules().subscribe({ next: d => this.vehicules = d });
    this.zoneService.getAllZones().subscribe({ next: d => this.zones = d });
  }

  // ── Chargement ────────────────────────────────────────────────────────────

  chargerGlobalList(): void {
    this.loadingGlobal = true;
    this.maintenanceService.getGlobalVehicleList().subscribe({
      next: (data: any[]) => {
        this.globalList = data as GlobalVehicleListItem[];
        this.globalListFiltree = [...this.globalList];
        this.loadingGlobal = false;
      },
      error: () => { this.loadingGlobal = false; }
    });
  }

  chargerDossiers(): void {
    this.loadingDossiers = true;
    this.maintenanceService.getAll().subscribe({
      next: d => {
        this.tousLesDossiers = d;
        this.dossiersFiltres = [...d];
        this.loadingDossiers = false;
      },
      error: () => { this.loadingDossiers = false; }
    });
  }

  chargerDashboard(): void {
    if (this.dashboard) return;
    this.loadingDashboard = true;
    this.maintenanceService.getDashboard().subscribe({
      next: (d: any) => {
        this.dashboard = d as MaintenanceDashboard;
        this.loadingDashboard = false;
      },
      error: () => { this.loadingDashboard = false; }
    });
  }

  // ── Filtres ───────────────────────────────────────────────────────────────

  filtrerGlobal(): void {
    let list = [...this.globalList];
    const q = this.searchGlobal.toLowerCase();
    if (q) {
      list = list.filter(i =>
        i.vehiculeId.toLowerCase().includes(q) ||
        (i.vehiculeMarque || '').toLowerCase().includes(q) ||
        (i.zoneNom || '').toLowerCase().includes(q)
      );
    }
    if (this.filtreZoneGlobal) {
      list = list.filter(i => i.zoneNom === this.filtreZoneGlobal);
    }
    if (this.filtrePrestataire) {
      list = list.filter(i => (i.brands || '').includes(this.filtrePrestataire));
    }
    this.globalListFiltree = list;
  }

  filtrerDossiers(): void {
    let list = [...this.tousLesDossiers];
    const q = this.searchDossiers.toLowerCase();
    if (q) {
      list = list.filter(d =>
        (d.numeroDossier || '').toLowerCase().includes(q) ||
        d.vehiculeMatricule.toLowerCase().includes(q) ||
        (d.vehiculeMarqueModele || '').toLowerCase().includes(q)
      );
    }
    if (this.filtreStatut) {
      list = list.filter(d => d.statut === this.filtreStatut);
    }
    if (this.filtreType) {
      list = list.filter(d => d.typeIntervention === this.filtreType);
    }
    this.dossiersFiltres = list;
  }

  // ── Detail panel ──────────────────────────────────────────────────────────

  ouvrirDetail(item: GlobalVehicleListItem): void {
    this.selectedGlobalItem = item;
    this.loadingDetail = true;
    this.maintenanceService.getByVehicule(item.vehiculeId).subscribe({
      next: d => { this.detailMaintenances = d; this.loadingDetail = false; },
      error: () => { this.loadingDetail = false; }
    });
  }

  fermerDetail(): void {
    this.selectedGlobalItem = null;
    this.detailMaintenances = [];
  }

  // ── CRUD ──────────────────────────────────────────────────────────────────

  ouvrirCreation(): void {
    this.maintenanceSelectionnee = null;
    this.matriculeDefaut = '';
    this.showForm = true;
  }

  ouvrirCreationPourVehicule(matricule: string): void {
    this.maintenanceSelectionnee = null;
    this.matriculeDefaut = matricule;
    this.showForm = true;
  }

  editerDossier(m: Maintenance): void {
    this.maintenanceSelectionnee = { ...m };
    this.showForm = true;
  }

  voirDossier(m: Maintenance): void {
    this.ouvrirDetail({ vehiculeId: m.vehiculeMatricule, vehiculeMarque: m.vehiculeMarqueModele || '', totalHtva: m.coutTotalHtva, brands: m.brands || '', zoneNom: m.vehiculeZoneNom, nbDossiers: 1 });
  }

  supprimerDossier(m: Maintenance): void {
    if (!m.id) return;
    if (!confirm(`Supprimer le dossier ${m.numeroDossier} ?`)) return;
    this.maintenanceService.supprimer(m.id).subscribe({
      next: () => {
        this.chargerDossiers();
        this.chargerGlobalList();
        if (this.selectedGlobalItem) {
          this.ouvrirDetail(this.selectedGlobalItem);
        }
      },
      error: (err: any) => alert(err.error?.message || 'Erreur lors de la suppression')
    });
  }

  onSaved(): void {
    this.showForm = false;
    this.maintenanceSelectionnee = null;
    this.chargerDossiers();
    this.chargerGlobalList();
    if (this.selectedGlobalItem) {
      this.ouvrirDetail(this.selectedGlobalItem);
    }
  }

  // ── Import / Export ───────────────────────────────────────────────────────

  onImport(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.maintenanceService.importerDataset(file).subscribe({
      next: (res: any) => {
        alert(`✅ Import terminé\nImportés: ${res.imported}\nIgnorés: ${res.skipped}\n${res.errors?.length ? 'Erreurs:\n' + res.errors.join('\n') : ''}`);
        this.chargerGlobalList();
        this.chargerDossiers();
      },
      error: (err: any) => alert('❌ Erreur import: ' + (err.error?.message || 'Erreur inconnue'))
    });
  }

  exporter(): void {
    this.exportLoading = true;
    this.maintenanceService.exporterExcel().subscribe({
      next: (blob) => {
        this.maintenanceService.downloadBlob(blob, 'maintenances.xlsx');
        this.exportLoading = false;
      },
      error: () => { alert('Erreur export'); this.exportLoading = false; }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getTypeLabel(type: string): string { return TYPE_INTERVENTION_LABELS[type] || type; }
  getStatutLabel(statut: string): string { return STATUT_LABELS[statut] || statut; }

  getTypeClass(type: string): string {
    const m: Record<string, string> = {
      PREVENTIVE: 'chip-preventive', CORRECTIVE: 'chip-corrective',
      VISITE_TECHNIQUE: 'chip-visite', ACCIDENT: 'chip-accident'
    };
    return m[type] || '';
  }

  getStatutClass(statut: string): string {
    const m: Record<string, string> = {
      EN_COURS: 'chip-en-cours', TERMINEE: 'chip-terminee', ANNULEE: 'chip-annulee'
    };
    return m[statut] || '';
  }

  getBrands(brands: string): string[] {
    if (!brands) return [];
    return brands.split(',').map(b => b.trim()).filter(Boolean);
  }

  getBrandClass(brand: string): string {
    const m: Record<string, string> = {
      'TAS': 'chip-tas', 'Peugeot': 'chip-peugeot', 'Citroen': 'chip-citroen', 'Citroën': 'chip-citroen'
    };
    return m[brand] || 'chip-other';
  }

  fmt3(v: number | undefined): string {
    return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }

  barPct(val: number, max: number): number {
    if (!max) return 0;
    return Math.round((val / max) * 100);
  }

  getMainDoeuvre(m: Maintenance): DetailMaintenance[] {
    return (m.details || []).filter(d => d.type === TypeDetailMaintenance.MAIN_D_OEUVRE);
  }

  getPieces(m: Maintenance): DetailMaintenance[] {
    return (m.details || []).filter(d => d.type === TypeDetailMaintenance.PIECE);
  }

  navigateTo(route: string): void { this.router.navigate([route]); }
}