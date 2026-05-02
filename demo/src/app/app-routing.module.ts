// src/app/app-routing.module.ts  (UPDATED — add etape3 route)
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { ZoneListComponent } from './zones/zone-list/zone-list.component';
import { AdminGuard, TechnicienGuard } from './guards/auth.guard';
import { DashboardAdminComponent } from './dashboard/dashboard-admin/dashboard-admin.component';
import { GestionutilisateurComponent } from './gestionutilisateur/gestionutilisateur.component';
import { VehiculeListComponent } from './vehicule/vehicule-list/vehicule-list.component';
import { CarburantListComponent } from './carburant/carburant-list/carburant-list.component';
import { ProfileComponent } from './profile/profile.component';
import { CarburantAnalyticsComponent } from './carburant/carburant-analytics/carburant-analytics.component';
import { MaintenanceListComponent } from './maintenance/maintenance-list/maintenance-list.component';
import { GroupeElectrogeneListComponent } from './groupe-electrogene/groupe-electrogene-list/groupe-electrogene-list.component';
import { GestionCarburantGEFormComponent } from './groupe-electrogene/gestion-carburant-ge-form/gestion-carburant-ge-form.component';
import { TechnicienDashboardComponent } from './technicien/technicien-dashboard/technicien-dashboard.component';
import { TechnicienZonesComponent } from './technicien/technicien-zones/technicien-zones.component';

// ▶ ÉTAPE 3 — nouveau composant
import { TechnicienEquipementsComponent } from './technicien/technicien-equipements/technicien-equipements.component';

const routes: Routes = [
  // Route par défaut
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // Route publique
  { path: 'login', component: LoginComponent },
  { path: 'profile', component: ProfileComponent },

  // Routes ADMIN (protégées)
  {
    path: 'admin',
    canActivate: [AdminGuard],
    children: [
      { path: '', redirectTo: 'utilisateurs', pathMatch: 'full' },
      { path: 'utilisateurs',       component: GestionutilisateurComponent },
      { path: 'zones',              component: ZoneListComponent },
      { path: 'dashboardAdmin',     component: DashboardAdminComponent },
      { path: 'vehicules',          component: VehiculeListComponent },
      { path: 'carburant',          component: CarburantListComponent },
      { path: 'carburant-analytics',component: CarburantAnalyticsComponent },
      { path: 'maintenance',        component: MaintenanceListComponent },
      { path: 'groupes-electrogenes', component: GroupeElectrogeneListComponent },
      { path: 'gestion-carburant-ge', component: GestionCarburantGEFormComponent },
    ]
  },

  // Routes TECHNICIEN (protégées)
  {
    path: 'technicien',
    canActivate: [TechnicienGuard],
    children: [
      { path: '',              redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',     component: TechnicienDashboardComponent },
      { path: 'zones',         component: TechnicienZonesComponent },
      // ▶ ÉTAPE 3 — véhicules + groupes électrogènes
      { path: 'equipements',   component: TechnicienEquipementsComponent },
      { path: 'vehicules',   redirectTo: 'equipements', pathMatch: 'full' },
    ]
  },

  // Fallback
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }