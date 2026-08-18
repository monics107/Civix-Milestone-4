import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PollService } from '../../../services/poll.service';
import { ToastService } from '../../../services/toast.service';

import { Poll } from '../../../models/poll.model';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-vote-poll',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './vote-poll.component.html',
  styleUrl: './vote-poll.component.css'
})
export class VotePollComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private pollService = inject(PollService);
  private toast = inject(ToastService);
  private auth = inject(AuthService);

  poll: Poll | null = null;
  selectedOption = '';
  submitting = false;
  loading = true;
  canVote = true;
  isCreator = false;
  votingMessage = '';

  // NEW
  hasVoted = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadPoll(id);
  }

  loadPoll(id: number): void {
    this.loading = true;

    this.pollService.getPollById(id).subscribe({
      next: (response: any) => {
        console.log('Poll:', response);

        this.poll = response;
        this.hasVoted = !!response.votedByCurrentUser;
        this.isCreator = !!response.createdByCurrentUser;
        const sameLocation = !!this.auth.getUser()?.location && this.auth.getUser()!.location.trim().toLowerCase() === response.targetLocation.trim().toLowerCase();
        this.canVote = !this.isCreator && sameLocation && response.status === 'ACTIVE';
        this.votingMessage = this.isCreator ? 'You created this poll and are not eligible to vote.' : !sameLocation ? 'You are not eligible to vote on this poll because it is targeted to another location.' : response.status === 'CLOSED' ? 'This poll is currently closed.' : '';
        this.loading = false;
      },
      error: (error) => {
        console.error('Load Poll Error:', error);
        this.loading = false;
        this.toast.error('Failed to load poll.');
      }
    });
  }
vote(): void {

  if (this.hasVoted) {
    this.toast.info('You have already voted.');
    return;
  }

  if (!this.canVote) {
    this.toast.error('You are not eligible to vote on this poll.');
    return;
  }

  if (!this.poll || !this.selectedOption) {
    this.toast.error('Please select an option.');
    return;
  }

  this.submitting = true;

  const voteRequest = {
    selectedOption: this.selectedOption
  };

  this.pollService.votePoll(this.poll.id, voteRequest).subscribe({

    next: () => {

      this.submitting = false;

      this.hasVoted = true;
      this.selectedOption = '';

      this.toast.success('Your vote has been submitted successfully.');

      // Stay on this page
      // this.router.navigate(['/polls', this.poll!.id, 'results']);
    },

    error: (error) => {

      this.submitting = false;

      if (
        error.status === 409 ||
        error.error?.message === 'You have already voted on this poll.'
      ) {
        this.hasVoted = true;
        this.toast.info('You have already voted on this poll.');
        return;
      }

      this.toast.error(error.error?.message || 'Failed to submit your vote.');
    }

  });

}
}
