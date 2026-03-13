import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Zone } from '../../models/zone';
import { ZoneService } from '../../services/zone.service';

@Component({
  selector: 'app-zone-form',
  standalone: false,
  templateUrl: './zone-form.component.html',
  styleUrls: ['./zone-form.component.css']
})
export class ZoneFormComponent implements OnInit {
  @Input() zone: Zone | null = null;
  @Output() onSave = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  zoneForm: FormGroup;
  isEditMode = false;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    private zoneService: ZoneService
  ) {
    this.zoneForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
      responsable: ['', [Validators.maxLength(100)]]
    });
  }

  ngOnInit(): void {
    if (this.zone) {
      this.isEditMode = true;
      this.zoneForm.patchValue({
        nom: this.zone.nom,
        description: this.zone.description || '',
        responsable: this.zone.responsable || ''
      });
    }
  }

  onSubmit(): void {
    if (this.zoneForm.invalid) {
      return;
    }

    this.submitting = true;
    const zoneData = this.zoneForm.value;

    const request = this.isEditMode
      ? this.zoneService.modifierZone(this.zone!.id, zoneData)
      : this.zoneService.creerZone(zoneData);

    request.subscribe({
      next: (response) => {
        alert(response.message || 'Zone sauvegardée avec succès');
        this.submitting = false;
        this.onSave.emit();
      },
      error: (err) => {
        console.error('Erreur', err);
        alert(err.error?.message || 'Erreur lors de la sauvegarde');
        this.submitting = false;
      }
    });
  }

  cancel(): void {
    this.onCancel.emit();
  }
}