import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { Utilisateur, StatutCompte, Role } from '../models/utilisateur';
import { Zone } from '../models/zone';
import { UtilisateurService } from '../services/utilisateur.service';
import { ZoneService } from '../services/zone.service';

@Component({
  selector: 'app-gestionutilisateur',
  standalone: false,
  templateUrl: './gestionutilisateur.component.html',
  styleUrls: ['./gestionutilisateur.component.css']
})
export class GestionutilisateurComponent implements OnInit {

  utilisateursEnAttente: Utilisateur[] = [];
  tousUtilisateurs: Utilisateur[] = [];
  toutesZones: Zone[] = [];

  selectedUser: Utilisateur | null = null;
  selectedZoneIds: number[] = [];

  // ── Champs éditables dans le modal Modifier ──
  editEmail: string = '';
  editType: string = '';       // 'ADMIN' | 'TECHNICIEN' | 'Conducteur'
  editSpecialite: string = '';

  showValidationModal = false;
  showEditModal = false;
  loading = false;
  submitting = false;

  zonesLoading = false;
  zonesLoaded = false;

  activeTab: 'en_attente' | 'tous' = 'en_attente';
  StatutCompte = StatutCompte;
  Role = Role;

  // Options de type disponibles dans le modal Modifier
  readonly typeOptions = [
    { value: 'ADMIN',      label: 'Admin' },
    { value: 'TECHNICIEN', label: 'Technicien' },
    { value: 'Conducteur', label: 'Conducteur' },
  ];

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

  // ── CHARGEMENT ────────────────────────────────────────────────────────────

  loadUtilisateursEnAttente(): void {
    this.loading = true;
    this.utilisateurService.getUtilisateursEnAttente().subscribe({
      next: (data) => { this.utilisateursEnAttente = data; this.loading = false; },
      error: (err) => { console.error(err); this.loading = false; }
    });
  }

  loadTousUtilisateurs(): void {
    this.utilisateurService.getAllUtilisateurs().subscribe({
      next: (data) => { this.tousUtilisateurs = data; },
      error: (err) => console.error(err)
    });
  }

  loadZones(): void {
    this.zonesLoading = true;
    this.zoneService.getAllZones().subscribe({
      next: (data) => {
        this.toutesZones = data;
        this.zonesLoading = false;
        this.zonesLoaded = true;
      },
      error: (err) => {
        console.error('Erreur chargement zones:', err);
        this.zonesLoading = false;
        this.zonesLoaded = false;
      }
    });
  }

  // ── MODAL VALIDATION ────────────────────────────────────────────────────

  openValidationModal(user: Utilisateur): void {
    this.selectedUser = user;
    this.selectedZoneIds = user.zones ? user.zones.map(z => z.id) : [];
    this.submitting = false;

    if (!this.zonesLoaded || this.toutesZones.length === 0) {
      this.zonesLoading = true;
      this.zoneService.getAllZones().subscribe({
        next: (data) => {
          this.toutesZones = data;
          this.zonesLoading = false;
          this.zonesLoaded = true;
          setTimeout(() => { this.showValidationModal = true; }, 0);
        },
        error: () => {
          this.zonesLoading = false;
          setTimeout(() => { this.showValidationModal = true; }, 0);
        }
      });
    } else {
      setTimeout(() => { this.showValidationModal = true; }, 0);
    }
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
    this.submitting = false;
  }

  // ── MODAL MODIFICATION ───────────────────────────────────────────────────

  openEditModal(user: Utilisateur): void {
    this.selectedUser = user;
    this.selectedZoneIds = user.zones ? user.zones.map(z => z.id) : [];
    this.submitting = false;

    // Initialiser les champs éditables
    this.editEmail      = user.email;
    this.editType       = this.isConducteur(user) ? 'Conducteur' : (user.role as string);
    this.editSpecialite = user.specialite || '';

    if (!this.zonesLoaded || this.toutesZones.length === 0) {
      this.zoneService.getAllZones().subscribe({
        next: (data) => {
          this.toutesZones = data;
          this.zonesLoaded = true;
          setTimeout(() => { this.showEditModal = true; }, 0);
        },
        error: () => { setTimeout(() => { this.showEditModal = true; }, 0); }
      });
    } else {
      setTimeout(() => { this.showEditModal = true; }, 0);
    }
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
    this.editEmail = '';
    this.editType = '';
    this.editSpecialite = '';
    this.submitting = false;
  }

  // ── ZONES ────────────────────────────────────────────────────────────────

  toggleZoneSelection(zoneId: number): void {
    const i = this.selectedZoneIds.indexOf(zoneId);
    if (i > -1) this.selectedZoneIds.splice(i, 1);
    else this.selectedZoneIds.push(zoneId);
  }

  isZoneSelected(zoneId: number): boolean {
    return this.selectedZoneIds.includes(zoneId);
  }

  // ── ACTIONS ──────────────────────────────────────────────────────────────

  validerCompte(): void {
    if (!this.selectedUser || this.submitting) return;

    if (this.selectedZoneIds.length === 0) {
      if (!confirm(`Valider le compte de ${this.selectedUser.nom} sans affecter de zone ?`)) return;
    }

    this.submitting = true;

    this.utilisateurService.validerCompteAvecZones({
      utilisateurId: this.selectedUser.id,
      zoneIds: this.selectedZoneIds
    }).pipe(
      timeout(30000),
      finalize(() => { setTimeout(() => { this.submitting = false; }, 500); })
    ).subscribe({
      next: (res) => {
        this.submitting = false;
        alert(res.message || 'Compte validé avec succès');
        this.closeValidationModal();
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
      },
      error: (err) => {
        this.submitting = false;
        let msg = err.error?.message || err.message || 'Erreur lors de la validation';
        if (err.name === 'TimeoutError') msg = 'Le serveur ne répond pas. Vérifiez votre connexion.';
        alert('❌ ' + msg);
      }
    });
  }

  /**
   * Enregistre : zones + email/type/spécialité si modifiés.
   */
  modifierUtilisateur(): void {
    if (!this.selectedUser || this.submitting) return;

    // Validation email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!this.editEmail || !emailRegex.test(this.editEmail)) {
      alert('❌ Veuillez saisir une adresse email valide.');
      return;
    }

    this.submitting = true;

    const uid = this.selectedUser.id;

    // ── 1. Calcul des delta-zones ──────────────────────────────────────────
    const current = this.selectedUser.zones?.map(z => z.id) || [];
    const toAdd    = this.selectedZoneIds.filter(id => !current.includes(id));
    const toRemove = current.filter(id => !this.selectedZoneIds.includes(id));

    const zoneOps = [
      ...toAdd.map(z    => this.utilisateurService.ajouterZone(uid, z).pipe(timeout(10000))),
      ...toRemove.map(z => this.utilisateurService.retirerZone(uid, z).pipe(timeout(10000)))
    ];

    // ── 2. Résolution du rôle + spécialité à envoyer ──────────────────────
    let newRole: string;
    let newSpecialite: string;

    if (this.editType === 'Conducteur') {
      newRole       = 'TECHNICIEN';
      newSpecialite = 'Conducteur';
    } else {
      newRole       = this.editType;           // 'ADMIN' | 'TECHNICIEN'
      newSpecialite = this.editSpecialite || '';
    }

    // Détection de changements sur les champs info
    const emailChanged      = this.editEmail !== this.selectedUser.email;
    const roleChanged       = newRole       !== (this.selectedUser.role as string);
    const specialiteChanged = newSpecialite !== (this.selectedUser.specialite || '');
    const infoChanged       = emailChanged || roleChanged || specialiteChanged;

    if (!toAdd.length && !toRemove.length && !infoChanged) {
      alert('Aucune modification détectée');
      this.submitting = false;
      return;
    }

    // ── 3. Appel backend modifier-info si nécessaire ──────────────────────
    const infoObs = infoChanged
      ? this.utilisateurService.modifierInfo(uid, {
          email:      this.editEmail,
          role:       newRole,
          specialite: newSpecialite
        }).pipe(timeout(10000))
      : null;

    // ── 4. Exécution ──────────────────────────────────────────────────────
    const allOps = infoObs ? [infoObs, ...zoneOps] : zoneOps;

    if (!allOps.length) {
      alert('Aucune modification détectée');
      this.submitting = false;
      return;
    }

    forkJoin(allOps).pipe(
      timeout(30000),
      finalize(() => { this.submitting = false; })
    ).subscribe({
      next: () => {
        alert('Modifications enregistrées avec succès');
        this.closeEditModal();
        this.loadTousUtilisateurs();
        this.loadUtilisateursEnAttente();
      },
      error: (err) => {
        console.error('Modification error:', err);
        alert('❌ ' + (err.error?.message || err.message || 'Erreur lors de la modification'));
        this.submitting = false;
      }
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

  // ── NAVIGATION ────────────────────────────────────────────────────────────

  navigateToDashboard(): void {
    this.router.navigate(['/admin/dashboardAdmin']);
  }

  // ── HELPERS UI ────────────────────────────────────────────────────────────

  showToggleButton(s: StatutCompte): boolean {
    return s !== StatutCompte.EN_ATTENTE;
  }

  canBeActivated(s: StatutCompte): boolean {
    return s === StatutCompte.DESACTIVE || s === StatutCompte.REFUSE;
  }

  /**
   * Un conducteur = rôle TECHNICIEN avec spécialité "Conducteur".
   * On teste aussi la valeur du select en cours d'édition.
   */
  isConducteur(user: Utilisateur): boolean {
    return !!user && user.specialite === 'Conducteur';
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

  getRoleLabel(user: Utilisateur): string {
    if (this.isConducteur(user)) return 'Conducteur';
    return user.role as string;
  }

  /** Indique si le type sélectionné dans le modal est Conducteur */
  get editTypeIsConducteur(): boolean {
    return this.editType === 'Conducteur';
  }

  get nbEnAttente(): number { return this.utilisateursEnAttente.length; }

  get conducteursEnAttente(): Utilisateur[] {
    return this.utilisateursEnAttente.filter(u => this.isConducteur(u));
  }

  get techniciensEnAttente(): Utilisateur[] {
    return this.utilisateursEnAttente.filter(u => !this.isConducteur(u));
  }
}