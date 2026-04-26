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
 * Crée automatiquement un compte TECHNICIEN (EN_ATTENTE)
 * pour chaque conducteur extrait d'un fichier Excel importé.
 *
 * Email généré  : prenom.nom@tunisietelecom.tn
 * Mot de passe  : 123456 (encodé BCrypt)
 * Statut        : EN_ATTENTE  → l'admin valide dans Gestion Utilisateurs
 */
@Service
public class ConducteurUserCreationService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder       passwordEncoder;

    public ConducteurUserCreationService(UtilisateurRepository utilisateurRepository,
                                         PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder       = passwordEncoder;
    }

    // ── Résultat par conducteur ──────────────────────────────────────────────
    public static class ConducteurCreationResult {
        public final String nomComplet;
        public final String email;
        public final String statut;   // "CREATED" | "ALREADY_EXISTS" | "SKIPPED"
        public final Long   userId;

        public ConducteurCreationResult(String nomComplet, String email,
                                        String statut,     Long userId) {
            this.nomComplet = nomComplet;
            this.email      = email;
            this.statut     = statut;
            this.userId     = userId;
        }
    }

    /**
     * Traite une liste de conducteurs et crée les comptes manquants.
     *
     * @param conducteurs  liste de Map avec clés "prenom" et "nom"
     * @return résultats par conducteur
     */
    @Transactional
    public List<ConducteurCreationResult> creerComptesConducteurs(
            List<Map<String, String>> conducteurs) {

        List<ConducteurCreationResult> results       = new ArrayList<>();
        Set<String>                    batchEmails   = new HashSet<>(); // doublons dans le même batch

        for (Map<String, String> c : conducteurs) {
            String prenom    = trim(c.get("prenom"));
            String nom       = trim(c.get("nom"));

            // Ignorer les lignes sans conducteur
            if (prenom.isEmpty() && nom.isEmpty()) continue;

            String nomComplet = (prenom + " " + nom).trim();
            String email      = genererEmail(prenom, nom, batchEmails);

            // Doublon dans ce même batch (conducteur listé deux fois)
            if (batchEmails.contains(email)) {
                results.add(new ConducteurCreationResult(nomComplet, email, "SKIPPED", null));
                continue;
            }

            // Déjà en base
            Optional<Utilisateur> existant = utilisateurRepository.findByEmail(email);
            if (existant.isPresent()) {
                results.add(new ConducteurCreationResult(
                        nomComplet, email, "ALREADY_EXISTS", existant.get().getId()));
                batchEmails.add(email);
                continue;
            }

            // Créer le compte
            Utilisateur u = new Utilisateur();
            u.setNom(nomComplet);
            u.setEmail(email);
            u.setMotDePasse(passwordEncoder.encode("123456"));
            u.setRole(Role.TECHNICIEN);
            u.setStatutCompte(StatutCompte.EN_ATTENTE);
            u.setSpecialite("Conducteur");
            utilisateurRepository.save(u);

            batchEmails.add(email);
            results.add(new ConducteurCreationResult(nomComplet, email, "CREATED", u.getId()));
        }

        return results;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String genererEmail(String prenom, String nom, Set<String> taken) {
        String base  = normaliser(prenom) + "." + normaliser(nom);
        if (base.equals(".")) base = "conducteur";

        String email = base + "@tunisietelecom.tn";
        if (!taken.contains(email) && !utilisateurRepository.existsByEmail(email))
            return email;

        // Trouver un suffixe libre
        for (int i = 1; i < 100; i++) {
            String candidate = base + i + "@tunisietelecom.tn";
            if (!taken.contains(candidate) && !utilisateurRepository.existsByEmail(candidate))
                return candidate;
        }
        // Fallback avec UUID court
        return base + "_" + UUID.randomUUID().toString().substring(0, 4) + "@tunisietelecom.tn";
    }

    /** Supprime les accents, met en minuscules, garde seulement a-z0-9. */
    private String normaliser(String s) {
        if (s == null || s.isEmpty()) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private String trim(String s) { return s == null ? "" : s.trim(); }
}