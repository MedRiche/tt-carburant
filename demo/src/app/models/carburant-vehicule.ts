// src/app/models/carburant-vehicule.ts

export interface CarburantVehicule {
  id?: number;
  vehiculeMatricule: string;
  vehiculeMarqueModele?: string;
  vehiculeZoneNom?: string;
  prixCarburant?: number;
  coutDuMois?: number;
  annee: number;
  mois: number;
  // Saisies
  indexDemarrageMois: number;
  indexFinMois: number;
  montantRestantMoisPrecedent: number;
  ravitaillementMoisPrecedent: number;
  ravitaillementMois: number;
  // Calculés DAF 2026
  totalRavitaillementLitres?: number;
  quantiteRestanteReservoir?: number;
  distanceParcourue?: number;
  pourcentageConsommation?: number;
  carburantDemandeDinars?: number;
  // NOUVEAU
  montantRestantReservoirFin?: number;
  budgetDepasse?: boolean;
  depassementMontant?: number;
}

export interface CarburantPrefill {
  vehiculeMatricule: string;
  annee: number;
  mois: number;
  indexDemarrageMois: number;
  montantRestantMoisPrecedent: number;
  ravitaillementMoisPrecedent: number;
  prixCarburant: number;
  coutDuMois: number;
}

export const MOIS_LABELS: Record<number, string> = {
  0: '', 1: 'Janvier', 2: 'Février', 3: 'Mars', 4: 'Avril',
  5: 'Mai', 6: 'Juin', 7: 'Juillet', 8: 'Août',
  9: 'Septembre', 10: 'Octobre', 11: 'Novembre', 12: 'Décembre'
};