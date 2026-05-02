// src/app/technicien/technicien-equipements/technicien-equipements.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TechnicienService } from '../../services/technicien.service';
import {
  TechnicienEquipementService,
  VehiculeStats,
  GEStats
} from '../../services/technicien-equipement.service';
import { Vehicule } from '../../models/vehicule';
import { GroupeElectrogene } from '../../models/groupe-electrogene';
import { Zone } from '../../models/zone';

export type TabType = 'vehicules' | 'groupes';
export type ViewMode = 'grid' | 'list';

@Component({
  selector: 'app-technicien-equipements',
  standalone: false,
  templateUrl: './technicien-equipements.component.html',
  styleUrls: ['./technicien-equipements.component.css']
})
export class TechnicienEquipementsComponent implements OnInit {

  // ── State ────────────────────────────────────────────────────
  activeTab: TabType = 'vehicules';
  viewMode: ViewMode = 'grid';

  zones: Zone[] = [];
  selectedZoneId: number | null = null;

  vehicules: Vehicule[] = [];
  vehiculesFiltres: Vehicule[] = [];
  vehiculeStats: VehiculeStats | null = null;

  groupes: GroupeElectrogene[] = [];
  groupesFiltres: GroupeElectrogene[] = [];
  geStats: GEStats | null = null;

  loading = true;
  error: string | null = null;

  searchQuery = '';
  filterType = '';

  selectedVehicule: Vehicule | null = null;
  selectedGroupe: GroupeElectrogene | null = null;
  showDetailModal = false;

  nomAffiche = localStorage.getItem('nom') || 'Technicien';

  // ← NOUVEAU : détecter si conducteur
  estConducteur = false;

  constructor(
    private authService: AuthService,
    private technicienService: TechnicienService,
    private equipementService: TechnicienEquipementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Détecter le rôle depuis localStorage (mis à jour au login)
    this.estConducteur = this.authService.isConducteur();
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.error = null;

    if (this.estConducteur) {
      // Conducteur : pas besoin des zones, on charge directement ses véhicules
      this.loadVehicules();
      // Les conducteurs ne voient pas les GE
    } else {
      // Technicien classique : charger zones + véhicules + GE
      this.technicienService.getMesZones().subscribe({
        next: (zones) => {
          this.zones = zones || [];
          this.loadVehicules();
          this.loadGroupes();
          this.loadStats();
        },
        error: () => {
          this.error = 'Impossible de charger vos zones.';
          this.loading = false;
        }
      });
    }
  }

  loadVehicules(): void {
    // Le backend gère le filtre automatiquement (conducteur vs technicien)
    this.equipementService.getMesVehicules().subscribe({
      next: (data) => {
        this.vehicules = data || [];
        this.applyFiltersVehicules();
        this.loading = false;

        // Pour les conducteurs, charger aussi les stats véhicule
        if (this.estConducteur) {
          this.loadStats();
        }
      },
      error: (err) => {
        console.error('Erreur chargement véhicules:', err);
        this.vehicules = [];
        this.loading = false;
      }
    });
  }

  loadGroupes(): void {
    if (this.estConducteur) return; // Les conducteurs ne voient pas les GE
    this.equipementService.getMesGroupes().subscribe({
      next: (data) => {
        this.groupes = data || [];
        this.applyFiltersGroupes();
      },
      error: () => { this.groupes = []; }
    });
  }

  loadStats(): void {
    this.equipementService.getVehiculeStats().subscribe({
      next: (s) => { this.vehiculeStats = s; },
      error: () => {}
    });
    if (!this.estConducteur) {
      this.equipementService.getGEStats().subscribe({
        next: (s) => { this.geStats = s; },
        error: () => {}
      });
    }
  }

  // ── Zone filter (technicien uniquement) ──────────────────────

  onZoneChange(zoneId: number | null): void {
    this.selectedZoneId = zoneId;
    if (this.estConducteur) return;
    this.loadEquipementsParZone();
  }

  loadEquipementsParZone(): void {
    if (!this.selectedZoneId) {
      this.loadVehicules();
      this.loadGroupes();
      return;
    }
    const zid = this.selectedZoneId;

    this.equipementService.getVehiculesByZone(zid).subscribe({
      next: (d) => { this.vehicules = d || []; this.applyFiltersVehicules(); },
      error: () => {}
    });

    this.equipementService.getGroupesByZone(zid).subscribe({
      next: (d) => { this.groupes = d || []; this.applyFiltersGroupes(); },
      error: () => {}
    });
  }

  // ── Search & filter ──────────────────────────────────────────

  onSearch(query: string): void {
    this.searchQuery = query;
    this.applyFiltersVehicules();
    this.applyFiltersGroupes();
  }

  onFilterType(type: string): void {
    this.filterType = type;
    this.applyFiltersVehicules();
    this.applyFiltersGroupes();
  }

  applyFiltersVehicules(): void {
    let list = [...this.vehicules];
    const q = this.searchQuery.toLowerCase().trim();
    if (q) {
      list = list.filter(v =>
        v.matricule?.toLowerCase().includes(q) ||
        v.marqueModele?.toLowerCase().includes(q) ||
        v.nomConducteur?.toLowerCase().includes(q) ||
        v.prenomConducteur?.toLowerCase().includes(q) ||
        v.typeVehicule?.toLowerCase().includes(q) ||
        v.zoneNom?.toLowerCase().includes(q)
      );
    }
    if (this.filterType) {
      list = list.filter(v => v.typeCarburant === this.filterType);
    }
    this.vehiculesFiltres = list;
  }

  applyFiltersGroupes(): void {
    let list = [...this.groupes];
    const q = this.searchQuery.toLowerCase().trim();
    if (q) {
      list = list.filter(g =>
        g.site?.toLowerCase().includes(q) ||
        g.zoneNom?.toLowerCase().includes(q) ||
        g.utilisateurRoc?.toLowerCase().includes(q)
      );
    }
    if (this.filterType) {
      list = list.filter(g => g.typeCarburant === this.filterType);
    }
    this.groupesFiltres = list;
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.filterType = '';
    this.applyFiltersVehicules();
    this.applyFiltersGroupes();
  }

  // ── Detail modal ─────────────────────────────────────────────

  openVehiculeDetail(v: Vehicule): void {
    this.selectedVehicule = v;
    this.selectedGroupe = null;
    this.showDetailModal = true;
  }

  openGroupeDetail(g: GroupeElectrogene): void {
    this.selectedGroupe = g;
    this.selectedVehicule = null;
    this.showDetailModal = true;
  }

  closeModal(): void {
    this.showDetailModal = false;
    this.selectedVehicule = null;
    this.selectedGroupe = null;
  }

  // ── Helpers ──────────────────────────────────────────────────

  logout(): void { this.authService.logout(); }
  navigateTo(r: string): void { this.router.navigate([r]); }

  getInitials(nom: string): string {
    return (nom || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  getZoneColor(index: number): string {
    const colors = ['#E2001A', '#1e3a70', '#00d4aa', '#7c3aed', '#ea580c', '#0891b2'];
    return colors[index % colors.length];
  }

  getZoneColorGradient(index: number): string {
    const colors = [
      'linear-gradient(135deg,#E2001A,#8b0010)',
      'linear-gradient(135deg,#1e3a70,#0c1a3d)',
      'linear-gradient(135deg,#00d4aa,#007a62)',
      'linear-gradient(135deg,#7c3aed,#4c1d95)',
      'linear-gradient(135deg,#ea580c,#9a3412)',
      'linear-gradient(135deg,#0891b2,#164e63)',
    ];
    return colors[index % colors.length];
  }

  getZoneIndex(zoneNom: string | undefined): number {
    if (!zoneNom) return 0;
    const idx = this.zones.findIndex(z => z.nom === zoneNom);
    return idx >= 0 ? idx : 0;
  }

  getCarburantLabel(type: string | undefined): string {
    const labels: Record<string, string> = {
      ESSENCE:            'Essence',
      GASOIL_ORDINAIRE:   'Gasoil Ordinaire',
      GASOIL_SANS_SOUFRE: 'Gasoil Sans Soufre',
      GASOIL_50:          'Gasoil 50',
      SUPER_SANS_PLOMB:   'Super Sans Plomb',
    };
    return labels[type || ''] || type || '—';
  }

  isVisiteDepassee(date: string | undefined): boolean {
    if (!date) return false;
    return new Date(date) < new Date();
  }

  isCarteExpiree(dateExp: string | undefined): boolean {
    if (!dateExp) return false;
    try {
      const [year, month] = dateExp.split('-').map(Number);
      const exp = new Date(year, month - 1, 1);
      const now = new Date();
      return exp < new Date(now.getFullYear(), now.getMonth(), 1);
    } catch { return false; }
  }

  formatDate(d: string | Date | undefined): string {
    if (!d) return '—';
    const date = new Date(d);
    return date.toLocaleDateString('fr-TN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  /** Nom complet conducteur du véhicule sélectionné */
  getNomConducteur(v: Vehicule | null): string {
    if (!v) return '—';
    const p = v.prenomConducteur || '';
    const n = v.nomConducteur || '';
    return (p + ' ' + n).trim() || '—';
  }

  get totalVehicules(): number { return this.vehiculesFiltres.length; }
  get totalGroupes(): number { return this.groupesFiltres.length; }

  /** Message contextuel selon le type d'utilisateur */
  get titreEquipements(): string {
    return this.estConducteur ? 'Mes Véhicules' : 'Mes Équipements';
  }

  get sousTitreEquipements(): string {
    return this.estConducteur
      ? 'Véhicules assignés à votre nom'
      : 'Véhicules et groupes électrogènes de vos zones';
  }

  get emptyVehiculeMessage(): string {
    if (this.searchQuery) return 'Aucun résultat pour votre recherche.';
    return this.estConducteur
      ? 'Aucun véhicule n\'est enregistré à votre nom.'
      : 'Aucun véhicule dans vos zones.';
  }
}