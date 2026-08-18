import { Component, inject, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../services/auth.service';
import { CivicNotification, NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    FormsModule
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit {

  @Input() sidenavOpened = true;

  @Output() toggleSidenav = new EventEmitter<void>();

  auth = inject(AuthService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);

  searchTerm = '';
  notifications: CivicNotification[] = [];
  searchResults: { type: 'Petition'|'Poll'; id: number; title: string; description: string; link: string }[] = [];

  get user() {
    return this.auth.getUser();
  }
  ngOnInit(): void { this.loadNotifications(); }

  logout() {
    this.auth.logout();
  }

  get unreadNotifications(): number {
    return this.notifications.length;
  }

  search(): void {
    const query = this.searchTerm.trim();
    if (!query) return;

    this.router.navigate(['/search'], { queryParams: { q: query } });
  }

  loadNotifications(): void { this.notificationService.getAll().subscribe({next: items => this.notifications = items, error: () => this.notifications = []}); }
  openNotification(notification: CivicNotification): void { this.notificationService.consume(notification.id).subscribe({next: () => { this.notifications = this.notifications.filter(item => item.id !== notification.id); if (notification.link) this.router.navigateByUrl(notification.link); }}); }

}
