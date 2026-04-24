import { TestBed } from '@angular/core/testing';

import { GroupeElectrogeneService } from './groupe-electrogene.service';

describe('GroupeElectrogeneService', () => {
  let service: GroupeElectrogeneService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GroupeElectrogeneService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
