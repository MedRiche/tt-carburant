package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.analytics.AnomalieDto;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires - Détection des anomalies (CarburantAnalyticsService)
 * Couvre les 4 types d'anomalies détectées par le moteur d'analyse :
 * dépassement de budget, consommation anormale, incohérence km/consommation
 * et kilométrage mensuel excessif.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - CarburantAnalyticsService (Anomalies)")
class CarburantAnalyticsServiceTest {

    @Mock
    private CarburantVehiculeRepository carburantRepo;

    @Mock
    private VehiculeRepository vehiculeRepo;

    @Mock
    private ZoneRepository zoneRepo;

    @InjectMocks
    private CarburantAnalyticsService analyticsService;

    private Vehicule creerVehicule(double prixCarburant, double coutDuMois) {
        Vehicule v = new Vehicule();
        v.setMatricule("123TU456");
        v.setDateMiseService(LocalDate.of(2022, 1, 1));
        v.setMarqueModele("Peugeot Partner");
        v.setTypeVehicule("Utilitaire");
        v.setTypeCarburant(TypeCarburant.GASOIL_ORDINAIRE);
        v.setPrixCarburant(prixCarburant);
        v.setCoutDuMois(coutDuMois);
        return v;
    }

    private GestionCarburantVehicule creerSaisie(Vehicule v, double distance,
                                                 double totalLitres, double restante,
                                                 boolean budgetDepasse, double depassement) {
        GestionCarburantVehicule g = new GestionCarburantVehicule();
        g.setVehicule(v);
        g.setAnnee(2026);
        g.setMois(9);
        g.setDistanceParcourue(distance);
        g.setTotalRavitaillementLitres(totalLitres);
        g.setQuantiteRestanteReservoir(restante);
        g.setBudgetDepasse(budgetDepasse);
        g.setDepassementMontant(depassement);
        return g;
    }

    @Nested
    @DisplayName("detecterAnomalies()")
    class DetecterAnomalies {

        @Test
        @DisplayName("doit détecter un dépassement de budget avec sévérité ELEVEE")
        void doitDetecterBudgetDepasse() {
            // prix=1, budget=15, consoL=20 (total=25, restante=5) -> coûtRéel=20 -> ratio=1.33 (ELEVEE)
            // distance=200 -> conso/100km = 10 (normale, n'active pas d'autre anomalie)
            Vehicule v = creerVehicule(1.0, 15.0);
            GestionCarburantVehicule g = creerSaisie(v, 200, 25, 5, true, 5);

            when(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(2026, 9))
                    .thenReturn(List.of(g));

            List<AnomalieDto> anomalies = analyticsService.detecterAnomalies(2026, 9, null);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).getTypeAnomalie()).isEqualTo("BUDGET_DEPASSE");
            assertThat(anomalies.get(0).getSeverite()).isEqualTo("ELEVEE");
        }

        @Test
        @DisplayName("doit détecter un dépassement de budget CRITIQUE quand le ratio dépasse 150%")
        void doitDetecterBudgetDepasseCritique() {
            // prix=1, budget=10, consoL=20 -> coûtRéel=20 -> ratio=2.0 (> 1.5 -> CRITIQUE)
            Vehicule v = creerVehicule(1.0, 10.0);
            GestionCarburantVehicule g = creerSaisie(v, 200, 25, 5, true, 10);

            when(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(2026, 9))
                    .thenReturn(List.of(g));

            List<AnomalieDto> anomalies = analyticsService.detecterAnomalies(2026, 9, null);

            assertThat(anomalies).anySatisfy(a -> {
                assertThat(a.getTypeAnomalie()).isEqualTo("BUDGET_DEPASSE");
                assertThat(a.getSeverite()).isEqualTo("CRITIQUE");
            });
        }

        @Test
        @DisplayName("doit détecter une consommation anormalement élevée (> 15 L/100km)")
        void doitDetecterConsommationAnormale() {
            // consoL=20 sur 100km -> 20 L/100km (> seuil 15)
            Vehicule v = creerVehicule(1.0, 100.0); // budget large -> pas de dépassement
            GestionCarburantVehicule g = creerSaisie(v, 100, 25, 5, false, 0);

            when(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(2026, 9))
                    .thenReturn(List.of(g));

            List<AnomalieDto> anomalies = analyticsService.detecterAnomalies(2026, 9, null);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).getTypeAnomalie()).isEqualTo("CONSO_ANORMALE");
            assertThat(anomalies.get(0).getValeurReelle()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("doit détecter une consommation anormalement basse (index suspect)")
        void doitDetecterConsommationIncoherente() {
            // consoL=2 sur 200km -> 1 L/100km (< seuil 3, données suspectes)
            Vehicule v = creerVehicule(1.0, 100.0);
            GestionCarburantVehicule g = creerSaisie(v, 200, 7, 5, false, 0);

            when(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(2026, 9))
                    .thenReturn(List.of(g));

            List<AnomalieDto> anomalies = analyticsService.detecterAnomalies(2026, 9, null);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).getTypeAnomalie()).isEqualTo("KM_INCOHERENT");
            assertThat(anomalies.get(0).getSeverite()).isEqualTo("MOYENNE");
        }

        @Test
        @DisplayName("doit détecter un kilométrage mensuel excessif (> 5000 km)")
        void doitDetecterKilometrageExcessif() {
            // distance=6000 > seuil 5000, conso/100km normale (10) pour isoler l'anomalie KM_ELEVE
            Vehicule v = creerVehicule(0.1, 100.0);
            GestionCarburantVehicule g = creerSaisie(v, 6000, 605, 5, false, 0);

            when(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(2026, 9))
                    .thenReturn(List.of(g));

            List<AnomalieDto> anomalies = analyticsService.detecterAnomalies(2026, 9, null);

            assertThat(anomalies).hasSize(1);
            assertThat(anomalies.get(0).getTypeAnomalie()).isEqualTo("KM_ELEVE");
            assertThat(anomalies.get(0).getValeurReelle()).isEqualTo(6000.0);
        }

        @Test
        @DisplayName("ne doit détecter aucune anomalie pour une saisie normale")
        void neDoitDetecterAucuneAnomaliePourSaisieNormale() {
            // Tout est dans les seuils normaux
            Vehicule v = creerVehicule(1.0, 100.0);
            GestionCarburantVehicule g = creerSaisie(v, 200, 25, 5, false, 0);

            when(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(2026, 9))
                    .thenReturn(List.of(g));

            List<AnomalieDto> anomalies = analyticsService.detecterAnomalies(2026, 9, null);

            assertThat(anomalies).isEmpty();
        }
    }
}