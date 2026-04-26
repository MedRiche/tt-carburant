// src/app/groupe-electrogene/gestion-carburant-ge-form/gestion-carburant-ge-form.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GroupeElectrogeneService } from '../../services/groupe-electrogene.service';
import { CarburantGeService } from '../../services/carburant-ge.service';
import { GroupeElectrogene } from '../../models/groupe-electrogene';
import { GestionCarburantGE } from '../../models/gestion-carburant-ge';

@Component({
  selector: 'app-gestion-carburant-ge-form',
  standalone: false,
  templateUrl: './gestion-carburant-ge-form.component.html',
  styleUrls: ['./gestion-carburant-ge-form.component.css']
})
export class GestionCarburantGEFormComponent implements OnInit {

  groupes:  GroupeElectrogene[]  = [];
  saisies:  GestionCarburantGE[] = [];
  loading = false;

  // Filtres période
  filtreAnnee: number = new Date().getFullYear();
  filtreSemestre: 'PREMIER' | 'DEUXIEME' = 'PREMIER';
  anneeOptions = [2024, 2025, 2026, 2027, 2028];

  // Modal saisie
  showSaisieModal   = false;
  selectedGE: GroupeElectrogene | null      = null;
  selectedSaisie: GestionCarburantGE | null = null;

  private readonly TYPE_LABELS: Record<string, string> = {
    GASOIL_ORDINAIRE:   'Gasoil Ord.',
    GASOIL_SANS_SOUFRE: 'Gasoil SS',
    SUPER_SANS_PLOMB:   'Super SP',
    ESSENCE:            'Essence'
  };

  constructor(
    private geService:   GroupeElectrogeneService,
    private carbService: CarburantGeService,
    private router:      Router
  ) {}

  ngOnInit(): void {
    this.chargerTout();
  }

  chargerTout(): void {
    this.loading = true;
    this.geService.getAllGroupes().subscribe({
      next: (g) => {
        this.groupes = g;
        this.chargerSaisies();
      },
      error: () => { this.loading = false; }
    });
  }

  chargerSaisies(): void {
    this.carbService.getAllSaisies().subscribe({
      next: (s) => {
        this.saisies = s;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onFiltreChange(): void {
    // Le filtrage est réactif via le getter saisiesFiltrees
  }

  get saisiesFiltrees(): GestionCarburantGE[] {
    return this.saisies.filter(
      (s) => s.annee === +this.filtreAnnee && s.semestre === this.filtreSemestre
    );
  }

  getSaisie(site: string): GestionCarburantGE | null {
    return this.saisiesFiltrees.find((s) => s.site === site) ?? null;
  }

  get totalRavit(): number {
    return this.saisiesFiltrees.reduce((acc, s) => acc + (s.totalRavitaillementLitres ?? 0), 0);
  }

  get totalHeures(): number {
    return this.saisiesFiltrees.reduce((acc, s) => acc + (s.nbHeuresTravail ?? 0), 0);
  }

  get totalDemande(): number {
    return this.saisiesFiltrees.reduce((acc, s) => acc + (s.carburantDemandeDinarsCours ?? 0), 0);
  }

  /** Ouvre le modal de saisie.
   * ge = null → saisie libre (sélection du site dans le modal)
   * saisie = null → création, sinon édition
   */
  openSaisieModal(ge: GroupeElectrogene | null, saisie: GestionCarburantGE | null): void {
    this.selectedGE     = ge;
    this.selectedSaisie = saisie;
    this.showSaisieModal = true;
  }

  onSaisieSaved(): void {
    this.showSaisieModal = false;
    this.chargerSaisies();
  }

  deleteSaisie(id: number, site: string): void {
    if (!confirm(`Supprimer la saisie pour "${site}" ?`)) return;
    this.carbService.supprimerSaisie(id).subscribe({
      next:  () => this.chargerSaisies(),
      error: (err) => alert(err?.error?.message || 'Erreur')
    });
  }

  getTypeLabel(type: string): string {
    return this.TYPE_LABELS[type] ?? type;
  }

  getPctClass(pct: number): string {
    if (pct <= 80)  return 'pct-ok';
    if (pct <= 100) return 'pct-warn';
    return 'pct-over';
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }
}