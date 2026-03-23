import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CarburantFormComponent } from './carburant-form.component';

describe('CarburantFormComponent', () => {
  let component: CarburantFormComponent;
  let fixture: ComponentFixture<CarburantFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CarburantFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CarburantFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
