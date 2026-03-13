// models/utilisateur.model.ts
import { Zone } from './zone';

export enum Role {
  ADMIN = 'ADMIN',
  TECHNICIEN = 'TECHNICIEN'
}

export enum StatutCompte {
  EN_ATTENTE = 'EN_ATTENTE',
  ACTIF = 'ACTIF',
  REFUSE = 'REFUSE'
}

export interface Utilisateur {
  id: number;
  nom: string;
  email: string;
  role: Role;
  statutCompte: StatutCompte;
  dateCreation?: Date;
  specialite?: string;
  zones?: Zone[];
}

export interface ValiderCompteRequest {
  utilisateurId: number;
  zoneIds: number[];
}