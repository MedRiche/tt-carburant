// src/app/models/vehicule.ts

export enum TypeCarburant {
  ESSENCE           = 'ESSENCE',
  GASOIL_ORDINAIRE  = 'GASOIL_ORDINAIRE',
  GASOIL_SANS_SOUFRE= 'GASOIL_SANS_SOUFRE',
  GASOIL_50         = 'GASOIL_50',
  SUPER_SANS_PLOMB  = 'SUPER_SANS_PLOMB'
}

export interface Vehicule {
  matricule: string;
  dateMiseService?: string;
  marqueModele: string;
  typeVehicule: string;
  subdivision?: string;
  centre?: string;
  residenceService?: string;
  nomConducteur?: string;
  prenomConducteur?: string;
  typeCarburant: TypeCarburant;
  prixCarburant: number;
  indexVidange?: number;
  visiteTechnique?: string;
  indexPneumatique?: number;
  kilometrageTotal?: number;
  consommationDinarsCumul?: number;
  consommationLitresCumul?: number;
  coutDuMois?: number;
  croxChaine?: number;
  indexBatterie?: number;
  zoneId?: number;
  zoneNom?: string;
}

export interface VehiculeRequest {
  matricule: string;
  dateMiseService?: string;
  marqueModele: string;
  typeVehicule: string;
  subdivision?: string;
  centre?: string;
  residenceService?: string;
  nomConducteur?: string;
  prenomConducteur?: string;
  typeCarburant: TypeCarburant;
  prixCarburant: number;
  indexVidange?: number;
  visiteTechnique?: string;
  indexPneumatique?: number;
  kilometrageTotal?: number;
  consommationDinarsCumul?: number;
  consommationLitresCumul?: number;
  coutDuMois?: number;
  croxChaine?: number;
  indexBatterie?: number;
  zoneId?: number;
}