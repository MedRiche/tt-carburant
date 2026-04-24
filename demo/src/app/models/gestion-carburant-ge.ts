// src/app/models/gestion-carburant-ge.ts

export enum Semestre {
  PREMIER  = 'PREMIER',
  DEUXIEME = 'DEUXIEME'
}

export const SEMESTRE_LABELS: Record<Semestre, string> = {
  [Semestre.PREMIER]: 'Premier semestre',
  [Semestre.DEUXIEME]: 'Deuxième semestre'
};

export interface GestionCarburantGE {
  id?: number;
  site: string;
  siteZoneNom?: string;
  annee: number;
  semestre: Semestre;

  // Saisies
  indexHeureSemestrePrecedent?: number;
  montantCarburantRestantReservoirPrecedent?: number;
  ravitaillementSemestrePrecedentDinars?: number;
  montantRestantAgilisFinSemestre?: number;
  indexFinSemestre?: number;

  // Calculés
  totalRavitaillementLitres?: number;
  quantiteRestanteReservoirAgilis?: number;
  nbHeuresTravail?: number;
  pourcentageConsommation?: number;
  carburantDemandeDinarsCours?: number;
  evaluationTauxConsommation?: string; // "OUI" | "NON"
  dateCreation?: string;
}

export interface GestionCarburantGERequest {
  site: string;
  annee: number;
  semestre: Semestre;
  indexHeureSemestrePrecedent?: number;
  montantCarburantRestantReservoirPrecedent?: number;
  ravitaillementSemestrePrecedentDinars?: number;
  montantRestantAgilisFinSemestre?: number;
  indexFinSemestre?: number;
}