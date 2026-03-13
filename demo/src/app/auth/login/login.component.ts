import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  isPanelActive = false;
  isLoading = false;

  registerForm: FormGroup;
  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    // Vérifier si l'utilisateur est déjà connecté
    if (this.authService.isLoggedIn()) {
      this.redirectToDashboard();
    }

    this.registerForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      motDePasse: ['', [Validators.required, Validators.minLength(6)]],
      role: ['TECHNICIEN'], // Par défaut TECHNICIEN
      specialite: ['']
    });

    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      motDePasse: ['', Validators.required]
    });
  }

  activateRegister() {
    this.isPanelActive = true;
  }

  activateLogin() {
    this.isPanelActive = false;
  }

  /**
   * Gestion de l'inscription
   */
  onRegister() {
    if (this.registerForm.invalid) {
      this.markFormGroupTouched(this.registerForm);
      return;
    }

    this.isLoading = true;
    const registerData = this.registerForm.value;

    this.authService.register(registerData).subscribe({
      next: (response) => {
        console.log('Registration success', response);
        alert('✅ Compte créé avec succès !\n\n' + 
              'Votre compte est en attente de validation par un administrateur.\n' +
              'Vous recevrez une notification une fois votre compte validé.');
        
        // Réinitialiser le formulaire
        this.registerForm.reset({ role: 'TECHNICIEN' });
        
        // Passer au formulaire de connexion
        this.activateLogin();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Registration error', err);
        const errorMessage = err.error?.message || 'Une erreur est survenue lors de l\'inscription';
        alert('❌ Erreur d\'inscription\n\n' + errorMessage);
        this.isLoading = false;
      }
    });
  }

  /**
   * Gestion de la connexion
   */
  onLogin() {
    if (this.loginForm.invalid) {
      this.markFormGroupTouched(this.loginForm);
      return;
    }

    this.isLoading = true;
    const loginData = this.loginForm.value;

    this.authService.login(loginData).subscribe({
      next: (response) => {
        console.log('Login success', response);
        
        // Message de bienvenue
        alert(`✅ Bienvenue ${response.nom} !\n\nConnexion réussie.`);
        
        // Réinitialiser le formulaire
        this.loginForm.reset();
        this.isLoading = false;

        // Redirection selon le rôle
        this.redirectToDashboard();
      },
      error: (err) => {
        console.error('Login error', err);
        let errorMessage = 'Email ou mot de passe incorrect';
        
        if (err.error?.message) {
          errorMessage = err.error.message;
        }
        
        alert('❌ Erreur de connexion\n\n' + errorMessage);
        this.isLoading = false;
      }
    });
  }

  /**
   * Redirection vers le dashboard selon le rôle
   */
  private redirectToDashboard() {
    const role = this.authService.getUserRole();

    if (role === 'ADMIN') {
      // Rediriger vers le dashboard admin
      this.router.navigate(['/admin/dashboardAdmin']);
    } else if (role === 'TECHNICIEN') {
      // Rediriger vers le dashboard technicien
      this.router.navigate(['/technicien/dashboard']);
    } else {
      // Par défaut, rediriger vers home
      this.router.navigate(['/home']);
    }
  }

  /**
   * Marquer tous les champs du formulaire comme touchés pour afficher les erreurs
   */
  private markFormGroupTouched(formGroup: FormGroup) {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  /**
   * Vérifier si un champ a une erreur
   */
  hasError(formGroup: FormGroup, fieldName: string, errorType: string): boolean {
    const field = formGroup.get(fieldName);
    return !!(field && field.hasError(errorType) && field.touched);
  }

  /**
   * Obtenir le message d'erreur pour un champ
   */
  getErrorMessage(formGroup: FormGroup, fieldName: string): string {
    const field = formGroup.get(fieldName);
    
    if (!field || !field.touched) {
      return '';
    }

    if (field.hasError('required')) {
      return 'Ce champ est obligatoire';
    }
    
    if (field.hasError('email')) {
      return 'Email invalide';
    }
    
    if (field.hasError('minlength')) {
      const minLength = field.getError('minlength').requiredLength;
      return `Minimum ${minLength} caractères requis`;
    }

    return '';
  }
}