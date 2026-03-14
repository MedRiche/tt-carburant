import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
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

  constructor(
    private zoneService: ZoneService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadZones();
  }

  loadZones(): void {
    this.loading = true;
    this.zoneService.getAllZones().subscribe({
      next: (data) => {
        this.zones = data;
        this.loading = false;
        console.log('Zones chargées:', data);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des zones', err);
        alert('Erreur lors du chargement des zones: ' + (err.error?.message || err.message));
        this.loading = false;
      }
    });
  }

  openCreateForm(): void {
    this.selectedZone = null;
    this.showForm = true;
  }

  openEditForm(zone: Zone): void {
    this.selectedZone = { ...zone }; // Clone l'objet
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
    if (!confirm(`⚠️ ATTENTION ⚠️\n\nÊtes-vous sûr de vouloir supprimer la zone "${zone.nom}" ?\n\nCette action est IRRÉVERSIBLE !`)) {
      return;
    }

    console.log('Tentative de suppression de la zone:', zone.id);

    this.zoneService.supprimerZone(zone.id).subscribe({
      next: (response) => {
        console.log('Zone supprimée avec succès', response);
        alert('Zone supprimée avec succès');
        this.loadZones();
      },
      error: (err) => {
        console.error('Erreur lors de la suppression', err);
        const errorMessage = err.error?.message || 'Erreur lors de la suppression de la zone';
        alert('❌ Erreur\n\n' + errorMessage);
      }
    });
  }

  navigateToDashboard(): void {
    this.router.navigate(['/admin/dashboardAdmin']);
  }
}