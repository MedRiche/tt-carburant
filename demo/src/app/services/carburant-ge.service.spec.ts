import { TestBed } from '@angular/core/testing';

import { CarburantGeService } from './carburant-ge.service';

describe('CarburantGeService', () => {
  let service: CarburantGeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CarburantGeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
