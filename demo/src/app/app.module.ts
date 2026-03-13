import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Components
import { LoginComponent } from './auth/login/login.component';
import { ZoneListComponent } from './zones/zone-list/zone-list.component';
import { ZoneFormComponent } from './zones/zone-form/zone-form.component';
import { UserValidationComponent } from './user/user-validation/user-validation.component';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { ProfileComponent } from './profile/profile.component';
// Services
import { AuthService } from './services/auth.service';
import { ZoneService } from './services/zone.service';
import { UtilisateurService } from './services/utilisateur.service';


// Guards
import { AuthGuard, AdminGuard, TechnicienGuard } from './guards/auth.guard';

// Interceptors
import { AuthInterceptor } from './interceptors/auth.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    ZoneListComponent,
    ZoneFormComponent,
    UserValidationComponent,
    HeaderComponent,
    FooterComponent,
    ProfileComponent,

  ],
    imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule
  ],
 providers: [
    // Services
    AuthService,
    ZoneService,
    UtilisateurService,
    
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