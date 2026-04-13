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

  submit(): void {
    if (!this.form.vehiculeMatricule || !this.form.numeroDossier) {
      alert('Matricule et N° dossier sont obligatoires');
      return;
    }
    this.submitting = true;

    // Recalculate totalHtva for each detail
    const req: MaintenanceRequest = {
      ...this.form,
      details: (this.form.details || []).map(d => ({
        ...d,
        totalHtva: d.quantite * d.montantUnitaire
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