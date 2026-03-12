import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { SendMoneyComponent } from './send-money';

describe('SendMoneyComponent', () => {
    let component: SendMoneyComponent;
    let fixture: ComponentFixture<SendMoneyComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [SendMoneyComponent, HttpClientTestingModule],
            providers: [provideRouter([])]
        })
            .compileComponents();

        fixture = TestBed.createComponent(SendMoneyComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
