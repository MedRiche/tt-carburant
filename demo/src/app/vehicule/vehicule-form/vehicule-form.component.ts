// src/app/vehicules/vehicule-form/vehicule-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Vehicule, VehiculeRequest, TypeCarburant } from '../../models/vehicule';
import { Zone } from '../../models/zone';
import { VehiculeService } from '../../services/vehicule.service';

@Component({
  selector: 'app-vehicule-form',
  standalone: false,
  templateUrl: './vehicule-form.component.html',
  styleUrls: ['./vehicule-form.component.css']
})
export class VehiculeFormComponent implements OnInit {
  @Input() vehicule: Vehicule | null = null;
  @Input() zones: Zone[] = [];
  @Output() onSave   = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = false;
  submitting = false;

  TypeCarburant = TypeCarburant;
  typeCarburantOptions = Object.values(TypeCarburant);

  typesVehicule = ['Voiture', 'Camionnette', 'Camion', 'Moto', 'Bus', 'Véhicule léger', 'Autre'];

  labelCarburant: Record<string, string> = {
    ESSENCE: 'Essence',
    GASOIL_ORDINAIRE: 'Gasoil Ordinaire',
    GASOIL_SANS_SOUFRE: 'Gasoil Sans Soufre',
    GASOIL_50: 'Gasoil 50',
    SUPER_SANS_PLOMB: 'Super Sans Plomb'
  };

  constructor(
    private fb: FormBuilder,
    private vehiculeService: VehiculeService
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.vehicule;
    this.form = this.fb.group({
      matricule:              [{ value: this.vehicule?.matricule || '', disabled: this.isEditMode },
                               [Validators.required]],
      dateMiseService:        [this.vehicule?.dateMiseService || ''],
      marqueModele:           [this.vehicule?.marqueModele || '', Validators.required],
      typeVehicule:           [this.vehicule?.typeVehicule || '', Validators.required],
      subdivision:            [this.vehicule?.subdivision || ''],
      centre:                 [this.vehicule?.centre || ''],
      residenceService:       [this.vehicule?.residenceService || ''],
      nomConducteur:          [this.vehicule?.nomConducteur || ''],
      prenomConducteur:       [this.vehicule?.prenomConducteur || ''],
      typeCarburant:          [this.vehicule?.typeCarburant || '', Validators.required],
      prixCarburant:          [this.vehicule?.prixCarburant || 0, [Validators.required, Validators.min(0)]],
      indexVidange:           [this.vehicule?.indexVidange || 0],
      visiteTechnique:        [this.vehicule?.visiteTechnique || ''],
      indexPneumatique:       [this.vehicule?.indexPneumatique || 0],
      kilometrageTotal:       [this.vehicule?.kilometrageTotal || 0],
      consommationDinarsCumul:[this.vehicule?.consommationDinarsCumul || 0],
      consommationLitresCumul:[this.vehicule?.consommationLitresCumul || 0],
      coutDuMois:             [this.vehicule?.coutDuMois || 0],
      croxChaine:             [this.vehicule?.croxChaine || 0],
      indexBatterie:          [this.vehicule?.indexBatterie || 0],
      zoneId:                 [this.vehicule?.zoneId || null]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;

    const raw = this.form.getRawValue();
    const req: VehiculeRequest = {
      matricule:              raw.matricule,
      dateMiseService:        raw.dateMiseService || undefined,
      marqueModele:           raw.marqueModele,
      typeVehicule:           raw.typeVehicule,
      subdivision:            raw.subdivision || undefined,
      centre:                 raw.centre || undefined,
      residenceService:       raw.residenceService || undefined,
      nomConducteur:          raw.nomConducteur || undefined,
      prenomConducteur:       raw.prenomConducteur || undefined,
      typeCarburant:          raw.typeCarburant,
      prixCarburant:          +raw.prixCarburant,
      indexVidange:           +raw.indexVidange || 0,
      visiteTechnique:        raw.visiteTechnique || undefined,
      indexPneumatique:       +raw.indexPneumatique || 0,
      kilometrageTotal:       +raw.kilometrageTotal || 0,
      consommationDinarsCumul:+raw.consommationDinarsCumul || 0,
      consommationLitresCumul:+raw.consommationLitresCumul || 0,
      coutDuMois:             +raw.coutDuMois || 0,
      croxChaine:             +raw.croxChaine || 0,
      indexBatterie:          +raw.indexBatterie || 0,
      zoneId:                 raw.zoneId ? +raw.zoneId : undefined
    };

    const obs = this.isEditMode
      ? this.vehiculeService.modifierVehicule(this.vehicule!.matricule, req)
      : this.vehiculeService.creerVehicule(req);

    obs.subscribe({
      next: (res) => {
        alert(res.message || 'Véhicule sauvegardé');
        this.submitting = false;
        this.onSave.emit();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur');
        this.submitting = false;
      }
    });
  }

  cancel(): void { this.onCancel.emit(); }

  getError(field: string): string {
    const c = this.form.get(field);
    if (!c || !c.touched) return '';
    if (c.hasError('required')) return 'Champ obligatoire';
    if (c.hasError('min'))      return 'Valeur doit être positive';
    return '';
  }
}