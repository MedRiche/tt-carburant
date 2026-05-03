import { TestBed } from '@angular/core/testing';

import { TechnicienCarburantService } from './technicien-carburant.service';

describe('TechnicienCarburantService', () => {
  let service: TechnicienCarburantService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TechnicienCarburantService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
