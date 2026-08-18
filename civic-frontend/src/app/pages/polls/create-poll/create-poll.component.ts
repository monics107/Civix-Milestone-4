import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { PollService } from '../../../services/poll.service';
import { ToastService } from '../../../services/toast.service';
import {
  DepartmentService,
  Department
} from '../../../services/department.service';


@Component({
  selector: 'app-create-poll',
  standalone: true,

  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,

    // Required for mat-select / mat-option
    MatSelectModule,

    MatDatepickerModule,
    MatNativeDateModule
  ],

  templateUrl: './create-poll.component.html',
  styleUrl: './create-poll.component.css'
})
export class CreatePollComponent implements OnInit {

  private fb = inject(FormBuilder);

  private pollService =
    inject(PollService);

  private router =
    inject(Router);
  private route = inject(ActivatedRoute);

  private toast =
    inject(ToastService);

  private departmentService =
    inject(DepartmentService);


  departments: Department[] = [];

  loading = false;

  minDate = new Date();
  editingPollId: number | null = null;


  // ============================================================
  // FORM
  // ============================================================

  form = this.fb.group({

    title: [
      '',
      [
        Validators.required,
        Validators.minLength(5)
      ]
    ],

    description: [
      '',
      Validators.required
    ],

    department: [
      '',
      Validators.required
    ],

    targetLocation: [
      '',
      Validators.required
    ],

    endDate: [null as Date | null, Validators.required],

    options: this.fb.array([

      this.fb.control(
        '',
        Validators.required
      ),

      this.fb.control(
        '',
        Validators.required
      )

    ])

  });


  // ============================================================
  // INIT
  // ============================================================

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.editingPollId = id;
      this.pollService.getPollById(id).subscribe({ next: poll => {
        if (!poll.createdByCurrentUser) { this.toast.error('You are not authorized to edit this poll.'); this.router.navigate(['/polls']); return; }
        this.form.patchValue({ title: poll.title, description: poll.description, department: poll.department || '', targetLocation: poll.targetLocation, endDate: new Date(poll.closeDate) });
        this.options.clear(); poll.options.forEach(option => this.options.push(this.fb.control(option, Validators.required)));
      }, error: () => { this.toast.error('Failed to load poll.'); this.router.navigate(['/polls']); } });
    }

    this.departmentService
      .getAll()
      .subscribe({

        next: (departments) => {

          this.departments =
            departments;
        },

        error: (error) => {

          console.error(
            'Failed to load departments:',
            error
          );

          this.departments = [];

          this.toast.error(
            'Failed to load departments.'
          );
        }

      });
  }


  // ============================================================
  // OPTIONS
  // ============================================================

  get options(): FormArray {

    return this.form.get(
      'options'
    ) as FormArray;
  }


  addOption(): void {

    this.options.push(
      this.fb.control(
        '',
        Validators.required
      )
    );
  }


  removeOption(index: number): void {

    if (this.options.length > 2) {

      this.options.removeAt(
        index
      );
    }
  }


  // ============================================================
  // SUBMIT
  // ============================================================

  onSubmit(): void {

    if (this.form.invalid) {

      this.form.markAllAsTouched();

      return;
    }


    this.loading = true;


    const value =
      this.form.getRawValue();


    const pollData = {

      title:
        value.title!,

      description:
        value.description!,

      options:
        value.options as string[],

      targetLocation:
        value.targetLocation!,

      department:
        value.department!,

      closeDate: this.toLocalDateTimeString(this.toEndOfDay(value.endDate!))
    };


    (this.editingPollId ? this.pollService.updatePoll(this.editingPollId, pollData) : this.pollService.createPoll(pollData))
      .subscribe({

        next: () => {

          this.loading = false;

          this.toast.success(
            this.editingPollId ? 'Poll updated successfully!' : 'Poll created successfully!'
          );

          this.router.navigate(
            ['/polls']
          );
        },

        error: (error) => {

          this.loading = false;

          console.error(
            'Failed to create poll:',
            error
          );

          this.toast.error(
            error?.error?.message ||
            'Failed to create poll.'
          );
        }

      });
  }

  private toEndOfDay(date: Date): Date {
    const result = new Date(date);
    result.setHours(23, 59, 59, 999);
    return result;
  }

  private toLocalDateTimeString(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

}
