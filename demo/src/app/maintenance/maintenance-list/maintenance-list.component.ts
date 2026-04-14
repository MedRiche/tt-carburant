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
import { forkJoin } from 'rxjs';

export interface DesignationCount {
  designation: string;
  count: number;
  type: 'MAIN_D_OEUVRE' | 'PIECE';
}

export interface MissingHtvaItem {
  vehiculeId: string;
  vehiculeMarque: string;
  zoneNom?: string;
  numeroDossier: string;
  excelRow?: number;
  numDossier?: number;
  numero?: string | number;
  designation: string;
  quantite: number;
  montantUnitaire: number;
  totalHtva: number;
  type: 'MAIN_D_OEUVRE' | 'PIECE';
}

@Component({
  selector: 'app-maintenance-list',
  standalone: false,
  templateUrl: './maintenance-list.component.html',
  styleUrls: ['./maintenance-list.component.css']
})
export class MaintenanceListComponent implements OnInit {

  activeTab: 'global' | 'dossiers' | 'analytics' | 'desig-main' | 'desig-piece' | 'missing-htva' = 'global';

  globalList: GlobalVehicleListItem[] = [];
  globalListFiltree: GlobalVehicleListItem[] = [];
  tousLesDossiers: Maintenance[] = [];
  tousLesDossiersAvecDetails: Maintenance[] = []; // ← Dossiers with full details
  dossiersFiltres: Maintenance[] = [];
  vehicules: Vehicule[] = [];
  zones: Zone[] = [];
  dashboard: MaintenanceDashboard | null = null;

  selectedGlobalItem: GlobalVehicleListItem | null = null;
  detailMaintenances: Maintenance[] = [];
  loadingDetail = false;

  showForm = false;
  maintenanceSelectionnee: Maintenance | null = null;
  matriculeDefaut = '';

  loadingGlobal = false;
  loadingDossiers = false;
  loadingDashboard = false;
  loadingDetails = false; // ← Loading full details
  exportLoading = false;

  searchGlobal = '';
  filtreZoneGlobal = '';
  filtrePrestataire = '';

  searchDossiers = '';
  filtreStatut = '';
  filtreType = '';

  // Designation Count - Main
  designationMainData: DesignationCount[] = [];
  designationMainFiltree: DesignationCount[] = [];
  filtreZoneDesigMain = '';
  filtreVehiculeDesigMain = '';
  searchDesigMain = '';
  loadingDesigMain = false;

  // Designation Count - Piece
  designationPieceData: DesignationCount[] = [];
  designationPieceFiltree: DesignationCount[] = [];
  filtreZoneDesigPiece = '';
  filtreVehiculeDesigPiece = '';
  searchDesigPiece = '';
  loadingDesigPiece = false;

  // Missing HTVA
  missingHtvaData: MissingHtvaItem[] = [];
  missingHtvaFiltree: MissingHtvaItem[] = [];
  filtreZoneMissing = '';
  filtreVehiculeMissing = '';
  filtreTypeMissing = '';
  loadingMissing = false;

  // Analytics
  top5MainDesignation: { designation: string; count: number; totalHtva: number }[] = [];
  top5PieceDesignation: { designation: string; count: number; totalHtva: number }[] = [];
  top5Vehicules: { matricule: string; marque: string; zone: string; totalHtva: number; nbDossiers: number }[] = [];
  statsParType: { type: string; count: number; totalHtva: number }[] = [];
  statsParZone: { zone: string; totalHtva: number; nbVehicules: number }[] = [];
  totalHtvaGlobal2 = 0;
  totalDossiers2 = 0;

  // Track whether we've loaded details
  private detailsLoaded = false;

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

  /**
   * Load all dossiers WITH their details (needed for designation count & missing HTVA).
   * Uses the getById() endpoint for each dossier to get full details.
   */
  chargerDossiersAvecDetails(callback?: () => void): void {
    if (this.detailsLoaded) {
      // Already loaded, just call callback
      if (callback) callback();
      return;
    }

    this.loadingDetails = true;

    // First ensure we have the basic list
    const ensureList = () => {
      if (this.tousLesDossiers.length === 0) {
        this.maintenanceService.getAll().subscribe({
          next: dossiers => {
            this.tousLesDossiers = dossiers;
            this.dossiersFiltres = [...dossiers];
            this.loadDetailsForAll(callback);
          },
          error: () => { this.loadingDetails = false; }
        });
      } else {
        this.loadDetailsForAll(callback);
      }
    };

    ensureList();
  }

  private loadDetailsForAll(callback?: () => void): void {
    if (this.tousLesDossiers.length === 0) {
      this.tousLesDossiersAvecDetails = [];
      this.detailsLoaded = true;
      this.loadingDetails = false;
      if (callback) callback();
      return;
    }

    // Load details for each dossier that has an id
    const dossiersAvecId = this.tousLesDossiers.filter(d => d.id);
    if (dossiersAvecId.length === 0) {
      this.tousLesDossiersAvecDetails = [];
      this.detailsLoaded = true;
      this.loadingDetails = false;
      if (callback) callback();
      return;
    }

    const requests = dossiersAvecId.map(d => this.maintenanceService.getById(d.id!));

    forkJoin(requests).subscribe({
      next: (detailedDossiers) => {
        this.tousLesDossiersAvecDetails = detailedDossiers;
        this.detailsLoaded = true;
        this.loadingDetails = false;
        if (callback) callback();
      },
      error: (err) => {
        console.error('Error loading details:', err);
        // Fallback: use existing dossiers without details
        this.tousLesDossiersAvecDetails = this.tousLesDossiers;
        this.detailsLoaded = true;
        this.loadingDetails = false;
        if (callback) callback();
      }
    });
  }

  chargerDashboard(): void {
    this.loadingDashboard = true;
    this.maintenanceService.getDashboard().subscribe({
      next: (d: any) => {
        this.dashboard = d as MaintenanceDashboard;
        this.chargerDossiersAvecDetails(() => {
          this.calculerAnalyticsAvances();
          this.loadingDashboard = false;
        });
      },
      error: () => { this.loadingDashboard = false; }
    });
  }

  // ── Calcul Analytics ──────────────────────────────────────────────────────

  calculerAnalyticsAvances(): void {
    const dossiers = this.tousLesDossiersAvecDetails;
    if (!dossiers.length) return;

    const moMap = new Map<string, { count: number; totalHtva: number }>();
    const pieceMap = new Map<string, { count: number; totalHtva: number }>();
    const vehiculeMap = new Map<string, { marque: string; zone: string; totalHtva: number; nbDossiers: number }>();

    dossiers.forEach(m => {
      const vKey = m.vehiculeMatricule;
      const vExist = vehiculeMap.get(vKey) || { marque: m.vehiculeMarqueModele || '', zone: m.vehiculeZoneNom || '', totalHtva: 0, nbDossiers: 0 };
      vehiculeMap.set(vKey, { ...vExist, totalHtva: vExist.totalHtva + (m.coutTotalHtva || 0), nbDossiers: vExist.nbDossiers + 1 });

      (m.details || []).forEach(d => {
        const htva = (d.quantite || 1) * (d.montantUnitaire || 0);
        if (d.type === TypeDetailMaintenance.MAIN_D_OEUVRE) {
          const ex = moMap.get(d.designation) || { count: 0, totalHtva: 0 };
          moMap.set(d.designation, { count: ex.count + (d.quantite || 1), totalHtva: ex.totalHtva + htva });
        } else {
          const ex = pieceMap.get(d.designation) || { count: 0, totalHtva: 0 };
          pieceMap.set(d.designation, { count: ex.count + (d.quantite || 1), totalHtva: ex.totalHtva + htva });
        }
      });
    });

    this.top5MainDesignation = Array.from(moMap.entries())
      .map(([designation, v]) => ({ designation, ...v }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);

    this.top5PieceDesignation = Array.from(pieceMap.entries())
      .map(([designation, v]) => ({ designation, ...v }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);

    this.top5Vehicules = Array.from(vehiculeMap.entries())
      .map(([matricule, v]) => ({ matricule, ...v }))
      .sort((a, b) => b.totalHtva - a.totalHtva)
      .slice(0, 5);

    this.totalHtvaGlobal2 = dossiers.reduce((s, m) => s + (m.coutTotalHtva || 0), 0);
    this.totalDossiers2 = dossiers.length;

    const zoneMap = new Map<string, { totalHtva: number; vehicules: Set<string> }>();
    dossiers.forEach(m => {
      const z = m.vehiculeZoneNom || 'Sans zone';
      const ex = zoneMap.get(z) || { totalHtva: 0, vehicules: new Set() };
      ex.totalHtva += m.coutTotalHtva || 0;
      ex.vehicules.add(m.vehiculeMatricule);
      zoneMap.set(z, ex);
    });
    this.statsParZone = Array.from(zoneMap.entries())
      .map(([zone, v]) => ({ zone, totalHtva: v.totalHtva, nbVehicules: v.vehicules.size }))
      .sort((a, b) => b.totalHtva - a.totalHtva);
  }

  // ── Designation Count - Main ──────────────────────────────────────────────

  chargerDesigMain(): void {
    this.loadingDesigMain = true;
    this.chargerDossiersAvecDetails(() => {
      this._computeDesigMain();
      this.loadingDesigMain = false;
    });
  }

  private _computeDesigMain(): void {
    const map = new Map<string, number>();
    let dossiers = this.tousLesDossiersAvecDetails;

    if (this.filtreZoneDesigMain) {
      dossiers = dossiers.filter(m => m.vehiculeZoneNom === this.filtreZoneDesigMain);
    }
    if (this.filtreVehiculeDesigMain) {
      dossiers = dossiers.filter(m => m.vehiculeMatricule === this.filtreVehiculeDesigMain);
    }

    dossiers.forEach(m => {
      (m.details || []).forEach(d => {
        if (d.type === TypeDetailMaintenance.MAIN_D_OEUVRE) {
          const key = d.designation.trim().toLowerCase();
          map.set(key, (map.get(key) || 0) + (d.quantite || 1));
        }
      });
    });

    this.designationMainData = Array.from(map.entries())
      .map(([designation, count]) => ({
        designation: this.capitalizeFirst(designation),
        count,
        type: 'MAIN_D_OEUVRE' as const
      }))
      .sort((a, b) => b.count - a.count);

    this.filtrerDesigMain();
  }

  filtrerDesigMain(): void {
    let data = [...this.designationMainData];
    if (this.searchDesigMain) {
      const q = this.searchDesigMain.toLowerCase();
      data = data.filter(d => d.designation.toLowerCase().includes(q));
    }
    this.designationMainFiltree = data;
  }

  // ── Designation Count - Piece ─────────────────────────────────────────────

  chargerDesigPiece(): void {
    this.loadingDesigPiece = true;
    this.chargerDossiersAvecDetails(() => {
      this._computeDesigPiece();
      this.loadingDesigPiece = false;
    });
  }

  private _computeDesigPiece(): void {
    const map = new Map<string, number>();
    let dossiers = this.tousLesDossiersAvecDetails;

    if (this.filtreZoneDesigPiece) {
      dossiers = dossiers.filter(m => m.vehiculeZoneNom === this.filtreZoneDesigPiece);
    }
    if (this.filtreVehiculeDesigPiece) {
      dossiers = dossiers.filter(m => m.vehiculeMatricule === this.filtreVehiculeDesigPiece);
    }

    dossiers.forEach(m => {
      (m.details || []).forEach(d => {
        if (d.type === TypeDetailMaintenance.PIECE) {
          const key = d.designation.trim().toLowerCase();
          map.set(key, (map.get(key) || 0) + (d.quantite || 1));
        }
      });
    });

    this.designationPieceData = Array.from(map.entries())
      .map(([designation, count]) => ({
        designation: this.capitalizeFirst(designation),
        count,
        type: 'PIECE' as const
      }))
      .sort((a, b) => b.count - a.count);

    this.filtrerDesigPiece();
  }

  filtrerDesigPiece(): void {
    let data = [...this.designationPieceData];
    if (this.searchDesigPiece) {
      const q = this.searchDesigPiece.toLowerCase();
      data = data.filter(d => d.designation.toLowerCase().includes(q));
    }
    this.designationPieceFiltree = data;
  }

  // ── Missing HTVA ──────────────────────────────────────────────────────────

  chargerMissingHtva(): void {
    this.loadingMissing = true;
    this.chargerDossiersAvecDetails(() => {
      this._computeMissingHtva();
      this.loadingMissing = false;
    });
  }

  private _computeMissingHtva(): void {
    const missing: MissingHtvaItem[] = [];
    let rowNum = 0;

    this.tousLesDossiersAvecDetails.forEach(m => {
      (m.details || []).forEach(d => {
        const htva = d.totalHtva ?? 0;
        if (htva === 0 || d.montantUnitaire === 0) {
          rowNum++;
          missing.push({
            vehiculeId: m.vehiculeMatricule,
            vehiculeMarque: m.vehiculeMarqueModele || '',
            zoneNom: m.vehiculeZoneNom,
            numeroDossier: m.numeroDossier,
            excelRow: rowNum,
            numDossier: parseInt(m.numeroDossier) || rowNum,
            numero: d.numero || d.numeroPiece,
            designation: d.designation,
            quantite: d.quantite || 1,
            montantUnitaire: d.montantUnitaire || 0,
            totalHtva: htva,
            type: d.type as 'MAIN_D_OEUVRE' | 'PIECE'
          });
        }
      });
    });

    this.missingHtvaData = missing;
    this.filtrerMissing();
  }

  filtrerMissing(): void {
    let data = [...this.missingHtvaData];
    if (this.filtreZoneMissing) {
      data = data.filter(d => d.zoneNom === this.filtreZoneMissing);
    }
    if (this.filtreVehiculeMissing) {
      data = data.filter(d => d.vehiculeId === this.filtreVehiculeMissing);
    }
    if (this.filtreTypeMissing) {
      data = data.filter(d => d.type === this.filtreTypeMissing);
    }
    this.missingHtvaFiltree = data;
  }

  // ── Navigation entre tabs ─────────────────────────────────────────────────

  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
    if (tab === 'analytics') this.chargerDashboard();
    if (tab === 'desig-main') this.chargerDesigMain();
    if (tab === 'desig-piece') this.chargerDesigPiece();
    if (tab === 'missing-htva') this.chargerMissingHtva();
  }

  // ── Filtres global ────────────────────────────────────────────────────────

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
    if (this.filtreStatut) list = list.filter(d => d.statut === this.filtreStatut);
    if (this.filtreType) list = list.filter(d => d.typeIntervention === this.filtreType);
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
        this.detailsLoaded = false; // Reset so details reload on next tab visit
        this.chargerDossiers();
        this.chargerGlobalList();
        if (this.selectedGlobalItem) this.ouvrirDetail(this.selectedGlobalItem);
      },
      error: (err: any) => alert(err.error?.message || 'Erreur lors de la suppression')
    });
  }

  onSaved(): void {
    this.showForm = false;
    this.maintenanceSelectionnee = null;
    this.detailsLoaded = false; // Reset so details reload on next tab visit
    this.chargerDossiers();
    this.chargerGlobalList();
    if (this.selectedGlobalItem) this.ouvrirDetail(this.selectedGlobalItem);
  }

  // ── Import / Export ───────────────────────────────────────────────────────

  onImport(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.maintenanceService.importerDataset(file).subscribe({
      next: (res: any) => {
        alert(`✅ Import terminé\nImportés: ${res.imported}\nIgnorés: ${res.skipped}\n${res.errors?.length ? 'Erreurs:\n' + res.errors.join('\n') : ''}`);
        this.detailsLoaded = false;
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

  getMaxDesigMain(): number {
    return this.designationMainFiltree.length ? this.designationMainFiltree[0].count : 1;
  }

  getMaxDesigPiece(): number {
    return this.designationPieceFiltree.length ? this.designationPieceFiltree[0].count : 1;
  }

  capitalizeFirst(s: string): string {
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  getUniqueZones(): string[] {
    const zones = new Set<string>();
    this.tousLesDossiersAvecDetails.forEach(m => { if (m.vehiculeZoneNom) zones.add(m.vehiculeZoneNom); });
    // Fallback to basic dossiers if details not loaded
    if (zones.size === 0) {
      this.tousLesDossiers.forEach(m => { if (m.vehiculeZoneNom) zones.add(m.vehiculeZoneNom); });
    }
    return Array.from(zones).sort();
  }

  getUniqueVehicules(): string[] {
    const mats = new Set<string>();
    this.tousLesDossiersAvecDetails.forEach(m => mats.add(m.vehiculeMatricule));
    if (mats.size === 0) {
      this.tousLesDossiers.forEach(m => mats.add(m.vehiculeMatricule));
    }
    return Array.from(mats).sort();
  }

  navigateTo(route: string): void { this.router.navigate([route]); }
}