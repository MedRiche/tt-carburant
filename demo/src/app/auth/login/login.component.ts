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

  // Password visibility
  showPass  = false;
  showPassR = false;

  // Focus states (for field icon color)
  emailFocused = false;
  passFocused  = false;

  registerForm: FormGroup;
  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    if (this.authService.isLoggedIn()) {
      this.redirectToDashboard();
    }

    this.registerForm = this.fb.group({
      nom:        ['', [Validators.required, Validators.minLength(2)]],
      email:      ['', [Validators.required, Validators.email]],
      motDePasse: ['', [Validators.required, Validators.minLength(6)]],
      role:       ['TECHNICIEN'],
      specialite: ['']
    });

    this.loginForm = this.fb.group({
      email:      ['', [Validators.required, Validators.email]],
      motDePasse: ['', Validators.required]
    });
  }

  activateRegister(): void { this.isPanelActive = true; }
  activateLogin():    void { this.isPanelActive = false; }

  onRegister(): void {
    if (this.registerForm.invalid) {
      this.markTouched(this.registerForm);
      return;
    }
    this.isLoading = true;
    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        alert('✅ Compte créé avec succès !\n\nVotre compte est en attente de validation par un administrateur.');
        this.registerForm.reset({ role: 'TECHNICIEN' });
        this.activateLogin();
        this.isLoading = false;
      },
      error: (err) => {
        alert('❌ ' + (err.error?.message || 'Erreur lors de l\'inscription'));
        this.isLoading = false;
      }
    });
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      this.markTouched(this.loginForm);
      return;
    }
    this.isLoading = true;
    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.loginForm.reset();
        this.isLoading = false;
        this.redirectToDashboard();
      },
      error: (err) => {
        alert('❌ ' + (err.error?.message || 'Email ou mot de passe incorrect'));
        this.isLoading = false;
      }
    });
  }

  private redirectToDashboard(): void {
    const role = this.authService.getUserRole();
    if (role === 'ADMIN')      this.router.navigate(['/admin/dashboardAdmin']);
    else if (role === 'TECHNICIEN') this.router.navigate(['/technicien/dashboard']);
    else this.router.navigate(['/home']);
  }

  private markTouched(fg: FormGroup): void {
    Object.values(fg.controls).forEach(c => c.markAsTouched());
  }

  getError(fg: FormGroup, field: string): string {
    const c = fg.get(field);
    if (!c || !c.touched) return '';
    if (c.hasError('required'))  return 'Ce champ est obligatoire';
    if (c.hasError('email'))     return 'Adresse email invalide';
    if (c.hasError('minlength')) return `Minimum ${c.getError('minlength').requiredLength} caractères requis`;
    return '';
  }
}