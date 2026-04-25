// src/app/groupe-electrogene/gestion-carburant-ge-form/gestion-carburant-ge-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { GroupeElectrogene } from '../../models/groupe-electrogene';
import { GestionCarburantGE, GestionCarburantGERequest, Semestre, SEMESTRE_LABELS } from '../../models/gestion-carburant-ge';
import { GroupeElectrogeneService } from '../../services/groupe-electrogene.service';
import { CarburantGeService } from '../../services/carburant-ge.service';

@Component({
  selector: 'app-gestion-carburant-ge-form',
  standalone: false,  
  templateUrl: './gestion-carburant-ge-form.component.html',
  styleUrls: ['./gestion-carburant-ge-form.component.css']
})
export class GestionCarburantGEFormComponent implements OnInit {

  @Input() groupe: GroupeElectrogene | null = null;
  @Input() saisie: GestionCarburantGE | null = null;
  @Output() onSave = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  form: GestionCarburantGERequest = {
    site: '',
    annee: new Date().getFullYear(),
    semestre: Semestre.PREMIER,
    indexHeureSemestrePrecedent: 0,
    montantCarburantRestantReservoirPrecedent: 0,
    ravitaillementSemestrePrecedentDinars: 0,
    montantRestantAgilisFinSemestre: 0,
    indexFinSemestre: 0
  };

  semestreOptions = Object.values(Semestre);
  semestreLabels = SEMESTRE_LABELS;
  anneeOptions = [2024, 2025, 2026, 2027, 2028];

  submitting = false;

  constructor(private geService: GroupeElectrogeneService, private carbService: CarburantGeService  ) {}

  ngOnInit(): void {
    if (this.groupe) {
      this.form.site = this.groupe.site;
    }
    if (this.saisie) {
      this.form.annee = this.saisie.annee;
      this.form.semestre = this.saisie.semestre;
      this.form.indexHeureSemestrePrecedent = this.saisie.indexHeureSemestrePrecedent;
      this.form.montantCarburantRestantReservoirPrecedent = this.saisie.montantCarburantRestantReservoirPrecedent;
      this.form.ravitaillementSemestrePrecedentDinars = this.saisie.ravitaillementSemestrePrecedentDinars;
      this.form.montantRestantAgilisFinSemestre = this.saisie.montantRestantAgilisFinSemestre;
      this.form.indexFinSemestre = this.saisie.indexFinSemestre;
    }
  }

  getSemestreLabel(semestre: Semestre): string {
  return SEMESTRE_LABELS[semestre];
}

  submit(): void {
    if (!this.form.site || !this.form.annee || !this.form.semestre) {
      alert('Site, année et semestre obligatoires');
      return;
    }
    this.submitting = true;
    const obs = this.saisie?.id
      ? this.carbService.modifierSaisie(this.saisie.id, this.form)
      : this.carbService.saisir(this.form);
    obs.subscribe({
      next: () => { this.submitting = false; this.onSave.emit(); },
      error: (err) => { alert(err.error?.message || 'Erreur'); this.submitting = false; }
    });
  }

  

  cancel(): void { this.onCancel.emit(); }
}

