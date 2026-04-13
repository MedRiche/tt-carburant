// src/app/models/maintenance.ts

export enum TypeIntervention {
  PREVENTIVE        = 'PREVENTIVE',
  CORRECTIVE        = 'CORRECTIVE',
  VISITE_TECHNIQUE  = 'VISITE_TECHNIQUE',
  ACCIDENT          = 'ACCIDENT'
}

export enum StatutMaintenance {
  EN_COURS = 'EN_COURS',
  TERMINEE = 'TERMINEE',
  ANNULEE  = 'ANNULEE'
}

export enum TypeDetailMaintenance {
  MAIN_D_OEUVRE = 'MAIN_D_OEUVRE',
  PIECE         = 'PIECE'
}

export interface DetailMaintenance {
  id?: number;
  type: TypeDetailMaintenance;
  numeroDossier?: string;
  marque?: string;
  numero?: string;
  numeroPiece?: string;
  designation: string;
  quantite: number;
  montantUnitaire: number;
  totalHtva?: number;
}

export interface Maintenance {
  id?: number;
  numeroDossier: string;
  vehiculeMatricule: string;
  vehiculeMarqueModele?: string;
  vehiculeZoneNom?: string;
  dateIntervention?: string;
  typeIntervention: TypeIntervention;
  statut: StatutMaintenance;
  description?: string;
  coutTotalHtva: number;
  brands?: string;
  nbDetails?: number;
  creePar?: string;
  dateCreation?: string;
  details?: DetailMaintenance[];
}

/** Vue "Global Vehicle List" */
export interface GlobalVehicleListItem {
  vehiculeId: string;
  vehiculeMarque: string;
  zoneNom?: string;
  totalHtva: number;
  brands: string;
  nbDossiers: number;
}

export interface MaintenanceRequest {
  numeroDossier: string;
  vehiculeMatricule: string;
  dateIntervention?: string;
  typeIntervention: TypeIntervention;
  statut?: StatutMaintenance;
  description?: string;
  details?: DetailMaintenance[];
}

export interface MaintenanceDashboard {
  nbDossiers: number;
  totalHtva: number;
  statsParType: { type: string; count: number; totalHtva: number }[];
  topVehicules: { matricule: string; totalHtva: number }[];
  coutParZone: { zone: string; totalHtva: number }[];
  coutParPrestataire: { prestataire: string; totalHtva: number }[];
}

export const TYPE_INTERVENTION_LABELS: Record<string, string> = {
  PREVENTIVE:       'Préventive',
  CORRECTIVE:       'Corrective',
  VISITE_TECHNIQUE: 'Visite technique',
  ACCIDENT:         'Accident'
};

export const STATUT_LABELS: Record<string, string> = {
  EN_COURS: 'En cours',
  TERMINEE: 'Terminée',
  ANNULEE:  'Annulée'
};