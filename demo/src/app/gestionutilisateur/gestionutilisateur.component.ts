import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { Utilisateur, StatutCompte } from '../models/utilisateur';
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

  showValidationModal = false;
  showEditModal = false;
  loading = false;
  submitting = false;

  zonesLoading = false;
  zonesLoaded = false;

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

  // ----- CHARGEMENT -------------------------------------------------
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

  // ----- MODALS ----------------------------------------------------
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
          // Ouvrir le modal APRÈS que les zones sont chargées
          setTimeout(() => { this.showValidationModal = true; }, 0);
        },
        error: () => {
          this.zonesLoading = false;
          setTimeout(() => { this.showValidationModal = true; }, 0);
        }
      });
    } else {
      // Forcer la détection de changement via setTimeout
      setTimeout(() => { this.showValidationModal = true; }, 0);
    }
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
    this.submitting = false;
  }

  openEditModal(user: Utilisateur): void {
    this.selectedUser = user;
    this.selectedZoneIds = user.zones ? user.zones.map(z => z.id) : [];
    this.submitting = false;

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
    this.submitting = false;
  }

  // ----- ZONES ----------------------------------------------------
  toggleZoneSelection(zoneId: number): void {
    const i = this.selectedZoneIds.indexOf(zoneId);
    if (i > -1) this.selectedZoneIds.splice(i, 1);
    else this.selectedZoneIds.push(zoneId);
  }

  isZoneSelected(zoneId: number): boolean {
    return this.selectedZoneIds.includes(zoneId);
  }

  // ----- ACTIONS --------------------------------------------------
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
      finalize(() => {
        setTimeout(() => { this.submitting = false; }, 500);
      })
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
        console.error('Validation error:', err);
        let msg = err.error?.message || err.message || 'Erreur lors de la validation';
        if (err.name === 'TimeoutError') msg = 'Le serveur ne répond pas. Vérifiez votre connexion.';
        alert('❌ ' + msg);
      }
    });
  }

  modifierUtilisateur(): void {
    if (!this.selectedUser || this.submitting) return;
    this.submitting = true;

    const current = this.selectedUser.zones?.map(z => z.id) || [];
    const toAdd = this.selectedZoneIds.filter(id => !current.includes(id));
    const toRemove = current.filter(id => !this.selectedZoneIds.includes(id));

    if (!toAdd.length && !toRemove.length) {
      alert('Aucune modification détectée');
      this.submitting = false;
      return;
    }

    const uid = this.selectedUser.id;
    const ops = [
      ...toAdd.map(z => this.utilisateurService.ajouterZone(uid, z).pipe(
        timeout(10000),
        finalize(() => {})
      )),
      ...toRemove.map(z => this.utilisateurService.retirerZone(uid, z).pipe(
        timeout(10000),
        finalize(() => {})
      ))
    ];

    forkJoin(ops).pipe(
      timeout(30000),
      finalize(() => { this.submitting = false; })
    ).subscribe({
      next: () => {
        alert('Zones modifiées avec succès');
        this.closeEditModal();
        this.loadTousUtilisateurs();
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

  // ----- NAVIGATION ------------------------------------------------
  navigateToDashboard(): void {
    this.router.navigate(['/admin/dashboardAdmin']);
  }

  // ----- HELPERS UI ------------------------------------------------
  showToggleButton(s: StatutCompte): boolean {
    return s !== StatutCompte.EN_ATTENTE;
  }

  canBeActivated(s: StatutCompte): boolean {
    return s === StatutCompte.DESACTIVE || s === StatutCompte.REFUSE;
  }

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

  /**
   * Retourne le label du rôle/type à afficher.
   * Les conducteurs (TECHNICIEN avec specialite=Conducteur) affichent "Conducteur".
   */
  getRoleLabel(user: Utilisateur): string {
    if (this.isConducteur(user)) return 'Conducteur';
    return user.role as string;
  }

  get nbEnAttente(): number {
    return this.utilisateursEnAttente.length;
  }

  get conducteursEnAttente(): Utilisateur[] {
    return this.utilisateursEnAttente.filter(u => this.isConducteur(u));
  }

  get techniciensEnAttente(): Utilisateur[] {
    return this.utilisateursEnAttente.filter(u => !this.isConducteur(u));
  }
}