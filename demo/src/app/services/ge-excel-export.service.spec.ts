import { TestBed } from '@angular/core/testing';

import { GEExcelExportService } from './ge-excel-export.service';

describe('GEExcelExportService', () => {
  let service: GEExcelExportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GEExcelExportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
