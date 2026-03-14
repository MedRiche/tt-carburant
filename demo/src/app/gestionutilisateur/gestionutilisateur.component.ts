import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Utilisateur, StatutCompte } from '../models/utilisateur';
import { Zone } from '../models/zone';
import { UtilisateurService } from '../services/utilisateur.service';
import { ZoneService } from '../services/zone.service';

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

  // Pour le formulaire d'édition
  editForm = {
    nom: '',
    email: '',
    specialite: '',
    zones: [] as number[]
  };

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

  loadUtilisateursEnAttente(): void {
    this.loading = true;
    this.utilisateurService.getUtilisateursEnAttente().subscribe({
      next: (data) => {
        this.utilisateursEnAttente = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur', err);
        this.loading = false;
      }
    });
  }

  loadTousUtilisateurs(): void {
    this.utilisateurService.getAllUtilisateurs().subscribe({
      next: (data) => {
        this.tousUtilisateurs = data;
      },
      error: (err) => {
        console.error('Erreur', err);
      }
    });
  }

  loadZones(): void {
    this.zoneService.getAllZones().subscribe({
      next: (data) => {
        this.toutesZones = data;
      },
      error: (err) => {
        console.error('Erreur', err);
      }
    });
  }

  openValidationModal(user: Utilisateur): void {
    this.selectedUser = user;
    this.selectedZoneIds = [];
    this.showValidationModal = true;
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
  }

  openEditModal(user: Utilisateur): void {
    this.selectedUser = user;
    this.editForm = {
      nom: user.nom,
      email: user.email,
      specialite: user.specialite || '',
      zones: user.zones ? user.zones.map(z => z.id) : []
    };
    this.selectedZoneIds = this.editForm.zones;
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUser = null;
    this.selectedZoneIds = [];
  }

  toggleZoneSelection(zoneId: number): void {
    const index = this.selectedZoneIds.indexOf(zoneId);
    if (index > -1) {
      this.selectedZoneIds.splice(index, 1);
    } else {
      this.selectedZoneIds.push(zoneId);
    }
  }

  isZoneSelected(zoneId: number): boolean {
    return this.selectedZoneIds.includes(zoneId);
  }

  validerCompte(): void {
    if (!this.selectedUser || this.selectedZoneIds.length === 0) {
      alert('Veuillez sélectionner au moins une zone');
      return;
    }

    this.submitting = true;
    const request = {
      utilisateurId: this.selectedUser.id,
      zoneIds: this.selectedZoneIds
    };

    this.utilisateurService.validerCompteAvecZones(request).subscribe({
      next: (response) => {
        alert(response.message || 'Compte validé avec succès');
        this.closeValidationModal();
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
        this.submitting = false;
      },
      error: (err) => {
        console.error('Erreur', err);
        alert(err.error?.message || 'Erreur lors de la validation');
        this.submitting = false;
      }
    });
  }

  modifierUtilisateur(): void {
    if (!this.selectedUser) return;

    this.submitting = true;

    // D'abord, obtenir les zones actuelles
    const currentZones = this.selectedUser.zones?.map(z => z.id) || [];
    const newZones = this.selectedZoneIds;

    // Zones à ajouter
    const zonesToAdd = newZones.filter(id => !currentZones.includes(id));
    // Zones à retirer
    const zonesToRemove = currentZones.filter(id => !newZones.includes(id));

    // Compteur pour suivre les opérations
    let operations = 0;
    const totalOperations = zonesToAdd.length + zonesToRemove.length;

    if (totalOperations === 0) {
      alert('Aucune modification à apporter');
      this.submitting = false;
      return;
    }

    const checkComplete = () => {
      operations++;
      if (operations === totalOperations) {
        alert('Utilisateur modifié avec succès');
        this.closeEditModal();
        this.loadTousUtilisateurs();
        this.submitting = false;
      }
    };

    // Ajouter les nouvelles zones
    zonesToAdd.forEach(zoneId => {
      this.utilisateurService.ajouterZone(this.selectedUser!.id, zoneId).subscribe({
        next: () => checkComplete(),
        error: (err) => {
          console.error('Erreur ajout zone', err);
          checkComplete();
        }
      });
    });

    // Retirer les zones
    zonesToRemove.forEach(zoneId => {
      this.utilisateurService.retirerZone(this.selectedUser!.id, zoneId).subscribe({
        next: () => checkComplete(),
        error: (err) => {
          console.error('Erreur retrait zone', err);
          checkComplete();
        }
      });
    });
  }

  refuserCompte(user: Utilisateur): void {
    if (!confirm(`Êtes-vous sûr de vouloir refuser le compte de ${user.nom} ?`)) {
      return;
    }

    this.utilisateurService.refuserCompte(user.id).subscribe({
      next: () => {
        alert('Compte refusé');
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur');
      }
    });
  }

  desactiverCompte(user: Utilisateur): void {
    if (!confirm(`Êtes-vous sûr de vouloir désactiver le compte de ${user.nom} ?`)) {
      return;
    }

    this.utilisateurService.desactiverCompte(user.id).subscribe({
      next: () => {
        alert('Compte désactivé avec succès');
        this.loadTousUtilisateurs();
      },
      error: (err) => {
        console.error('Erreur désactivation', err);
        alert(err.error?.message || 'Erreur lors de la désactivation');
      }
    });
  }

  supprimerUtilisateur(user: Utilisateur): void {
    if (!confirm(`⚠️ ATTENTION ⚠️\n\nÊtes-vous sûr de vouloir supprimer définitivement ${user.nom} ?\n\nCette action est IRRÉVERSIBLE !`)) {
      return;
    }

    this.utilisateurService.supprimerUtilisateur(user.id).subscribe({
      next: () => {
        alert('Utilisateur supprimé avec succès');
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
      },
      error: (err) => {
        console.error('Erreur suppression', err);
        alert(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  navigateToDashboard(): void {
    this.router.navigate(['/admin/dashboardAdmin']);
  }

  getStatutClass(statut: StatutCompte): string {
    switch (statut) {
      case StatutCompte.ACTIF:
        return 'statut-actif';
      case StatutCompte.EN_ATTENTE:
        return 'statut-attente';
      case StatutCompte.REFUSE:
        return 'statut-refuse';
      default:
        return '';
    }
  }

  getStatutLabel(statut: StatutCompte): string {
    switch (statut) {
      case StatutCompte.ACTIF:
        return 'Actif';
      case StatutCompte.EN_ATTENTE:
        return 'En attente';
      case StatutCompte.REFUSE:
        return 'Refusé';
      default:
        return statut;
    }
  }
}