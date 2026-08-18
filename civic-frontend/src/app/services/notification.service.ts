import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
export interface CivicNotification { id:number; title:string; message:string; link?:string; createdAt:string; }
@Injectable({providedIn:'root'}) export class NotificationService { private http=inject(HttpClient); private base=`${environment.apiUrl}/notifications`; getAll():Observable<CivicNotification[]>{return this.http.get<CivicNotification[]>(this.base);} consume(id:number){return this.http.delete<void>(`${this.base}/${id}`);} }
