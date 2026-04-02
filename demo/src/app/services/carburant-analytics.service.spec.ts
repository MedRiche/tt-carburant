import { TestBed } from '@angular/core/testing';

import { CarburantAnalyticsService } from './carburant-analytics.service';

describe('CarburantAnalyticsService', () => {
  let service: CarburantAnalyticsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CarburantAnalyticsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
