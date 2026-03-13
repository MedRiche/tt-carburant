import { Component, OnInit } from '@angular/core';
import { Utilisateur, StatutCompte } from '../../models/utilisateur';
import { Zone } from '../../models/zone';
import { UtilisateurService } from '../../services/utilisateur.service';
import { ZoneService } from '../../services/zone.service';

@Component({
  selector: 'app-user-validation',
  standalone: false,
  templateUrl: './user-validation.component.html',
  styleUrls: ['./user-validation.component.css']
})
export class UserValidationComponent implements OnInit {
  utilisateursEnAttente: Utilisateur[] = [];
  tousUtilisateurs: Utilisateur[] = [];
  toutesZones: Zone[] = [];
  
  selectedUser: Utilisateur | null = null;
  selectedZoneIds: number[] = [];
  
  showValidationModal = false;
  loading = false;
  submitting = false;
  
  activeTab: 'en_attente' | 'tous' = 'en_attente';

  constructor(
    private utilisateurService: UtilisateurService,
    private zoneService: ZoneService
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
        alert('Compte désactivé');
        this.loadTousUtilisateurs();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur');
      }
    });
  }

  supprimerUtilisateur(user: Utilisateur): void {
    if (!confirm(`Êtes-vous sûr de vouloir supprimer définitivement ${user.nom} ?`)) {
      return;
    }

    this.utilisateurService.supprimerUtilisateur(user.id).subscribe({
      next: () => {
        alert('Utilisateur supprimé');
        this.loadUtilisateursEnAttente();
        this.loadTousUtilisateurs();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur');
      }
    });
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