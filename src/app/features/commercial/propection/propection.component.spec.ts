import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProspectionComponent } from './propection.component';

describe('ProspectionComponent', () => {
  let component: ProspectionComponent;
  let fixture: ComponentFixture<ProspectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProspectionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProspectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
