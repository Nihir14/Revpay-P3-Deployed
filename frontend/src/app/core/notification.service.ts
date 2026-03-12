import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../shared/constants';

declare var Swal: any;

export interface NotificationDTO {
    id: number;
    userId: number;
    message: string;
    isRead: boolean;
    createdAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class NotificationService {
    private apiUrl = `${environment.apiUrl}/notifications`;

    constructor(private http: HttpClient) { }

    getNotifications(page: number, size: number): Observable<any> {
        const userId = localStorage.getItem('userId');
        return this.http.get<any>(`${this.apiUrl}/user/${userId}?page=${page}&size=${size}`);
    }

    markAsRead(id: number): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/${id}/read`, {});
    }

    testNotification(): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/test`, {});
    }

    success(message: string, title: string = 'Success') {
        return Swal.fire({
            icon: 'success',
            title: title,
            text: message,
            confirmButtonColor: '#3085d6',
            confirmButtonText: 'Great!'
        });
    }

    error(message: string, title: string = 'Error') {
        return Swal.fire({
            icon: 'error',
            title: title,
            text: message,
            confirmButtonColor: '#d33',
            confirmButtonText: 'Try Again'
        });
    }

    info(message: string, title: string = 'Info') {
        return Swal.fire({
            icon: 'info',
            title: title,
            text: message
        });
    }

    confirm(message: string, title: string = 'Are you sure?') {
        return Swal.fire({
            title: title,
            text: message,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Yes, proceed!',
            cancelButtonText: 'Cancel'
        });
    }

    promptPin(message: string = 'Please enter your Transaction PIN to authorize this action') {
        return Swal.fire({
            title: 'Authorize Transaction',
            text: message,
            input: 'password',
            inputLabel: 'Transaction PIN',
            inputPlaceholder: 'Enter 4-6 digit PIN',
            inputAttributes: {
                maxlength: '6',
                autocapitalize: 'off',
                autocorrect: 'off'
            },
            showCancelButton: true,
            confirmButtonText: 'Confirm',
            showLoaderOnConfirm: true,
            preConfirm: (pin: string) => {
                if (!pin || pin.length < 4) {
                    Swal.showValidationMessage('PIN must be at least 4 digits');
                }
                return pin;
            },
            allowOutsideClick: () => !Swal.isLoading()
        });
    }
}
