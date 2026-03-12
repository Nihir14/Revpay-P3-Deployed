import { Component, ElementRef, ViewChild, HostListener, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../auth/auth.service';
import { NotificationService, NotificationDTO } from '../notification.service';

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class NavbarComponent implements OnInit, OnDestroy {
  showNotifications = false;
  notificationsEnabled: boolean = true;
  notifications: NotificationDTO[] = [];
  unreadCount = 0;
  isAuthPage = false;
  private authSub?: Subscription;
  private routerSub?: Subscription;

  @ViewChild('bellIcon') bellIcon?: ElementRef;

  constructor(
    public authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) { }

  ngOnInit() {
    this.checkRoute(this.router.url);
    this.routerSub = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.checkRoute(event.urlAfterRedirects || event.url);
    });

    const savedToggle = localStorage.getItem('notificationsEnabled');
    if (savedToggle !== null) {
      this.notificationsEnabled = savedToggle === 'true';
    }

    if (this.authService.isLoggedIn() && this.notificationsEnabled) {
      this.loadNotifications();
    }
    this.authSub = this.authService.authStatusChanged.subscribe(isLoggedIn => {
      if (isLoggedIn && this.notificationsEnabled) {
        this.loadNotifications();
      } else {
        this.clearNotifications();
      }
    });
  }

  clearNotifications() {
    this.notifications = [];
    this.unreadCount = 0;
    this.showNotifications = false;
  }

  ngOnDestroy() {
    if (this.authSub) {
      this.authSub.unsubscribe();
    }
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
  }

  private checkRoute(url: string) {
    this.isAuthPage = url.includes('/login') || url.includes('/register');
  }

  loadNotifications() {
    this.notificationService.getNotifications(0, 20).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          // Flatten 'read' vs 'isRead' API nuances
          this.notifications = res.data.content.map((n: any) => {
            n.isRead = n.isRead === true || n.read === true;
            return n;
          });
          this.unreadCount = this.notifications.filter((n: any) => !n.isRead).length;
        }
      },
      error: (err: any) => {
        console.error('Failed to load notifications', err);
      }
    });
  }

  markAsRead(notification: NotificationDTO) {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.isRead = true;
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        },
        error: (err: any) => console.error('Failed to mark notification as read', err)
      });
    }
  }

  testNotification() {
    this.notificationService.testNotification().subscribe({
      next: () => {
        this.loadNotifications(); // Reload to show the new notification
      },
      error: (err: any) => console.error('Test notification failed', err)
    });
  }

  toggleNotifications(event: Event) {
    event.stopPropagation();
    this.showNotifications = !this.showNotifications;
    // Auto refresh when opening if enabled
    if (this.showNotifications && this.notificationsEnabled) {
      this.loadNotifications();
    }
  }

  toggleNotificationSystem() {
    this.notificationsEnabled = !this.notificationsEnabled;
    localStorage.setItem('notificationsEnabled', this.notificationsEnabled.toString());

    if (this.notificationsEnabled) {
      this.loadNotifications();
    } else {
      this.notifications = [];
      this.unreadCount = 0;
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.showNotifications && this.bellIcon && !this.bellIcon.nativeElement.contains(event.target)) {
      this.showNotifications = false;
    }
  }

  getDashboardRoute(): string {
    const role = this.authService.getUserRole();
    if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
      return '/admin/dashboard';
    }
    // Both USER and BUSINESS roles should land on the personal wallet dashboard
    return '/dashboard';
  }

  logout() { this.authService.logout(); this.router.navigate(['/login']); }
}
