// src/app/groupe-electrogene/groupe-electrogene-list/groupe-electrogene-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GroupeElectrogeneService } from '../../services/groupe-electrogene.service';
import { ZoneService } from '../../services/zone.service';
import { GroupeElectrogene, TypeCarburantGE } from '../../models/groupe-electrogene';
import { GestionCarburantGE, Semestre, SEMESTRE_LABELS } from '../../models/gestion-carburant-ge';
import { Zone } from '../../models/zone';

@Component({
  selector: 'app-groupe-electrogene-list',
  standalone: false,
  templateUrl: './groupe-electrogene-list.component.html',
  styleUrls: ['./groupe-electrogene-list.component.css']
})
export class GroupeElectrogeneListComponent implements OnInit {

  groupes: GroupeElectrogene[] = [];
  zones: Zone[]                = [];
  saisies: GestionCarburantGE[]= [];
  loading    = false;
  loadError  = '';

  // Filtres
  searchText  = '';
  filtreZone  = '';

  // Modal / form
  showForm           = false;
  selectedGE: GroupeElectrogene | null = null;
  formMode: 'create' | 'edit' | 'fuel' = 'create';
  selectedFuelSaisie: GestionCarburantGE | null = null;

  // Import
  showImportModal = false;
  importLoading   = false;
  importResult: any = null;

  readonly Semestre      = Semestre;
  readonly semestreLabels = SEMESTRE_LABELS;

  private readonly TYPE_LABELS: Record<string, string> = {
    GASOIL_ORDINAIRE:   'Gasoil Ord.',
    GASOIL_SANS_SOUFRE: 'Gasoil SS',
    SUPER_SANS_PLOMB:   'Super SP',
    ESSENCE:            'Essence'
  };

  constructor(
    private geService:   GroupeElectrogeneService,
    private zoneService: ZoneService,
    private router:      Router
  ) {}

  ngOnInit(): void {
    this.chargerZones();
    this.chargerGroupes();
    this.chargerSaisies();
  }

  // ── Chargement ──────────────────────────────────────────────────

  chargerZones(): void {
    this.zoneService.getAllZones().subscribe({
      next: (z) => this.zones = z,
      error: () => {}
    });
  }

  chargerGroupes(): void {
    this.loading   = true;
    this.loadError = '';
    this.geService.getAllGroupes().subscribe({
      next: (g) => {
        this.groupes = g;
        this.loading = false;
      },
      error: (err) => {
        this.loading   = false;
        this.loadError = 'Impossible de charger les groupes électrogènes.';
        console.error('Erreur chargement groupes:', err);
      }
    });
  }

  chargerSaisies(): void {
    this.geService.getAllSaisies().subscribe({
      next: (s) => this.saisies = s,
      error: () => {}
    });
  }

  loadAll(): void {
    this.chargerGroupes();
    this.chargerSaisies();
  }

  // ── Filtrage ─────────────────────────────────────────────────────

  get filteredGroupes(): GroupeElectrogene[] {
    let list = [...this.groupes];
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(g =>
        g.site.toLowerCase().includes(q) ||
        (g.utilisateurRoc || '').toLowerCase().includes(q) ||
        (g.zoneNom || '').toLowerCase().includes(q)
      );
    }
    if (this.filtreZone) {
      list = list.filter(g => String(g.zoneId) === this.filtreZone);
    }
    return list;
  }

  // ── Saisies carburant ────────────────────────────────────────────

  getSaisiesForSite(site: string): GestionCarburantGE[] {
    return this.saisies.filter(s => s.site === site);
  }

  // ── Actions CRUD ────────────────────────────────────────────────

  openCreateGE(): void {
    this.selectedGE = null;
    this.formMode   = 'create';
    this.showForm   = true;
  }

  openEditGE(ge: GroupeElectrogene): void {
    this.selectedGE = { ...ge };
    this.formMode   = 'edit';
    this.showForm   = true;
  }

  openFuelForm(ge: GroupeElectrogene, saisie?: GestionCarburantGE): void {
    this.selectedGE        = ge;
    this.selectedFuelSaisie = saisie || null;
    this.formMode          = 'fuel';
    this.showForm          = true;
  }

  deleteGE(site: string): void {
    if (!confirm(`Supprimer le groupe électrogène "${site}" ?`)) return;
    this.geService.supprimer(site).subscribe({
      next: () => this.loadAll(),
      error: (err) => alert(err?.error?.message || 'Erreur lors de la suppression')
    });
  }

  deleteSaisie(id: number, site: string): void {
    if (!confirm(`Supprimer la saisie pour "${site}" ?`)) return;
    this.geService.supprimerSaisie(id).subscribe({
      next: () => this.chargerSaisies(),
      error: (err) => alert(err?.error?.message || 'Erreur lors de la suppression')
    });
  }

  onFormSaved(): void {
    this.showForm = false;
    this.loadAll();
  }

  // ── Import Excel ────────────────────────────────────────────────

  ouvrirImport(): void {
    this.showImportModal = true;
    this.importResult    = null;
  }

  fermerImport(): void {
    this.showImportModal = false;
    if (this.importResult) this.loadAll();
  }

  onFichierSelectionne(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';
    this.lancerImport(file);
  }

  lancerImport(file: File): void {
    this.importLoading = true;
    this.geService.importerExcel(file).subscribe({
      next: (res) => {
        this.importResult  = res;
        this.importLoading = false;
        if (res.imported > 0 || res.updated > 0) this.loadAll();
      },
      error: (err) => {
        this.importLoading = false;
        alert('Erreur import : ' + (err?.error?.message || err?.message || 'Erreur inconnue'));
      }
    });
  }

  // ── Utilitaires ─────────────────────────────────────────────────

  getTypeLabel(type: string): string {
    return this.TYPE_LABELS[type] || type;
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }
}