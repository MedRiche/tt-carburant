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
  tousLesDossiersAvecDetails: Maintenance[] = [];
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
  loadingDetails = false;
  exportLoading = false;
  exportDesigMainLoading = false;
  exportDesigPieceLoading = false;
  exportGlobalLoading = false;

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

  chargerDossiersAvecDetails(callback?: () => void): void {
    if (this.detailsLoaded) {
      if (callback) callback();
      return;
    }
    this.loadingDetails = true;
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
      error: () => {
        this.tousLesDossiersAvecDetails = this.tousLesDossiers;
        this.detailsLoaded = true;
        this.loadingDetails = false;
        if (callback) callback();
      }
    });
  }

  chargerDashboard(): void {
    this.loadingDashboard = true;
    this.chargerDossiersAvecDetails(() => {
      this.calculerAnalyticsAvances();
      try {
        this.maintenanceService.getDashboard().subscribe({
          next: (d: any) => {
            this.dashboard = d as MaintenanceDashboard;
            this.loadingDashboard = false;
          },
          error: () => {
            this.dashboard = null;
            this.loadingDashboard = false;
          }
        });
      } catch {
        this.dashboard = null;
        this.loadingDashboard = false;
      }
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
      .sort((a, b) => b.count - a.count).slice(0, 5);

    this.top5PieceDesignation = Array.from(pieceMap.entries())
      .map(([designation, v]) => ({ designation, ...v }))
      .sort((a, b) => b.count - a.count).slice(0, 5);

    this.top5Vehicules = Array.from(vehiculeMap.entries())
      .map(([matricule, v]) => ({ matricule, ...v }))
      .sort((a, b) => b.totalHtva - a.totalHtva).slice(0, 5);

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
    if (this.filtreZoneDesigMain) dossiers = dossiers.filter(m => m.vehiculeZoneNom === this.filtreZoneDesigMain);
    if (this.filtreVehiculeDesigMain) dossiers = dossiers.filter(m => m.vehiculeMatricule === this.filtreVehiculeDesigMain);
    dossiers.forEach(m => {
      (m.details || []).forEach(d => {
        if (d.type === TypeDetailMaintenance.MAIN_D_OEUVRE) {
          const key = d.designation.trim().toLowerCase();
          map.set(key, (map.get(key) || 0) + (d.quantite || 1));
        }
      });
    });
    this.designationMainData = Array.from(map.entries())
      .map(([designation, count]) => ({ designation: this.capitalizeFirst(designation), count, type: 'MAIN_D_OEUVRE' as const }))
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
    if (this.filtreZoneDesigPiece) dossiers = dossiers.filter(m => m.vehiculeZoneNom === this.filtreZoneDesigPiece);
    if (this.filtreVehiculeDesigPiece) dossiers = dossiers.filter(m => m.vehiculeMatricule === this.filtreVehiculeDesigPiece);
    dossiers.forEach(m => {
      (m.details || []).forEach(d => {
        if (d.type === TypeDetailMaintenance.PIECE) {
          const key = d.designation.trim().toLowerCase();
          map.set(key, (map.get(key) || 0) + (d.quantite || 1));
        }
      });
    });
    this.designationPieceData = Array.from(map.entries())
      .map(([designation, count]) => ({ designation: this.capitalizeFirst(designation), count, type: 'PIECE' as const }))
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
    if (this.filtreZoneMissing) data = data.filter(d => d.zoneNom === this.filtreZoneMissing);
    if (this.filtreVehiculeMissing) data = data.filter(d => d.vehiculeId === this.filtreVehiculeMissing);
    if (this.filtreTypeMissing) data = data.filter(d => d.type === this.filtreTypeMissing);
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

  filtrerGlobal(): void {
    let list = [...this.globalList];
    const q = this.searchGlobal.toLowerCase();
    if (q) list = list.filter(i => i.vehiculeId.toLowerCase().includes(q) || (i.vehiculeMarque || '').toLowerCase().includes(q) || (i.zoneNom || '').toLowerCase().includes(q));
    if (this.filtreZoneGlobal) list = list.filter(i => i.zoneNom === this.filtreZoneGlobal);
    if (this.filtrePrestataire) list = list.filter(i => (i.brands || '').includes(this.filtrePrestataire));
    this.globalListFiltree = list;
  }

  filtrerDossiers(): void {
    let list = [...this.tousLesDossiers];
    const q = this.searchDossiers.toLowerCase();
    if (q) list = list.filter(d => (d.numeroDossier || '').toLowerCase().includes(q) || d.vehiculeMatricule.toLowerCase().includes(q) || (d.vehiculeMarqueModele || '').toLowerCase().includes(q));
    if (this.filtreStatut) list = list.filter(d => d.statut === this.filtreStatut);
    if (this.filtreType) list = list.filter(d => d.typeIntervention === this.filtreType);
    this.dossiersFiltres = list;
  }

  // ── Detail panel ──────────────────────────────────────────────────────────

  ouvrirDetail(item: GlobalVehicleListItem): void {
    this.selectedGlobalItem = item;
    this.loadingDetail = true;
    this.maintenanceService.getByVehicule(item.vehiculeId).subscribe({
      next: d => {
        // Load full details for each maintenance
        if (d.length === 0) { this.detailMaintenances = []; this.loadingDetail = false; return; }
        const reqs = d.filter(m => m.id).map(m => this.maintenanceService.getById(m.id!));
        if (reqs.length === 0) { this.detailMaintenances = d; this.loadingDetail = false; return; }
        forkJoin(reqs).subscribe({
          next: full => { this.detailMaintenances = full; this.loadingDetail = false; },
          error: () => { this.detailMaintenances = d; this.loadingDetail = false; }
        });
      },
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
    // Load full details before editing
    if (m.id && (!m.details || m.details.length === 0)) {
      this.maintenanceService.getById(m.id).subscribe({
        next: full => { this.maintenanceSelectionnee = { ...full }; this.showForm = true; },
        error: () => { this.maintenanceSelectionnee = { ...m }; this.showForm = true; }
      });
    } else {
      this.maintenanceSelectionnee = { ...m };
      this.showForm = true;
    }
  }

  voirDossier(m: Maintenance): void {
    this.ouvrirDetail({ vehiculeId: m.vehiculeMatricule, vehiculeMarque: m.vehiculeMarqueModele || '', totalHtva: m.coutTotalHtva, brands: m.brands || '', zoneNom: m.vehiculeZoneNom, nbDossiers: 1 });
  }

  supprimerDossier(m: Maintenance): void {
    if (!m.id) return;
    if (!confirm(`Supprimer le dossier ${m.numeroDossier} ?`)) return;
    this.maintenanceService.supprimer(m.id).subscribe({
      next: () => {
        this.detailsLoaded = false;
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
    this.detailsLoaded = false;
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
      next: (blob) => { this.maintenanceService.downloadBlob(blob, 'maintenances.xlsx'); this.exportLoading = false; },
      error: () => { alert('Erreur export'); this.exportLoading = false; }
    });
  }

  // ── Export Excel — Global Vehicle List ───────────────────────────────────

  exporterGlobalList(): void {
    this.exportGlobalLoading = true;
    try {
      this._exportGlobalListToExcel();
    } catch (e) {
      alert('Erreur export');
    } finally {
      this.exportGlobalLoading = false;
    }
  }

  private _exportGlobalListToExcel(): void {
    const rows: string[][] = [];
    // Header
    rows.push(['Véhicule ID', 'Marque / Modèle', 'Zone', 'Total HTVA (DT)', 'Brand(s)', 'Nb Dossiers']);
    // Data
    this.globalListFiltree.forEach(item => {
      rows.push([
        item.vehiculeId,
        item.vehiculeMarque || '',
        item.zoneNom || '—',
        this.fmt3(item.totalHtva),
        item.brands || '',
        String(item.nbDossiers || 0)
      ]);
    });
    // Total row
    rows.push(['TOTAL', '', '', this.fmt3(this.totalHtvaGlobal), '', '']);
    this._downloadCsv(rows, 'global_vehicle_list.csv');
  }

  // ── Export Excel — Designation Count Main ────────────────────────────────

  exporterDesigMain(): void {
    this.exportDesigMainLoading = true;
    this.chargerDossiersAvecDetails(() => {
      this._exportDesigMainToExcel();
      this.exportDesigMainLoading = false;
    });
  }

  private _exportDesigMainToExcel(): void {
    const rows: string[][] = [];
    rows.push(['Rang', 'Désignation (Main d\'œuvre)', 'Count (Occurrences)']);
    this.designationMainFiltree.forEach((item, i) => {
      rows.push([String(i + 1), item.designation, String(item.count)]);
    });
    this._downloadCsv(rows, 'designation_count_main_oeuvre.csv');
  }

  // ── Export Excel — Designation Count Piece ───────────────────────────────

  exporterDesigPiece(): void {
    this.exportDesigPieceLoading = true;
    this.chargerDossiersAvecDetails(() => {
      this._exportDesigPieceToExcel();
      this.exportDesigPieceLoading = false;
    });
  }

  private _exportDesigPieceToExcel(): void {
    const rows: string[][] = [];
    rows.push(['Rang', 'Désignation (Pièce)', 'Count (Occurrences)']);
    this.designationPieceFiltree.forEach((item, i) => {
      rows.push([String(i + 1), item.designation, String(item.count)]);
    });
    this._downloadCsv(rows, 'designation_count_pieces.csv');
  }

  // ── Export combiné — Global + Desig Main + Desig Piece ───────────────────

  exporterTout(): void {
    this.exportGlobalLoading = true;
    this.chargerDossiersAvecDetails(() => {
      this._exportCombine();
      this.exportGlobalLoading = false;
    });
  }

  private _exportCombine(): void {
    // CSV with sections separated by blank lines
    const allRows: string[][] = [];

    // Section 1 — Global Vehicle List
    allRows.push(['=== GLOBAL VEHICLE LIST ===']);
    allRows.push(['Véhicule ID', 'Marque / Modèle', 'Zone', 'Total HTVA (DT)', 'Brand(s)', 'Nb Dossiers']);
    this.globalListFiltree.forEach(item => {
      allRows.push([item.vehiculeId, item.vehiculeMarque || '', item.zoneNom || '—', this.fmt3(item.totalHtva), item.brands || '', String(item.nbDossiers || 0)]);
    });
    allRows.push(['TOTAL', '', '', this.fmt3(this.totalHtvaGlobal), '', '']);
    allRows.push([]);

    // Section 2 — Designation Count Main
    allRows.push(['=== DESIGNATION COUNT — MAIN D\'OEUVRE ===']);
    allRows.push(['Rang', 'Désignation', 'Count']);
    this.designationMainFiltree.forEach((item, i) => {
      allRows.push([String(i + 1), item.designation, String(item.count)]);
    });
    allRows.push([]);

    // Section 3 — Designation Count Piece
    allRows.push(['=== DESIGNATION COUNT — PIÈCES ===']);
    allRows.push(['Rang', 'Désignation', 'Count']);
    this.designationPieceFiltree.forEach((item, i) => {
      allRows.push([String(i + 1), item.designation, String(item.count)]);
    });

    this._downloadCsv(allRows, 'maintenance_export_complet.csv');
  }

  private _downloadCsv(rows: string[][], filename: string): void {
    const bom = '\uFEFF'; // UTF-8 BOM for Excel
    const csvContent = bom + rows.map(row => row.map(cell => `"${(cell || '').replace(/"/g, '""')}"`).join(';')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getTypeLabel(type: string): string { return TYPE_INTERVENTION_LABELS[type] || type; }
  getStatutLabel(statut: string): string { return STATUT_LABELS[statut] || statut; }

  getTypeClass(type: string): string {
    const m: Record<string, string> = { PREVENTIVE: 'chip-preventive', CORRECTIVE: 'chip-corrective', VISITE_TECHNIQUE: 'chip-visite', ACCIDENT: 'chip-accident' };
    return m[type] || '';
  }

  getStatutClass(statut: string): string {
    const m: Record<string, string> = { EN_COURS: 'chip-en-cours', TERMINEE: 'chip-terminee', ANNULEE: 'chip-annulee' };
    return m[statut] || '';
  }

  getBrands(brands: string): string[] {
    if (!brands) return [];
    return brands.split(',').map(b => b.trim()).filter(Boolean);
  }

  getBrandClass(brand: string): string {
    const m: Record<string, string> = { 'TAS': 'chip-tas', 'Peugeot': 'chip-peugeot', 'Citroen': 'chip-citroen', 'Citroën': 'chip-citroen' };
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

  getMaxDesigMain(): number { return this.designationMainFiltree.length ? this.designationMainFiltree[0].count : 1; }
  getMaxDesigPiece(): number { return this.designationPieceFiltree.length ? this.designationPieceFiltree[0].count : 1; }

  capitalizeFirst(s: string): string { return s.charAt(0).toUpperCase() + s.slice(1); }

  getUniqueZones(): string[] {
    const zones = new Set<string>();
    this.tousLesDossiersAvecDetails.forEach(m => { if (m.vehiculeZoneNom) zones.add(m.vehiculeZoneNom); });
    if (zones.size === 0) this.tousLesDossiers.forEach(m => { if (m.vehiculeZoneNom) zones.add(m.vehiculeZoneNom); });
    return Array.from(zones).sort();
  }

  getUniqueVehicules(): string[] {
    const mats = new Set<string>();
    this.tousLesDossiersAvecDetails.forEach(m => mats.add(m.vehiculeMatricule));
    if (mats.size === 0) this.tousLesDossiers.forEach(m => mats.add(m.vehiculeMatricule));
    return Array.from(mats).sort();
  }

  navigateTo(route: string): void { this.router.navigate([route]); }
}