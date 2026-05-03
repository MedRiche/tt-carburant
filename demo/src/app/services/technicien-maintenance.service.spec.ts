import { TestBed } from '@angular/core/testing';

import { TechnicienMaintenanceService } from './technicien-maintenance.service';

describe('TechnicienMaintenanceService', () => {
  let service: TechnicienMaintenanceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TechnicienMaintenanceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
