import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { TransactionsComponent } from './transactions';

describe('TransactionsComponent', () => {
    let component: TransactionsComponent;
    let fixture: ComponentFixture<TransactionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TransactionsComponent, HttpClientTestingModule],
            providers: [provideRouter([])]
        })
            .compileComponents();

        fixture = TestBed.createComponent(TransactionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
