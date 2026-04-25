// src/app/groupe-electrogene/groupe-electrogene-form/groupe-electrogene-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  GroupeElectrogene,
  GroupeElectrogeneRequest,
  TypeCarburantGE,
  TYPE_CARBURANT_LABELS
} from '../../models/groupe-electrogene';
import { Zone } from '../../models/zone';
import { GroupeElectrogeneService } from '../../services/groupe-electrogene.service';

@Component({
  selector: 'app-groupe-electrogene-form',
  standalone: false,
  templateUrl: './groupe-electrogene-form.component.html',
  styleUrls: ['./groupe-electrogene-form.component.css']
})
export class GroupeElectrogeneFormComponent implements OnInit {

  @Input() mode: 'create' | 'edit' = 'create';
  @Input() groupe: GroupeElectrogene | null = null;
  @Input() zones: Zone[] = [];
  @Output() onSave   = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  form: GroupeElectrogeneRequest = {
    site: '',
    typeCarburant: TypeCarburantGE.GASOIL_ORDINAIRE,
    puissanceKVA: undefined,
    tauxConsommationParHeure: undefined,
    consommationTotaleMaxParSemestre: undefined,
    prixCarburant: undefined,
    typeCarte: '',
    numeroCarte: '',
    dateExpiration: '',
    codePIN: '',
    codePUK: '',
    utilisateurRoc: '',
    zoneId: undefined
  };

  typeOptions = Object.values(TypeCarburantGE);
  typeLabels  = TYPE_CARBURANT_LABELS;
  submitting  = false;

  constructor(private geService: GroupeElectrogeneService) {}

  ngOnInit(): void {
    if (this.groupe && this.mode === 'edit') {
      this.form = {
        site:                          this.groupe.site,
        typeCarburant:                 this.groupe.typeCarburant,
        puissanceKVA:                  this.groupe.puissanceKVA,
        tauxConsommationParHeure:      this.groupe.tauxConsommationParHeure,
        consommationTotaleMaxParSemestre: this.groupe.consommationTotaleMaxParSemestre,
        prixCarburant:                 this.groupe.prixCarburant,
        typeCarte:                     this.groupe.typeCarte   || '',
        numeroCarte:                   this.groupe.numeroCarte || '',
        dateExpiration:                this.groupe.dateExpiration || '',
        codePIN:                       this.groupe.codePIN  || '',
        codePUK:                       this.groupe.codePUK  || '',
        utilisateurRoc:                this.groupe.utilisateurRoc || '',
        zoneId:                        this.groupe.zoneId
      };
    }
  }

  getTypeLabel(type: TypeCarburantGE): string {
    return TYPE_CARBURANT_LABELS[type] || type;
  }

  submit(): void {
    if (!this.form.site || !this.form.site.trim()) {
      alert('Le site est obligatoire');
      return;
    }
    if (!this.form.typeCarburant) {
      alert('Le type de carburant est obligatoire');
      return;
    }

    this.submitting = true;

    const payload: GroupeElectrogeneRequest = {
      ...this.form,
      site: this.form.site.trim().toUpperCase()
    };

    const obs = this.mode === 'edit' && this.groupe
      ? this.geService.modifier(this.groupe.site, payload)
      : this.geService.creer(payload);

    obs.subscribe({
      next: () => {
        this.submitting = false;
        this.onSave.emit();
      },
      error: (err) => {
        this.submitting = false;
        const msg = err?.error?.message || err?.message || 'Erreur lors de l\'enregistrement';
        alert(msg);
      }
    });
  }

  cancel(): void {
    this.onCancel.emit();
  }
}