import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AdminService } from '../../services/admin.service';
import { DepartmentService, Department } from '../../services/department.service';
import { User } from '../../models/user.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './super-admin.component.html',
  styleUrl: './super-admin.component.css'
})
export class SuperAdminComponent implements OnInit {
  private admin = inject(AdminService);
  private departmentsService = inject(DepartmentService);
  private toast = inject(ToastService);

  stats: any = {};
  officials: User[] = [];
  pending: User[] = [];
  citizens: User[] = [];
  departments: Department[] = [];
  categories: string[] = [];

  selectedDepartment: Record<number, string> = {};
  designation: Record<number, string> = {};
  newDepartmentName = '';
  newCategoryName = '';
  officialPage = 0;
  citizenPage = 0;
  readonly pageSize = 10;
  get visibleOfficials(): User[] { return this.officials.slice(this.officialPage * this.pageSize, (this.officialPage + 1) * this.pageSize); }
  get visibleCitizens(): User[] { return this.citizens.slice(this.citizenPage * this.pageSize, (this.citizenPage + 1) * this.pageSize); }
  get officialPages(): number { return Math.max(1, Math.ceil(this.officials.length / this.pageSize)); }
  get citizenPages(): number { return Math.max(1, Math.ceil(this.citizens.length / this.pageSize)); }

  private readonly defaultCategories = ['Environment', 'Infrastructure', 'Education', 'Public Safety', 'Transportation', 'Healthcare', 'Housing', 'Other'];

  ngOnInit(): void {
    this.loadCategories();
    this.load();
  }

  load(): void {
    this.admin.dashboard().subscribe({ next: s => this.stats = s });
    this.admin.officials().subscribe({ next: x => this.officials = x });
    this.admin.pendingOfficials().subscribe({ next: x => this.pending = x });
    this.admin.citizens().subscribe({ next: x => this.citizens = x });
    this.departmentsService.getAll().subscribe({ next: x => this.departments = x });
  }

  approve(u: User): void {
    this.admin.manageOfficial(u.id, {
      approved: true,
      active: true,
      department: this.selectedDepartment[u.id],
      designation: this.designation[u.id] || u.designation
    }).subscribe({
      next: () => { this.toast.success('Official approved and access granted.'); this.load(); },
      error: e => this.toast.error(e.error?.message || 'Unable to approve official.')
    });
  }

  reject(u: User): void {
    this.admin.manageOfficial(u.id, {
      approved: false,
      active: false,
      department: u.department,
      designation: u.designation
    }).subscribe({ next: () => { this.toast.success('Official rejected.'); this.load(); } });
  }

  toggle(u: User): void {
    this.admin.manageOfficial(u.id, {
      approved: u.verified,
      active: !u.active,
      department: u.department,
      designation: u.designation
    }).subscribe({ next: () => this.load() });
  }
  toggleCitizen(u: User): void { this.admin.manageCitizen(u.id, !u.active).subscribe({ next: updated => u.active = updated.active, error: e => this.toast.error(e.error?.message || 'Unable to update citizen status.') }); }

  createDepartment(): void {
    const name = this.newDepartmentName.trim();
    if (!name) return;
    this.departmentsService.create(name).subscribe({
      next: () => { this.toast.success('Department created.'); this.newDepartmentName = ''; this.load(); },
      error: e => this.toast.error(e.error?.message || 'Unable to create department.')
    });
  }

  deactivateDepartment(department: Department): void {
    this.departmentsService.delete(department.id).subscribe({
      next: () => { this.toast.success('Department deactivated.'); this.load(); },
      error: e => this.toast.error(e.error?.message || 'Unable to update department.')
    });
  }

  addCategory(): void {
    const name = this.newCategoryName.trim();
    if (!name || this.categories.some(category => category.toLowerCase() === name.toLowerCase())) return;
    this.categories = [...this.categories, name].sort();
    localStorage.setItem('civix_admin_categories', JSON.stringify(this.categories));
    this.newCategoryName = '';
    this.toast.success('Category added to admin list.');
  }

  removeCategory(category: string): void {
    this.categories = this.categories.filter(item => item !== category);
    localStorage.setItem('civix_admin_categories', JSON.stringify(this.categories));
  }

  private loadCategories(): void {
    const saved = localStorage.getItem('civix_admin_categories');
    this.categories = saved ? JSON.parse(saved) : [...this.defaultCategories];
  }
}
