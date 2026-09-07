package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.ModifierUtilisateurRequest;
import com.example.ttcarburant.dto.UtilisateurDto;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires - Gestion des rôles et des comptes (UtilisateurService)
 * Couvre le changement de rôle, le refus de compte et l'activation/désactivation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - UtilisateurService (Rôles & comptes)")
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private AffectationUtilisateurZoneRepository affectationRepository;

    @InjectMocks
    private UtilisateurService utilisateurService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setNom("Sami Trabelsi");
        utilisateur.setEmail("sami@tt.tn");
        utilisateur.setRole(Role.TECHNICIEN);
        utilisateur.setStatutCompte(StatutCompte.ACTIF);
    }

    // ---------------------------------------------------------
    // modifierInfo() → changement de rôle
    // ---------------------------------------------------------
    @Nested
    @DisplayName("modifierInfo() - changement de rôle")
    class ModifierInfo {

        @Test
        @DisplayName("doit changer le rôle d'un utilisateur de TECHNICIEN vers ADMIN")
        void doitChangerRoleTechnicienVersAdmin() {
            ModifierUtilisateurRequest request = new ModifierUtilisateurRequest();
            request.setEmail("sami@tt.tn");
            request.setRole(Role.ADMIN);
            request.setSpecialite(null);

            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
            when(affectationRepository.findByUtilisateur(utilisateur)).thenReturn(Collections.emptyList());

            UtilisateurDto result = utilisateurService.modifierInfo(1L, request);

            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
            verify(utilisateurRepository, times(1)).save(utilisateur);
        }

        @Test
        @DisplayName("doit lever une exception si le nouvel email appartient déjà à un autre compte")
        void doitLeverExceptionQuandEmailDejaUtilise() {
            ModifierUtilisateurRequest request = new ModifierUtilisateurRequest();
            request.setEmail("autre@tt.tn");
            request.setRole(Role.ADMIN);

            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
            when(utilisateurRepository.existsByEmail("autre@tt.tn")).thenReturn(true);

            assertThatThrownBy(() -> utilisateurService.modifierInfo(1L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("déjà utilisé");

            verify(utilisateurRepository, never()).save(any(Utilisateur.class));
        }
    }

    // ---------------------------------------------------------
    // refuserCompte()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("refuserCompte()")
    class RefuserCompte {

        @Test
        @DisplayName("doit refuser un compte EN_ATTENTE")
        void doitRefuserCompteEnAttente() {
            utilisateur.setStatutCompte(StatutCompte.EN_ATTENTE);
            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
            when(affectationRepository.findByUtilisateur(utilisateur)).thenReturn(Collections.emptyList());

            UtilisateurDto result = utilisateurService.refuserCompte(1L);

            assertThat(result.getStatutCompte()).isEqualTo(StatutCompte.REFUSE);
        }

        @Test
        @DisplayName("doit lever une exception si le compte n'est pas EN_ATTENTE")
        void doitLeverExceptionQuandCompteDejaTraite() {
            utilisateur.setStatutCompte(StatutCompte.ACTIF);
            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

            assertThatThrownBy(() -> utilisateurService.refuserCompte(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("pas en attente");
        }
    }

    // ---------------------------------------------------------
    // toggleActivation()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("toggleActivation()")
    class ToggleActivation {

        @Test
        @DisplayName("doit désactiver un compte ACTIF")
        void doitDesactiverCompteActif() {
            utilisateur.setStatutCompte(StatutCompte.ACTIF);
            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
            when(affectationRepository.findByUtilisateur(utilisateur)).thenReturn(Collections.emptyList());

            UtilisateurDto result = utilisateurService.toggleActivation(1L);

            assertThat(result.getStatutCompte()).isEqualTo(StatutCompte.DESACTIVE);
        }

        @Test
        @DisplayName("doit réactiver un compte DESACTIVE")
        void doitReactiverCompteDesactive() {
            utilisateur.setStatutCompte(StatutCompte.DESACTIVE);
            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
            when(affectationRepository.findByUtilisateur(utilisateur)).thenReturn(Collections.emptyList());

            UtilisateurDto result = utilisateurService.toggleActivation(1L);

            assertThat(result.getStatutCompte()).isEqualTo(StatutCompte.ACTIF);
        }

        @Test
        @DisplayName("doit lever une exception pour un compte EN_ATTENTE")
        void doitLeverExceptionPourCompteEnAttente() {
            utilisateur.setStatutCompte(StatutCompte.EN_ATTENTE);
            when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

            assertThatThrownBy(() -> utilisateurService.toggleActivation(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Action impossible");
        }
    }
}