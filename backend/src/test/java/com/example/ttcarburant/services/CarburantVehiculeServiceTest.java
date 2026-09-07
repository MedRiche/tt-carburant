package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.CarburantVehiculeDto;
import com.example.ttcarburant.dto.CarburantVehiculeRequest;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires - Saisie carburant (CarburantVehiculeService)
 * Couvre la saisie mensuelle du carburant, les formules DAF 2026,
 * ainsi que les règles de verrouillage mensuel et de doublon.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - CarburantVehiculeService (Saisie carburant)")
class CarburantVehiculeServiceTest {

    @Mock
    private CarburantVehiculeRepository carburantRepo;

    @Mock
    private VehiculeRepository vehiculeRepo;

    @Mock
    private VerrouillageService verrouillageService;

    @Mock
    private HistoriqueModificationService historiqueService;

    @InjectMocks
    private CarburantVehiculeService carburantVehiculeService;

    private Vehicule vehicule;
    private CarburantVehiculeRequest request;

    @BeforeEach
    void setUp() {
        vehicule = new Vehicule();
        vehicule.setMatricule("123TU456");
        vehicule.setDateMiseService(LocalDate.of(2022, 1, 1));
        vehicule.setMarqueModele("Peugeot Partner");
        vehicule.setTypeVehicule("Utilitaire");
        vehicule.setTypeCarburant(TypeCarburant.GASOIL_ORDINAIRE);
        vehicule.setPrixCarburant(2.0);   // 2 DT / litre
        vehicule.setCoutDuMois(200.0);    // budget mensuel 200 DT
        // vehicule.setZone(null) -> zone non affectée

        request = new CarburantVehiculeRequest();
        request.setVehiculeMatricule("123TU456");
        request.setAnnee(2026);
        request.setMois(9);
        request.setIndexDemarrageMois(1000);
        request.setIndexFinMois(1500);              // 500 km parcourus
        request.setMontantRestantMoisPrecedent(40);  // 40 DT restants -> 20 L (qteRestante)
        request.setRavitaillementMoisPrecedent(160); // 160 DT -> pèse dans totalLitres ET dans le coût réel
        request.setRavitaillementMois(150);          // n'intervient que dans montantRestantReservoirFin
    }

    // ---------------------------------------------------------
    // saisir()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("saisir()")
    class Saisir {

        @Test
        @DisplayName("doit enregistrer une saisie et calculer correctement les formules DAF 2026")
        void doitEnregistrerSaisieEtCalculerFormules() {
            when(vehiculeRepo.findById("123TU456")).thenReturn(Optional.of(vehicule));
            when(verrouillageService.isVerrouille(2026, 9, null)).thenReturn(false);
            when(carburantRepo.findByVehiculeAndAnneeAndMois(vehicule, 2026, 9)).thenReturn(Optional.empty());
            when(carburantRepo.save(any(GestionCarburantVehicule.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CarburantVehiculeDto result = carburantVehiculeService.saisir(request);

            // Total ravitaillement = (160 + 40) / 2.0 = 100 L
            assertThat(result.getTotalRavitaillementLitres()).isEqualTo(100.0);
            // Quantité restante réservoir = 40 / 2.0 = 20 L
            assertThat(result.getQuantiteRestanteReservoir()).isEqualTo(20.0);
            // Distance parcourue = 1500 - 1000 = 500 km
            assertThat(result.getDistanceParcourue()).isEqualTo(500.0);
            // Coût réel = (100 - 20) * 2.0 = 160 DT <= budget 200 DT -> pas de dépassement
            assertThat(result.isBudgetDepasse()).isFalse();

            verify(historiqueService, times(1)).enregistrerCreation(any(GestionCarburantVehicule.class));
        }

        @Test
        @DisplayName("doit détecter un dépassement de budget")
        void doitDetecterDepassementBudget() {
            // Coût réel = ravitaillementMoisPrecedent (ici 500 DT) > budget (200 DT)
            request.setRavitaillementMoisPrecedent(500);
            when(vehiculeRepo.findById("123TU456")).thenReturn(Optional.of(vehicule));
            when(verrouillageService.isVerrouille(2026, 9, null)).thenReturn(false);
            when(carburantRepo.findByVehiculeAndAnneeAndMois(vehicule, 2026, 9)).thenReturn(Optional.empty());
            when(carburantRepo.save(any(GestionCarburantVehicule.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CarburantVehiculeDto result = carburantVehiculeService.saisir(request);

            assertThat(result.isBudgetDepasse()).isTrue();
            assertThat(result.getDepassementMontant()).isGreaterThan(0);
        }

        @Test
        @DisplayName("doit rejeter la saisie si le mois est verrouillé")
        void doitRejeterSaisieQuandMoisVerrouille() {
            when(vehiculeRepo.findById("123TU456")).thenReturn(Optional.of(vehicule));
            when(verrouillageService.isVerrouille(2026, 9, null)).thenReturn(true);

            assertThatThrownBy(() -> carburantVehiculeService.saisir(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verrouillé");

            verify(carburantRepo, never()).save(any(GestionCarburantVehicule.class));
            verify(historiqueService, never()).enregistrerCreation(any());
        }

        @Test
        @DisplayName("doit rejeter la saisie si un enregistrement existe déjà pour ce véhicule/mois/année")
        void doitRejeterSaisieEnDoublon() {
            GestionCarburantVehicule existant = new GestionCarburantVehicule();
            when(vehiculeRepo.findById("123TU456")).thenReturn(Optional.of(vehicule));
            when(verrouillageService.isVerrouille(2026, 9, null)).thenReturn(false);
            when(carburantRepo.findByVehiculeAndAnneeAndMois(vehicule, 2026, 9))
                    .thenReturn(Optional.of(existant));

            assertThatThrownBy(() -> carburantVehiculeService.saisir(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("existe déjà");

            verify(carburantRepo, never()).save(any(GestionCarburantVehicule.class));
        }

        @Test
        @DisplayName("doit lever une exception si le véhicule n'existe pas")
        void doitLeverExceptionQuandVehiculeInexistant() {
            when(vehiculeRepo.findById("INCONNU")).thenReturn(Optional.empty());
            request.setVehiculeMatricule("INCONNU");

            assertThatThrownBy(() -> carburantVehiculeService.saisir(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Véhicule non trouvé");
        }
    }

    // ---------------------------------------------------------
    // supprimer()
    // ---------------------------------------------------------
    @Nested
    @DisplayName("supprimer()")
    class Supprimer {

        @Test
        @DisplayName("doit rejeter la suppression si le mois est verrouillé")
        void doitRejeterSuppressionQuandMoisVerrouille() {
            GestionCarburantVehicule g = new GestionCarburantVehicule();
            g.setVehicule(vehicule);
            g.setAnnee(2026);
            g.setMois(9);

            when(carburantRepo.findById(1L)).thenReturn(Optional.of(g));
            when(verrouillageService.isVerrouille(2026, 9, null)).thenReturn(true);

            assertThatThrownBy(() -> carburantVehiculeService.supprimer(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("verrouillé");

            verify(carburantRepo, never()).deleteById(any());
        }
    }
}