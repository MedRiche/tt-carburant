import { TestBed } from '@angular/core/testing';

import { TechnicienEquipementService } from './technicien-equipement.service';

describe('TechnicienEquipementService', () => {
  let service: TechnicienEquipementService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TechnicienEquipementService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
