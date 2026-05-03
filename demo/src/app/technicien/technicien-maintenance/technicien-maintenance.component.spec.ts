import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TechnicienMaintenanceComponent } from './technicien-maintenance.component';

describe('TechnicienMaintenanceComponent', () => {
  let component: TechnicienMaintenanceComponent;
  let fixture: ComponentFixture<TechnicienMaintenanceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TechnicienMaintenanceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TechnicienMaintenanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
