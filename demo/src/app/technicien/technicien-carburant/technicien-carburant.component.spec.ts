import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TechnicienCarburantComponent } from './technicien-carburant.component';

describe('TechnicienCarburantComponent', () => {
  let component: TechnicienCarburantComponent;
  let fixture: ComponentFixture<TechnicienCarburantComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TechnicienCarburantComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TechnicienCarburantComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
