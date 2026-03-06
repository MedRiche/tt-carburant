import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  isPanelActive = false;

  registerForm: FormGroup;
  loginForm: FormGroup;

  API = "http://localhost:8081/api/auth";

  constructor(private fb: FormBuilder, private http: HttpClient) {

    this.registerForm = this.fb.group({
      nom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      motDePasse: ['', Validators.required],
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

  onRegister() {

    if (this.registerForm.invalid) return;

    this.http.post(`${this.API}/register`, this.registerForm.value)
      .subscribe({
        next: (res) => {
          console.log("Register success", res);
          alert("Compte créé. En attente de validation admin.");
          this.activateLogin();
        },
        error: (err) => {
          console.error(err);
          alert(err.error.message);
        }
      });
  }

  onLogin() {

    if (this.loginForm.invalid) return;

    this.http.post<any>(`${this.API}/login`, this.loginForm.value)
      .subscribe({
        next: (res) => {

          console.log("Login success", res);

          localStorage.setItem("token", res.token);
          localStorage.setItem("role", res.role);

          alert("Connexion réussie");

        },
        error: (err) => {
          console.error(err);
          alert(err.error.message);
        }
      });
  }

}