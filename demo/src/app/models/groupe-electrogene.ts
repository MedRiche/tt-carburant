// src/app/models/groupe-electrogene.ts

export enum TypeCarburantGE {
  GASOIL_ORDINAIRE   = 'GASOIL_ORDINAIRE',
  GASOIL_SANS_SOUFRE = 'GASOIL_SANS_SOUFRE',
  SUPER_SANS_PLOMB   = 'SUPER_SANS_PLOMB',
  ESSENCE            = 'ESSENCE'
}

export const TYPE_CARBURANT_LABELS: Record<TypeCarburantGE, string> = {
  [TypeCarburantGE.GASOIL_ORDINAIRE]:   'Gasoil Ordinaire',
  [TypeCarburantGE.GASOIL_SANS_SOUFRE]: 'Gasoil Sans Soufre',
  [TypeCarburantGE.SUPER_SANS_PLOMB]:   'Super Sans Plomb',
  [TypeCarburantGE.ESSENCE]:            'Essence'
};

export interface GroupeElectrogene {
  site: string;
  typeCarburant: TypeCarburantGE;
  puissanceKVA?: number;
  tauxConsommationParHeure?: number;
  consommationTotaleMaxParSemestre?: number;
  prixCarburant?: number;
  typeCarte?: string;
  numeroCarte?: string;
  dateExpiration?: string;   // format "yyyy-MM" retourné par le backend
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
  dateExpiration?: string;   // format "yyyy-MM" envoyé depuis input[type=month]
  codePIN?: string;
  codePUK?: string;
  utilisateurRoc?: string;
  zoneId?: number;
}