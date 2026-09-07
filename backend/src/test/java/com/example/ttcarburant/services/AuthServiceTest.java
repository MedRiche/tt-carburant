package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.AuthResponse;
import com.example.ttcarburant.dto.LoginRequest;
import com.example.ttcarburant.dto.RegisterRequest;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires - Authentification (AuthService)
 * Couvre l'inscription (register) et la connexion (login), y compris
 * les règles métier liées au statut du compte (EN_ATTENTE, REFUSE, ACTIF).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AuthService (Authentification)")
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Utilisateur utilisateurActif;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        utilisateurActif = new Utilisateur();
        utilisateurActif.setNom("Ahmed Ben Salah");
        utilisateurActif.setEmail("ahmed@tt.tn");
        utilisateurActif.setMotDePasse("$2a$hashedPassword");
        utilisateurActif.setRole(Role.TECHNICIEN);
        utilisateurActif.setStatutCompte(StatutCompte.ACTIF);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("ahmed@tt.tn");
        loginRequest.setMotDePasse("motdepasse123");

        registerRequest = new RegisterRequest();
        registerRequest.setNom("Ahmed Ben Salah");
        registerRequest.setEmail("ahmed@tt.tn");
        registerRequest.setMotDePasse("motdepasse123");
        registerRequest.setRole(Role.TECHNICIEN);
        registerRequest.setSpecialite("Mécanique");
    }

    // ---------------------------------------------------------
    // register()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("doit inscrire un utilisateur avec le statut EN_ATTENTE")
        void doitInscrireNouvelUtilisateur() {
            when(utilisateurRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getMotDePasse())).thenReturn("hashed123");

            AuthResponse response = authService.register(registerRequest);

            assertThat(response.getEmail()).isEqualTo("ahmed@tt.tn");
            assertThat(response.getStatutCompte()).isEqualTo(StatutCompte.EN_ATTENTE);
            assertThat(response.getMessage()).contains("attente de validation");
            verify(utilisateurRepository, times(1)).save(any(Utilisateur.class));
        }

        @Test
        @DisplayName("doit lever une exception si l'email est déjà utilisé")
        void doitLeverExceptionQuandEmailDejaUtilise() {
            when(utilisateurRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("déjà utilisé");

            verify(utilisateurRepository, never()).save(any(Utilisateur.class));
        }
    }

    // ---------------------------------------------------------
    // login()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("doit connecter un utilisateur actif avec des identifiants valides")
        void doitConnecterUtilisateurActif() {
            when(utilisateurRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(utilisateurActif));
            when(jwtService.generateToken(utilisateurActif)).thenReturn("fake-jwt-token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("fake-jwt-token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getRole()).isEqualTo(Role.TECHNICIEN);
            assertThat(response.getMessage()).isEqualTo("Connexion réussie");
            verify(authenticationManager, times(1)).authenticate(any());
        }

        @Test
        @DisplayName("doit lever une exception si l'email n'existe pas")
        void doitLeverExceptionQuandEmailInconnu() {
            when(utilisateurRepository.findByEmail("inconnu@tt.tn")).thenReturn(Optional.empty());
            loginRequest.setEmail("inconnu@tt.tn");

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email ou mot de passe incorrect");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("doit lever une exception si le compte est EN_ATTENTE")
        void doitLeverExceptionQuandCompteEnAttente() {
            utilisateurActif.setStatutCompte(StatutCompte.EN_ATTENTE);
            when(utilisateurRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(utilisateurActif));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("attente de validation");

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("doit lever une exception si le compte a été refusé")
        void doitLeverExceptionQuandCompteRefuse() {
            utilisateurActif.setStatutCompte(StatutCompte.REFUSE);
            when(utilisateurRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(utilisateurActif));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("refusé");
        }

        @Test
        @DisplayName("doit lever une exception si le mot de passe est incorrect")
        void doitLeverExceptionQuandMotDePasseIncorrect() {
            when(utilisateurRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(utilisateurActif));
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email ou mot de passe incorrect");

            verify(jwtService, never()).generateToken(any());
        }
    }
}