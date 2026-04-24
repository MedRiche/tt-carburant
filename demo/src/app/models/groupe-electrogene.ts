// src/app/models/groupe-electrogene.ts

export enum TypeCarburantGE {
  GASOIL_ORDINAIRE   = 'GASOIL_ORDINAIRE',
  GASOIL_SANS_SOUFRE = 'GASOIL_SANS_SOUFRE',
  SUPER_SANS_PLOMB   = 'SUPER_SANS_PLOMB',
  ESSENCE            = 'ESSENCE'
}

export interface GroupeElectrogene {
  site: string;
  typeCarburant: TypeCarburantGE;
  puissanceKVA?: number;
  tauxConsommationParHeure?: number;
  consommationTotaleMaxParSemestre?: number;
  prixCarburant?: number;
  typeCarte?: string;
  numeroCarte?: string;
  dateExpiration?: string;
  codePIN?: string;
  codePUK?: string;
  utilisateurRoc?: string;
  zoneId?: number;
  zoneNom?: string;
}

export interface GroupeElectrogeneRequest {
  site: string;
  typeCarburant: TypeCarburantGE;
  puissanceKVA?: number;
  tauxConsommationParHeure?: number;
  consommationTotaleMaxParSemestre?: number;
  prixCarburant?: number;
  typeCarte?: string;
  numeroCarte?: string;
  dateExpiration?: string;
  codePIN?: string;
  codePUK?: string;
  utilisateurRoc?: string;
  zoneId?: number;
}