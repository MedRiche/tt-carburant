package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.HistoriqueModificationDto;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.HistoriqueModificationCarburant;
import com.example.ttcarburant.repository.HistoriqueModificationCarburantRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoriqueModificationService {

    private static final String[] MOIS_LABELS = {
            "", "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    private final HistoriqueModificationCarburantRepository historiqueRepo;
    private final VehiculeRepository vehiculeRepo;

    public HistoriqueModificationService(HistoriqueModificationCarburantRepository historiqueRepo,
                                         VehiculeRepository vehiculeRepo) {
        this.historiqueRepo = historiqueRepo;
        this.vehiculeRepo = vehiculeRepo;
    }

    // ── Enregistrer une action ─────────────────────────────────────

    @Transactional
    public void enregistrerCreation(GestionCarburantVehicule g) {
        String email = getEmailCourant();
        HistoriqueModificationCarburant h = new HistoriqueModificationCarburant();
        h.setGestionId(g.getId());
        h.setVehiculeMatricule(g.getVehicule().getMatricule());
        h.setAnnee(g.getAnnee());
        h.setMois(g.getMois());
        h.setAction("CREATE");
        h.setModifiePar(email);
        h.setValeursApres(toJson(g));
        h.setDescription("Création de la saisie carburant " + MOIS_LABELS[g.getMois()] + " " + g.getAnnee()
                + " pour " + g.getVehicule().getMatricule());
        historiqueRepo.save(h);
    }

    @Transactional
    public void enregistrerModification(GestionCarburantVehicule avant, GestionCarburantVehicule apres) {
        String email = getEmailCourant();
        HistoriqueModificationCarburant h = new HistoriqueModificationCarburant();
        h.setGestionId(apres.getId());
        h.setVehiculeMatricule(apres.getVehicule().getMatricule());
        h.setAnnee(apres.getAnnee());
        h.setMois(apres.getMois());
        h.setAction("UPDATE");
        h.setModifiePar(email);
        h.setValeursAvant(toJson(avant));
        h.setValeursApres(toJson(apres));
        h.setDescription("Modification de la saisie carburant " + MOIS_LABELS[apres.getMois()] + " " + apres.getAnnee()
                + " pour " + apres.getVehicule().getMatricule());
        historiqueRepo.save(h);
    }

    @Transactional
    public void enregistrerSuppression(GestionCarburantVehicule g) {
        String email = getEmailCourant();
        HistoriqueModificationCarburant h = new HistoriqueModificationCarburant();
        h.setGestionId(g.getId());
        h.setVehiculeMatricule(g.getVehicule().getMatricule());
        h.setAnnee(g.getAnnee());
        h.setMois(g.getMois());
        h.setAction("DELETE");
        h.setModifiePar(email);
        h.setValeursAvant(toJson(g));
        h.setDescription("Suppression de la saisie carburant " + MOIS_LABELS[g.getMois()] + " " + g.getAnnee()
                + " pour " + g.getVehicule().getMatricule());
        historiqueRepo.save(h);
    }

    // ── Lire l'historique ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HistoriqueModificationDto> getHistoriqueParGestionId(Long gestionId) {
        return historiqueRepo.findByGestionIdOrderByModifieLeDesc(gestionId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationDto> getHistoriqueParVehicule(String matricule) {
        return historiqueRepo.findByVehiculeMatriculeOrderByModifieLeDesc(matricule)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationDto> getHistoriqueParPeriode(int annee, int mois) {
        return historiqueRepo.findByAnneeAndMoisOrderByModifieLeDesc(annee, mois)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationDto> getToutHistorique() {
        return historiqueRepo.findAllOrderByDateDesc()
                .stream().limit(200).map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationDto> getHistoriqueParVehiculeEtAnnee(String matricule, int annee) {
        return historiqueRepo.findByVehiculeMatriculeAndAnneeOrderByModifieLeDesc(matricule, annee)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String getEmailCourant() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private String toJson(GestionCarburantVehicule g) {
        return String.format(
                "{\"id\":%d,\"indexDemarrageMois\":%.3f,\"indexFinMois\":%.3f," +
                        "\"montantRestantMoisPrecedent\":%.3f,\"ravitaillementMoisPrecedent\":%.3f," +
                        "\"ravitaillementMois\":%.3f,\"totalRavitaillementLitres\":%.3f," +
                        "\"quantiteRestanteReservoir\":%.3f,\"distanceParcourue\":%.3f," +
                        "\"pourcentageConsommation\":%.6f,\"carburantDemandeDinars\":%.3f," +
                        "\"montantRestantReservoirFin\":%.3f,\"budgetDepasse\":%b,\"depassementMontant\":%.3f}",
                g.getId(),
                g.getIndexDemarrageMois(), g.getIndexFinMois(),
                g.getMontantRestantMoisPrecedent(), g.getRavitaillementMoisPrecedent(),
                g.getRavitaillementMois(), g.getTotalRavitaillementLitres(),
                g.getQuantiteRestanteReservoir(), g.getDistanceParcourue(),
                g.getPourcentageConsommation(), g.getCarburantDemandeDinars(),
                g.getMontantRestantReservoirFin(), g.isBudgetDepasse(), g.getDepassementMontant()
        );
    }

    private HistoriqueModificationDto toDto(HistoriqueModificationCarburant h) {
        HistoriqueModificationDto dto = new HistoriqueModificationDto();
        dto.setId(h.getId());
        dto.setGestionId(h.getGestionId());
        dto.setVehiculeMatricule(h.getVehiculeMatricule());
        dto.setAnnee(h.getAnnee());
        dto.setMois(h.getMois());
        dto.setPeriodeLabel(MOIS_LABELS[h.getMois()] + " " + h.getAnnee());
        dto.setAction(h.getAction());
        dto.setActionLabel(switch (h.getAction()) {
            case "CREATE" -> "Créé";
            case "UPDATE" -> "Modifié";
            case "DELETE" -> "Supprimé";
            default -> h.getAction();
        });
        dto.setModifiePar(h.getModifiePar());
        dto.setModifieLe(h.getModifieLe());
        dto.setValeursAvant(h.getValeursAvant());
        dto.setValeursApres(h.getValeursApres());
        dto.setDescription(h.getDescription());

        // Enrichir avec marque/modèle si disponible
        vehiculeRepo.findById(h.getVehiculeMatricule())
                .ifPresent(v -> dto.setVehiculeMarqueModele(v.getMarqueModele()));

        return dto;
    }
}