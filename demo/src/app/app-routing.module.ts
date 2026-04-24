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
      { path: 'utilisateurs', component: GestionutilisateurComponent },
      { path: 'zones', component: ZoneListComponent },
      { path: 'dashboardAdmin', component: DashboardAdminComponent },
      { path: 'vehicules', component: VehiculeListComponent },
      { path: 'carburant', component: CarburantListComponent },
      { path: 'carburant-analytics', component: CarburantAnalyticsComponent },
      { path: 'maintenance', component: MaintenanceListComponent },
      { path: 'groupes-electrogenes', component: GroupeElectrogeneListComponent },
      
    ]
      
  },

    // Route 404
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }