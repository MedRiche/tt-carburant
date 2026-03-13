// models/zone.model.ts
export interface Zone {
  id: number;
  nom: string;
  description?: string;
  responsable?: string;
  dateCreation?: Date;
  nombreUtilisateurs?: number;
}

export interface ZoneRequest {
  nom: string;
  description?: string;
  responsable?: string;
}