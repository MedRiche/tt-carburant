// ── src/app/models/carburant-vehicule.ts ─────────────────────────

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
  // Calculés
  totalRavitaillementLitres?: number;
  quantiteRestanteReservoir?: number;
  distanceParcourue?: number;
  pourcentageConsommation?: number;
  carburantDemandeDinars?: number;
}

export const MOIS_LABELS = [
  '', 'Janvier','Février','Mars','Avril','Mai','Juin',
  'Juillet','Août','Septembre','Octobre','Novembre','Décembre'
];