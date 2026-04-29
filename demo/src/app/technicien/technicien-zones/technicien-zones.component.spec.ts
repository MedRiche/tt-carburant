import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TechnicienZonesComponent } from './technicien-zones.component';

describe('TechnicienZonesComponent', () => {
  let component: TechnicienZonesComponent;
  let fixture: ComponentFixture<TechnicienZonesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TechnicienZonesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TechnicienZonesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
