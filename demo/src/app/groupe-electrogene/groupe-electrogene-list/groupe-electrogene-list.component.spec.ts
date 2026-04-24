import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupeElectrogeneListComponent } from './groupe-electrogene-list.component';

describe('GroupeElectrogeneListComponent', () => {
  let component: GroupeElectrogeneListComponent;
  let fixture: ComponentFixture<GroupeElectrogeneListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GroupeElectrogeneListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GroupeElectrogeneListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
