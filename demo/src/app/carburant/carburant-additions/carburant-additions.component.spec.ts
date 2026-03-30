import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CarburantAdditionsComponent } from './carburant-additions.component';

describe('CarburantAdditionsComponent', () => {
  let component: CarburantAdditionsComponent;
  let fixture: ComponentFixture<CarburantAdditionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CarburantAdditionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CarburantAdditionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
