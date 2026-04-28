// src/app/services/excel-export.service.ts
// Requires: npm install exceljs file-saver @types/file-saver
// npm install exceljs && npm install file-saver && npm install --save-dev @types/file-saver

import { Injectable } from '@angular/core';
import { GroupeElectrogene } from '../models/groupe-electrogene';
import { GestionCarburantGE } from '../models/gestion-carburant-ge';

/* ─────────────────────────────────────────────────────────────────────
   COLOUR PALETTE (matching the reference Excel file)
   ───────────────────────────────────────────────────────────────────── */
const DARK_BLUE  = 'FF1F497D';   // Title row
const MED_BLUE   = 'FF4472C4';   // Column headers
const LT_BLUE    = 'FF5B9BD5';   // Saisie section header
const ORANGE_COL = 'FFED7D31';   // Calc section header
const SAISIE_BG  = 'FFBDD7EE';   // Saisie sub-header bg
const CALC_BG    = 'FFFCE4D6';   // Calc sub-header bg
const SAISIE_DATA= 'FFDDEEFF';   // Saisie data cells
const CALC_DATA  = 'FFFFF0E8';   // Calc data cells
const EVEN_ROW   = 'FFFFFFFF';   // White
const ODD_ROW    = 'FFEBF3FB';   // Light blue-white
const ALT_ODD    = 'FFF5F8FE';   // Very light
const GREEN_EVAL = 'FFE2EFDA';
const RED_EVAL   = 'FFFFDDDD';
const TOTALS_BG  = 'FF1F497D';

@Injectable({ providedIn: 'root' })
export class ExcelExportService {

  /* ══════════════════════════════════════════════════════════════
     Export 1 — Groupes Électrogènes (full list + Agilis details)
     ══════════════════════════════════════════════════════════════ */
  async exportGroupes(groupes: GroupeElectrogene[]): Promise<void> {
    const ExcelJS = await import('exceljs');
    const { saveAs } = await import('file-saver');

    const wb = new ExcelJS.Workbook();
    wb.creator = 'TT Énergie';
    wb.created = new Date();

    const ws = wb.addWorksheet('Groupes Électrogènes', {
      views: [{ state: 'frozen', ySplit: 3 }]
    });

    /* ── Title ── */
    ws.mergeCells('A1:M1');
    const title = ws.getCell('A1');
    title.value = 'GESTION DES GROUPES ÉLECTROGÈNES — TT ÉNERGIE';
    title.font  = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' }, size: 14 };
    title.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: DARK_BLUE } };
    title.alignment = { horizontal: 'center', vertical: 'middle' };
    ws.getRow(1).height = 32;

    /* ── Subtitle ── */
    ws.mergeCells('A2:M2');
    const sub = ws.getCell('A2');
    sub.value = `Exporté le ${new Date().toLocaleDateString('fr-TN', { day: '2-digit', month: '2-digit', year: 'numeric' })} — ${groupes.length} site(s) configuré(s)`;
    sub.font  = { name: 'Arial', italic: true, color: { argb: 'FF4472C4' }, size: 10 };
    sub.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFEBF3FB' } };
    sub.alignment = { horizontal: 'center', vertical: 'middle' };
    ws.getRow(2).height = 18;

    /* ── Column definitions ── */
    const cols: { header: string; key: string; width: number }[] = [
      { header: 'Site',              key: 'site',                          width: 20 },
      { header: 'Type Carburant',    key: 'typeCarburant',                 width: 20 },
      { header: 'Puissance (KVA)',   key: 'puissanceKVA',                  width: 14 },
      { header: 'Taux Conso (L/h)', key: 'tauxConsommationParHeure',       width: 14 },
      { header: 'Conso Max Sem. (L)',key: 'consommationTotaleMaxParSemestre',width: 16 },
      { header: 'Prix (DT/L)',       key: 'prixCarburant',                 width: 12 },
      { header: 'Zone',              key: 'zoneNom',                       width: 16 },
      { header: 'Type Carte',        key: 'typeCarte',                     width: 16 },
      { header: 'N° Carte',          key: 'numeroCarte',                   width: 22 },
      { header: 'Date Expiration',   key: 'dateExpiration',                width: 14 },
      { header: 'Code PIN',          key: 'codePIN',                       width: 12 },
      { header: 'Code PUK',          key: 'codePUK',                       width: 14 },
      { header: 'Utilisateur ROC',   key: 'utilisateurRoc',                width: 20 },
    ];

    ws.columns = cols.map(c => ({ key: c.key, width: c.width }));

    /* ── Header row ── */
    const hdrRow = ws.getRow(3);
    cols.forEach((col, idx) => {
      const cell = hdrRow.getCell(idx + 1);
      cell.value = col.header;
      cell.font  = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' }, size: 10 };
      cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: MED_BLUE } };
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true };
      cell.border = this.thinBorder();
    });
    hdrRow.height = 40;

    const TYPE_LABELS: Record<string, string> = {
      GASOIL_ORDINAIRE:   'Gasoil Ordinaire',
      GASOIL_SANS_SOUFRE: 'Gasoil Sans Soufre',
      SUPER_SANS_PLOMB:   'Super Sans Plomb',
      ESSENCE:            'Essence',
    };

    /* ── Data rows ── */
    groupes.forEach((ge, idx) => {
      const rowIdx = idx + 4;
      const bg = idx % 2 === 0 ? EVEN_ROW : ODD_ROW;
      const row = ws.getRow(rowIdx);

      const values = [
        ge.site,
        TYPE_LABELS[ge.typeCarburant] ?? ge.typeCarburant,
        ge.puissanceKVA     ?? '—',
        ge.tauxConsommationParHeure ?? '—',
        ge.consommationTotaleMaxParSemestre ?? '—',
        ge.prixCarburant    ?? '—',
        ge.zoneNom          || '—',
        ge.typeCarte        || '—',
        ge.numeroCarte      || '—',
        ge.dateExpiration   || '—',
        ge.codePIN          || '—',
        ge.codePUK          || '—',
        ge.utilisateurRoc   || '—',
      ];

      values.forEach((val, ci) => {
        const cell = row.getCell(ci + 1);
        cell.value = val;
        cell.font  = { name: 'Arial', size: 10 };
        cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: bg } };
        cell.alignment = {
          horizontal: ci === 0 || ci === 1 ? 'left' : 'center',
          vertical: 'middle',
          wrapText: true
        };
        cell.border = this.thinBorder();
      });

      /* Prix highlight */
      if (ge.prixCarburant) {
        const priceCell = row.getCell(6);
        priceCell.font = { name: 'Arial', size: 10, bold: true, color: { argb: 'FF006600' } };
        priceCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2EFDA' } };
      }
      row.height = 20;
    });

    const buf = await wb.xlsx.writeBuffer();
    saveAs(new Blob([buf]), `Groupes_Electrogenes_${new Date().toISOString().slice(0,10)}.xlsx`);
  }

  /* ══════════════════════════════════════════════════════════════
     Export 2 — Gestion Carburant GE (saisies semestrielles)
     ══════════════════════════════════════════════════════════════ */
  async exportGestionCarburant(
    groupes: GroupeElectrogene[],
    saisies: GestionCarburantGE[],
    annee: number,
    semestre: string
  ): Promise<void> {
    const ExcelJS = await import('exceljs');
    const { saveAs } = await import('file-saver');

    const wb = new ExcelJS.Workbook();
    wb.creator = 'TT Énergie';
    wb.created = new Date();

    const semLabel = semestre === 'PREMIER' ? '1er Semestre' : '2ème Semestre';
    const saisiesMap = new Map<string, GestionCarburantGE>(
      saisies.map(s => [s.site, s])
    );

    const ws = wb.addWorksheet(`Carburant GE S${semestre === 'PREMIER' ? 1 : 2} ${annee}`, {
      views: [{ state: 'frozen', ySplit: 4 }]
    });

    /* ── Title ── */
    ws.mergeCells('A1:Q1');
    const title = ws.getCell('A1');
    title.value = `GESTION CARBURANT — GROUPES ÉLECTROGÈNES — TT ÉNERGIE`;
    title.font  = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' }, size: 14 };
    title.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: DARK_BLUE } };
    title.alignment = { horizontal: 'center', vertical: 'middle' };
    ws.getRow(1).height = 32;

    /* ── Subtitle ── */
    ws.mergeCells('A2:Q2');
    const sub = ws.getCell('A2');
    sub.value = `${semLabel} ${annee} — ${groupes.length} site(s) — Exporté le ${new Date().toLocaleDateString('fr-TN')}`;
    sub.font  = { name: 'Arial', italic: true, color: { argb: 'FF4472C4' }, size: 10 };
    sub.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFEBF3FB' } };
    sub.alignment = { horizontal: 'center', vertical: 'middle' };
    ws.getRow(2).height = 18;

    /* ── Column widths ── */
    const colWidths = [18,16,12,14,10, 13,17,17,15,13, 13,12,12,10,15, 12,14];
    colWidths.forEach((w, i) => {
      ws.getColumn(i + 1).width = w;
    });

    /* ── Row 3 — group headers ── */
    const r3 = ws.getRow(3);

    const applyGroupHeader = (colLetter: string, text: string, argb: string) => {
      const cell = ws.getCell(`${colLetter}3`);
      cell.value = text;
      cell.font  = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' }, size: 10 };
      cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb } };
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true };
      cell.border = this.thinBorder();
    };

    // Static headers (will be merged with row 4)
    ['A','B','C','D','E'].forEach((col, i) => {
      const labels = ['Site','Type Carburant','Taux Conso\n(L/h)','Conso Max\nSem. (L)','Prix\n(DT/L)'];
      applyGroupHeader(col, labels[i], MED_BLUE);
    });
    applyGroupHeader('F',  'DONNÉES À SAISIR',        LT_BLUE);
    applyGroupHeader('K',  'CALCULÉS AUTOMATIQUEMENT', ORANGE_COL);
    applyGroupHeader('Q',  'Zone',                     MED_BLUE);

    ws.mergeCells('A3:A4');
    ws.mergeCells('B3:B4');
    ws.mergeCells('C3:C4');
    ws.mergeCells('D3:D4');
    ws.mergeCells('E3:E4');
    ws.mergeCells('F3:J3');
    ws.mergeCells('K3:P3');
    ws.mergeCells('Q3:Q4');
    r3.height = 26;

    /* ── Row 4 — sub-headers ── */
    const r4 = ws.getRow(4);
    const saisieSubHdrs = [
      'Index Début\n(h) (1)',
      'Montant Réserv.\nPréc. (DT) (2)',
      'Ravitaillement\nPréc. (DT) (3)',
      'Montant Fin\nSem. (DT) (4)',
      'Index Fin\n(h) (5)',
    ];
    const calcSubHdrs = [
      'Total Ravit.\n(L)',
      'Qté Restante\n(L)',
      'Nb Heures\n(5)-(1)',
      '% Conso',
      'Carburant\nDemandé (DT)',
      'Évaluation',
    ];

    saisieSubHdrs.forEach((h, i) => {
      const cell = r4.getCell(6 + i);
      cell.value = h;
      cell.font  = { name: 'Arial', bold: true, size: 9, color: { argb: 'FF1F497D' } };
      cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: SAISIE_BG } };
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true };
      cell.border = this.thinBorder();
    });

    calcSubHdrs.forEach((h, i) => {
      const cell = r4.getCell(11 + i);
      cell.value = h;
      cell.font  = { name: 'Arial', bold: true, size: 9, color: { argb: 'FF7F3F00' } };
      cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: CALC_BG } };
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true };
      cell.border = this.thinBorder();
    });

    r4.height = 44;

    /* ── Data rows ── */
    const TYPE_LABELS: Record<string, string> = {
      GASOIL_ORDINAIRE: 'Gasoil Ord.', GASOIL_SANS_SOUFRE: 'Gasoil SS',
      SUPER_SANS_PLOMB: 'Super SP',    ESSENCE: 'Essence',
    };

    let totalRavit = 0, totalHeures = 0, totalDemande = 0;

    groupes.forEach((ge, idx) => {
      const rowIdx = idx + 5;
      const s = saisiesMap.get(ge.site);
      const bg = idx % 2 === 0 ? EVEN_ROW : ALT_ODD;
      const row = ws.getRow(rowIdx);

      const staticVals = [
        ge.site,
        TYPE_LABELS[ge.typeCarburant] ?? ge.typeCarburant,
        ge.tauxConsommationParHeure ?? '—',
        ge.consommationTotaleMaxParSemestre ?? '—',
        ge.prixCarburant ?? '—',
      ];

      staticVals.forEach((val, ci) => {
        const cell = row.getCell(ci + 1);
        cell.value = val;
        cell.font  = { name: 'Arial', size: 10 };
        cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: bg } };
        cell.alignment = { horizontal: ci < 2 ? 'left' : 'center', vertical: 'middle' };
        cell.border = this.thinBorder();
      });

      /* Prix highlight */
      if (ge.prixCarburant) {
        row.getCell(5).font = { name: 'Arial', size: 10, bold: true, color: { argb: 'FF006600' } };
        row.getCell(5).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2EFDA' } };
      }

      if (s) {
        /* Saisie columns (6-10) */
        const saisieVals = [
          s.indexHeureSemestrePrecedent               ?? '—',
          s.montantCarburantRestantReservoirPrecedent ?? '—',
          s.ravitaillementSemestrePrecedentDinars     ?? '—',
          s.montantRestantAgilisFinSemestre           ?? '—',
          s.indexFinSemestre                         ?? '—',
        ];
        saisieVals.forEach((val, ci) => {
          const cell = row.getCell(6 + ci);
          cell.value = val;
          cell.font  = { name: 'Arial', size: 10 };
          cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: SAISIE_DATA } };
          cell.alignment = { horizontal: 'right', vertical: 'middle' };
          cell.border = this.thinBorder();
          if (typeof val === 'number') cell.numFmt = '#,##0.000';
        });

        /* Calc columns (11-16) */
        const calcVals = [
          s.totalRavitaillementLitres          ?? '—',
          s.quantiteRestanteReservoirAgilis    ?? '—',
          s.nbHeuresTravail                   ?? '—',
          s.pourcentageConsommation != null ? s.pourcentageConsommation / 100 : '—',
          s.carburantDemandeDinarsCours        ?? '—',
          s.evaluationTauxConsommation         ?? '—',
        ];
        calcVals.forEach((val, ci) => {
          const cell = row.getCell(11 + ci);
          cell.value = val;
          cell.font  = { name: 'Arial', size: 10, bold: true };
          cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: CALC_DATA } };
          cell.alignment = { horizontal: 'center', vertical: 'middle' };
          cell.border = this.thinBorder();
          if (ci === 3 && typeof val === 'number') cell.numFmt = '0.0%';
          else if (typeof val === 'number') cell.numFmt = '#,##0.0';
        });

        /* Evaluation coloring */
        const evalCell = row.getCell(16);
        if (s.evaluationTauxConsommation === 'OUI') {
          evalCell.font = { name: 'Arial', bold: true, size: 10, color: { argb: 'FF006600' } };
          evalCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2EFDA' } };
        } else if (s.evaluationTauxConsommation === 'NON') {
          evalCell.font = { name: 'Arial', bold: true, size: 10, color: { argb: 'FFCC0000' } };
          evalCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFDDDD' } };
        }

        /* % Conso coloring */
        const pct = s.pourcentageConsommation ?? 0;
        const pctCell = row.getCell(14);
        if (pct <= 80)        pctCell.font = { name: 'Arial', bold: true, size: 10, color: { argb: 'FF006600' } };
        else if (pct <= 100)  pctCell.font = { name: 'Arial', bold: true, size: 10, color: { argb: 'FFB8860B' } };
        else                  pctCell.font = { name: 'Arial', bold: true, size: 10, color: { argb: 'FFCC0000' } };

        totalRavit   += s.totalRavitaillementLitres    ?? 0;
        totalHeures  += s.nbHeuresTravail              ?? 0;
        totalDemande += s.carburantDemandeDinarsCours  ?? 0;
      } else {
        /* No saisie — empty cells */
        for (let ci = 6; ci <= 16; ci++) {
          const cell = row.getCell(ci);
          cell.value = '—';
          cell.font  = { name: 'Arial', size: 10, color: { argb: 'FF888888' } };
          cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: bg } };
          cell.alignment = { horizontal: 'center', vertical: 'middle' };
          cell.border = this.thinBorder();
        }
      }

      /* Zone column */
      const zoneCell = row.getCell(17);
      zoneCell.value = ge.zoneNom || '—';
      zoneCell.font  = { name: 'Arial', size: 10 };
      zoneCell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: bg } };
      zoneCell.alignment = { horizontal: 'center', vertical: 'middle' };
      zoneCell.border = this.thinBorder();

      row.height = 20;
    });

    /* ── Totals row ── */
    const totRow = groupes.length + 5;
    ws.mergeCells(`A${totRow}:E${totRow}`);
    const totLabel = ws.getCell(`A${totRow}`);
    totLabel.value = 'TOTAUX';
    totLabel.font  = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' }, size: 11 };
    totLabel.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: DARK_BLUE } };
    totLabel.alignment = { horizontal: 'right', vertical: 'middle' };
    totLabel.border = this.thinBorder();

    for (let ci = 6; ci <= 17; ci++) {
      const cell = ws.getCell(totRow, ci);
      let val: string | number = '—';
      if (ci === 11) val = totalRavit;
      else if (ci === 13) val = totalHeures;
      else if (ci === 15) val = totalDemande;
      cell.value = val;
      cell.font  = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' }, size: 10 };
      cell.fill  = { type: 'pattern', pattern: 'solid', fgColor: { argb: DARK_BLUE } };
      cell.alignment = { horizontal: 'center', vertical: 'middle' };
      cell.border = this.thinBorder();
      if (typeof val === 'number') {
        if (ci === 11) cell.numFmt = '#,##0.0 "L"';
        else if (ci === 13) cell.numFmt = '#,##0 "h"';
        else if (ci === 15) cell.numFmt = '#,##0 "DT"';
      }
    }
    ws.getRow(totRow).height = 22;

    const buf = await wb.xlsx.writeBuffer();
    saveAs(
      new Blob([buf]),
      `Gestion_Carburant_GE_S${semestre === 'PREMIER' ? 1 : 2}_${annee}_${new Date().toISOString().slice(0,10)}.xlsx`
    );
  }

  /* ── Helper ── */
  private thinBorder() {
    const s = { style: 'thin' as const, color: { argb: 'FFBFBFBF' } };
    return { top: s, bottom: s, left: s, right: s };
  }
}