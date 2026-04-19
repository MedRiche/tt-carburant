// src/app/maintenance/maintenance-form/maintenance-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output, OnChanges } from '@angular/core';
import {
  Maintenance, MaintenanceRequest, DetailMaintenance,
  TypeIntervention, StatutMaintenance, TypeDetailMaintenance,
  TYPE_INTERVENTION_LABELS, STATUT_LABELS
} from '../../models/maintenance';
import { Vehicule } from '../../models/vehicule';
import { MaintenanceService } from '../../services/maintenance.service';

@Component({
  selector: 'app-maintenance-form',
  standalone: false,
  templateUrl: './maintenance-form.component.html',
  styleUrls: ['./maintenance-form.component.css']
})
export class MaintenanceFormComponent implements OnInit, OnChanges {

  @Input() maintenance: Maintenance | null = null;
  @Input() vehicules: Vehicule[] = [];
  @Input() matriculeDefaut = '';
  @Output() onSave   = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  isEditMode = false;
  submitting = false;

  TypeDetailMaintenance = TypeDetailMaintenance;
  typesIntervention = Object.values(TypeIntervention);
  statuts = Object.values(StatutMaintenance);
  typeLabels = TYPE_INTERVENTION_LABELS;
  statutLabels = STATUT_LABELS;

  form: MaintenanceRequest = {
    numeroDossier: '',
    vehiculeMatricule: '',
    dateIntervention: undefined,
    typeIntervention: TypeIntervention.CORRECTIVE,
    statut: StatutMaintenance.EN_COURS,
    description: '',
    details: []
  };

  // Nouveau détail en cours de saisie
  newDetail: Partial<DetailMaintenance> = {
    type: TypeDetailMaintenance.MAIN_D_OEUVRE,
    marque: '',
    numero: '',
    numeroPiece: '',
    designation: '',
    quantite: 1,
    montantUnitaire: 0
  };

  constructor(private maintenanceService: MaintenanceService) {}

  ngOnInit(): void { this.init(); }
  ngOnChanges(): void { this.init(); }

  private init(): void {
    this.isEditMode = !!this.maintenance;
    if (this.maintenance) {
      this.form = {
        numeroDossier: this.maintenance.numeroDossier,
        vehiculeMatricule: this.maintenance.vehiculeMatricule,
        dateIntervention: this.maintenance.dateIntervention,
        typeIntervention: this.maintenance.typeIntervention,
        statut: this.maintenance.statut,
        description: this.maintenance.description || '',
        // Deep copy des détails pour permettre l'édition inline sans affecter l'original
        details: JSON.parse(JSON.stringify(this.maintenance.details || []))
      };
    } else {
      this.form = {
        numeroDossier: '',
        vehiculeMatricule: this.matriculeDefaut || '',
        dateIntervention: new Date().toISOString().split('T')[0],
        typeIntervention: TypeIntervention.CORRECTIVE,
        statut: StatutMaintenance.EN_COURS,
        description: '',
        details: []
      };
    }
    this.resetNewDetail();
  }

  resetNewDetail(): void {
    this.newDetail = {
      type: TypeDetailMaintenance.MAIN_D_OEUVRE,
      marque: '',
      numero: '',
      numeroPiece: '',
      designation: '',
      quantite: 1,
      montantUnitaire: 0
    };
  }

  // ── Getters pour filtrer par type ─────────────────────────

  get totalHtva(): number {
    return (this.form.details || []).reduce((s, d) => s + ((d.quantite || 0) * (d.montantUnitaire || 0)), 0);
  }

  get mainDoeuvreDetails(): DetailMaintenance[] {
    return (this.form.details || []).filter(d => d.type === TypeDetailMaintenance.MAIN_D_OEUVRE);
  }

  get piecesDetails(): DetailMaintenance[] {
    return (this.form.details || []).filter(d => d.type === TypeDetailMaintenance.PIECE);
  }

  get newDetailTotal(): number {
    return (this.newDetail.quantite || 0) * (this.newDetail.montantUnitaire || 0);
  }

  // ── Édition inline ────────────────────────────────────────

  /**
   * Recalcule le totalHtva d'un détail après modification inline.
   * Appelé par (ngModelChange) sur quantite ou montantUnitaire.
   */
  recalcDetail(d: DetailMaintenance): void {
    d.totalHtva = Math.round((d.quantite || 0) * (d.montantUnitaire || 0) * 1000) / 1000;
  }

  /**
   * Retourne l'index global d'un détail dans form.details
   * (car mainDoeuvreDetails / piecesDetails sont des sous-ensembles filtrés,
   *  mais l'objet est le même par référence → indexOf fonctionne).
   */
  getGlobalIndex(d: DetailMaintenance): number {
    return (this.form.details || []).indexOf(d);
  }

  // ── Ajout / Suppression ───────────────────────────────────

  ajouterDetail(): void {
    if (!this.newDetail.designation?.trim()) {
      alert('La désignation est obligatoire');
      return;
    }
    const d: DetailMaintenance = {
      type: this.newDetail.type || TypeDetailMaintenance.MAIN_D_OEUVRE,
      numeroDossier: this.form.numeroDossier,
      marque: this.newDetail.marque || undefined,
      numero: this.newDetail.type === TypeDetailMaintenance.MAIN_D_OEUVRE ? (this.newDetail.numero || undefined) : undefined,
      numeroPiece: this.newDetail.type === TypeDetailMaintenance.PIECE ? (this.newDetail.numeroPiece || undefined) : undefined,
      designation: this.newDetail.designation!,
      quantite: this.newDetail.quantite || 1,
      montantUnitaire: this.newDetail.montantUnitaire || 0,
      totalHtva: this.newDetailTotal
    };
    if (!this.form.details) this.form.details = [];
    this.form.details.push(d);
    this.resetNewDetail();
  }

  supprimerDetail(index: number): void {
    this.form.details?.splice(index, 1);
  }

  // ── Soumission ────────────────────────────────────────────

  submit(): void {
    if (!this.form.vehiculeMatricule || !this.form.numeroDossier) {
      alert('Matricule et N° dossier sont obligatoires');
      return;
    }
    this.submitting = true;

    // Recalculer totalHtva pour chaque détail avant envoi
    const req: MaintenanceRequest = {
      ...this.form,
      details: (this.form.details || []).map(d => ({
        ...d,
        totalHtva: Math.round((d.quantite || 0) * (d.montantUnitaire || 0) * 1000) / 1000
      }))
    };

    const obs = this.isEditMode && this.maintenance?.id
      ? this.maintenanceService.modifier(this.maintenance.id, req)
      : this.maintenanceService.creer(req);

    obs.subscribe({
      next: () => { this.submitting = false; this.onSave.emit(); },
      error: (err: any) => {
        alert(err.error?.message || 'Erreur lors de la sauvegarde');
        this.submitting = false;
      }
    });
  }

  cancel(): void { this.onCancel.emit(); }

  fmt3(v: number): string {
    return (v || 0).toLocaleString('fr-TN', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  }
}