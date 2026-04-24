import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { GroupeElectrogene, GroupeElectrogeneRequest, TypeCarburantGE } from '../../models/groupe-electrogene';
import { Zone } from '../../models/zone';
import { GroupeElectrogeneService } from '../../services/groupe-electrogene.service';

@Component({
  selector: 'app-groupe-electrogene-form',
  standalone: false,
  templateUrl: './groupe-electrogene-form.component.html',
  styleUrls: ['./groupe-electrogene-form.component.css']
})
export class GroupeElectrogeneFormComponent implements OnInit {

  @Input() mode: 'create' | 'edit' | 'fuel' = 'create';
  @Input() groupe: GroupeElectrogene | null = null;
  @Input() zones: Zone[] = [];
  @Output() onSave = new EventEmitter<void>();
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
  submitting = false;

  constructor(private geService: GroupeElectrogeneService) {}

  ngOnInit(): void {
    if (this.groupe) {
      this.form = {
        site: this.groupe.site,
        typeCarburant: this.groupe.typeCarburant,
        puissanceKVA: this.groupe.puissanceKVA,
        tauxConsommationParHeure: this.groupe.tauxConsommationParHeure,
        consommationTotaleMaxParSemestre: this.groupe.consommationTotaleMaxParSemestre,
        prixCarburant: this.groupe.prixCarburant,
        typeCarte: this.groupe.typeCarte,
        numeroCarte: this.groupe.numeroCarte,
        dateExpiration: this.groupe.dateExpiration,
        codePIN: this.groupe.codePIN,
        codePUK: this.groupe.codePUK,
        utilisateurRoc: this.groupe.utilisateurRoc,
        zoneId: this.groupe.zoneId
      };
    }
  }

  submit(): void {
    if (!this.form.site || !this.form.typeCarburant) {
      alert('Site et type carburant obligatoires');
      return;
    }
    this.submitting = true;
    const obs = this.mode === 'edit' && this.groupe
      ? this.geService.modifier(this.groupe.site, this.form)
      : this.geService.creer(this.form);
    obs.subscribe({
      next: () => { this.submitting = false; this.onSave.emit(); },
      error: (err) => { alert(err.error?.message || 'Erreur'); this.submitting = false; }
    });
  }

  cancel(): void { this.onCancel.emit(); }

  getTypeLabel(type: string): string {
    const map: Record<string, string> = {
      GASOIL_ORDINAIRE: 'Gasoil Ordinaire',
      GASOIL_SANS_SOUFRE: 'Gasoil Sans Soufre',
      SUPER_SANS_PLOMB: 'Super Sans Plomb',
      ESSENCE: 'Essence'
    };
    return map[type] || type;
  }
}