// src/app/carburant/carburant-form/carburant-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output, OnChanges } from '@angular/core';
import { CarburantVehicule, MOIS_LABELS } from '../../models/carburant-vehicule';
import { Vehicule } from '../../models/vehicule';
import { CarburantVehiculeService } from '../../services/carburant-vehicule.service';

@Component({
  selector: 'app-carburant-form',
  standalone: false,
  templateUrl: './carburant-form.component.html',
  styleUrls: ['./carburant-form.component.css']
})
export class CarburantFormComponent implements OnInit, OnChanges {

  @Input() enregistrement: CarburantVehicule | null = null;
  @Input() vehicules: Vehicule[] = [];
  @Input() anneeDefaut: number = new Date().getFullYear();
  @Input() moisDefaut: number  = new Date().getMonth() + 1;

  @Output() onSave   = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  isEditMode = false;
  submitting = false;
  prefillLoading = false;
  moisLabels = MOIS_LABELS;

  annees      = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i);
  moisOptions = Array.from({ length: 12 }, (_, i) => i + 1);

  selectedVehicule: Vehicule | null = null;

  // Alerte budget
  showBudgetAlert = false;
  budgetAlertMessage = '';

  form: Partial<CarburantVehicule> & {
    vehiculeMatricule: string; annee: number; mois: number;
    indexDemarrageMois: number; indexFinMois: number;
    montantRestantMoisPrecedent: number; ravitaillementMoisPrecedent: number;
    ravitaillementMois: number;
  } = {
    vehiculeMatricule: '', annee: this.anneeDefaut, mois: this.moisDefaut,
    indexDemarrageMois: 0, indexFinMois: 0,
    montantRestantMoisPrecedent: 0, ravitaillementMoisPrecedent: 0, ravitaillementMois: 0
  };

  constructor(private carburantService: CarburantVehiculeService) {}

  ngOnInit(): void  { this.init(); }
  ngOnChanges(): void { this.init(); }

  private init(): void {
    this.isEditMode = !!this.enregistrement;
    this.showBudgetAlert = false;

    if (this.enregistrement) {
      this.form = {
        vehiculeMatricule: this.enregistrement.vehiculeMatricule,
        annee: this.enregistrement.annee, mois: this.enregistrement.mois,
        indexDemarrageMois: this.enregistrement.indexDemarrageMois,
        indexFinMois: this.enregistrement.indexFinMois,
        montantRestantMoisPrecedent: this.enregistrement.montantRestantMoisPrecedent,
        ravitaillementMoisPrecedent: this.enregistrement.ravitaillementMoisPrecedent,
        ravitaillementMois: this.enregistrement.ravitaillementMois
      };
      this.onVehiculeChange();
    } else {
      this.form = {
        vehiculeMatricule: '', annee: this.anneeDefaut, mois: this.moisDefaut,
        indexDemarrageMois: 0, indexFinMois: 0,
        montantRestantMoisPrecedent: 0, ravitaillementMoisPrecedent: 0, ravitaillementMois: 0
      };
      this.selectedVehicule = null;
    }
  }

  onVehiculeChange(): void {
    this.selectedVehicule = this.vehicules.find(v => v.matricule === this.form.vehiculeMatricule) || null;
    // Déclencher le pré-remplissage si on est en mode création
    if (!this.isEditMode && this.selectedVehicule) {
      this.chargerPrefill();
    }
  }

  onPeriodeChange(): void {
    // Recharger le pré-remplissage quand la période change
    if (!this.isEditMode && this.form.vehiculeMatricule) {
      this.chargerPrefill();
    }
  }

  // ── NOUVEAU : pré-remplissage règles 6 & 7 ───────────────────

  chargerPrefill(): void {
    if (!this.form.vehiculeMatricule || !this.form.annee || !this.form.mois) return;
    this.prefillLoading = true;

    this.carburantService.getPrefill(
      this.form.vehiculeMatricule, this.form.annee, this.form.mois
    ).subscribe({
      next: (prefill) => {
        // Règle 6 : index démarrage = index fin mois précédent
        this.form.indexDemarrageMois = prefill.indexDemarrageMois;
        // Règle 7 : montant restant = montant restant réservoir fin mois précédent
        this.form.montantRestantMoisPrecedent = prefill.montantRestantMoisPrecedent;
        // Ravitaillement précédent
        this.form.ravitaillementMoisPrecedent = prefill.ravitaillementMoisPrecedent;
        this.prefillLoading = false;
      },
      error: () => { this.prefillLoading = false; }
    });
  }

  // ── Calculs temps réel ────────────────────────────────────────

  get prix(): number { return this.selectedVehicule?.prixCarburant || 0; }
  get cout(): number { return this.selectedVehicule?.coutDuMois    || 0; }

  get totalRavitaillementLitres(): number {
    if (!this.prix) return 0;
    return (+this.form.ravitaillementMoisPrecedent + +this.form.montantRestantMoisPrecedent) / this.prix;
  }

  get quantiteRestanteReservoir(): number {
    if (!this.prix) return 0;
    return +this.form.montantRestantMoisPrecedent / this.prix;
  }

  get distanceParcourue(): number {
    return +this.form.indexFinMois - +this.form.indexDemarrageMois;
  }

  get pourcentageConsommation(): number {
    if (!this.distanceParcourue) return 0;
    return (this.totalRavitaillementLitres - this.quantiteRestanteReservoir) / this.distanceParcourue;
  }

  get carburantDemandeDinars(): number {
    return this.cout - +this.form.montantRestantMoisPrecedent;
  }

  // ── NOUVEAU : alerte budget en temps réel ────────────────────

  get consommationReelleDT(): number {
    return (this.totalRavitaillementLitres - this.quantiteRestanteReservoir) * this.prix;
  }

  get budgetDepasse(): boolean {
    return this.cout > 0 && this.consommationReelleDT > this.cout;
  }

  get depassementMontant(): number {
    return Math.max(0, this.consommationReelleDT - this.cout);
  }

  // ── Soumission ────────────────────────────────────────────────

  submit(): void {
    if (!this.form.vehiculeMatricule) return;
    this.submitting = true;
    this.showBudgetAlert = false;

    const obs = this.isEditMode && this.enregistrement?.id
      ? this.carburantService.modifier(this.enregistrement.id, { ...this.form })
      : this.carburantService.saisir({ ...this.form });

    obs.subscribe({
      next: (res: any) => {
        this.submitting = false;
        // Vérifier alerte budget dans la réponse
        if (res?.alert || res?.data?.budgetDepasse) {
          this.showBudgetAlert = true;
          this.budgetAlertMessage = res.message || '';
          // Laisser 3s puis fermer
          setTimeout(() => {
            this.showBudgetAlert = false;
            this.onSave.emit();
          }, 3000);
        } else {
          this.onSave.emit();
        }
      },
      error: (err: any) => {
        alert(err.error?.message || 'Erreur lors de la sauvegarde');
        this.submitting = false;
      }
    });
  }

  cancel(): void { this.onCancel.emit(); }

  fmt(n: number, dec = 3): string {
    return (n || 0).toLocaleString('fr-TN', {
      minimumFractionDigits: dec, maximumFractionDigits: dec
    });
  }
}