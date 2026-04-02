// src/app/models/carburant-analytics.ts

export interface HistoriqueRavitaillement {
  id: number;
  vehiculeMatricule: string;
  vehiculeMarqueModele: string;
  vehiculeZoneNom?: string;
  annee: number;
  mois: number;
  periodeLabel: string;
  indexDemarrageMois: number;
  indexFinMois: number;
  montantRestantMoisPrecedent: number;
  ravitaillementMoisPrecedent: number;
  ravitaillementMois: number;
  totalRavitaillementLitres: number;
  quantiteRestanteReservoir: number;
  distanceParcourue: number;
  pourcentageConsommation: number;
  carburantDemandeDinars: number;
  montantRestantReservoirFin: number;
  coutDuMois: number;
  prixCarburant: number;
  budgetDepasse: boolean;
  depassementMontant: number;
  tauxBudget: number;
  dateCreation?: string;
  statut: 'NORMAL' | 'ALERTE_BUDGET' | 'ANOMALIE_CONSO' | 'ANOMALIE_KM' | 'CRITIQUE';
}

export interface EvolutionData {
  titre: string;
  annee: number;
  labels: string[];
  consommationLitres: number[];
  coutDinars: number[];
  kmParcourus: number[];
  pourcentageConso: number[];
  budgetMensuel: number[];
  totalKm: number;
  totalLitres: number;
  totalCout: number;
  moyenneConsommation: number;
}

export interface Anomalie {
  vehiculeMatricule: string;
  vehiculeMarqueModele: string;
  vehiculeZoneNom?: string;
  annee: number;
  mois: number;
  periodeLabel: string;
  typeAnomalie: 'BUDGET_DEPASSE' | 'CONSO_ANORMALE' | 'KM_INCOHERENT' | 'KM_ELEVE';
  severite: 'FAIBLE' | 'MOYENNE' | 'ELEVEE' | 'CRITIQUE';
  description: string;
  valeurReelle: number;
  valeurSeuil: number;
  ecart: number;
  ecartPourcentage: number;
}

export interface VehiculeRank {
  matricule: string;
  marqueModele: string;
  zoneNom?: string;
  totalKm: number;
  totalLitres: number;
  totalCout: number;
  rendementLPour100km: number;
  rendementDTPour100km: number;
  tauxBudgetMoyen: number;
  nbMoisSaisis: number;
  nbAnomalies: number;
}

export interface ComparaisonData {
  top5Consommation: VehiculeRank[];
  top5KmParcourus: VehiculeRank[];
  meilleursRendements: VehiculeRank[];
  piresRendements: VehiculeRank[];
  plusGrandsBudgetDepasses: VehiculeRank[];
  moyenneGlobaleRendement: number;
  seuilRendementAnomalie: number;
}

export interface ZoneStat {
  zoneId: number;
  zoneNom: string;
  nbVehicules: number;
  totalKm: number;
  totalLitres: number;
  totalCout: number;
  totalBudget: number;
  tauxBudget: number;
  nbAnomalies: number;
  rendementMoyen: number;
}

export interface MoisStat {
  mois: number;
  label: string;
  totalKm: number;
  totalLitres: number;
  totalCout: number;
  totalBudget: number;
  nbVehicules: number;
}

export interface DashboardCarburant {
  nbVehiculesSaisis: number;
  totalKm: number;
  totalLitres: number;
  totalCoutDT: number;
  totalBudgetDT: number;
  tauxBudgetGlobal: number;
  nbBudgetsDepasses: number;
  nbAnomalies: number;
  statsParZone: ZoneStat[];
  evolutionMensuelle: MoisStat[];
  consommationParTypeCarburant: Record<string, number>;
  dernieresAnomalies: Anomalie[];
}