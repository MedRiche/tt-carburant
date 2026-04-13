// src/app/vehicule/vehicule-list/vehicule-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Vehicule, TypeCarburant } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { VehiculeService } from '../../services/vehicule.service';
import { ZoneService } from '../../services/zone.service';

@Component({
  selector: 'app-vehicule-list',
  standalone: false,
  templateUrl: './vehicule-list.component.html',
  styleUrls: ['./vehicule-list.component.css']
})
export class VehiculeListComponent implements OnInit {

  vehicules: Vehicule[] = [];
  vehiculesFiltres: Vehicule[] = [];
  zones: Zone[] = [];

  loading = false;
  showForm = false;
  selectedVehicule: Vehicule | null = null;

  // Filtres
  searchText   = '';
  filtreZoneId = '';
  filtreType   = '';

  // Import Excel
  showImportModal = false;
  importZoneNom   = '';
  importLoading   = false;
  importResult: {
    imported: number; updated: number; skipped: number;
    total: number; zone: string; errors: string[];
  } | null = null;

  TypeCarburant = TypeCarburant;

  // Zones prédéfinies correspondant aux fichiers Excel
  readonly ZONES_EXCEL = [
    'DAF',
    'COMMERCIALE',
    'CSC BARDO',
    'CSC BELVEDERE',
    'CSC KASBAH',
    'DIVISION CLIENTELE',
    'Réseaux&S.I',
    'HACHED',
    'BL NORD'
  ];

  constructor(
    private vehiculeService: VehiculeService,
    private zoneService: ZoneService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadVehicules();
    this.loadZones();
  }

  loadVehicules(): void {
    this.loading = true;
    this.vehiculeService.getAllVehicules().subscribe({
      next: (data) => {
        this.vehicules = data;
        this.appliquerFiltres();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  loadZones(): void {
    this.zoneService.getAllZones().subscribe({
      next: (zones) => { this.zones = zones; }
    });
  }

  appliquerFiltres(): void {
    let result = [...this.vehicules];
    if (this.searchText.trim()) {
      const q = this.searchText.toLowerCase();
      result = result.filter(v =>
        v.matricule.toLowerCase().includes(q) ||
        v.marqueModele.toLowerCase().includes(q) ||
        (v.nomConducteur || '').toLowerCase().includes(q)
      );
    }
    if (this.filtreZoneId) {
      result = result.filter(v => String(v.zoneId) === this.filtreZoneId);
    }
    if (this.filtreType) {
      result = result.filter(v => v.typeVehicule === this.filtreType);
    }
    this.vehiculesFiltres = result;
  }

  openCreate(): void { this.selectedVehicule = null; this.showForm = true; }
  openEdit(v: Vehicule): void { this.selectedVehicule = { ...v }; this.showForm = true; }
  closeForm(): void { this.showForm = false; this.selectedVehicule = null; }

  onSaved(): void {
    this.closeForm();
    this.loadVehicules();
  }

  supprimer(v: Vehicule): void {
    if (!confirm(`Supprimer le véhicule ${v.matricule} ?`)) return;
    this.vehiculeService.supprimerVehicule(v.matricule).subscribe({
      next: () => { this.loadVehicules(); },
      error: (err) => alert(err.error?.message || 'Erreur')
    });
  }

  // ── Import Excel ──────────────────────────────────────────────────────────

  ouvrirImport(): void {
    this.showImportModal = true;
    this.importResult = null;
    this.importZoneNom = '';
  }

  fermerImport(): void {
    this.showImportModal = false;
    this.importResult = null;
    if (this.importResult !== null) {
      // Rafraîchir la liste si un import a eu lieu
      this.loadVehicules();
    }
  }

  onFichierSelectionne(event: Event, zoneNom: string): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    // Réinitialiser l'input pour permettre de re-sélectionner le même fichier
    input.value = '';
    this.lancerImport(file, zoneNom);
  }

  lancerImport(file: File, zoneNom: string): void {
    this.importLoading = true;
    this.importResult  = null;

    this.vehiculeService.importerExcel(file, zoneNom).subscribe({
      next: (res: any) => {
        this.importResult = res;
        this.importLoading = false;
        this.loadVehicules(); // Rafraîchir après import
      },
      error: (err: any) => {
        this.importLoading = false;
        alert('❌ Erreur lors de l\'import : ' + (err.error?.message || err.message || 'Erreur inconnue'));
      }
    });
  }

  // Déclenche l'import automatiquement quand un fichier est choisi
  onImportFichier(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';
    this.lancerImport(file, this.importZoneNom);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getTypeLabel(t: TypeCarburant): string {
    const map: Record<string, string> = {
      ESSENCE: 'Essence',
      GASOIL_ORDINAIRE: 'Gasoil Ordinaire',
      GASOIL_SANS_SOUFRE: 'Gasoil Sans Soufre',
      GASOIL_50: 'Gasoil 50',
      SUPER_SANS_PLOMB: 'Super Sans Plomb'
    };
    return map[t] || t;
  }

  typesVehicule(): string[] {
    return [...new Set(this.vehicules.map(v => v.typeVehicule))].filter(Boolean);
  }

  navigateTo(route: string): void { this.router.navigate([route]); }
}