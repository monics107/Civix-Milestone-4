import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PetitionService } from '../../services/petition.service';
import { PollService } from '../../services/poll.service';

type Result = { type: 'Petition' | 'Poll'; id: number; title: string; description: string; link: string; date?: string; count: number; status: string };
@Component({selector:'app-search', standalone:true, imports:[CommonModule,RouterModule], templateUrl:'./search.component.html', styleUrl:'./search.component.css'})
export class SearchComponent implements OnInit {
  private route=inject(ActivatedRoute); private petitions=inject(PetitionService); private polls=inject(PollService);
  query=''; results: Result[]=[]; loading=true;
  ngOnInit(){this.route.queryParams.subscribe(params=>{this.query=(params['q']||'').trim();this.runSearch();});}
  private runSearch(){if(!this.query){this.results=[];this.loading=false;return;} this.loading=true; const term=this.query.toLowerCase(); forkJoin({petitions:this.petitions.getAllPetitions(0,500),polls:this.polls.getAllPolls()}).subscribe({next:data=>{this.results=[...(data.petitions.content||[]).filter(p=>`${p.title} ${p.description}`.toLowerCase().includes(term)).map(p=>({type:'Petition' as const,id:p.id,title:p.title,description:p.description,link:`/petitions/${p.id}`,date:p.createdAt,count:p.currentSignatures,status:p.status})),...data.polls.filter(p=>`${p.title} ${p.description} ${p.options.join(' ')}`.toLowerCase().includes(term)).map(p=>({type:'Poll' as const,id:p.id,title:p.title,description:p.description,link:`/polls/${p.id}/vote`,date:p.createdAt,count:p.totalVotes,status:p.status}))];this.loading=false;},error:()=>{this.results=[];this.loading=false;}});}
}
