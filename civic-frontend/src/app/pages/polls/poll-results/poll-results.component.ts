import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { Chart, registerables } from 'chart.js';

import { PollService } from '../../../services/poll.service';
import { ToastService } from '../../../services/toast.service';

import {
  PollDashboardStats,
  PollResponse,
  PollResultResponse
} from '../../../models/poll.model';

Chart.register(...registerables);

@Component({
  selector: 'app-poll-results',
  standalone: true,

  imports: [
    CommonModule,
    RouterModule,
    FormsModule,

    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],

  templateUrl: './poll-results.component.html',
  styleUrl: './poll-results.component.css'
})
export class PollResultsComponent implements OnInit, OnDestroy {

  private pollService = inject(PollService);
  private toast = inject(ToastService);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild('barCanvas')
  barCanvas!: ElementRef<HTMLCanvasElement>;

  @ViewChild('pieCanvas')
  pieCanvas!: ElementRef<HTMLCanvasElement>;

  stats: PollDashboardStats | null = null;

  polls: PollResponse[] = [];

  pollResult: PollResultResponse | null = null;

  selectedPollId: number | null = null;

  isLoading = false;

  errorMsg = '';

  private barChart: Chart | null = null;

  private pieChart: Chart | null = null;

  private refreshTimer?: ReturnType<typeof setInterval>;

  // =========================================================
  // INIT
  // =========================================================

  ngOnInit(): void {

    // Load dashboard statistics
    this.loadStats();

    // Load all polls
    this.loadPollsList();

    // Auto refresh every 10 seconds
    this.refreshTimer = setInterval(() => {

      this.loadStats();

      this.refreshSelectedPoll();

    }, 10000);
  }

  // =========================================================
  // DESTROY
  // =========================================================

  ngOnDestroy(): void {

    this.destroyCharts();

    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  // =========================================================
  // LOAD DASHBOARD STATS
  // =========================================================

  loadStats(): void {

    this.pollService.getDashboardStats().subscribe({

      next: data => {

        this.stats = data;

      },

      error: error => {

        console.error(
          'Failed to load dashboard statistics:',
          error
        );

      }

    });
  }

  // =========================================================
  // LOAD POLLS LIST
  // =========================================================

  loadPollsList(): void {

    this.isLoading = true;

    this.errorMsg = '';

    this.pollService.getAllPolls().subscribe({

      next: polls => {

        this.polls = polls;

        this.isLoading = false;

        // -----------------------------------------------------
        // Get poll ID from URL
        // Example:
        // /poll-results/5
        // -----------------------------------------------------

        const routePollId =
          Number(
            this.route.snapshot.paramMap.get('id')
          );

        // -----------------------------------------------------
        // If URL contains valid poll ID
        // -----------------------------------------------------

        if (
          routePollId &&
          polls.some(
            poll => poll.id === routePollId
          )
        ) {

          this.selectedPollId = routePollId;

        }

        // -----------------------------------------------------
        // Otherwise select first poll automatically
        // -----------------------------------------------------

        else if (
          !this.selectedPollId &&
          polls.length > 0
        ) {

          this.selectedPollId = polls[0].id;

        }

        // -----------------------------------------------------
        // Load selected poll result
        // -----------------------------------------------------

        if (this.selectedPollId) {

          this.onPollSelect(
            this.selectedPollId
          );

        }

      },

      error: error => {

        console.error(
          'Failed to load polls:',
          error
        );

        this.isLoading = false;

        this.errorMsg =
          'Failed to load polls.';

        this.toast.error(
          'Failed to load polls.'
        );

      }

    });
  }

  // =========================================================
  // SELECT POLL
  // =========================================================

  onPollSelect(id: number): void {

    this.selectedPollId = id;

    this.isLoading = true;

    this.errorMsg = '';

    // -------------------------------------------------------
    // Destroy old charts immediately
    // -------------------------------------------------------

    this.destroyCharts();

    // -------------------------------------------------------
    // Load selected poll results
    // -------------------------------------------------------

    this.pollService.getPollResults(id).subscribe({

      next: data => {

        console.log(
          'Poll result loaded:',
          data
        );

        // Store result
        this.pollResult = data;

        this.isLoading = false;

        // Refresh dashboard stats
        this.loadStats();

        // ---------------------------------------------------
        // IMPORTANT:
        // Tell Angular to update the HTML/canvas first
        // ---------------------------------------------------

        this.cdr.detectChanges();

        // ---------------------------------------------------
        // Wait until browser renders canvas
        // ---------------------------------------------------

        requestAnimationFrame(() => {

          this.renderCharts();

        });

      },

      error: error => {

        console.error(
          'Unable to load poll results:',
          error
        );

        this.isLoading = false;

        this.errorMsg =
          'Unable to load poll results.';

        this.toast.error(
          'Unable to load poll results.'
        );

      }

    });
  }

  // =========================================================
  // AUTO REFRESH SELECTED POLL
  // =========================================================

  refreshSelectedPoll(): void {

    // No poll selected
    if (!this.selectedPollId) {
      return;
    }

    this.pollService
      .getPollResults(this.selectedPollId)
      .subscribe({

        next: data => {

          console.log(
            'Poll result auto-refreshed:',
            data
          );

          this.pollResult = data;

          // Update Angular view
          this.cdr.detectChanges();

          // Render updated chart
          requestAnimationFrame(() => {

            this.renderCharts();

          });

        },

        error: error => {

          console.error(
            'Auto-refresh failed:',
            error
          );

        }

      });
  }

  // =========================================================
  // DESTROY EXISTING CHARTS
  // =========================================================

  private destroyCharts(): void {

    if (this.barChart) {

      this.barChart.destroy();

      this.barChart = null;

    }

    if (this.pieChart) {

      this.pieChart.destroy();

      this.pieChart = null;

    }
  }

  // =========================================================
  // RENDER CHARTS
  // =========================================================

  private renderCharts(): void {

    // -------------------------------------------------------
    // Always destroy previous chart before creating new one
    // -------------------------------------------------------

    this.destroyCharts();

    // -------------------------------------------------------
    // No result available
    // -------------------------------------------------------

    if (
      !this.pollResult ||
      !this.pollResult.results ||
      !this.pollResult.results.length
    ) {

      console.log(
        'No poll result data available.'
      );

      return;

    }

    // -------------------------------------------------------
    // Prepare chart data
    // -------------------------------------------------------

    const labels =
      this.pollResult.results.map(
        item => item.option
      );

    const votes =
      this.pollResult.results.map(
        item => item.votes
      );

    // -------------------------------------------------------
    // Chart colors
    // -------------------------------------------------------

    const colors = [

      '#2563eb',
      '#16a34a',
      '#f97316',
      '#8b5cf6',
      '#dc2626',
      '#0891b2',
      '#db2777',
      '#ca8a04'

    ];

    // =======================================================
    // BAR CHART
    // =======================================================

    const barContext =
      this.barCanvas?.nativeElement
        ?.getContext('2d');

    if (barContext) {

      this.barChart = new Chart(
        barContext,
        {

          type: 'bar',

          data: {

            labels: labels,

            datasets: [

              {

                label: 'Votes',

                data: votes,

                backgroundColor:
                  colors.slice(
                    0,
                    labels.length
                  ),

                borderRadius: 8,

                borderSkipped: false

              }

            ]

          },

          options: {

            responsive: true,

            maintainAspectRatio: false,

            animation: {

              duration: 600

            },

            plugins: {

              legend: {

                display: false

              }

            },

            scales: {

              y: {

                beginAtZero: true,

                ticks: {

                  stepSize: 1

                }

              },

              x: {

                grid: {

                  display: false

                }

              }

            }

          }

        }
      );

    }

    // =======================================================
    // PIE / DOUGHNUT CHART
    // =======================================================

    const pieContext =
      this.pieCanvas?.nativeElement
        ?.getContext('2d');

    if (pieContext) {

      this.pieChart = new Chart(
        pieContext,
        {

          type: 'doughnut',

          data: {

            labels: labels,

            datasets: [

              {

                data: votes,

                backgroundColor:
                  colors.slice(
                    0,
                    labels.length
                  ),

                borderWidth: 0

              }

            ]

          },

          options: {

            responsive: true,

            maintainAspectRatio: false,

            cutout: '62%',

            animation: {

              duration: 600

            },

            plugins: {

              legend: {

                position: 'bottom'

              }

            }

          }

        }
      );

    }
  }
}