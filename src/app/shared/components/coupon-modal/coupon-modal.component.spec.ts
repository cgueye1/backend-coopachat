import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CouponModalComponent } from './coupon-modal.component';

describe('CouponModalComponent', () => {
  let component: CouponModalComponent;
  let fixture: ComponentFixture<CouponModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CouponModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CouponModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
