import { TestBed } from '@angular/core/testing';

import { VerrouillageService } from './verrouillage.service';

describe('VerrouillageService', () => {
  let service: VerrouillageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(VerrouillageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
