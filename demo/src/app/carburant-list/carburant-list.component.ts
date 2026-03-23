// src/app/carburant/carburant-list/carburant-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CarburantVehicule, MOIS_LABELS } from '../models/carburant-vehicule';
import { Vehicule } from '../models/vehicule';
import { Zone } from '../models/zone';
import { CarburantVehiculeService } from '../services/carburant-vehicule.service';
import { VehiculeService } from '../services/vehicule.service';
import { ZoneService } from '../services/zone.service';

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

  // Filtres période
  annee  = new Date().getFullYear();
  mois   = new Date().getMonth() + 1;
  filtreZoneId = '';

  loading   = false;
  showForm  = false;
  selected: CarburantVehicule | null = null;

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
      next: d => { this.enregistrements = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

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

  // KPI totaux du tableau affiché
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

  navigateTo(r: string): void { this.router.navigate([r]); }

  fmt3(v: number | undefined): string {
    return (v ?? 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }
}