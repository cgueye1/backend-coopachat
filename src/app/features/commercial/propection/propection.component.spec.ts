import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PropectionComponent } from './propection.component';

describe('PropectionComponent', () => {
  let component: PropectionComponent;
  let fixture: ComponentFixture<PropectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PropectionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PropectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
