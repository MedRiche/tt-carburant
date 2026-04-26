package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;
import com.example.ttcarburant.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

/**
 * Service de création automatique des comptes utilisateurs
 * à partir des noms/prénoms des conducteurs importés depuis Excel.
 *
 * Algorithme :
 *   1. Normaliser prénom + nom → supprimer accents, espaces → minuscules
 *   2. Générer email : prenom.nom@tunisietelecom.tn
 *   3. Si email déjà pris → ajouter un suffixe numérique
 *   4. Créer compte avec StatutCompte = EN_ATTENTE
 *   5. Mot de passe par défaut = "TT@2026!" (hashé)
 *   6. Retourner la liste des comptes créés / existants
 */
@Service
public class ConducteurUserCreationService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public ConducteurUserCreationService(UtilisateurRepository utilisateurRepository,
                                         PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Résultat de la création d'un utilisateur conducteur.
     */
    public static class ConducteurCreationResult {
        public final String nomComplet;
        public final String email;
        public final String statut; // "CREATED" | "ALREADY_EXISTS" | "SKIPPED"
        public final Long userId;

        public ConducteurCreationResult(String nomComplet, String email, String statut, Long userId) {
            this.nomComplet = nomComplet;
            this.email = email;
            this.statut = statut;
            this.userId = userId;
        }
    }

    /**
     * Traite une liste de conducteurs (prenom + nom) et crée les comptes manquants.
     *
     * @param conducteurs  liste de Map avec clés "prenom" et "nom" (peuvent être null)
     * @return liste de résultats par conducteur
     */
    @Transactional
    public List<ConducteurCreationResult> creerComptesConducteurs(
            List<Map<String, String>> conducteurs) {

        List<ConducteurCreationResult> results = new ArrayList<>();
        // Évite les doublons dans le même batch
        Set<String> processedEmails = new HashSet<>();

        for (Map<String, String> conducteur : conducteurs) {
            String prenom = trim(conducteur.get("prenom"));
            String nom    = trim(conducteur.get("nom"));

            // Ignorer les lignes sans conducteur
            if (prenom.isEmpty() && nom.isEmpty()) continue;

            String nomComplet = (prenom + " " + nom).trim();
            String email      = genererEmail(prenom, nom, processedEmails);

            // Déjà traité dans ce batch
            if (processedEmails.contains(email) && results.stream()
                    .anyMatch(r -> r.email.equals(email))) {
                results.add(new ConducteurCreationResult(nomComplet, email, "SKIPPED", null));
                continue;
            }

            // Vérifier en base
            Optional<Utilisateur> existant = utilisateurRepository.findByEmail(email);
            if (existant.isPresent()) {
                results.add(new ConducteurCreationResult(
                        nomComplet, email, "ALREADY_EXISTS", existant.get().getId()));
                processedEmails.add(email);
                continue;
            }

            // Créer l'utilisateur
            Utilisateur u = new Utilisateur();
            u.setNom(nomComplet);
            u.setEmail(email);
            u.setMotDePasse(passwordEncoder.encode("123456"));
            u.setRole(Role.TECHNICIEN);
            u.setStatutCompte(StatutCompte.EN_ATTENTE);
            u.setSpecialite("Conducteur");
            utilisateurRepository.save(u);

            processedEmails.add(email);
            results.add(new ConducteurCreationResult(nomComplet, email, "CREATED", u.getId()));
        }

        return results;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String genererEmail(String prenom, String nom, Set<String> taken) {
        String base = normaliser(prenom) + "." + normaliser(nom);
        if (base.equals(".")) base = "conducteur";
        String email = base + "@tunisietelecom.tn";
        if (!taken.contains(email) && !utilisateurRepository.existsByEmail(email))
            return email;

        int suffix = 1;
        while (true) {
            String candidate = base + suffix + "@tunisietelecom.tn";
            if (!taken.contains(candidate) && !utilisateurRepository.existsByEmail(candidate))
                return candidate;
            suffix++;
        }
    }

    private String normaliser(String s) {
        if (s == null || s.isEmpty()) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")   // supprimer diacritiques
                .toLowerCase()
                .replaceAll("[^a-z0-9]", ""); // garder lettres/chiffres
        return n;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}