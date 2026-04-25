// src/app/groupe-electrogene/groupe-electrogene-list/groupe-electrogene-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GroupeElectrogeneService } from '../../services/groupe-electrogene.service';
import { CarburantGeService } from '../../services/carburant-ge.service';
import { ZoneService } from '../../services/zone.service';
import { GroupeElectrogene, TypeCarburantGE, TYPE_CARBURANT_LABELS } from '../../models/groupe-electrogene';
import { GestionCarburantGE, Semestre, SEMESTRE_LABELS } from '../../models/gestion-carburant-ge';
import { Zone } from '../../models/zone';

@Component({
  selector: 'app-groupe-electrogene-list',
  standalone: false,
  templateUrl: './groupe-electrogene-list.component.html',
  styleUrls: ['./groupe-electrogene-list.component.css']
})
export class GroupeElectrogeneListComponent implements OnInit {

  groupes: GroupeElectrogene[]  = [];
  zones:   Zone[]               = [];
  saisies: GestionCarburantGE[] = [];
  loading   = false;
  loadError = '';

  // Filtres
  searchText = '';

  // Modal / form
  showForm            = false;
  selectedGE: GroupeElectrogene | null       = null;
  formMode: 'create' | 'edit' | 'fuel'       = 'create';
  selectedFuelSaisie: GestionCarburantGE | null = null;

  // ✅ NOUVEAU : Modal détail carte Agilis
  showDetailModal = false;
  detailGE: GroupeElectrogene | null = null;
  showPin = false;
  showPuk = false;

  // Import
  showImportModal = false;
  importLoading   = false;
  importResult: any = null;

  readonly Semestre       = Semestre;
  readonly semestreLabels = SEMESTRE_LABELS;

  private readonly TYPE_LABELS: Record<string, string> = {
    GASOIL_ORDINAIRE:   'Gasoil Ord.',
    GASOIL_SANS_SOUFRE: 'Gasoil SS',
    SUPER_SANS_PLOMB:   'Super SP',
    ESSENCE:            'Essence'
  };

  constructor(
    private geService:   GroupeElectrogeneService,
    private carbService: CarburantGeService,
    private zoneService: ZoneService,
    private router:      Router
  ) {}

  ngOnInit(): void {
    this.chargerZones();
    this.chargerGroupes();
    this.chargerSaisies();
  }

  // ── Chargement ──────────────────────────────────────────────────────────

  chargerZones(): void {
    this.zoneService.getAllZones().subscribe({
      next:  (z) => (this.zones = z),
      error: ()  => {}
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
    this.carbService.getAllSaisies().subscribe({
      next:  (s) => (this.saisies = s),
      error: ()  => {}
    });
  }

  loadAll(): void {
    this.chargerGroupes();
    this.chargerSaisies();
  }

  // ── Filtrage (sans zone) ─────────────────────────────────────────────────

  get filteredGroupes(): GroupeElectrogene[] {
    if (!this.searchText) return [...this.groupes];
    const q = this.searchText.toLowerCase();
    return this.groupes.filter(
      (g) =>
        g.site.toLowerCase().includes(q) ||
        (g.utilisateurRoc ?? '').toLowerCase().includes(q)
    );
  }

  // ── Saisies carburant ────────────────────────────────────────────────────

  getSaisiesForSite(site: string): GestionCarburantGE[] {
    return this.saisies.filter((s) => s.site === site);
  }

  // ── Actions CRUD ─────────────────────────────────────────────────────────

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
    this.selectedGE         = ge;
    this.selectedFuelSaisie = saisie ?? null;
    this.formMode           = 'fuel';
    this.showForm           = true;
  }

  deleteGE(site: string): void {
    if (!confirm(`Supprimer le groupe électrogène "${site}" ?`)) return;
    this.geService.supprimer(site).subscribe({
      next:  () => this.loadAll(),
      error: (err) => alert(err?.error?.message || 'Erreur lors de la suppression')
    });
  }

  deleteSaisie(id: number, site: string): void {
    if (!confirm(`Supprimer la saisie pour "${site}" ?`)) return;
    this.carbService.supprimerSaisie(id).subscribe({
      next:  () => this.chargerSaisies(),
      error: (err) => alert(err?.error?.message || 'Erreur lors de la suppression')
    });
  }

  onFormSaved(): void {
    this.showForm = false;
    this.loadAll();
  }

  // ── Modal Détail Carte Agilis ─────────────────────────────────────────────

  openDetail(ge: GroupeElectrogene): void {
    this.detailGE       = ge;
    this.showDetailModal = true;
    this.showPin        = false;
    this.showPuk        = false;
  }

  closeDetail(): void {
    this.showDetailModal = false;
    this.detailGE        = null;
    this.showPin         = false;
    this.showPuk         = false;
  }

  togglePin(event: Event): void {
    event.stopPropagation();
    this.showPin = !this.showPin;
  }

  togglePuk(event: Event): void {
    event.stopPropagation();
    this.showPuk = !this.showPuk;
  }

  /**
   * Formate la date d'expiration "yyyy-MM" → "MM/yyyy"
   * pour l'affichage dans le modal.
   */
  formatDateExp(raw?: string): string {
    if (!raw) return '—';
    // Format retourné par le backend : "2028-08"
    const parts = raw.split('-');
    if (parts.length === 2) return `${parts[1]}/${parts[0]}`;
    return raw;
  }

  /**
   * Retourne true si la carte expire dans moins de 6 mois.
   */
  isExpiringSoon(raw?: string): boolean {
    if (!raw) return false;
    try {
      const [year, month] = raw.split('-').map(Number);
      const exp = new Date(year, month - 1, 1);
      const sixMonthsFromNow = new Date();
      sixMonthsFromNow.setMonth(sixMonthsFromNow.getMonth() + 6);
      return exp <= sixMonthsFromNow;
    } catch { return false; }
  }

  // ── Import Excel ──────────────────────────────────────────────────────────

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
    this.importResult  = null;
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

  // ── Utilitaires ───────────────────────────────────────────────────────────

  getTypeLabel(type: string): string {
    return this.TYPE_LABELS[type] ?? type;
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }
}