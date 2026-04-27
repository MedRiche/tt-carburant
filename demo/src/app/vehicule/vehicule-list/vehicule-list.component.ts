// src/app/vehicule/vehicule-list/vehicule-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Vehicule, TypeCarburant } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { VehiculeService } from '../../services/vehicule.service';
import { ZoneService } from '../../services/zone.service';
import { UtilisateurService } from '../../services/utilisateur.service';

export interface ImportResult {
  imported: number;
  updated: number;
  skipped: number;
  total: number;
  zone: string;
  errors: string[];
  conducteursCreated: number;
  conducteursExistants: number;
  conducteursDetails: { nomComplet: string; email: string; statut: string; userId?: number }[];
}

export interface ConducteurAValider {
  userId: number;
  nomComplet: string;
  email: string;
  selectedZoneIds: number[];
  statut: 'pending' | 'validating' | 'validated' | 'error';
  errorMsg?: string;
}

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

  searchText   = '';
  filtreZoneId = '';
  filtreType   = '';

  // Import Excel
  showImportModal     = false;
  importZoneNom       = '';
  importLoading       = false;
  importResult: ImportResult | null = null;

  // Panel conducteurs
  showConducteurPanel = false;

  // ── NOUVEAU : Validation en masse des conducteurs ──────────────────────
  /** Conducteurs prêts à être validés depuis la modal d'import */
  conducteursAValider: ConducteurAValider[] = [];
  /** Zone sélectionnée pour la validation en masse (toutes les conducteurs) */
  zonesMasseIds: number[] = [];
  validationEnCours = false;
  validationTerminee = false;
  nbValides = 0;

  TypeCarburant = TypeCarburant;

  constructor(
    private vehiculeService: VehiculeService,
    private zoneService: ZoneService,
    private utilisateurService: UtilisateurService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadVehicules();
    this.loadZones();
  }

  loadVehicules(): void {
    this.loading = true;
    this.vehiculeService.getAllVehicules().subscribe({
      next: (data) => { this.vehicules = data; this.appliquerFiltres(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadZones(): void {
    this.zoneService.getAllZones().subscribe({ next: (zones) => { this.zones = zones; } });
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
    if (this.filtreZoneId) result = result.filter(v => String(v.zoneId) === this.filtreZoneId);
    if (this.filtreType)   result = result.filter(v => v.typeVehicule === this.filtreType);
    this.vehiculesFiltres = result;
  }

  openCreate(): void { this.selectedVehicule = null; this.showForm = true; }
  openEdit(v: Vehicule): void { this.selectedVehicule = { ...v }; this.showForm = true; }
  closeForm(): void { this.showForm = false; this.selectedVehicule = null; }
  onSaved(): void { this.closeForm(); this.loadVehicules(); }

  supprimer(v: Vehicule): void {
    if (!confirm(`Supprimer le véhicule ${v.matricule} ?`)) return;
    this.vehiculeService.supprimerVehicule(v.matricule).subscribe({
      next: () => this.loadVehicules(),
      error: (err) => alert(err.error?.message || 'Erreur')
    });
  }

  // ── Import Excel ──────────────────────────────────────────────────────────

  ouvrirImport(): void {
    this.showImportModal      = true;
    this.importResult         = null;
    this.importZoneNom        = '';
    this.showConducteurPanel  = false;
    this.conducteursAValider  = [];
    this.zonesMasseIds        = [];
    this.validationTerminee   = false;
    this.nbValides            = 0;
  }

  fermerImport(): void {
    this.showImportModal = false;
    if (this.importResult) this.loadVehicules();
    this.importResult = null;
    this.conducteursAValider = [];
    this.zonesMasseIds = [];
    this.validationTerminee = false;
  }

  onFichierSelectionne(event: Event, zoneNom: string): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';
    this.lancerImport(file, zoneNom);
  }

  onImportFichier(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';
    this.lancerImport(file, this.importZoneNom);
  }

  lancerImport(file: File, zoneNom: string): void {
    this.importLoading       = true;
    this.importResult        = null;
    this.showConducteurPanel = false;
    this.conducteursAValider = [];
    this.zonesMasseIds       = [];
    this.validationTerminee  = false;
    this.nbValides           = 0;

    this.vehiculeService.importerExcel(file, zoneNom).subscribe({
      next: (res: any) => {
        this.importResult  = res as ImportResult;
        this.importLoading = false;

        // Construire la liste des conducteurs à valider (uniquement les CRÉÉS)
        if (this.importResult.conducteursCreated > 0) {
          this.showConducteurPanel = true;
          this.conducteursAValider = this.importResult.conducteursDetails
            .filter(c => c.statut === 'CREATED' && c.userId != null)
            .map(c => ({
              userId: c.userId!,
              nomComplet: c.nomComplet,
              email: c.email,
              selectedZoneIds: [],
              statut: 'pending' as const
            }));
          // Pré-sélectionner la zone d'import si connue
          if (zoneNom) {
            const zone = this.zones.find(z => z.nom.toLowerCase() === zoneNom.toLowerCase());
            if (zone) {
              this.zonesMasseIds = [zone.id];
            }
          }
        }

        this.loadVehicules();
      },
      error: (err: any) => {
        this.importLoading = false;
        alert('❌ Erreur import : ' + (err.error?.message || err.message || 'Erreur'));
      }
    });
  }

  // ── Validation en masse des conducteurs ───────────────────────────────────

  toggleZoneMasse(zoneId: number): void {
    const i = this.zonesMasseIds.indexOf(zoneId);
    if (i > -1) this.zonesMasseIds.splice(i, 1);
    else this.zonesMasseIds.push(zoneId);
  }

  isZoneMasseSelected(zoneId: number): boolean {
    return this.zonesMasseIds.includes(zoneId);
  }

  /** Valide tous les conducteurs créés lors de l'import avec les zones sélectionnées */
  validerTousLesConducteurs(): void {
    if (this.zonesMasseIds.length === 0) {
      alert('Veuillez sélectionner au moins une zone.');
      return;
    }
    if (this.conducteursAValider.length === 0) return;

    this.validationEnCours = true;
    this.nbValides = 0;

    const pending = this.conducteursAValider.filter(c => c.statut === 'pending');
    let completed = 0;

    for (const conducteur of pending) {
      conducteur.statut = 'validating';
      this.utilisateurService.validerCompteAvecZones({
        utilisateurId: conducteur.userId,
        zoneIds: this.zonesMasseIds
      }).subscribe({
        next: () => {
          conducteur.statut = 'validated';
          this.nbValides++;
          completed++;
          if (completed === pending.length) {
            this.validationEnCours  = false;
            this.validationTerminee = true;
          }
        },
        error: (err) => {
          conducteur.statut  = 'error';
          conducteur.errorMsg = err.error?.message || 'Erreur';
          completed++;
          if (completed === pending.length) {
            this.validationEnCours  = false;
            this.validationTerminee = true;
          }
        }
      });
    }
  }

  /** Compter combien sont encore en attente */
  get nbConducteursPending(): number {
    return this.conducteursAValider.filter(c => c.statut === 'pending').length;
  }

  get nbConducteursValidated(): number {
    return this.conducteursAValider.filter(c => c.statut === 'validated').length;
  }

  allerGestionUtilisateurs(): void {
    this.fermerImport();
    this.router.navigate(['/admin/utilisateurs']);
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

  getInitials(nom: string): string {
    return (nom || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }
}