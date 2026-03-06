package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.UtilisateurDto;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.enums.StatutCompte;
import com.example.ttcarburant.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service

public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;

    }
    /**
     * Récupérer tous les utilisateurs en attente de validation
     */
    @Transactional(readOnly = true)
    public List<UtilisateurDto> getUtilisateursEnAttente() {
        return utilisateurRepository.findByStatutCompteOrderByDateCreationDesc(StatutCompte.EN_ATTENTE)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les utilisateurs
     */
    @Transactional(readOnly = true)
    public List<UtilisateurDto> getAllUtilisateurs() {
        return utilisateurRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer un utilisateur par ID
     */
    @Transactional(readOnly = true)
    public UtilisateurDto getUtilisateurById(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return convertToDto(utilisateur);
    }

    /**
     * Valider un compte utilisateur (passer de EN_ATTENTE à ACTIF)
     */
    @Transactional
    public UtilisateurDto validerCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getStatutCompte() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte n'est pas en attente de validation");
        }

        utilisateur.setStatutCompte(StatutCompte.ACTIF);
        utilisateurRepository.save(utilisateur);

        return convertToDto(utilisateur);
    }

    /**
     * Refuser un compte utilisateur (passer de EN_ATTENTE à REFUSE)
     */
    @Transactional
    public UtilisateurDto refuserCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getStatutCompte() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte n'est pas en attente de validation");
        }

        utilisateur.setStatutCompte(StatutCompte.REFUSE);
        utilisateurRepository.save(utilisateur);

        return convertToDto(utilisateur);
    }

    /**
     * Désactiver un compte utilisateur
     */
    @Transactional
    public UtilisateurDto desactiverCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        utilisateur.setStatutCompte(StatutCompte.REFUSE);
        utilisateurRepository.save(utilisateur);

        return convertToDto(utilisateur);
    }

    /**
     * Supprimer un utilisateur
     */
    @Transactional
    public void supprimerUtilisateur(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        utilisateurRepository.deleteById(id);
    }

    /**
     * Convertir une entité Utilisateur en DTO
     */
    private UtilisateurDto convertToDto(Utilisateur utilisateur) {
        return new UtilisateurDto(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getEmail(),
                utilisateur.getRole(),
                utilisateur.getStatutCompte(),
                utilisateur.getDateCreation(),
                utilisateur.getSpecialite()
        );
    }
}