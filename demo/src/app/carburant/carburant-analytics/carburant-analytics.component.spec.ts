import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CarburantAnalyticsComponent } from './carburant-analytics.component';

describe('CarburantAnalyticsComponent', () => {
  let component: CarburantAnalyticsComponent;
  let fixture: ComponentFixture<CarburantAnalyticsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CarburantAnalyticsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CarburantAnalyticsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
