import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TechnicienEquipementsComponent } from './technicien-equipements.component';

describe('TechnicienEquipementsComponent', () => {
  let component: TechnicienEquipementsComponent;
  let fixture: ComponentFixture<TechnicienEquipementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TechnicienEquipementsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TechnicienEquipementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
