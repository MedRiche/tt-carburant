import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Utilisateur, StatutCompte } from '../models/utilisateur';
import { Zone } from '../models/zone';
import { UtilisateurService } from '../services/utilisateur.service';
import { ZoneService } from '../services/zone.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-gestionutilisateur',
  standalone: false,
  templateUrl: './gestionutilisateur.component.html',
  styleUrl: './gestionutilisateur.component.css'
})
export class GestionutilisateurComponent implements OnInit {

  utilisateursEnAttente: Utilisateur[] = [];
  tousUtilisateurs: Utilisateur[] = [];
  toutesZones: Zone[] = [];

  selectedUser: Utilisateur | null = null;
  selectedZoneIds: number[] = [];

  showValidationModal = false;
  showEditModal = false;
  loading = false;
  submitting = false;

  activeTab: 'en_attente' | 'tous' = 'en_attente';
  StatutCompte = StatutCompte;

  constructor(
    private utilisateurService: UtilisateurService,
    private zoneService: ZoneService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUtilisateursEnAttente();
    this.loadTousUtilisateurs();
    this.loadZones();
  }

  // ── Chargement ─────────────────────────────────────────────────────────────

  loadUtilisateursEnAttente(): void {
    this.loading = true;
    this.utilisateurService.getUtilisateursEnAttente().subscribe({
      next: (data) => { this.utilisateursEnAttente = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadTousUtilisateurs(): void {
    this.utilisateurService.getAllUtilisateurs().subscribe({
      next: (data) => { this.tousUtilisateurs = data; },
      error: (err) => console.error(err)
    });
  }

  loadZones(): void {
    this.zoneService.getAllZones().subscribe({
      next: (data) => { this.toutesZones = data; },
      error: (err) => console.error(err)
    });
  }

  // ── Modals ─────────────────────────────────────────────────────────────────

  openValidationModal(user: Utilisateur): void {
    this.selectedUser = user;
    // Pré-sélectionner les zones déjà affectées si modification
    this.selectedZoneIds = user.zones ? user.zones.map(z => z.id) : [];
    this.showValidationModal = true;
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
  }

  openEditModal(user: Utilisateur): void {
    this.selectedUser = user;
    this.selectedZoneIds = user.zones ? user.zones.map(z => z.id) : [];
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
  }

  // ── Sélection des zones ────────────────────────────────────────────────────

  toggleZoneSelection(zoneId: number): void {
    const i = this.selectedZoneIds.indexOf(zoneId);
    if (i > -1) this.selectedZoneIds.splice(i, 1);
    else this.selectedZoneIds.push(zoneId);
  }

  isZoneSelected(zoneId: number): boolean {
    return this.selectedZoneIds.includes(zoneId);
  }

  // ── Actions ────────────────────────────────────────────────────────────────

  validerCompte(): void {
    if (!this.selectedUser || this.selectedZoneIds.length === 0) return;
    this.submitting = true;
    this.utilisateurService.validerCompteAvecZones({
      utilisateurId: this.selectedUser.id,
      zoneIds: this.selectedZoneIds
    }).subscribe({
      next: (res) => {
        alert(res.message || 'Compte validé avec succès');
        this.closeValidationModal();
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
        this.submitting = false;
      },
      error: (err) => { alert(err.error?.message || 'Erreur'); this.submitting = false; }
    });
  }

  modifierUtilisateur(): void {
    if (!this.selectedUser) return;
    this.submitting = true;

    const current  = this.selectedUser.zones?.map(z => z.id) || [];
    const toAdd    = this.selectedZoneIds.filter(id => !current.includes(id));
    const toRemove = current.filter(id => !this.selectedZoneIds.includes(id));

    if (!toAdd.length && !toRemove.length) {
      alert('Aucune modification détectée');
      this.submitting = false;
      return;
    }

    const uid = this.selectedUser.id;
    const ops = [
      ...toAdd.map(z    => this.utilisateurService.ajouterZone(uid, z).pipe(catchError(() => of(null)))),
      ...toRemove.map(z => this.utilisateurService.retirerZone(uid, z).pipe(catchError(() => of(null))))
    ];

    forkJoin(ops).subscribe({
      next: () => {
        alert('Zones modifiées avec succès');
        this.closeEditModal();
        this.loadTousUtilisateurs();
        this.submitting = false;
      },
      error: () => { alert('Erreur lors de la modification'); this.submitting = false; }
    });
  }

  refuserCompte(user: Utilisateur): void {
    if (!confirm(`Refuser le compte de ${user.nom} ?`)) return;
    this.utilisateurService.refuserCompte(user.id).subscribe({
      next: () => {
        alert('Compte refusé');
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
      },
      error: (err) => alert(err.error?.message || 'Erreur')
    });
  }

  toggleActivation(user: Utilisateur): void {
    const isActive = user.statutCompte === StatutCompte.ACTIF;
    if (!confirm(`${isActive ? 'Désactiver' : 'Activer'} le compte de ${user.nom} ?`)) return;
    this.utilisateurService.toggleActivation(user.id).subscribe({
      next: (res) => { alert(res.message || 'Succès'); this.loadTousUtilisateurs(); },
      error: (err) => alert(err.error?.message || 'Erreur')
    });
  }

  supprimerUtilisateur(user: Utilisateur): void {
    if (!confirm(`Supprimer définitivement ${user.nom} ? Cette action est IRRÉVERSIBLE !`)) return;
    this.utilisateurService.supprimerUtilisateur(user.id).subscribe({
      next: () => {
        alert('Utilisateur supprimé');
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
      },
      error: (err) => alert(err.error?.message || 'Erreur')
    });
  }

  // ── Navigation ─────────────────────────────────────────────────────────────

  navigateToDashboard(): void { this.router.navigate(['/admin/dashboardAdmin']); }

  // ── Helpers UI ─────────────────────────────────────────────────────────────

  showToggleButton(s: StatutCompte): boolean { return s !== StatutCompte.EN_ATTENTE; }
  canBeActivated(s: StatutCompte): boolean   { return s === StatutCompte.DESACTIVE || s === StatutCompte.REFUSE; }

  /** Indique si le compte est un conducteur importé depuis Excel */
  isConducteur(user: Utilisateur): boolean {
    return user.specialite === 'Conducteur';
  }

  getInitials(nom: string): string {
    return (nom || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  getStatutClass(s: StatutCompte): string {
    const map: Record<string, string> = {
      ACTIF: 'chip-actif', EN_ATTENTE: 'chip-attente',
      REFUSE: 'chip-refuse', DESACTIVE: 'chip-desactive'
    };
    return map[s] || '';
  }

  getStatutLabel(s: StatutCompte): string {
    const map: Record<string, string> = {
      ACTIF: 'Actif', EN_ATTENTE: 'En attente',
      REFUSE: 'Refusé', DESACTIVE: 'Désactivé'
    };
    return map[s] || s;
  }

  /** Compteur d'utilisateurs en attente (pour le badge sidebar) */
  get nbEnAttente(): number { return this.utilisateursEnAttente.length; }

  /** Conducteurs en attente de validation (importés depuis Excel) */
  get conducteursEnAttente(): Utilisateur[] {
    return this.utilisateursEnAttente.filter(u => this.isConducteur(u));
  }

  /** Techniciens normaux en attente (inscrits via le formulaire) */
  get techniciensEnAttente(): Utilisateur[] {
    return this.utilisateursEnAttente.filter(u => !this.isConducteur(u));
  }
}