import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupeElectrogeneFormComponent } from './groupe-electrogene-form.component';

describe('GroupeElectrogeneFormComponent', () => {
  let component: GroupeElectrogeneFormComponent;
  let fixture: ComponentFixture<GroupeElectrogeneFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GroupeElectrogeneFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GroupeElectrogeneFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
