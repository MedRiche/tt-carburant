// src/app/technicien/zones/technicien-zones.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TechnicienService } from '../../services/technicien.service';
import { Zone } from '../../models/zone';

@Component({
  selector: 'app-technicien-zones',
  standalone: false,
  templateUrl: './technicien-zones.component.html',
  styleUrls: ['./technicien-zones.component.css']
})
export class TechnicienZonesComponent implements OnInit {

  zones: Zone[] = [];
  loading = true;
  selectedZone: Zone | null = null;
  showDetail = false;

  nomAffiche = localStorage.getItem('nom') || 'Technicien';

  constructor(
    private authService: AuthService,
    private technicienService: TechnicienService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadZones();
  }

  loadZones(): void {
    this.loading = true;
    this.technicienService.getMesZones().subscribe({
      next: (data) => {
        this.zones   = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openDetail(zone: Zone): void {
    this.selectedZone = zone;
    this.showDetail   = true;
  }

  closeDetail(): void {
    this.showDetail   = false;
    this.selectedZone = null;
  }

  logout(): void {
    this.authService.logout();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  getInitials(nom: string): string {
    return (nom || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  getZoneInitial(nom: string): string {
    return (nom || 'Z').charAt(0).toUpperCase();
  }

  getZoneColor(index: number): string {
    const colors = [
      'linear-gradient(135deg,#E2001A,#8b0010)',
      'linear-gradient(135deg,#1e3a70,#0c1a3d)',
      'linear-gradient(135deg,#00d4aa,#007a62)',
      'linear-gradient(135deg,#7c3aed,#4c1d95)',
      'linear-gradient(135deg,#ea580c,#9a3412)',
      'linear-gradient(135deg,#0891b2,#164e63)',
    ];
    return colors[index % colors.length];
  }
}