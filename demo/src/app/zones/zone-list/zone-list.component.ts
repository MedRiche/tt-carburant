import { Component, OnInit } from '@angular/core';
import { Zone } from '../../models/zone';
import { ZoneService } from '../../services/zone.service';

@Component({
  selector: 'app-zone-list',
  standalone: false,
  templateUrl: './zone-list.component.html',
  styleUrls: ['./zone-list.component.css']
})
export class ZoneListComponent implements OnInit {
  zones: Zone[] = [];
  loading = false;
  showForm = false;
  selectedZone: Zone | null = null;

  constructor(private zoneService: ZoneService) {}

  ngOnInit(): void {
    this.loadZones();
  }

  loadZones(): void {
    this.loading = true;
    this.zoneService.getAllZones().subscribe({
      next: (data) => {
        this.zones = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des zones', err);
        alert('Erreur lors du chargement des zones');
        this.loading = false;
      }
    });
  }

  openCreateForm(): void {
    this.selectedZone = null;
    this.showForm = true;
  }

  openEditForm(zone: Zone): void {
    this.selectedZone = zone;
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.selectedZone = null;
  }

  onZoneSaved(): void {
    this.closeForm();
    this.loadZones();
  }

  supprimerZone(zone: Zone): void {
    if (!confirm(`Êtes-vous sûr de vouloir supprimer la zone "${zone.nom}" ?`)) {
      return;
    }

    this.zoneService.supprimerZone(zone.id).subscribe({
      next: () => {
        alert('Zone supprimée avec succès');
        this.loadZones();
      },
      error: (err) => {
        console.error('Erreur lors de la suppression', err);
        alert(err.error?.message || 'Erreur lors de la suppression de la zone');
      }
    });
  }
}