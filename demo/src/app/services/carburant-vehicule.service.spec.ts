import { TestBed } from '@angular/core/testing';

import { CarburantVehiculeService } from './carburant-vehicule.service';

describe('CarburantVehiculeService', () => {
  let service: CarburantVehiculeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CarburantVehiculeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
