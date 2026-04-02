// src/app/carburant/carburant-list/carburant-list.component.ts
// UPDATED: Intègre verrouillage mensuel + historique modifications
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CarburantVehicule, MOIS_LABELS } from '../../models/carburant-vehicule';
import { Vehicule } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { VerrouillageDto, HistoriqueModificationDto } from '../../models/verrouillage';
import { CarburantVehiculeService } from '../../services/carburant-vehicule.service';
import { VehiculeService } from '../../services/vehicule.service';
import { ZoneService } from '../../services/zone.service';
import { VerrouillageService } from '../../services/verrouillage.service';

@Component({
  selector: 'app-carburant-list',
  standalone: false,
  templateUrl: './carburant-list.component.html',
  styleUrls: ['./carburant-list.component.css']
})
export class CarburantListComponent implements OnInit {

  enregistrements: CarburantVehicule[] = [];
  vehicules: Vehicule[] = [];
  zones: Zone[] = [];
  moisLabels = MOIS_LABELS;

  annee  = new Date().getFullYear();
  mois   = new Date().getMonth() + 1;
  filtreZoneId = '';

  loading   = false;
  showForm  = false;
  selected: CarburantVehicule | null = null;

  // ── Récap annuel ──────────────────────────────────────────────
  showRecapAnnuel = false;
  recapAnnuelData: CarburantVehicule[] = [];
  recapAnnee = new Date().getFullYear();
  recapLoading = false;

  // ── Alertes budget ────────────────────────────────────────────
  budgetAlertes: CarburantVehicule[] = [];
  showAlertes = false;

  // ── Export ───────────────────────────────────────────────────
  exportingMensuel = false;
  exportingAnnuel  = false;

  // ── 🔒 VERROUILLAGE MENSUEL ───────────────────────────────────
  verrouillageStatut: VerrouillageDto | null = null;
  verrouillageLoading = false;
  isMoisVerrouille = false;

  // ── 📋 HISTORIQUE MODIFICATIONS ───────────────────────────────
  showHistorique = false;
  historique: HistoriqueModificationDto[] = [];
  historiqueLoading = false;
  historiqueVehicule = '';   // pour filtrer par véhicule
  historiqueMode: 'periode' | 'vehicule' | 'saisie' = 'periode';
  historiqueSaisieId: number | null = null;
  showHistoriqueDetail: HistoriqueModificationDto | null = null;

  annees = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i);
  moisOptions = Array.from({ length: 12 }, (_, i) => i + 1);

  constructor(
    private carburantService: CarburantVehiculeService,
    private vehiculeService:  VehiculeService,
    private zoneService:      ZoneService,
    private verrouillageService: VerrouillageService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadZones();
    this.loadVehicules();
    this.charger();
  }

  loadZones(): void {
    this.zoneService.getAllZones().subscribe({ next: d => this.zones = d });
  }

  loadVehicules(): void {
    this.vehiculeService.getAllVehicules().subscribe({ next: d => this.vehicules = d });
  }

  charger(): void {
    this.loading = true;
    const obs = this.filtreZoneId
      ? this.carburantService.getByZoneAndPeriode(+this.filtreZoneId, this.annee, this.mois)
      : this.carburantService.getByPeriode(this.annee, this.mois);

    obs.subscribe({
      next: d => {
        this.enregistrements = d;
        this.loading = false;
        this.chargerAlertesBudget();
        this.chargerVerrouillage();
      },
      error: () => { this.loading = false; }
    });
  }

  // ── 🔒 VERROUILLAGE ──────────────────────────────────────────

  chargerVerrouillage(): void {
    this.verrouillageLoading = true;
    const zoneId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    this.verrouillageService.getStatut(this.annee, this.mois, zoneId).subscribe({
      next: v => {
        this.verrouillageStatut = v;
        this.isMoisVerrouille = v.verrouille;
        this.verrouillageLoading = false;
      },
      error: () => {
        this.verrouillageLoading = false;
        this.isMoisVerrouille = false;
      }
    });
  }

  toggleVerrouillage(): void {
    if (!this.verrouillageStatut) return;
    const zoneId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    const msg = this.isMoisVerrouille
      ? `Déverrouiller ${this.moisLabels[this.mois]} ${this.annee} ? Les modifications redeviendront possibles.`
      : `Verrouiller ${this.moisLabels[this.mois]} ${this.annee} ? Plus aucune modification ne sera possible.`;

    if (!confirm(msg)) return;

    const obs = this.isMoisVerrouille
      ? this.verrouillageService.deverrouiller(this.annee, this.mois, zoneId)
      : this.verrouillageService.verrouiller(this.annee, this.mois, zoneId);

    obs.subscribe({
      next: (res: any) => {
        this.verrouillageStatut = res.data || res;
        this.isMoisVerrouille = res.data?.verrouille ?? res.verrouille ?? !this.isMoisVerrouille;
        alert(res.message || (this.isMoisVerrouille ? 'Mois verrouillé' : 'Mois déverrouillé'));
      },
      error: (err: any) => alert(err.error?.message || 'Erreur')
    });
  }

  // ── 📋 HISTORIQUE ─────────────────────────────────────────────

  ouvrirHistorique(mode: 'periode' | 'vehicule' | 'saisie' = 'periode', vehicule?: string, saisieId?: number): void {
    this.showHistorique = true;
    this.historiqueMode = mode;
    this.historiqueVehicule = vehicule || '';
    this.historiqueSaisieId = saisieId || null;
    this.chargerHistorique();
  }

  chargerHistorique(): void {
    this.historiqueLoading = true;
    let obs;

    if (this.historiqueMode === 'saisie' && this.historiqueSaisieId) {
      obs = this.verrouillageService.getHistoriqueSaisie(this.historiqueSaisieId);
    } else if (this.historiqueMode === 'vehicule' && this.historiqueVehicule) {
      obs = this.verrouillageService.getHistoriqueVehicule(this.historiqueVehicule, this.annee);
    } else {
      obs = this.verrouillageService.getHistoriquePeriode(this.annee, this.mois);
    }

    obs.subscribe({
      next: d => { this.historique = d; this.historiqueLoading = false; },
      error: () => { this.historiqueLoading = false; }
    });
  }

  fermerHistorique(): void { this.showHistorique = false; this.showHistoriqueDetail = null; }

  voirDetailHistorique(h: HistoriqueModificationDto): void {
    this.showHistoriqueDetail = this.showHistoriqueDetail?.id === h.id ? null : h;
  }

  formatJson(json?: string): string {
    if (!json) return '—';
    try { return JSON.stringify(JSON.parse(json), null, 2); }
    catch { return json; }
  }

  getActionClass(action: string): string {
    return { CREATE: 'action-create', UPDATE: 'action-update', DELETE: 'action-delete' }[action] || '';
  }

  getActionIcon(action: string): string {
    return { CREATE: '✅', UPDATE: '✏️', DELETE: '🗑️' }[action] || '?';
  }

  // ── Alertes budget ────────────────────────────────────────────

  chargerAlertesBudget(): void {
    this.carburantService.getBudgetDepasses(this.annee, this.mois).subscribe({
      next: d => { this.budgetAlertes = d; },
      error: () => {}
    });
  }

  toggleAlertes(): void { this.showAlertes = !this.showAlertes; }

  // ── Récap annuel ──────────────────────────────────────────────

  ouvrirRecapAnnuel(): void {
    this.showRecapAnnuel = true;
    this.chargerRecapAnnuel();
  }

  fermerRecapAnnuel(): void { this.showRecapAnnuel = false; }

  chargerRecapAnnuel(): void {
    this.recapLoading = true;
    const obs = this.filtreZoneId
      ? this.carburantService.getRecapAnnuelZone(+this.filtreZoneId, this.recapAnnee)
      : this.carburantService.getRecapAnnuelVehicule(
          this.enregistrements[0]?.vehiculeMatricule || '', this.recapAnnee);
    obs.subscribe({
      next: d => { this.recapAnnuelData = d; this.recapLoading = false; },
      error: () => { this.recapLoading = false; }
    });
  }

  // ── Export ───────────────────────────────────────────────────

  exporterMensuel(): void {
    this.exportingMensuel = true;
    const zoneId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    this.carburantService.exportExcelMensuel(this.annee, this.mois, zoneId).subscribe({
      next: (blob) => {
        this.carburantService.downloadBlob(blob, `carburant_${this.annee}_${String(this.mois).padStart(2,'0')}.xlsx`);
        this.exportingMensuel = false;
      },
      error: () => { alert('Erreur export'); this.exportingMensuel = false; }
    });
  }

  exporterAnnuel(): void {
    this.exportingAnnuel = true;
    const zoneId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    this.carburantService.exportExcelAnnuel(this.annee, zoneId).subscribe({
      next: (blob) => {
        this.carburantService.downloadBlob(blob, `carburant_annuel_${this.annee}.xlsx`);
        this.exportingAnnuel = false;
      },
      error: () => { alert('Erreur export annuel'); this.exportingAnnuel = false; }
    });
  }

  // ── CRUD ──────────────────────────────────────────────────────

  openCreate(): void {
    if (this.isMoisVerrouille) {
      alert(`🔒 Ce mois est verrouillé.\nAucune nouvelle saisie n'est possible pour ${this.moisLabels[this.mois]} ${this.annee}.`);
      return;
    }
    this.selected = null;
    this.showForm = true;
  }

  openEdit(e: CarburantVehicule): void {
    if (this.isMoisVerrouille) {
      alert(`🔒 Ce mois est verrouillé.\nLa modification est impossible pour ${this.moisLabels[this.mois]} ${this.annee}.`);
      return;
    }
    this.selected = { ...e };
    this.showForm = true;
  }

  closeForm(): void { this.showForm = false; this.selected = null; }
  onSaved(): void { this.closeForm(); this.charger(); }

  supprimer(e: CarburantVehicule): void {
    if (this.isMoisVerrouille) {
      alert(`🔒 Ce mois est verrouillé.\nLa suppression est impossible pour ${this.moisLabels[this.mois]} ${this.annee}.`);
      return;
    }
    if (!confirm(`Supprimer la saisie ${e.vehiculeMatricule} — ${this.moisLabels[e.mois]} ${e.annee} ?`)) return;
    this.carburantService.supprimer(e.id!).subscribe({
      next: () => this.charger(),
      error: err => alert(err.error?.message || 'Erreur')
    });
  }

  // ── Voir historique d'une saisie spécifique ───────────────────
  voirHistoriqueSaisie(e: CarburantVehicule): void {
    if (e.id) this.ouvrirHistorique('saisie', e.vehiculeMatricule, e.id);
  }

  voirHistoriqueVehicule(e: CarburantVehicule): void {
    this.ouvrirHistorique('vehicule', e.vehiculeMatricule);
  }

  // ── KPI totaux ────────────────────────────────────────────────

  get totalDistance(): number {
    return this.enregistrements.reduce((s, e) => s + (e.distanceParcourue || 0), 0);
  }
  get totalLitres(): number {
    return this.enregistrements.reduce((s, e) => s + (e.totalRavitaillementLitres || 0), 0);
  }
  get totalDemande(): number {
    return this.enregistrements.reduce((s, e) => s + (e.carburantDemandeDinars || 0), 0);
  }
  get moisLabel(): string { return this.moisLabels[this.mois]; }
  get nbBudgetDepasses(): number { return this.budgetAlertes.length; }

  // ── Récap helpers ─────────────────────────────────────────────

  getMoisData(matricule: string, mois: number): CarburantVehicule | undefined {
    return this.recapAnnuelData.find(d => d.vehiculeMatricule === matricule && d.mois === mois);
  }
  getVehiculesRecap(): string[] {
    return [...new Set(this.recapAnnuelData.map(d => d.vehiculeMatricule))];
  }
  getMarqueVehicule(matricule: string): string {
    return this.recapAnnuelData.find(d => d.vehiculeMatricule === matricule)?.vehiculeMarqueModele || '—';
  }
  getTotalKmVehicule(matricule: string): number {
    return this.recapAnnuelData.filter(d => d.vehiculeMatricule === matricule)
      .reduce((s, d) => s + (d.distanceParcourue || 0), 0);
  }
  getTotalLitresVehicule(matricule: string): number {
    return this.recapAnnuelData.filter(d => d.vehiculeMatricule === matricule)
      .reduce((s, d) => s + (d.totalRavitaillementLitres || 0), 0);
  }
  getTotalDtVehicule(matricule: string): number {
    return this.recapAnnuelData.filter(d => d.vehiculeMatricule === matricule)
      .reduce((s, d) => s + (d.carburantDemandeDinars || 0), 0);
  }

  navigateTo(r: string): void { this.router.navigate([r]); }

  fmt3(v: number | undefined): string {
    return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }
}