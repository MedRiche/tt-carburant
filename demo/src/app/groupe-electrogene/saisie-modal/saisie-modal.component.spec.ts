import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SaisieModalComponent } from './saisie-modal.component';

describe('SaisieModalComponent', () => {
  let component: SaisieModalComponent;
  let fixture: ComponentFixture<SaisieModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SaisieModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SaisieModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
