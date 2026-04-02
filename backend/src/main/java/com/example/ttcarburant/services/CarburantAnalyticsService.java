package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.analytics.*;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarburantAnalyticsService {

    // Seuils de détection d'anomalies
    private static final double SEUIL_CONSO_MAX_L_100KM = 15.0;  // > 15L/100km = anomalie
    private static final double SEUIL_CONSO_MIN_L_100KM = 3.0;   // < 3L/100km  = incohérence
    private static final double SEUIL_KM_MAX_MENSUEL    = 5000.0; // > 5000km/mois = suspect
    private static final double SEUIL_BUDGET_DEPASSE_CRITIQUE = 1.5; // > 150% = critique
    private static final double SEUIL_ECART_BUDGET      = 1.2;   // > 120% = alerte budget

    private static final String[] MOIS_LABELS = {
            "", "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    private final CarburantVehiculeRepository carburantRepo;
    private final VehiculeRepository vehiculeRepo;
    private final ZoneRepository zoneRepo;

    public CarburantAnalyticsService(CarburantVehiculeRepository carburantRepo,
                                     VehiculeRepository vehiculeRepo,
                                     ZoneRepository zoneRepo) {
        this.carburantRepo = carburantRepo;
        this.vehiculeRepo  = vehiculeRepo;
        this.zoneRepo      = zoneRepo;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. HISTORIQUE RAVITAILLEMENT DÉTAILLÉ
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<HistoriqueRavitaillementDto> getHistoriqueRavitaillement(String matricule, Integer annee) {
        Vehicule v = vehiculeRepo.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

        List<GestionCarburantVehicule> data;
        if (annee != null) {
            data = carburantRepo.findByVehiculeAndAnneeOrderByMois(v, annee);
        } else {
            data = carburantRepo.findByVehiculeOrderByAnneeDescMoisDesc(v);
        }

        return data.stream().map(g -> toHistoriqueDto(g, true)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistoriqueRavitaillementDto> getHistoriqueZone(Long zoneId, int annee) {
        List<GestionCarburantVehicule> data = carburantRepo.findByZoneAndAnnee(zoneId, annee);
        return data.stream().map(g -> toHistoriqueDto(g, true)).collect(Collectors.toList());
    }

    private HistoriqueRavitaillementDto toHistoriqueDto(GestionCarburantVehicule g, boolean analyseStatut) {
        HistoriqueRavitaillementDto dto = new HistoriqueRavitaillementDto();
        dto.setId(g.getId());
        dto.setVehiculeMatricule(g.getVehicule().getMatricule());
        dto.setVehiculeMarqueModele(g.getVehicule().getMarqueModele());
        dto.setVehiculeZoneNom(g.getVehicule().getZone() != null ? g.getVehicule().getZone().getNom() : null);
        dto.setAnnee(g.getAnnee());
        dto.setMois(g.getMois());
        dto.setPeriodeLabel(MOIS_LABELS[g.getMois()] + " " + g.getAnnee());
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
        dto.setCoutDuMois(g.getVehicule().getCoutDuMois());
        dto.setPrixCarburant(g.getVehicule().getPrixCarburant());
        dto.setBudgetDepasse(g.isBudgetDepasse());
        dto.setDepassementMontant(g.getDepassementMontant());
        dto.setDateCreation(g.getDateCreation());

        // Taux budget
        double cout = g.getVehicule().getCoutDuMois();
        double coutReel = (g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())
                * g.getVehicule().getPrixCarburant();
        dto.setTauxBudget(cout > 0 ? round2(coutReel / cout * 100) : 0);

        // Statut
        if (analyseStatut) {
            dto.setStatut(determinerStatut(g));
        }
        return dto;
    }

    private String determinerStatut(GestionCarburantVehicule g) {
        if (g.isBudgetDepasse()) {
            double ratio = g.getVehicule().getCoutDuMois() > 0
                    ? (g.getVehicule().getCoutDuMois() + g.getDepassementMontant()) / g.getVehicule().getCoutDuMois()
                    : 0;
            return ratio > SEUIL_BUDGET_DEPASSE_CRITIQUE ? "CRITIQUE" : "ALERTE_BUDGET";
        }
        double km = g.getDistanceParcourue();
        if (km > SEUIL_KM_MAX_MENSUEL) return "ANOMALIE_KM";
        double consoL100 = km > 0
                ? (g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir()) / km * 100
                : 0;
        if (consoL100 > SEUIL_CONSO_MAX_L_100KM) return "ANOMALIE_CONSO";
        if (km > 10 && consoL100 < SEUIL_CONSO_MIN_L_100KM && consoL100 > 0) return "ANOMALIE_CONSO";
        return "NORMAL";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. GRAPHIQUES D'ÉVOLUTION
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public EvolutionDto getEvolutionVehicule(String matricule, int annee) {
        Vehicule v = vehiculeRepo.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

        List<GestionCarburantVehicule> data = carburantRepo.findByVehiculeAndAnneeOrderByMois(v, annee);
        Map<Integer, GestionCarburantVehicule> parMois = data.stream()
                .collect(Collectors.toMap(GestionCarburantVehicule::getMois, g -> g));

        return buildEvolutionDto(v.getMarqueModele() + " " + v.getMatricule(), annee,
                parMois, v.getCoutDuMois(), v.getPrixCarburant());
    }

    @Transactional(readOnly = true)
    public EvolutionDto getEvolutionZone(Long zoneId, int annee) {
        Zone zone = zoneRepo.findById(zoneId).orElseThrow(() -> new RuntimeException("Zone non trouvée"));
        List<GestionCarburantVehicule> data = carburantRepo.findByZoneAndAnnee(zoneId, annee);

        // Agréger par mois
        Map<Integer, List<GestionCarburantVehicule>> parMoisGroupe = data.stream()
                .collect(Collectors.groupingBy(GestionCarburantVehicule::getMois));

        List<String> labels = new ArrayList<>();
        List<Double> consoL = new ArrayList<>();
        List<Double> cout   = new ArrayList<>();
        List<Double> km     = new ArrayList<>();
        List<Double> pct    = new ArrayList<>();
        List<Double> budget = new ArrayList<>();

        double totalKm = 0, totalL = 0, totalC = 0;

        for (int m = 1; m <= 12; m++) {
            labels.add(MOIS_LABELS[m].substring(0, 3));
            List<GestionCarburantVehicule> moisData = parMoisGroupe.getOrDefault(m, Collections.emptyList());

            double mL = moisData.stream().mapToDouble(g ->
                    Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())).sum();
            double mC = moisData.stream().mapToDouble(g ->
                    Math.max(0, (g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir()) * g.getVehicule().getPrixCarburant())).sum();
            double mK = moisData.stream().mapToDouble(GestionCarburantVehicule::getDistanceParcourue).sum();
            double mB = moisData.stream().mapToDouble(g -> g.getVehicule().getCoutDuMois()).sum();
            double mP = mK > 0 ? round3(mL / mK * 100) : 0;

            consoL.add(round3(mL));
            cout.add(round3(mC));
            km.add(round3(mK));
            pct.add(mP);
            budget.add(round3(mB));
            totalKm += mK; totalL += mL; totalC += mC;
        }

        EvolutionDto dto = new EvolutionDto();
        dto.setTitre("Évolution Zone " + zone.getNom() + " — " + annee);
        dto.setAnnee(annee);
        dto.setLabels(labels);
        dto.setConsommationLitres(consoL);
        dto.setCoutDinars(cout);
        dto.setKmParcourus(km);
        dto.setPourcentageConso(pct);
        dto.setBudgetMensuel(budget);
        dto.setTotalKm(round3(totalKm));
        dto.setTotalLitres(round3(totalL));
        dto.setTotalCout(round3(totalC));
        dto.setMoyenneConsommation(totalKm > 0 ? round3(totalL / totalKm * 100) : 0);
        return dto;
    }

    private EvolutionDto buildEvolutionDto(String titre, int annee,
                                           Map<Integer, GestionCarburantVehicule> parMois,
                                           double coutMensuel, double prix) {
        List<String> labels = new ArrayList<>();
        List<Double> consoL = new ArrayList<>();
        List<Double> cout   = new ArrayList<>();
        List<Double> km     = new ArrayList<>();
        List<Double> pct    = new ArrayList<>();
        List<Double> budget = new ArrayList<>();

        double totalKm = 0, totalL = 0, totalC = 0;

        for (int m = 1; m <= 12; m++) {
            labels.add(MOIS_LABELS[m].substring(0, 3));
            GestionCarburantVehicule g = parMois.get(m);
            if (g != null) {
                double mL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
                double mC = mL * prix;
                double mK = g.getDistanceParcourue();
                consoL.add(round3(mL));
                cout.add(round3(mC));
                km.add(round3(mK));
                pct.add(mK > 0 ? round3(mL / mK * 100) : 0.0);
                totalKm += mK; totalL += mL; totalC += mC;
            } else {
                consoL.add(0.0); cout.add(0.0); km.add(0.0); pct.add(0.0);
            }
            budget.add(coutMensuel);
        }

        EvolutionDto dto = new EvolutionDto();
        dto.setTitre(titre + " — " + annee);
        dto.setAnnee(annee);
        dto.setLabels(labels);
        dto.setConsommationLitres(consoL);
        dto.setCoutDinars(cout);
        dto.setKmParcourus(km);
        dto.setPourcentageConso(pct);
        dto.setBudgetMensuel(budget);
        dto.setTotalKm(round3(totalKm));
        dto.setTotalLitres(round3(totalL));
        dto.setTotalCout(round3(totalC));
        dto.setMoyenneConsommation(totalKm > 0 ? round3(totalL / totalKm * 100) : 0);
        return dto;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. DÉTECTION D'ANOMALIES
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<AnomalieDto> detecterAnomalies(int annee, int mois, Long zoneId) {
        List<GestionCarburantVehicule> data = zoneId != null
                ? carburantRepo.findByZoneAndPeriode(zoneId, annee, mois)
                : carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, mois);

        List<AnomalieDto> anomalies = new ArrayList<>();
        for (GestionCarburantVehicule g : data) {
            anomalies.addAll(analyserAnomalie(g));
        }
        anomalies.sort(Comparator.comparing(AnomalieDto::getSeverite).reversed());
        return anomalies;
    }

    @Transactional(readOnly = true)
    public List<AnomalieDto> detecterAnomaliesAnnee(int annee, Long zoneId) {
        List<AnomalieDto> anomalies = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            List<GestionCarburantVehicule> data = zoneId != null
                    ? carburantRepo.findByZoneAndPeriode(zoneId, annee, m)
                    : carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, m);
            for (GestionCarburantVehicule g : data) {
                anomalies.addAll(analyserAnomalie(g));
            }
        }
        anomalies.sort(Comparator.comparing(AnomalieDto::getSeverite).reversed());
        return anomalies;
    }

    private List<AnomalieDto> analyserAnomalie(GestionCarburantVehicule g) {
        List<AnomalieDto> list = new ArrayList<>();
        double km = g.getDistanceParcourue();
        double prix = g.getVehicule().getPrixCarburant();
        double budget = g.getVehicule().getCoutDuMois();
        double consoL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
        double coutReel = consoL * prix;
        double consoL100 = km > 10 ? consoL / km * 100 : 0;

        // Anomalie 1 : Budget dépassé
        if (g.isBudgetDepasse() && budget > 0) {
            AnomalieDto a = buildAnomalieBase(g);
            a.setTypeAnomalie("BUDGET_DEPASSE");
            double ratio = coutReel / budget;
            a.setSeverite(ratio > SEUIL_BUDGET_DEPASSE_CRITIQUE ? "CRITIQUE" : "ELEVEE");
            a.setValeurReelle(round3(coutReel));
            a.setValeurSeuil(budget);
            a.setEcart(round3(g.getDepassementMontant()));
            a.setEcartPourcentage(round2((ratio - 1) * 100));
            a.setDescription("Coût réel " + round2(coutReel) + " DT > Budget " + budget + " DT (+" + round2(g.getDepassementMontant()) + " DT)");
            list.add(a);
        }

        // Anomalie 2 : Consommation anormale (trop haute)
        if (km > 10 && consoL100 > SEUIL_CONSO_MAX_L_100KM) {
            AnomalieDto a = buildAnomalieBase(g);
            a.setTypeAnomalie("CONSO_ANORMALE");
            a.setSeverite(consoL100 > SEUIL_CONSO_MAX_L_100KM * 1.5 ? "CRITIQUE" : "ELEVEE");
            a.setValeurReelle(round3(consoL100));
            a.setValeurSeuil(SEUIL_CONSO_MAX_L_100KM);
            a.setEcart(round3(consoL100 - SEUIL_CONSO_MAX_L_100KM));
            a.setEcartPourcentage(round2((consoL100 / SEUIL_CONSO_MAX_L_100KM - 1) * 100));
            a.setDescription("Consommation " + round3(consoL100) + " L/100km > seuil " + SEUIL_CONSO_MAX_L_100KM + " L/100km");
            list.add(a);
        }

        // Anomalie 3 : Consommation trop basse (données incohérentes)
        if (km > 10 && consoL100 > 0 && consoL100 < SEUIL_CONSO_MIN_L_100KM) {
            AnomalieDto a = buildAnomalieBase(g);
            a.setTypeAnomalie("KM_INCOHERENT");
            a.setSeverite("MOYENNE");
            a.setValeurReelle(round3(consoL100));
            a.setValeurSeuil(SEUIL_CONSO_MIN_L_100KM);
            a.setEcart(round3(SEUIL_CONSO_MIN_L_100KM - consoL100));
            a.setEcartPourcentage(round2((1 - consoL100 / SEUIL_CONSO_MIN_L_100KM) * 100));
            a.setDescription("Consommation anormalement basse " + round3(consoL100) + " L/100km (index suspect)");
            list.add(a);
        }

        // Anomalie 4 : Km parcourus excessifs
        if (km > SEUIL_KM_MAX_MENSUEL) {
            AnomalieDto a = buildAnomalieBase(g);
            a.setTypeAnomalie("KM_ELEVE");
            a.setSeverite("MOYENNE");
            a.setValeurReelle(km);
            a.setValeurSeuil(SEUIL_KM_MAX_MENSUEL);
            a.setEcart(round3(km - SEUIL_KM_MAX_MENSUEL));
            a.setEcartPourcentage(round2((km / SEUIL_KM_MAX_MENSUEL - 1) * 100));
            a.setDescription("Distance mensuelle " + round0(km) + " km > seuil " + round0(SEUIL_KM_MAX_MENSUEL) + " km");
            list.add(a);
        }

        return list;
    }

    private AnomalieDto buildAnomalieBase(GestionCarburantVehicule g) {
        AnomalieDto a = new AnomalieDto();
        a.setVehiculeMatricule(g.getVehicule().getMatricule());
        a.setVehiculeMarqueModele(g.getVehicule().getMarqueModele());
        a.setVehiculeZoneNom(g.getVehicule().getZone() != null ? g.getVehicule().getZone().getNom() : null);
        a.setAnnee(g.getAnnee());
        a.setMois(g.getMois());
        a.setPeriodeLabel(MOIS_LABELS[g.getMois()] + " " + g.getAnnee());
        return a;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. COMPARAISON ENTRE VÉHICULES
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ComparaisonDto getComparaisonVehicules(int annee, int mois, Long zoneId) {
        List<GestionCarburantVehicule> data = zoneId != null
                ? carburantRepo.findByZoneAndPeriode(zoneId, annee, mois)
                : carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, mois);

        return buildComparaison(data);
    }

    @Transactional(readOnly = true)
    public ComparaisonDto getComparaisonAnnuelle(int annee, Long zoneId) {
        List<GestionCarburantVehicule> data = new ArrayList<>();
        if (zoneId != null) {
            data = carburantRepo.findByZoneAndAnnee(zoneId, annee);
        } else {
            for (int m = 1; m <= 12; m++) {
                data.addAll(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, m));
            }
        }
        return buildComparaison(data);
    }

    private ComparaisonDto buildComparaison(List<GestionCarburantVehicule> data) {
        // Agréger par véhicule
        Map<String, List<GestionCarburantVehicule>> parVehicule = data.stream()
                .collect(Collectors.groupingBy(g -> g.getVehicule().getMatricule()));

        List<ComparaisonDto.VehiculeRank> ranks = new ArrayList<>();

        for (Map.Entry<String, List<GestionCarburantVehicule>> entry : parVehicule.entrySet()) {
            List<GestionCarburantVehicule> vehiculeData = entry.getValue();
            GestionCarburantVehicule first = vehiculeData.get(0);

            double totalKm = vehiculeData.stream().mapToDouble(GestionCarburantVehicule::getDistanceParcourue).sum();
            double totalL  = vehiculeData.stream().mapToDouble(g ->
                    Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())).sum();
            double totalC  = vehiculeData.stream().mapToDouble(g ->
                    Math.max(0, (g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir()) * g.getVehicule().getPrixCarburant())).sum();

            int nbAnomalies = (int) vehiculeData.stream().filter(GestionCarburantVehicule::isBudgetDepasse).count();
            // compter aussi anomalies conso
            for (GestionCarburantVehicule g : vehiculeData) {
                double km = g.getDistanceParcourue();
                double consoL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
                double consoL100 = km > 10 ? consoL / km * 100 : 0;
                if (consoL100 > SEUIL_CONSO_MAX_L_100KM) nbAnomalies++;
            }

            double rendL100 = totalKm > 0 ? round3(totalL / totalKm * 100) : 0;
            double rendDT100 = totalKm > 0 ? round3(totalC / totalKm * 100) : 0;
            double budget = first.getVehicule().getCoutDuMois() * vehiculeData.size();
            double tauxBudget = budget > 0 ? round2(totalC / budget * 100) : 0;

            ComparaisonDto.VehiculeRank rank = new ComparaisonDto.VehiculeRank();
            rank.setMatricule(entry.getKey());
            rank.setMarqueModele(first.getVehicule().getMarqueModele());
            rank.setZoneNom(first.getVehicule().getZone() != null ? first.getVehicule().getZone().getNom() : null);
            rank.setTotalKm(round3(totalKm));
            rank.setTotalLitres(round3(totalL));
            rank.setTotalCout(round3(totalC));
            rank.setRendementLPour100km(rendL100);
            rank.setRendementDTPour100km(rendDT100);
            rank.setTauxBudgetMoyen(tauxBudget);
            rank.setNbMoisSaisis(vehiculeData.size());
            rank.setNbAnomalies(nbAnomalies);
            ranks.add(rank);
        }

        // Moyenne globale
        double moyRendement = ranks.stream().filter(r -> r.getRendementLPour100km() > 0)
                .mapToDouble(ComparaisonDto.VehiculeRank::getRendementLPour100km).average().orElse(0);

        ComparaisonDto dto = new ComparaisonDto();

        // Top 5 consommation (en DT)
        dto.setTop5Consommation(ranks.stream()
                .sorted(Comparator.comparingDouble(ComparaisonDto.VehiculeRank::getTotalCout).reversed())
                .limit(5).collect(Collectors.toList()));

        // Top 5 km parcourus
        dto.setTop5KmParcourus(ranks.stream()
                .sorted(Comparator.comparingDouble(ComparaisonDto.VehiculeRank::getTotalKm).reversed())
                .limit(5).collect(Collectors.toList()));

        // Meilleurs rendements (moins de L/100km, km > 0)
        dto.setMeilleursRendements(ranks.stream()
                .filter(r -> r.getRendementLPour100km() > SEUIL_CONSO_MIN_L_100KM && r.getTotalKm() > 50)
                .sorted(Comparator.comparingDouble(ComparaisonDto.VehiculeRank::getRendementLPour100km))
                .limit(5).collect(Collectors.toList()));

        // Pires rendements (plus de L/100km)
        dto.setPiresRendements(ranks.stream()
                .filter(r -> r.getRendementLPour100km() > 0 && r.getTotalKm() > 50)
                .sorted(Comparator.comparingDouble(ComparaisonDto.VehiculeRank::getRendementLPour100km).reversed())
                .limit(5).collect(Collectors.toList()));

        // Budget dépassé
        dto.setPlusGrandsBudgetDepasses(ranks.stream()
                .filter(r -> r.getNbAnomalies() > 0)
                .sorted(Comparator.comparingDouble(ComparaisonDto.VehiculeRank::getTauxBudgetMoyen).reversed())
                .limit(5).collect(Collectors.toList()));

        dto.setMoyenneGlobaleRendement(round3(moyRendement));
        dto.setSeuilRendementAnomalie(SEUIL_CONSO_MAX_L_100KM);
        return dto;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. DASHBOARD AVANCÉ CARBURANT
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardCarburantDto getDashboardAvance(int annee, int mois, Long zoneId) {
        List<GestionCarburantVehicule> data = zoneId != null
                ? carburantRepo.findByZoneAndPeriode(zoneId, annee, mois)
                : carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, mois);

        return buildDashboard(data, annee, false);
    }

    @Transactional(readOnly = true)
    public DashboardCarburantDto getDashboardAnnuel(int annee, Long zoneId) {
        List<GestionCarburantVehicule> allData = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            List<GestionCarburantVehicule> moisData = zoneId != null
                    ? carburantRepo.findByZoneAndPeriode(zoneId, annee, m)
                    : carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, m);
            allData.addAll(moisData);
        }
        return buildDashboard(allData, annee, true);
    }

    private DashboardCarburantDto buildDashboard(List<GestionCarburantVehicule> data, int annee, boolean annuel) {
        DashboardCarburantDto dto = new DashboardCarburantDto();

        double totalKm = 0, totalL = 0, totalC = 0, totalBudget = 0;
        int nbBudgetDepasses = 0;
        Set<String> vehiculesVus = new HashSet<>();
        Map<String, Double> consoParType = new LinkedHashMap<>();

        // Stats par zone
        Map<Long, DashboardCarburantDto.ZoneStat> statsZone = new LinkedHashMap<>();
        // Évolution mensuelle
        Map<Integer, DashboardCarburantDto.MoisStat> statsMois = new LinkedHashMap<>();

        for (GestionCarburantVehicule g : data) {
            double km = g.getDistanceParcourue();
            double prix = g.getVehicule().getPrixCarburant();
            double consoL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
            double coutReel = consoL * prix;
            double coutMensuel = g.getVehicule().getCoutDuMois();

            totalKm     += km;
            totalL      += consoL;
            totalC      += coutReel;
            totalBudget += coutMensuel;
            vehiculesVus.add(g.getVehicule().getMatricule());
            if (g.isBudgetDepasse()) nbBudgetDepasses++;

            // Consommation par type carburant
            String type = g.getVehicule().getTypeCarburant().name();
            consoParType.merge(type, consoL, Double::sum);

            // Stats par zone
            if (g.getVehicule().getZone() != null) {
                Long zoneId2 = g.getVehicule().getZone().getId();
                DashboardCarburantDto.ZoneStat zs = statsZone.computeIfAbsent(zoneId2, k -> {
                    DashboardCarburantDto.ZoneStat z = new DashboardCarburantDto.ZoneStat();
                    z.setZoneId(k);
                    z.setZoneNom(g.getVehicule().getZone().getNom());
                    return z;
                });
                zs.setNbVehicules(zs.getNbVehicules() + 1);
                zs.setTotalKm(zs.getTotalKm() + km);
                zs.setTotalLitres(zs.getTotalLitres() + consoL);
                zs.setTotalCout(zs.getTotalCout() + coutReel);
                zs.setTotalBudget(zs.getTotalBudget() + coutMensuel);
                if (g.isBudgetDepasse()) zs.setNbAnomalies(zs.getNbAnomalies() + 1);
            }

            // Évolution mensuelle
            if (annuel) {
                int m = g.getMois();
                DashboardCarburantDto.MoisStat ms = statsMois.computeIfAbsent(m, k -> {
                    DashboardCarburantDto.MoisStat s = new DashboardCarburantDto.MoisStat();
                    s.setMois(k);
                    s.setLabel(MOIS_LABELS[k]);
                    return s;
                });
                ms.setTotalKm(ms.getTotalKm() + km);
                ms.setTotalLitres(ms.getTotalLitres() + consoL);
                ms.setTotalCout(ms.getTotalCout() + coutReel);
                ms.setTotalBudget(ms.getTotalBudget() + coutMensuel);
                ms.setNbVehicules(ms.getNbVehicules() + 1);
            }
        }

        // Finaliser stats zones
        List<DashboardCarburantDto.ZoneStat> zoneList = new ArrayList<>(statsZone.values());
        for (DashboardCarburantDto.ZoneStat zs : zoneList) {
            double tb = zs.getTotalBudget() > 0 ? round2(zs.getTotalCout() / zs.getTotalBudget() * 100) : 0;
            zs.setTauxBudget(tb);
            double rendZ = zs.getTotalKm() > 0 ? round3(zs.getTotalLitres() / zs.getTotalKm() * 100) : 0;
            zs.setRendementMoyen(rendZ);
        }
        zoneList.sort(Comparator.comparingDouble(DashboardCarburantDto.ZoneStat::getTotalCout).reversed());

        // Évolution mensuelle triée
        List<DashboardCarburantDto.MoisStat> moisList = new ArrayList<>(statsMois.values());
        moisList.sort(Comparator.comparingInt(DashboardCarburantDto.MoisStat::getMois));

        // Anomalies
        List<AnomalieDto> anomalies = new ArrayList<>();
        for (GestionCarburantVehicule g : data) {
            anomalies.addAll(analyserAnomalie(g));
        }
        anomalies.sort(Comparator.comparing(AnomalieDto::getSeverite).reversed());

        // Finaliser DTO
        dto.setNbVehiculesSaisis(vehiculesVus.size());
        dto.setTotalKm(round3(totalKm));
        dto.setTotalLitres(round3(totalL));
        dto.setTotalCoutDT(round3(totalC));
        dto.setTotalBudgetDT(round3(totalBudget));
        dto.setTauxBudgetGlobal(totalBudget > 0 ? round2(totalC / totalBudget * 100) : 0);
        dto.setNbBudgetsDepasses(nbBudgetDepasses);
        dto.setNbAnomalies(anomalies.size());
        dto.setStatsParZone(zoneList);
        dto.setEvolutionMensuelle(moisList);
        // Arrondir consommation par type
        Map<String, Double> consoArrondie = new LinkedHashMap<>();
        consoParType.forEach((k, v) -> consoArrondie.put(k, round3(v)));
        dto.setConsommationParTypeCarburant(consoArrondie);
        dto.setDernieresAnomalies(anomalies.stream().limit(10).collect(Collectors.toList()));

        return dto;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round0(double v) { return Math.round(v); }
}