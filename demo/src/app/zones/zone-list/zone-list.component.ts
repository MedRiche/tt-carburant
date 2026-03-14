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

  constructor(private zoneService: ZoneService, private router: Router) {}

  ngOnInit(): void { this.loadZones(); }

  loadZones(): void {
    this.loading = true;
    this.zoneService.getAllZones().subscribe({
      next: (data) => { this.zones = data; this.loading = false; },
      error: (err) => {
        alert('Erreur: ' + (err.error?.message || err.message));
        this.loading = false;
      }
    });
  }

  openCreateForm(): void { this.selectedZone = null; this.showForm = true; }
  openEditForm(zone: Zone): void { this.selectedZone = { ...zone }; this.showForm = true; }
  closeForm(): void { this.showForm = false; this.selectedZone = null; }
  onZoneSaved(): void { this.closeForm(); this.loadZones(); }

  supprimerZone(zone: Zone): void {
    if (!confirm(`Supprimer la zone "${zone.nom}" ? Cette action est IRRÉVERSIBLE !`)) return;
    this.zoneService.supprimerZone(zone.id).subscribe({
      next: () => { alert('Zone supprimée'); this.loadZones(); },
      error: (err) => alert('❌ ' + (err.error?.message || 'Erreur'))
    });
  }

  navigateToDashboard(): void { this.router.navigate(['/admin/dashboardAdmin']); }
}