import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionCarburantGeFormComponent } from './gestion-carburant-ge-form.component';

describe('GestionCarburantGeFormComponent', () => {
  let component: GestionCarburantGeFormComponent;
  let fixture: ComponentFixture<GestionCarburantGeFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GestionCarburantGeFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionCarburantGeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
