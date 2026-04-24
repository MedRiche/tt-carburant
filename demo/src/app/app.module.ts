import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';


import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Components
import { LoginComponent } from './auth/login/login.component';
import { ZoneListComponent } from './zones/zone-list/zone-list.component';
import { ZoneFormComponent } from './zones/zone-form/zone-form.component';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { ProfileComponent } from './profile/profile.component';
// Services
import { AuthService } from './services/auth.service';
import { ZoneService } from './services/zone.service';
import { UtilisateurService } from './services/utilisateur.service';
import { VehiculeService }       from './services/vehicule.service';
import { CarburantVehiculeService } from './services/carburant-vehicule.service';
import { CarburantAnalyticsService } from './services/carburant-analytics.service';
import { MaintenanceService } from './services/maintenance.service';
import { GroupeElectrogeneService } from './services/groupe-electrogene.service';


// Guards
import { AuthGuard, AdminGuard, TechnicienGuard } from './guards/auth.guard';

// Interceptors
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { DashboardAdminComponent } from './dashboard/dashboard-admin/dashboard-admin.component';
import { GestionutilisateurComponent } from './gestionutilisateur/gestionutilisateur.component';
import { VehiculeListComponent } from './vehicule/vehicule-list/vehicule-list.component';
import { VehiculeFormComponent } from './vehicule/vehicule-form/vehicule-form.component';
import { CarburantListComponent } from './carburant/carburant-list/carburant-list.component';
import { CarburantFormComponent } from './carburant/carburant-form/carburant-form.component';
import { CarburantAdditionsComponent } from './carburant/carburant-additions/carburant-additions.component';
import { CountByPipe } from './pipes/count-by.pipe';
import { CarburantAnalyticsComponent } from './carburant/carburant-analytics/carburant-analytics.component';
import { MaintenanceListComponent } from './maintenance/maintenance-list/maintenance-list.component';
import { MaintenanceFormComponent } from './maintenance/maintenance-form/maintenance-form.component';
import { GroupeElectrogeneListComponent } from './groupe-electrogene/groupe-electrogene-list/groupe-electrogene-list.component';
import { GestionCarburantGEFormComponent } from './groupe-electrogene/gestion-carburant-ge-form/gestion-carburant-ge-form.component';
import { GroupeElectrogeneFormComponent } from './groupe-electrogene/groupe-electrogene-form/groupe-electrogene-form.component';
@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    ZoneListComponent,
    ZoneFormComponent,
    HeaderComponent,
    FooterComponent,
    ProfileComponent,
    DashboardAdminComponent,
    GestionutilisateurComponent,
    VehiculeListComponent,
    VehiculeFormComponent,
    CarburantListComponent,
    CarburantFormComponent,
    CarburantAdditionsComponent,
    CountByPipe,
    CarburantAnalyticsComponent,
    MaintenanceListComponent,
    MaintenanceFormComponent,
    GroupeElectrogeneListComponent,
    GestionCarburantGEFormComponent,
    GroupeElectrogeneFormComponent,


    

  ],
    imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    CommonModule,

  ],
 providers: [
    // Services
    AuthService,
    ZoneService,
    UtilisateurService,
    VehiculeService,
    CarburantVehiculeService,
    CarburantAnalyticsService,
    MaintenanceService,
    GroupeElectrogeneService,

    
    // Guards
    AuthGuard,
    AdminGuard,
    TechnicienGuard,
    
    // HTTP Interceptor pour ajouter automatiquement le token
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }