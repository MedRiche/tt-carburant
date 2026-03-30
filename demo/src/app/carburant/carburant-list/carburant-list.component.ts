// src/app/carburant/carburant-list/carburant-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CarburantVehicule, MOIS_LABELS } from '../../models/carburant-vehicule';
import { Vehicule } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { CarburantVehiculeService } from '../../services/carburant-vehicule.service';
import { VehiculeService } from '../../services/vehicule.service';
import { ZoneService } from '../../services/zone.service';

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

  // NOUVEAU : récap annuel
  showRecapAnnuel = false;
  recapAnnuelData: CarburantVehicule[] = [];
  recapAnnee = new Date().getFullYear();
  recapLoading = false;

  // NOUVEAU : alertes budget
  budgetAlertes: CarburantVehicule[] = [];
  showAlertes = false;

  // NOUVEAU : export en cours
  exportingMensuel = false;
  exportingAnnuel  = false;

  annees = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i);
  moisOptions = Array.from({ length: 12 }, (_, i) => i + 1);

  constructor(
    private carburantService: CarburantVehiculeService,
    private vehiculeService:  VehiculeService,
    private zoneService:      ZoneService,
    private router:           Router
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
        // Charger les alertes budget du même mois
        this.chargerAlertesBudget();
      },
      error: () => { this.loading = false; }
    });
  }

  // ── NOUVEAU : alertes budget ──────────────────────────────────

  chargerAlertesBudget(): void {
    this.carburantService.getBudgetDepasses(this.annee, this.mois).subscribe({
      next: d => { this.budgetAlertes = d; },
      error: () => {}
    });
  }

  toggleAlertes(): void { this.showAlertes = !this.showAlertes; }

  // ── NOUVEAU : récap annuel ────────────────────────────────────

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

  // ── NOUVEAU : export Excel ────────────────────────────────────

  exporterMensuel(): void {
    this.exportingMensuel = true;
    const zoneId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    this.carburantService.exportExcelMensuel(this.annee, this.mois, zoneId).subscribe({
      next: (blob) => {
        const filename = `carburant_${this.annee}_${String(this.mois).padStart(2,'0')}.xlsx`;
        this.carburantService.downloadBlob(blob, filename);
        this.exportingMensuel = false;
      },
      error: () => {
        alert('Erreur lors de l\'export');
        this.exportingMensuel = false;
      }
    });
  }

  exporterAnnuel(): void {
    this.exportingAnnuel = true;
    const zoneId = this.filtreZoneId ? +this.filtreZoneId : undefined;
    this.carburantService.exportExcelAnnuel(this.annee, zoneId).subscribe({
      next: (blob) => {
        const filename = `carburant_annuel_${this.annee}.xlsx`;
        this.carburantService.downloadBlob(blob, filename);
        this.exportingAnnuel = false;
      },
      error: () => {
        alert('Erreur lors de l\'export annuel');
        this.exportingAnnuel = false;
      }
    });
  }

  // ── Récap annuel : totaux par véhicule ───────────────────────

  getTotalKmVehicule(matricule: string): number {
    return this.recapAnnuelData
      .filter(d => d.vehiculeMatricule === matricule)
      .reduce((s, d) => s + (d.distanceParcourue || 0), 0);
  }

  getTotalLitresVehicule(matricule: string): number {
    return this.recapAnnuelData
      .filter(d => d.vehiculeMatricule === matricule)
      .reduce((s, d) => s + (d.totalRavitaillementLitres || 0), 0);
  }

  getTotalDtVehicule(matricule: string): number {
    return this.recapAnnuelData
      .filter(d => d.vehiculeMatricule === matricule)
      .reduce((s, d) => s + (d.carburantDemandeDinars || 0), 0);
  }

  getMarqueVehicule(matricule: string): string {
    return this.recapAnnuelData.find(d => d.vehiculeMatricule === matricule)?.vehiculeMarqueModele || '—';
  }

  // ── CRUD ──────────────────────────────────────────────────────

  openCreate(): void { this.selected = null; this.showForm = true; }
  openEdit(e: CarburantVehicule): void { this.selected = { ...e }; this.showForm = true; }
  closeForm(): void { this.showForm = false; this.selected = null; }
  onSaved(): void { this.closeForm(); this.charger(); }

  supprimer(e: CarburantVehicule): void {
    if (!confirm(`Supprimer la saisie ${e.vehiculeMatricule} — ${this.moisLabels[e.mois]} ${e.annee} ?`)) return;
    this.carburantService.supprimer(e.id!).subscribe({
      next: () => this.charger(),
      error: err => alert(err.error?.message || 'Erreur')
    });
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

  // ── Récap annuel helpers ──────────────────────────────────────

  getMoisData(matricule: string, mois: number): CarburantVehicule | undefined {
    return this.recapAnnuelData.find(d => d.vehiculeMatricule === matricule && d.mois === mois);
  }

  getVehiculesRecap(): string[] {
    return [...new Set(this.recapAnnuelData.map(d => d.vehiculeMatricule))];
  }

  navigateTo(r: string): void { this.router.navigate([r]); }

  fmt3(v: number | undefined): string {
    return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }


}