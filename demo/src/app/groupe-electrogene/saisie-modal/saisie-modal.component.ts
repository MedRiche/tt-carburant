// src/app/groupe-electrogene/saisie-carburant-modal/saisie-carburant-modal.component.ts
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CarburantGeService } from '../../services/carburant-ge.service';
import { GroupeElectrogene } from '../../models/groupe-electrogene';
import { GestionCarburantGE, GestionCarburantGERequest, Semestre } from '../../models/gestion-carburant-ge';

@Component({
  selector: 'app-saisie-carburant-modal',
  standalone: false,
  templateUrl: './saisie-modal.component.html',
  styleUrls: ['./saisie-modal.component.css']
})
export class SaisieCarburantModalComponent implements OnInit {

  /** Groupe pré-sélectionné (depuis la liste ou la page gestion) */
  @Input() groupe: GroupeElectrogene | null = null;
  /** Saisie existante (mode édition) */
  @Input() saisie: GestionCarburantGE | null = null;
  /** Liste de tous les groupes (pour le select si groupe non pré-sélectionné) */
  @Input() groupes: GroupeElectrogene[] = [];
  /** Filtre de période courant (pour pré-remplir) */
  @Input() defaultAnnee: number = new Date().getFullYear();
  @Input() defaultSemestre: 'PREMIER' | 'DEUXIEME' = 'PREMIER';

  @Output() onSave   = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  anneeOptions = [2024, 2025, 2026, 2027, 2028];
  submitting   = false;
  errorMessage = '';

  form: GestionCarburantGERequest = {
    site: '',
    annee: new Date().getFullYear(),
    semestre: Semestre.PREMIER,
    indexHeureSemestrePrecedent: undefined,
    montantCarburantRestantReservoirPrecedent: undefined,
    ravitaillementSemestrePrecedentDinars: undefined,
    montantRestantAgilisFinSemestre: undefined,
    indexFinSemestre: undefined
  };

  constructor(private carbService: CarburantGeService) {}

  ngOnInit(): void {
    if (this.saisie) {
      // Mode édition : pré-remplir le formulaire
      this.form = {
        site:     this.saisie.site,
        annee:    this.saisie.annee,
        semestre: this.saisie.semestre,
        indexHeureSemestrePrecedent:                this.saisie.indexHeureSemestrePrecedent,
        montantCarburantRestantReservoirPrecedent:  this.saisie.montantCarburantRestantReservoirPrecedent,
        ravitaillementSemestrePrecedentDinars:      this.saisie.ravitaillementSemestrePrecedentDinars,
        montantRestantAgilisFinSemestre:            this.saisie.montantRestantAgilisFinSemestre,
        indexFinSemestre:                           this.saisie.indexFinSemestre
      };
    } else {
      // Mode création : pré-remplir site et période
      this.form.site     = this.groupe?.site ?? '';
      this.form.annee    = this.defaultAnnee;
      this.form.semestre = this.defaultSemestre as Semestre;
    }
  }

  // ── Calculs en temps réel (aperçu) ──────────────────────────────

  private v(n: number | undefined | null): number {
    return n ?? 0;
  }

  calcHeures(): number {
    return this.v(this.form.indexFinSemestre) - this.v(this.form.indexHeureSemestrePrecedent);
  }

  calcTotalLitres(): number {
    const prix = this.groupe?.prixCarburant ?? 0;
    if (!prix) return 0;
    return (this.v(this.form.montantCarburantRestantReservoirPrecedent) +
            this.v(this.form.ravitaillementSemestrePrecedentDinars)) / prix;
  }

  calcQteRestante(): number {
    const prix = this.groupe?.prixCarburant ?? 0;
    if (!prix) return 0;
    return this.v(this.form.montantRestantAgilisFinSemestre) / prix;
  }

  calcPct(): number {
    const heures = this.calcHeures();
    if (!heures) return 0;
    return (this.v(this.form.montantCarburantRestantReservoirPrecedent) +
            this.v(this.form.ravitaillementSemestrePrecedentDinars) -
            this.v(this.form.montantRestantAgilisFinSemestre)) * 100 / heures;
  }

  // ── Soumission ───────────────────────────────────────────────────

  submit(): void {
    this.errorMessage = '';

    const site = this.groupe?.site ?? this.form.site;
    if (!site) {
      this.errorMessage = 'Veuillez sélectionner un site.';
      return;
    }

    const payload: GestionCarburantGERequest = { ...this.form, site };
    this.submitting = true;

    const obs = this.saisie?.id
      ? this.carbService.modifierSaisie(this.saisie.id, payload)
      : this.carbService.saisir(payload);

    obs.subscribe({
      next: () => {
        this.submitting = false;
        this.onSave.emit();
      },
      error: (err) => {
        this.submitting   = false;
        this.errorMessage = err?.error?.message ?? err?.message ?? 'Erreur lors de l\'enregistrement';
      }
    });
  }

  cancel(): void {
    this.onCancel.emit();
  }
}