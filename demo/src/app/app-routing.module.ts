import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { ZoneListComponent } from './zones/zone-list/zone-list.component';
import { AdminGuard, TechnicienGuard } from './guards/auth.guard';
import { DashboardAdminComponent } from './dashboard/dashboard-admin/dashboard-admin.component';
import { GestionutilisateurComponent } from './gestionutilisateur/gestionutilisateur.component';
import { VehiculeListComponent } from './vehicule/vehicule-list/vehicule-list.component';
import { CarburantListComponent } from './carburant/carburant-list/carburant-list.component';
const routes: Routes = [
  // Route par défaut
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // Route publique
  { path: 'login', component: LoginComponent },

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