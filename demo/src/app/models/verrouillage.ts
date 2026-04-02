// src/app/models/verrouillage.ts

export interface VerrouillageDto {
  id?: number;
  annee: number;
  mois: number;
  moisLabel: string;
  zoneId?: number;
  zoneNom?: string;
  verrouille: boolean;
  verrouilleParEmail?: string;
  verrouillerLe?: string;
  deverrouilleParEmail?: string;
  deverrouillerLe?: string;
}

export interface HistoriqueModificationDto {
  id: number;
  gestionId?: number;
  vehiculeMatricule: string;
  vehiculeMarqueModele?: string;
  annee: number;
  mois: number;
  periodeLabel: string;
  action: 'CREATE' | 'UPDATE' | 'DELETE';
  actionLabel: string;
  modifiePar: string;
  modifieLe: string;
  valeursAvant?: string;
  valeursApres?: string;
  description: string;
}