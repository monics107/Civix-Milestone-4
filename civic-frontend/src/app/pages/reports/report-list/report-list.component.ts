import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { ReportService, MonthlyReport } from '../../../services/report.service';
import { ToastService } from '../../../services/toast.service';

@Component({ selector: 'app-report-list', standalone: true, imports: [CommonModule, MatButtonModule, MatIconModule, FormsModule], templateUrl: './report-list.component.html', styleUrl: './report-list.component.css' })
export class ReportListComponent implements OnInit {
  private reports = inject(ReportService); private toast = inject(ToastService);
  year = new Date().getFullYear(); month = new Date().getMonth() + 1; report: MonthlyReport | null = null; loading = true;
  months = Array.from({ length: 12 }, (_, index) => ({ value: index + 1, label: new Date(2000, index).toLocaleString('en', { month: 'long' }) }));
  statuses = [
    { key: 'ACTIVE', label: 'ACTIVE', color: '#1976e9', tint: '#e8f1ff' }, { key: 'UNDER_REVIEW', label: 'UNDER REVIEW', color: '#ffad12', tint: '#fff4dc' },
    { key: 'APPROVED', label: 'APPROVED', color: '#16af69', tint: '#e5f8ed' }, { key: 'REJECTED', label: 'REJECTED', color: '#ee3d48', tint: '#ffeaec' }, { key: 'CLOSED', label: 'CLOSED', color: '#8590a7', tint: '#eef1f5' }
  ];
  ngOnInit() { this.load(); }
  load() { this.loading = true; this.reports.monthly(this.year, Number(this.month)).subscribe({ next: r => { this.report = r; this.loading = false; }, error: e => { this.loading = false; this.toast.error(e.error?.message || 'Failed to load report.'); } }); }
  download(blob: Blob, name: string) { const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = name; a.click(); URL.revokeObjectURL(url); }
  exportPdf() { this.reports.exportPdf(this.year, Number(this.month)).subscribe(b => this.download(b, `civix-report-${this.year}-${this.month}.pdf`)); }
  exportExcel() { this.reports.exportExcel(this.year, Number(this.month)).subscribe(b => this.download(b, `civix-report-${this.year}-${this.month}.xlsx`)); }
  get petitionTotal() { return this.report?.totalPetitions || 0; } get activePetitions() { return this.report?.activePetitions || 0; } get resolvedPetitions() { return (this.report?.approvedPetitions || 0) + (this.report?.closedPetitions || 0); } get totalVotes() { return this.report?.totalVotes || 0; } get activePolls() { return this.report?.activePolls || 0; } get pollTotal() { return this.report?.totalPolls || 0; } get engagementRate() { return this.report?.activeEngagement || 0; }
  petitionCount(status: string) { if (!this.report) return 0; return ({ ACTIVE: this.report.activePetitions, UNDER_REVIEW: this.report.underReviewPetitions, APPROVED: this.report.approvedPetitions, REJECTED: this.report.rejectedPetitions, CLOSED: this.report.closedPetitions } as Record<string, number>)[status] || 0; }
  statusPercentage(status: string) { return this.petitionTotal ? Math.round((this.petitionCount(status) / this.petitionTotal) * 100) : 0; }
  get donutGradient() { if (!this.petitionTotal) return 'conic-gradient(#e9eef6 0deg 360deg)'; let cursor = 0; const stops = this.statuses.map(status => { const next = cursor + (this.petitionCount(status.key) / this.petitionTotal) * 360; const segment = `${status.color} ${cursor}deg ${next}deg`; cursor = next; return segment; }); return `conic-gradient(${stops.join(', ')})`; }
}
