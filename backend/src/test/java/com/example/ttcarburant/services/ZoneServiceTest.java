package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.ZoneDto;
import com.example.ttcarburant.dto.ZoneRequest;
import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ZoneService avec JUnit 5 + Mockito.
 *
 * Principe : on isole le Service en simulant (mockant) ses dépendances
 * (ZoneRepository, AffectationUtilisateurZoneRepository) pour tester
 * uniquement la LOGIQUE MÉTIER, sans base de données réelle.
 */
@ExtendWith(MockitoExtension.class)          // active les annotations Mockito
@DisplayName("Tests unitaires - ZoneService")
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private AffectationUtilisateurZoneRepository affectationRepository;

    @InjectMocks                              // injecte les 2 mocks ci-dessus dans ZoneService
    private ZoneService zoneService;

    private Zone zone;
    private ZoneRequest zoneRequest;

    @BeforeEach
    void setUp() {
        // Jeu de données réutilisé par plusieurs tests
        zone = new Zone("Tunis Nord", "Zone couvrant le nord de Tunis", "Ahmed Ben Salah");
        zone.setId(1L);

        zoneRequest = new ZoneRequest("Tunis Nord", "Zone couvrant le nord de Tunis", "Ahmed Ben Salah");
    }

    // ---------------------------------------------------------
    // creerZone()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("creerZone()")
    class CreerZone {

        @Test
        @DisplayName("doit créer une zone quand le nom n'existe pas déjà")
        void doitCreerZoneQuandNomUnique() {
            // Arrange (préparation des mocks)
            when(zoneRepository.existsByNom(zoneRequest.getNom())).thenReturn(false);
            when(zoneRepository.save(any(Zone.class))).thenReturn(zone);
            when(affectationRepository.findByZone(zone)).thenReturn(Collections.emptyList());

            // Act (appel de la méthode testée)
            ZoneDto result = zoneService.creerZone(zoneRequest);

            // Assert (vérification du résultat)
            assertThat(result).isNotNull();
            assertThat(result.getNom()).isEqualTo("Tunis Nord");
            assertThat(result.getResponsable()).isEqualTo("Ahmed Ben Salah");
            assertThat(result.getNombreUtilisateurs()).isZero();

            // Vérifie que save() a bien été appelé une seule fois
            verify(zoneRepository, times(1)).save(any(Zone.class));
        }

        @Test
        @DisplayName("doit lever une exception si le nom de zone existe déjà")
        void doitLeverExceptionQuandNomDejaExistant() {
            when(zoneRepository.existsByNom(zoneRequest.getNom())).thenReturn(true);

            assertThatThrownBy(() -> zoneService.creerZone(zoneRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("existe déjà");

            // save() ne doit jamais être appelé dans ce cas
            verify(zoneRepository, never()).save(any(Zone.class));
        }
    }

    // ---------------------------------------------------------
    // getZoneById()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("getZoneById()")
    class GetZoneById {

        @Test
        @DisplayName("doit retourner la zone quand l'id existe")
        void doitRetournerZoneQuandIdExiste() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
            when(affectationRepository.findByZone(zone)).thenReturn(Collections.emptyList());

            ZoneDto result = zoneService.getZoneById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getNom()).isEqualTo("Tunis Nord");
        }

        @Test
        @DisplayName("doit lever une exception quand l'id n'existe pas")
        void doitLeverExceptionQuandIdInexistant() {
            when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> zoneService.getZoneById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("non trouvée");
        }
    }

    // ---------------------------------------------------------
    // getAllZones()
    // ---------------------------------------------------------
    @Test
    @DisplayName("getAllZones() doit retourner la liste convertie en DTO")
    void doitRetournerToutesLesZones() {
        Zone zone2 = new Zone("Tunis Sud", "Zone Sud", "Sami Trabelsi");
        zone2.setId(2L);

        when(zoneRepository.findAll()).thenReturn(List.of(zone, zone2));
        when(affectationRepository.findByZone(any(Zone.class))).thenReturn(Collections.emptyList());

        List<ZoneDto> result = zoneService.getAllZones();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNom()).isEqualTo("Tunis Nord");
        assertThat(result.get(1).getNom()).isEqualTo("Tunis Sud");
    }

    // ---------------------------------------------------------
    // modifierZone()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("modifierZone()")
    class ModifierZone {

        @Test
        @DisplayName("doit modifier la zone quand le nouveau nom est libre")
        void doitModifierZoneQuandNomLibre() {
            ZoneRequest nouvelleRequete = new ZoneRequest("Tunis Centre", "Nouvelle description", "Nouveau responsable");

            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
            when(zoneRepository.existsByNom("Tunis Centre")).thenReturn(false);
            when(zoneRepository.save(any(Zone.class))).thenReturn(zone);
            when(affectationRepository.findByZone(zone)).thenReturn(Collections.emptyList());

            ZoneDto result = zoneService.modifierZone(1L, nouvelleRequete);

            assertThat(result.getNom()).isEqualTo("Tunis Centre");
            verify(zoneRepository).save(zone);
        }

        @Test
        @DisplayName("doit lever une exception si le nouveau nom appartient déjà à une autre zone")
        void doitLeverExceptionQuandNouveauNomPrisParAutreZone() {
            ZoneRequest nouvelleRequete = new ZoneRequest("Tunis Sud", "desc", "resp");

            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
            when(zoneRepository.existsByNom("Tunis Sud")).thenReturn(true);

            assertThatThrownBy(() -> zoneService.modifierZone(1L, nouvelleRequete))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("existe déjà");

            verify(zoneRepository, never()).save(any(Zone.class));
        }

        @Test
        @DisplayName("ne doit PAS lever d'exception si on garde le même nom")
        void nedoitPasLeverExceptionQuandNomInchange() {
            // Même nom que l'existant -> pas de vérification d'unicité déclenchée
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
            when(zoneRepository.save(any(Zone.class))).thenReturn(zone);
            when(affectationRepository.findByZone(zone)).thenReturn(Collections.emptyList());

            ZoneDto result = zoneService.modifierZone(1L, zoneRequest); // même "nom"

            assertThat(result).isNotNull();
            verify(zoneRepository, never()).existsByNom(anyString());
        }
    }

    // ---------------------------------------------------------
    // supprimerZone()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("supprimerZone()")
    class SupprimerZone {

        @Test
        @DisplayName("doit supprimer la zone si aucun utilisateur n'y est affecté")
        void doitSupprimerZoneSansAffectation() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
            when(affectationRepository.findByZone(zone)).thenReturn(Collections.emptyList());

            zoneService.supprimerZone(1L);

            verify(zoneRepository, times(1)).delete(zone);
        }

        @Test
        @DisplayName("doit lever une exception si des utilisateurs sont affectés à la zone")
        void doitLeverExceptionSiUtilisateursAffectes() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
            when(affectationRepository.findByZone(zone))
                    .thenReturn(List.of(new AffectationUtilisateurZone()));

            assertThatThrownBy(() -> zoneService.supprimerZone(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("affectés");

            verify(zoneRepository, never()).delete(any(Zone.class));
        }

        @Test
        @DisplayName("doit lever une exception si la zone à supprimer n'existe pas")
        void doitLeverExceptionQuandZoneInexistante() {
            when(zoneRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> zoneService.supprimerZone(404L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("non trouvée");
        }
    }
}