package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.CarburantVehiculeDto;
import com.example.ttcarburant.dto.CarburantVehiculeRequest;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarburantVehiculeService {

    private final CarburantVehiculeRepository carburantRepo;
    private final VehiculeRepository vehiculeRepo;

    public CarburantVehiculeService(CarburantVehiculeRepository carburantRepo,
                                    VehiculeRepository vehiculeRepo) {
        this.carburantRepo = carburantRepo;
        this.vehiculeRepo  = vehiculeRepo;
    }

    // ── Lire ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CarburantVehiculeDto> getByVehicule(String matricule) {
        Vehicule v = findVehicule(matricule);
        return carburantRepo.findByVehiculeOrderByAnneeDescMoisDesc(v)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CarburantVehiculeDto> getByZoneAndPeriode(Long zoneId, int annee, int mois) {
        return carburantRepo.findByZoneAndPeriode(zoneId, annee, mois)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CarburantVehiculeDto> getByPeriode(int annee, int mois) {
        return carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, mois)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CarburantVehiculeDto getById(Long id) {
        return toDto(carburantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enregistrement non trouvé")));
    }

    // ── Créer / Modifier ────────────────────────────────────────

    @Transactional
    public CarburantVehiculeDto saisir(CarburantVehiculeRequest req) {
        Vehicule v = findVehicule(req.getVehiculeMatricule());

        // Vérifier doublon
        if (carburantRepo.findByVehiculeAndAnneeAndMois(v, req.getAnnee(), req.getMois()).isPresent()) {
            throw new RuntimeException(
                    "Un enregistrement existe déjà pour ce véhicule / mois / année. Utilisez modifier.");
        }

        GestionCarburantVehicule g = new GestionCarburantVehicule();
        g.setVehicule(v);
        g.setAnnee(req.getAnnee());
        g.setMois(req.getMois());
        appliquerSaisieEtCalculer(g, req, v);
        return toDto(carburantRepo.save(g));
    }

    @Transactional
    public CarburantVehiculeDto modifier(Long id, CarburantVehiculeRequest req) {
        GestionCarburantVehicule g = carburantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enregistrement non trouvé"));
        Vehicule v = g.getVehicule();
        appliquerSaisieEtCalculer(g, req, v);
        return toDto(carburantRepo.save(g));
    }

    @Transactional
    public void supprimer(Long id) {
        if (!carburantRepo.existsById(id))
            throw new RuntimeException("Enregistrement non trouvé");
        carburantRepo.deleteById(id);
    }

    // ── Formules DAF 2026 ────────────────────────────────────────
    /**
     * 1) Total ravitaillement litres =
     *       (ravitaillement mois précédent + montant restant mois précédent) / prix
     *
     * 2) Quantité restante réservoir =
     *       montant restant mois précédent / prix
     *
     * 3) Distance parcourue =
     *       index fin mois - index démarrage mois
     *
     * 4) % consommation =
     *       (total litres - qté restante) / distance
     *
     * 5) Carburant demandé DT =
     *       coût du mois (budget) - montant restant mois précédent
     */
    private void appliquerSaisieEtCalculer(GestionCarburantVehicule g,
                                           CarburantVehiculeRequest req,
                                           Vehicule v) {
        g.setIndexDemarrageMois(req.getIndexDemarrageMois());
        g.setIndexFinMois(req.getIndexFinMois());
        g.setMontantRestantMoisPrecedent(req.getMontantRestantMoisPrecedent());
        g.setRavitaillementMoisPrecedent(req.getRavitaillementMoisPrecedent());
        g.setRavitaillementMois(req.getRavitaillementMois());

        double prix = v.getPrixCarburant();

        // Calcul 1
        double totalLitres = prix > 0
                ? (req.getRavitaillementMoisPrecedent() + req.getMontantRestantMoisPrecedent()) / prix
                : 0;
        g.setTotalRavitaillementLitres(round3(totalLitres));

        // Calcul 2
        double qteRestante = prix > 0
                ? req.getMontantRestantMoisPrecedent() / prix
                : 0;
        g.setQuantiteRestanteReservoir(round3(qteRestante));

        // Calcul 3
        double distance = req.getIndexFinMois() - req.getIndexDemarrageMois();
        g.setDistanceParcourue(round3(distance));

        // Calcul 4
        double pct = distance > 0
                ? (totalLitres - qteRestante) / distance
                : 0;
        g.setPourcentageConsommation(round3(pct));

        // Calcul 5
        double demande = v.getCoutDuMois() - req.getMontantRestantMoisPrecedent();
        g.setCarburantDemandeDinars(round3(demande));
    }

    private double round3(double val) {
        return Math.round(val * 1000.0) / 1000.0;
    }

    // ── Mapping ─────────────────────────────────────────────────

    private CarburantVehiculeDto toDto(GestionCarburantVehicule g) {
        CarburantVehiculeDto dto = new CarburantVehiculeDto();
        dto.setId(g.getId());
        dto.setVehiculeMatricule(g.getVehicule().getMatricule());
        dto.setVehiculeMarqueModele(g.getVehicule().getMarqueModele());
        dto.setVehiculeZoneNom(g.getVehicule().getZone() != null
                ? g.getVehicule().getZone().getNom() : null);
        dto.setPrixCarburant(g.getVehicule().getPrixCarburant());
        dto.setCoutDuMois(g.getVehicule().getCoutDuMois());
        dto.setAnnee(g.getAnnee());
        dto.setMois(g.getMois());
        dto.setIndexDemarrageMois(g.getIndexDemarrageMois());
        dto.setIndexFinMois(g.getIndexFinMois());
        dto.setMontantRestantMoisPrecedent(g.getMontantRestantMoisPrecedent());
        dto.setRavitaillementMoisPrecedent(g.getRavitaillementMoisPrecedent());
        dto.setRavitaillementMois(g.getRavitaillementMois());
        dto.setTotalRavitaillementLitres(g.getTotalRavitaillementLitres());
        dto.setQuantiteRestanteReservoir(g.getQuantiteRestanteReservoir());
        dto.setDistanceParcourue(g.getDistanceParcourue());
        dto.setPourcentageConsommation(g.getPourcentageConsommation());
        dto.setCarburantDemandeDinars(g.getCarburantDemandeDinars());
        return dto;
    }

    private Vehicule findVehicule(String matricule) {
        return vehiculeRepo.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
    }
}