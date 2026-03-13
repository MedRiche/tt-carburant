import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { ZoneListComponent } from './zones/zone-list/zone-list.component';
import { UserValidationComponent } from './user/user-validation/user-validation.component';
import { ProfileComponent } from './profile/profile.component';
import { AdminGuard, TechnicienGuard } from './guards/auth.guard';
  

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
      { path: 'utilisateurs', component: UserValidationComponent },
      { path: 'zones', component: ZoneListComponent },
      { path: 'profile', component: ProfileComponent }
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