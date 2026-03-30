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
import java.util.Optional;
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

    // ── NOUVEAU : récupérer les valeurs pré-remplies du mois précédent (règles 6 & 7) ──

    /**
     * Règle 6 : Index démarrage = Index fin du mois précédent
     * Règle 7 : Montant restant mois précédent = Montant restant réservoir du mois précédent
     * Retourne un DTO pré-rempli avec ces deux valeurs (0 si pas de mois précédent)
     */
    @Transactional(readOnly = true)
    public CarburantVehiculeDto getPrefillFromPreviousMonth(String matricule, int annee, int mois) {
        Vehicule v = findVehicule(matricule);

        // Calculer le mois précédent
        int prevMois  = (mois == 1) ? 12 : mois - 1;
        int prevAnnee = (mois == 1) ? annee - 1 : annee;

        CarburantVehiculeDto prefill = new CarburantVehiculeDto();
        prefill.setVehiculeMatricule(matricule);
        prefill.setAnnee(annee);
        prefill.setMois(mois);
        prefill.setPrixCarburant(v.getPrixCarburant());
        prefill.setCoutDuMois(v.getCoutDuMois());

        Optional<GestionCarburantVehicule> prevOpt =
                carburantRepo.findByVehiculeAndAnneeAndMois(v, prevAnnee, prevMois);

        if (prevOpt.isPresent()) {
            GestionCarburantVehicule prev = prevOpt.get();
            // Règle 6 : index démarrage = index fin du mois précédent
            prefill.setIndexDemarrageMois(prev.getIndexFinMois());
            // Règle 7 : montant restant = montant restant du mois précédent
            prefill.setMontantRestantMoisPrecedent(prev.getMontantRestantReservoirFin());
            // Ravitaillement précédent = ravitaillement du mois précédent
            prefill.setRavitaillementMoisPrecedent(prev.getRavitaillementMois());
        } else {
            prefill.setIndexDemarrageMois(0);
            prefill.setMontantRestantMoisPrecedent(0);
            prefill.setRavitaillementMoisPrecedent(0);
        }

        return prefill;
    }

    // ── NOUVEAU : récupérer les véhicules avec budget dépassé ──
    @Transactional(readOnly = true)
    public List<CarburantVehiculeDto> getBudgetDepasses(int annee, int mois) {
        return carburantRepo.findBudgetDepasses(annee, mois)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    // ── NOUVEAU : récapitulatif annuel par véhicule ──

    @Transactional(readOnly = true)
    public List<CarburantVehiculeDto> getRecapAnnuelByVehicule(String matricule, int annee) {
        Vehicule v = findVehicule(matricule);
        return carburantRepo.findByVehiculeAndAnneeOrderByMois(v, annee)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── NOUVEAU : récapitulatif annuel par zone ──

    @Transactional(readOnly = true)
    public List<CarburantVehiculeDto> getRecapAnnuelByZone(Long zoneId, int annee) {
        return carburantRepo.findByZoneAndAnnee(zoneId, annee)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Créer / Modifier ────────────────────────────────────────

    @Transactional
    public CarburantVehiculeDto saisir(CarburantVehiculeRequest req) {
        Vehicule v = findVehicule(req.getVehiculeMatricule());

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

    private void appliquerSaisieEtCalculer(GestionCarburantVehicule g,
                                           CarburantVehiculeRequest req,
                                           Vehicule v) {
        g.setIndexDemarrageMois(req.getIndexDemarrageMois());
        g.setIndexFinMois(req.getIndexFinMois());
        g.setMontantRestantMoisPrecedent(req.getMontantRestantMoisPrecedent());
        g.setRavitaillementMoisPrecedent(req.getRavitaillementMoisPrecedent());
        g.setRavitaillementMois(req.getRavitaillementMois());

        double prix = v.getPrixCarburant();

        // Formule 1 : Total ravitaillement litres
        double totalLitres = prix > 0
                ? (req.getRavitaillementMoisPrecedent() + req.getMontantRestantMoisPrecedent()) / prix
                : 0;
        g.setTotalRavitaillementLitres(round3(totalLitres));

        // Formule 2 : Quantité restante réservoir
        double qteRestante = prix > 0
                ? req.getMontantRestantMoisPrecedent() / prix
                : 0;
        g.setQuantiteRestanteReservoir(round3(qteRestante));

        // Formule 3 : Distance parcourue
        double distance = req.getIndexFinMois() - req.getIndexDemarrageMois();
        g.setDistanceParcourue(round3(distance));

        // Formule 4 : % consommation
        double pct = distance > 0
                ? (totalLitres - qteRestante) / distance
                : 0;
        g.setPourcentageConsommation(round3(pct));

        // Formule 5 : Carburant demandé DT
        double demande = v.getCoutDuMois() - req.getMontantRestantMoisPrecedent();
        g.setCarburantDemandeDinars(round3(demande));

        // ── NOUVEAU : montant restant réservoir FIN de ce mois (pour règle 7 du mois suivant) ──
        // = ravitaillement du mois en cours + montant restant précédent - consommation réelle
        // Consommation réelle = (totalLitres - qteRestante) * prix
        double consommationDT = round3((totalLitres - qteRestante) * prix);
        double montantRestantFin = req.getRavitaillementMois()
                + req.getMontantRestantMoisPrecedent()
                - consommationDT;
        g.setMontantRestantReservoirFin(round3(Math.max(0, montantRestantFin)));

        // ── NOUVEAU : alerte budget dépassé ──
        double coutReel = (totalLitres - qteRestante) * prix;
        g.setBudgetDepasse(coutReel > v.getCoutDuMois() && v.getCoutDuMois() > 0);
        g.setDepassementMontant(round3(Math.max(0, coutReel - v.getCoutDuMois())));
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
        dto.setMontantRestantReservoirFin(g.getMontantRestantReservoirFin());
        dto.setBudgetDepasse(g.isBudgetDepasse());
        dto.setDepassementMontant(g.getDepassementMontant());
        return dto;
    }

    private Vehicule findVehicule(String matricule) {
        return vehiculeRepo.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
    }
}