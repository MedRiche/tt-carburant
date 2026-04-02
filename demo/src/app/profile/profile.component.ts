import { Component } from '@angular/core';

// Interfaces pour un typage fort
interface MaintenanceDetail {
  type: string;
  ndossier?: string;
  vehicule?: string;
  marque?: string;
  numero?: string;
  npiece?: string;
  designation: string;
  qte: number;
  montant: number;
  total: number;
}

interface MaintenanceEntry {
  vehicleId: string;
  totalHTVA: number;
  brands: string;
  details: MaintenanceDetail[];
}

@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent {
  // ---------- DATA ----------
  vehicles = [
    { id: 1, mat: '356814', modele: 'FORD FIGO', type: 'Essence', prix: 2.525, cout: 250, prenom: 'ATEF', nom: 'GHOUILI', zone: 'DAF', subdiv: '', centre: 'CHEF DAF', visite: '2026-02-09', idx_dem: 0, montant_restant_prec: 0, ravit_prec: null },
    { id: 2, mat: '357088', modele: 'PEUGEOT BIPPER', type: 'Essence', prix: 2.525, cout: 250, prenom: 'Mourad', nom: 'Touati', zone: 'DAF', subdiv: 'MOYENS', centre: 'CHEF SUBDIVISION', visite: '2026-02-10', idx_dem: 307000, montant_restant_prec: 0, ravit_prec: 250 },
    { id: 3, mat: '346724', modele: 'CITROEN BERLINGO', type: 'Gasoil', prix: 1.985, cout: 200, prenom: 'Hedia', nom: 'Bouchaddekh', zone: 'CHEF CFRT', subdiv: 'ZONE GRAND TUNIS', centre: 'Chef Centre Facturation', visite: '2025-08-31', idx_dem: null, montant_restant_prec: null, ravit_prec: 250 },
    { id: 4, mat: '351250', modele: 'PEUGEOT PARTNER', type: 'Gasoil', prix: 1.985, cout: 200, prenom: 'MAHMOUD', nom: 'DARGHOUTHI', zone: 'DAF', subdiv: 'JURIDIQUE', centre: '', visite: null, idx_dem: 356992, montant_restant_prec: 0, ravit_prec: null },
    { id: 5, mat: '357089', modele: 'PEUGEOT BIPPER', type: 'Essence', prix: 2.525, cout: 250, prenom: 'ZIED', nom: 'CHARFEDDINE', zone: 'DAF', subdiv: 'ACHAT', centre: '', visite: null, idx_dem: 191760, montant_restant_prec: 0, ravit_prec: 250 },
    { id: 6, mat: '354444', modele: 'CITROEN NEMO', type: 'Essence', prix: 2.525, cout: 250, prenom: 'FATMA', nom: 'CHAMROUKHI', zone: 'DAF', subdiv: 'RECOUVREMENT', centre: '', visite: null, idx_dem: 242657, montant_restant_prec: 10, ravit_prec: null },
    { id: 7, mat: '349214', modele: 'VW CADDY', type: 'Gasoil', prix: 1.985, cout: 200, prenom: 'MOHAMED YASSINE', nom: 'BOURAOUI', zone: 'DAF', subdiv: 'MOYENS', centre: '', visite: null, idx_dem: null, montant_restant_prec: null, ravit_prec: null },
    { id: 8, mat: '354443', modele: 'CITROEN NEMO', type: 'Essence', prix: 2.525, cout: 250, prenom: '', nom: '', zone: 'DAF', subdiv: 'MOYENS', centre: '', visite: null, idx_dem: 165400, montant_restant_prec: null, ravit_prec: null },
    { id: 9, mat: '351268', modele: 'PARTNER (DRTT)', type: 'Gasoil', prix: 1.985, cout: 200, prenom: '', nom: 'Immobilisé', zone: 'DRTT', subdiv: 'en attente Réparation', centre: '', visite: null, idx_dem: null, montant_restant_prec: null, ravit_prec: null },
    { id: 10, mat: '353449', modele: 'CITROEN BERLINGO', type: 'Essence', prix: 2.525, cout: 250, prenom: 'MOUNIR', nom: 'DERBEL', zone: 'DAF', subdiv: 'RH', centre: 'Chef Division RH', visite: null, idx_dem: 289250, montant_restant_prec: null, ravit_prec: null },
    { id: 11, mat: '351249', modele: 'PEUGEOT PARTNER', type: 'Gasoil', prix: 1.985, cout: 200, prenom: 'MOURAD', nom: 'HLEL', zone: 'DAF', subdiv: 'SUBD MOYENS', centre: '', visite: null, idx_dem: null, montant_restant_prec: null, ravit_prec: null },
    { id: 12, mat: '353448', modele: 'CITROEN BERLINGO', type: 'Essence', prix: 2.525, cout: 250, prenom: 'ALI', nom: 'EL BOUALI', zone: 'ZGT/DRTT', subdiv: 'DRTT', centre: '', visite: null, idx_dem: null, montant_restant_prec: null, ravit_prec: null },
    { id: 13, mat: '351426', modele: '—', type: 'Essence', prix: 2.525, cout: 250, prenom: 'Halima', nom: 'Chermiti', zone: 'DAF', subdiv: '', centre: '', visite: null, idx_dem: null, montant_restant_prec: null, ravit_prec: null },
    { id: 14, mat: '351256', modele: 'PEUGEOT PARTNER', type: 'Gasoil', prix: 1.985, cout: 200, prenom: 'Kais', nom: 'Riahi', zone: 'CLIENTELE', subdiv: 'CSC BARDO', centre: 'UNITE ENTREPRISE', visite: null, idx_dem: null, montant_restant_prec: null, ravit_prec: null },
    { id: 15, mat: '351285', modele: '—', type: 'Essence', prix: 1.985, cout: 250, prenom: 'Kais', nom: 'Riahi', zone: 'CSC BARDO', subdiv: 'CSC BARDO', centre: 'UNITE ENTREPRISE', visite: null, idx_dem: 265183, montant_restant_prec: 0, ravit_prec: null },
    { id: 16, mat: '351304', modele: '—', type: 'Essence', prix: 1.985, cout: 250, prenom: 'Kais', nom: 'Riahi', zone: 'CSC BARDO', subdiv: 'CSC BARDO', centre: 'UNITE ENTREPRISE', visite: null, idx_dem: 316010, montant_restant_prec: 40, ravit_prec: null }
  ];

  geData: any[] = [
    { id: 1, site: 'HACHED 1', carb: 'GASOIL ORDINAIRE', kva: 500, taux: 115, max: 800, carte: '7420545770107101', exp: '08/2028', pin: '0177', puk: '13783790', user: 'MOEZ KHLIF' },
    { id: 2, site: 'HACHED 2', carb: 'GASOIL ORDINAIRE', kva: 'démonté', taux: null, max: null, carte: '7420545770107200', exp: '08/2028', pin: '5127', puk: '45775093', user: '' },
    { id: 3, site: 'HACHED 3', carb: 'GASOIL ORDINAIRE', kva: 500, taux: 115, max: null, carte: '7420545770107309', exp: '08/2028', pin: '8997', puk: '19631391', user: '' },
    { id: 4, site: 'OUARDIA CTN', carb: 'GASOIL ORDINAIRE', kva: 500, taux: 115, max: 750, carte: '7420545770105808', exp: '08/2028', pin: '4052', puk: '56668250', user: '' },
    { id: 5, site: 'OUARDIA OND COELMO', carb: 'GASOIL ORDINAIRE', kva: 250, taux: 50, max: 300, carte: '7420545770106004', exp: '08/2028', pin: '5066', puk: '24124993', user: '' },
    { id: 6, site: 'OUARDIA CEN COELMO', carb: 'GASOIL ORDINAIRE', kva: 250, taux: 50, max: null, carte: '7420545770105907', exp: '08/2028', pin: '3147', puk: '59866409', user: '' },
    { id: 7, site: 'MOUROUJ 2', carb: 'GASOIL ORDINAIRE', kva: 40, taux: 12, max: 50, carte: '7420545770107507', exp: '08/2028', pin: '7503', puk: '75753351', user: '' },
    { id: 8, site: 'AVICENNE', carb: 'GASOIL ORDINAIRE', kva: 90, taux: 22, max: 250, carte: '7420545770105709', exp: '08/2028', pin: '9101', puk: '24676947', user: '' },
    { id: 9, site: 'RUE ANGLETERRE', carb: 'GASOIL ORDINAIRE', kva: 'démonté', taux: null, max: null, carte: '7420545770106103', exp: '08/2028', pin: '6984', puk: '88383576', user: '' },
    { id: 10, site: 'DIR GENERALE GE 400 KVA', carb: 'GASOIL SANS SOUFFRE', kva: 400, taux: 100, max: null, carte: '7420545770137801', exp: '11/2030', pin: '4979', puk: '75098759', user: 'IKBEL DACHRAOUI' },
    { id: 11, site: 'BELVEDERE 1', carb: 'GASOIL SANS SOUFFRE', kva: 500, taux: 115, max: 1500, carte: '7420545770108307', exp: '08/2028', pin: '9681', puk: '33696822', user: '' },
    { id: 12, site: 'BELVEDERE 2', carb: 'GASOIL SANS SOUFFRE', kva: 400, taux: 100, max: null, carte: '7420545770104504', exp: '08/2028', pin: '8168', puk: '51564137', user: '' },
    { id: 13, site: 'MARSA', carb: 'GASOIL SANS SOUFFRE', kva: 250, taux: 50, max: 300, carte: '7420545770107606', exp: '08/2028', pin: '9421', puk: '40011935', user: '' },
    { id: 14, site: 'AIN ZAGHOUAN DC 1', carb: 'GASOIL SANS SOUFFRE', kva: 650, taux: 150, max: 1200, carte: '7420545770108604', exp: '08/2028', pin: '0421', puk: '03803007', user: '' },
    { id: 15, site: 'AIN ZAGHOUAN DC 2', carb: 'GASOIL SANS SOUFFRE', kva: 650, taux: 150, max: null, carte: '7420545770108703', exp: '08/2028', pin: '5632', puk: '12567890', user: '' },
    { id: 16, site: 'CITE MAHRAJENE', carb: 'GASOIL SANS SOUFFRE', kva: 400, taux: 100, max: 600, carte: '7420545770109001', exp: '08/2028', pin: '3211', puk: '45678901', user: '' }
  ];

  constructor() {
    for (let i = this.geData.length; i < 45; i++) {
      this.geData.push({
        id: i + 1,
        site: 'SITE ' + String.fromCharCode(65 + (i % 26)) + Math.floor(i / 26 + 1),
        carb: i % 2 === 0 ? 'GASOIL ORDINAIRE' : 'GASOIL SANS SOUFFRE',
        kva: [40, 90, 250, 400, 500][i % 5],
        taux: [12, 22, 50, 100, 115][i % 5],
        max: null,
        carte: '7420545770' + (100000 + i),
        exp: '08/2028',
        pin: '0000',
        puk: '00000000',
        user: ''
      });
    }
  }

  maintenanceList: MaintenanceEntry[] = [
    {
      vehicleId: '17-335662', totalHTVA: 87.011, brands: 'Citroen',
      details: [
        { type: 'MAIN_D_OEUVRE', ndossier: '44', vehicule: '17-335662', marque: 'jumpy', numero: '201', designation: "réparations d'une serrure de porte (unité)", qte: 1, montant: 9.7, total: 9.7 },
        { type: 'PIECE', ndossier: '44', vehicule: '17-335662', marque: 'jumpy', numero: '', designation: 'necessair barrelet jumpy', qte: 1, montant: 77.311, total: 77.311 }
      ]
    },
    {
      vehicleId: '17-344186', totalHTVA: 682.083, brands: 'TAS', details: [
        { type: 'MAIN_D_OEUVRE', ndossier: '12', vehicule: '17-344186', marque: 'TAS', numero: '105', designation: 'Vidange moteur complète', qte: 1, montant: 45, total: 45 },
        { type: 'PIECE', ndossier: '12', vehicule: '17-344186', marque: 'TAS', numero: 'FLT-001', designation: 'Filtre à huile TAS', qte: 1, montant: 22.5, total: 22.5 },
        { type: 'PIECE', ndossier: '12', vehicule: '17-344186', marque: 'TAS', numero: 'HUI-5W40', designation: "Huile moteur 5W40 (5L)", qte: 2, montant: 307.29, total: 614.583 }
      ]
    },
    {
      vehicleId: '17-345444', totalHTVA: 606.55, brands: 'Citroen, Peugeot', details: [
        { type: 'MAIN_D_OEUVRE', ndossier: '22', vehicule: '17-345444', marque: 'Citroen', numero: '301', designation: 'Remplacement courroie distribution', qte: 1, montant: 180, total: 180 },
        { type: 'PIECE', ndossier: '22', vehicule: '17-345444', marque: 'Peugeot', numero: 'PEU-CR-001', designation: 'Kit courroie distribution', qte: 1, montant: 426.55, total: 426.55 }
      ]
    },
    {
      vehicleId: '17-345724', totalHTVA: 630.585, brands: 'Peugeot', details: [
        { type: 'MAIN_D_OEUVRE', ndossier: '31', vehicule: '17-345724', marque: 'Peugeot', numero: '401', designation: 'Contrôle freins avant/arrière', qte: 1, montant: 60, total: 60 },
        { type: 'PIECE', ndossier: '31', vehicule: '17-345724', marque: 'Peugeot', numero: 'PEU-PLQ-001', designation: 'Plaquettes frein avant', qte: 2, montant: 285.29, total: 570.585 }
      ]
    },
    {
      vehicleId: '17-346724', totalHTVA: 1269.732, brands: 'Citroen, Peugeot, TAS', details: [
        { type: 'MAIN_D_OEUVRE', ndossier: '55', vehicule: '17-346724', marque: 'Citroen', numero: '501', designation: 'Diagnostic électronique complet', qte: 1, montant: 120, total: 120 },
        { type: 'PIECE', ndossier: '55', vehicule: '17-346724', marque: 'TAS', numero: 'TAS-BTR-001', designation: 'Batterie 70Ah TAS', qte: 1, montant: 349.7, total: 349.7 },
        { type: 'PIECE', ndossier: '55', vehicule: '17-346724', marque: 'Peugeot', numero: 'PEU-ALT-001', designation: 'Alternateur reconditionné', qte: 1, montant: 800.032, total: 800.032 }
      ]
    }
  ];

  // ---------- UI STATE ----------
  currentView = 'dashboard';
  topbarTitle = 'Vue d\'ensemble';

  // ---------- FUEL FORM ----------
  fuelForm = {
    selectedVehicle: null as any,
    mois: 3,
    prix: 0,
    typeCarb: '',
    coutMois: 0,
    idxDem: 0,
    montantRestantPrec: 0,
    ravitPrec: 0,
    montantRestantFin: 0,
    idxFin: 0,
    totalRavit: 0,
    qteRestante: 0,
    distance: 0,
    pctConso: 0,
    carbDemande: 0,
    // optional
    idxVidange: null as number | null,
    visiteTech: '',
    idxPneu: null as number | null,
    idxBatt: null as number | null
  };

  // ---------- MAINTENANCE FORM ----------
  tempMaintenanceDetails: MaintenanceDetail[] = [];
  newDetail = {
    type: 'MAIN_D_OEUVRE',
    numero: '',
    marque: '',
    npiece: '',
    designation: '',
    qte: 1,
    montant: 0,
    total: 0
  };
  maintenanceForm = {
    ndossier: '',
    vehicule: '',
    date: '',
    type: '',
    statut: 'EN_COURS',
    description: ''
  };

  // ---------- MAINTENANCE LIST DETAIL PANEL ----------
  selectedMaintenance: MaintenanceEntry | null = null;

  // ---------- GENERATOR FORM ----------
  generatorForm = {
    semestre: 'S1',
    selectedGe: null as any,
    typeCarb: '',
    puissance: '',
    tauxConso: null as number | null,
    consoMax: null as number | null,
    carte: '',
    userRoc: '',
    prix: 1.985,
    idxPrec: 0,
    montantRestantPrec: 0,
    ravitPrec: 0,
    montantRestantFin: 0,
    idxFin: 0,
    totalRavit: 0,
    qteRestante: 0,
    nbHeures: 0,
    pctConso: 0,
    carbDemande: 0,
    eval: ''
  };

  // ---------- FILTERS ----------
  fuelListFilter = {
    search: '',
    mois: '3'
  };
  maintenanceListFilter = {
    zone: 'DAF',
    type: '',
    search: ''
  };
  geListFilter = {
    semestre: 'S1',
    carb: '',
    search: ''
  };

  // ---------- LIFECYCLE ----------
  ngOnInit() {
    this.renderDashboard();
  }

  // ---------- VIEWS ----------
  showView(view: string) {
    this.currentView = view;
    switch (view) {
      case 'dashboard':
        this.topbarTitle = 'Vue d\'ensemble';
        break;
      case 'carburant-saisie':
        this.topbarTitle = 'Saisie carburant mensuelle';
        break;
      case 'carburant-liste':
        this.topbarTitle = 'Liste des véhicules — Carburant';
        this.renderCarburantListe();
        break;
      case 'maintenance-saisie':
        this.topbarTitle = 'Saisir une maintenance';
        break;
      case 'maintenance-liste':
        this.topbarTitle = 'Historique maintenance';
        this.renderMaintenanceListe();
        break;
      case 'ge-saisie':
        this.topbarTitle = 'Saisie ravitaillement GE';
        break;
      case 'ge-liste':
        this.topbarTitle = 'Suivi groupes électrogènes';
        this.renderGEListe();
        break;
    }
  }

  // ---------- DASHBOARD ----------
  renderDashboard() {
    // La logique est déjà dans le template via *ngFor
  }

  getDashboardVehicles() {
    return this.vehicles.slice(0, 8);
  }

  getMaintenanceMoDetails(): MaintenanceDetail[] {
    // Typage explicite du paramètre d
    return this.selectedMaintenance?.details.filter((d: MaintenanceDetail) => d.type === 'MAIN_D_OEUVRE') || [];
  }

  getMaintenancePieceDetails(): MaintenanceDetail[] {
    // Typage explicite du paramètre d
    return this.selectedMaintenance?.details.filter((d: MaintenanceDetail) => d.type === 'PIECE') || [];
  }

  getVehicleStatus(visite: string | null): { class: string, text: string } {
    if (!visite) return { class: 'badge-gray', text: 'Non renseignée' };
    const date = new Date(visite);
    const today = new Date();
    const diffDays = (date.getTime() - today.getTime()) / (1000 * 3600 * 24);
    if (diffDays < 0) return { class: 'badge-red', text: 'Expirée' };
    if (diffDays < 60) return { class: 'badge-amber', text: 'Proche' };
    return { class: 'badge-green', text: 'OK' };
  }

  goToCarburantSaisie(mat: string) {
    this.showView('carburant-saisie');
    setTimeout(() => {
      const vehicle = this.vehicles.find(v => v.mat === mat);
      if (vehicle) {
        this.fuelForm.selectedVehicle = vehicle;
        this.fillVehicleData();
      }
    });
  }

  // ---------- FUEL FORM ----------
  fillVehicleData() {
    const v = this.fuelForm.selectedVehicle;
    if (!v) return;
    this.fuelForm.typeCarb = v.type;
    this.fuelForm.prix = v.prix;
    this.fuelForm.coutMois = v.cout;
    this.fuelForm.idxDem = v.idx_dem !== null ? v.idx_dem : 0;
    this.fuelForm.montantRestantPrec = v.montant_restant_prec !== null ? v.montant_restant_prec : 0;
    this.fuelForm.ravitPrec = v.ravit_prec !== null ? v.ravit_prec : 0;
    this.fuelForm.visiteTech = v.visite || '';
    this.fuelForm.montantRestantFin = 0;
    this.fuelForm.idxFin = 0;
    this.calcCarburant();
  }

  calcCarburant() {
    const { prix, ravitPrec, montantRestantPrec, montantRestantFin, idxDem, idxFin, coutMois } = this.fuelForm;
    if (prix === 0) return;
    this.fuelForm.totalRavit = (ravitPrec + montantRestantPrec) / prix;
    this.fuelForm.qteRestante = montantRestantFin / prix;
    this.fuelForm.distance = idxFin - idxDem;
    this.fuelForm.pctConso = this.fuelForm.distance !== 0 ? ((this.fuelForm.totalRavit - this.fuelForm.qteRestante) / this.fuelForm.distance) * 100 : 0;
    this.fuelForm.carbDemande = coutMois - montantRestantFin;
  }

  clearCarburantForm() {
    this.fuelForm.selectedVehicle = null;
    this.fuelForm.montantRestantFin = 0;
    this.fuelForm.idxFin = 0;
    this.fuelForm.idxVidange = null;
    this.fuelForm.visiteTech = '';
    this.fuelForm.idxPneu = null;
    this.fuelForm.idxBatt = null;
    if (this.fuelForm.selectedVehicle) this.fillVehicleData();
    else this.calcCarburant();
  }

  saveCarburant() {
    if (!this.fuelForm.selectedVehicle) {
      this.showToast('Veuillez sélectionner un véhicule');
      return;
    }
    if (!this.fuelForm.idxFin) {
      this.showToast('Veuillez saisir l\'index fin du mois');
      return;
    }
    this.showToast('Carburant enregistré pour ' + this.fuelForm.selectedVehicle.mat + ' ✓');
    this.clearCarburantForm();
  }

  // ---------- FUEL LIST ----------
  renderCarburantListe() {
    // Filtering is done in template
  }

  getFilteredVehicles() {
    const search = this.fuelListFilter.search.toLowerCase();
    return this.vehicles.filter(v => {
      const fullName = (v.prenom + ' ' + v.nom).toLowerCase();
      return !search || v.mat.toLowerCase().includes(search) || fullName.includes(search);
    });
  }

  // ---------- MAINTENANCE FORM ----------
  calcDetailTotal() {
    this.newDetail.total = (this.newDetail.qte || 0) * (this.newDetail.montant || 0);
  }

  addDetail() {
    if (!this.newDetail.designation.trim()) {
      this.showToast('Veuillez saisir une désignation');
      return;
    }
    this.tempMaintenanceDetails.push({
      ...this.newDetail,
      total: (this.newDetail.qte || 0) * (this.newDetail.montant || 0)
    } as MaintenanceDetail);
    // Reset new detail
    this.newDetail = {
      type: 'MAIN_D_OEUVRE',
      numero: '',
      marque: '',
      npiece: '',
      designation: '',
      qte: 1,
      montant: 0,
      total: 0
    };
    this.renderDetailsTable();
  }

  removeDetail(index: number) {
    this.tempMaintenanceDetails.splice(index, 1);
    this.renderDetailsTable();
  }

  renderDetailsTable() {
    // No need, the template uses *ngFor directly
  }

  getTempMaintenanceTotal() {
    return this.tempMaintenanceDetails.reduce((sum, d) => sum + d.total, 0).toFixed(3);
  }

  getMoDetails(): MaintenanceDetail[] {
    return this.tempMaintenanceDetails.filter((d: MaintenanceDetail) => d.type === 'MAIN_D_OEUVRE');
  }

  getPieceDetails(): MaintenanceDetail[] {
    return this.tempMaintenanceDetails.filter((d: MaintenanceDetail) => d.type === 'PIECE');
  }

  clearMaintenanceForm() {
    this.maintenanceForm = {
      ndossier: '',
      vehicule: '',
      date: '',
      type: '',
      statut: 'EN_COURS',
      description: ''
    };
    this.tempMaintenanceDetails = [];
  }

  saveMaintenance() {
    if (!this.maintenanceForm.ndossier || !this.maintenanceForm.vehicule) {
      this.showToast('Veuillez remplir N° dossier et véhicule');
      return;
    }
    if (this.tempMaintenanceDetails.length === 0) {
      this.showToast('Ajoutez au moins un détail');
      return;
    }
    const totalHTVA = this.tempMaintenanceDetails.reduce((s, d) => s + d.total, 0);
    const entry: MaintenanceEntry = {
      vehicleId: '17-' + this.maintenanceForm.vehicule,
      totalHTVA: totalHTVA,
      brands: 'Citroen', // could be dynamic
      details: this.tempMaintenanceDetails.map(d => ({ ...d, ndossier: this.maintenanceForm.ndossier, vehicule: this.maintenanceForm.vehicule }))
    };
    this.maintenanceList.push(entry);
    this.showToast('Dossier ' + this.maintenanceForm.ndossier + ' soumis avec succès ✓');
    this.clearMaintenanceForm();
  }

  // ---------- MAINTENANCE LIST ----------
  renderMaintenanceListe() {
    // Filtering done in template
  }

  getFilteredMaintenance(): MaintenanceEntry[] {
    let list = this.maintenanceList;
    const search = this.maintenanceListFilter.search.toLowerCase();
    // const type = this.maintenanceListFilter.type.toLowerCase(); // non utilisé car pas de propriété type dans MaintenanceEntry
    if (search) {
      list = list.filter(m => m.vehicleId.toLowerCase().includes(search));
    }
    // Le filtre par type n'est pas implémenté car les entrées n'ont pas de propriété type
    return list;
  }

  showMaintenanceDetail(index: number) {
    this.selectedMaintenance = this.getFilteredMaintenance()[index];
  }

  closeMaintenanceDetail() {
    this.selectedMaintenance = null;
  }

  // ---------- GENERATOR FORM ----------
  fillGEData() {
    const ge = this.generatorForm.selectedGe;
    if (!ge) return;
    this.generatorForm.typeCarb = ge.carb;
    this.generatorForm.puissance = ge.kva || 'démonté';
    this.generatorForm.tauxConso = ge.taux;
    this.generatorForm.consoMax = ge.max;
    this.generatorForm.carte = ge.carte;
    this.generatorForm.userRoc = ge.user || '—';
    this.generatorForm.montantRestantFin = 0;
    this.generatorForm.idxFin = 0;
    this.calcGE();
  }

  calcGE() {
    const { prix, ravitPrec, montantRestantPrec, montantRestantFin, idxPrec, idxFin } = this.generatorForm;
    if (prix === 0) return;
    this.generatorForm.totalRavit = (ravitPrec + montantRestantPrec) / prix;
    this.generatorForm.qteRestante = montantRestantFin / prix;
    this.generatorForm.nbHeures = idxFin - idxPrec;
    this.generatorForm.pctConso = this.generatorForm.nbHeures !== 0 ? ((this.generatorForm.totalRavit - this.generatorForm.qteRestante) / this.generatorForm.nbHeures) * 100 : 0;
    this.generatorForm.carbDemande = (this.generatorForm.consoMax ? this.generatorForm.consoMax * prix : 200) - montantRestantFin;
    const maxH = this.generatorForm.tauxConso ?? 100;
    let evalText = '—';
    if (this.generatorForm.nbHeures > 0) {
      const ratio = this.generatorForm.pctConso / maxH * 100;
      evalText = ratio < 80 ? '✓ Conforme (' + ratio.toFixed(0) + '%)' : ratio < 100 ? '⚠ Proche limite' : '✗ Dépassé';
    }
    this.generatorForm.eval = evalText;
  }

  clearGEForm() {
    this.generatorForm.selectedGe = null;
    this.generatorForm.montantRestantFin = 0;
    this.generatorForm.idxFin = 0;
    this.fillGEData();
  }

  saveGE() {
    if (!this.generatorForm.selectedGe) {
      this.showToast('Veuillez sélectionner un site GE');
      return;
    }
    if (!this.generatorForm.idxFin) {
      this.showToast('Veuillez saisir l\'index fin de semestre');
      return;
    }
    this.showToast('Ravitaillement GE enregistré ✓');
    this.clearGEForm();
  }

  // ---------- GENERATOR LIST ----------
  renderGEListe() {
    // Filtering done in template
  }

  getFilteredGe() {
    let list = this.geData;
    const search = this.geListFilter.search.toLowerCase();
    const carb = this.geListFilter.carb.toLowerCase();
    if (search) {
      list = list.filter(g => g.site.toLowerCase().includes(search));
    }
    if (carb) {
      list = list.filter(g => g.carb.toLowerCase().includes(carb));
    }
    return list;
  }

  goToGESaisie(id: number) {
    this.showView('ge-saisie');
    setTimeout(() => {
      const ge = this.geData.find(g => g.id === id);
      if (ge) {
        this.generatorForm.selectedGe = ge;
        this.fillGEData();
      }
    });
  }

  // ---------- TOAST ----------
  showToast(message: string) {
    alert(message);
  }
}