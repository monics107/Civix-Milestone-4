package com.civic.service.impl;

import com.civic.dto.*;
import com.civic.entity.*;
import com.civic.enums.PollStatus;
import com.civic.exception.ApiException;
import com.civic.repository.*;
import com.civic.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class PollServiceImpl implements PollService {
    private final PollRepository polls; private final UserRepository users; private final VoteRepository votes; private final com.civic.service.NotificationService notifications;
    private User me() { return users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user not found.")); }
    private PollStatus refreshStatus(Poll poll) {
        LocalDateTime now = LocalDateTime.now();
        PollStatus next = now.isAfter(poll.getCloseDate()) ? PollStatus.CLOSED : PollStatus.ACTIVE;
        if (poll.getStatus() != next) { poll.setStatus(next); polls.save(poll); } return next;
    }
    private void validate(PollRequest r) {
        if (r.getCloseDate().toLocalDate().isBefore(LocalDate.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "Poll end date cannot be in the past.");
        if (r.getOptions().size() < 2 || r.getOptions().stream().map(String::trim).anyMatch(String::isEmpty) || r.getOptions().stream().map(String::trim).distinct().count() != r.getOptions().size()) throw new ApiException(HttpStatus.BAD_REQUEST, "Provide at least two unique poll options.");
    }
    private PollResponse map(Poll p, User user) { PollStatus status = refreshStatus(p); return PollResponse.builder().id(p.getId()).title(p.getTitle()).description(p.getDescription()).options(p.getOptions()).status(status).targetLocation(p.getTargetLocation()).department(p.getDepartment()).createdById(p.getCreatedBy().getId()).createdByName(p.getCreatedBy().getName()).startDate(p.getStartDate()).endDate(p.getCloseDate()).closeDate(p.getCloseDate()).totalVotes(votes.countByPoll(p)).votedByCurrentUser(votes.existsByPollAndUser(p, user)).createdByCurrentUser(p.getCreatedBy().getId().equals(user.getId())).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build(); }
    public PollResponse createPoll(PollRequest r) { User u = me(); if (u.getRole() != Role.CITIZEN && u.getRole() != Role.OFFICIAL) throw new ApiException(HttpStatus.FORBIDDEN, "Only citizens and officials can create polls."); validate(r); Poll p = polls.save(Poll.builder().title(r.getTitle().trim()).description(r.getDescription().trim()).options(r.getOptions().stream().map(String::trim).toList()).targetLocation(r.getTargetLocation().trim()).department(r.getDepartment()).closeDate(r.getCloseDate()).status(PollStatus.ACTIVE).createdBy(u).build()); notifications.notifyOfficials(p.getTargetLocation(),p.getDepartment(),"New poll created","A new poll was created: " + p.getTitle(),"/polls/" + p.getId() + "/vote"); return map(p, u); }
    @Transactional public PollResponse updatePoll(Long id, PollRequest r) { User u=me(); Poll p=find(id); creator(p,u); validate(r); if(refreshStatus(p)==PollStatus.CLOSED) throw new ApiException(HttpStatus.BAD_REQUEST,"Closed polls cannot be updated."); p.setTitle(r.getTitle().trim());p.setDescription(r.getDescription().trim());p.getOptions().clear();p.getOptions().addAll(r.getOptions().stream().map(String::trim).toList());p.setTargetLocation(r.getTargetLocation().trim());p.setDepartment(r.getDepartment());p.setCloseDate(r.getCloseDate());return map(polls.save(p),u); }
    @Transactional public void deletePoll(Long id) { User u=me(); Poll p=find(id); creator(p,u); votes.deleteByPoll(p); polls.delete(p); }
    public PollResponse getPollById(Long id) { User u=me(); return map(find(id),u); }
    public List<PollResponse> getAllPolls() { User u=me(); return polls.findAll().stream().filter(p -> u.getRole()==Role.SUPER_ADMIN || p.getCreatedBy().getId().equals(u.getId()) || sameLocation(p.getTargetLocation(),u.getLocation())).map(p->map(p,u)).toList(); }
    public List<PollResponse> getMyPolls() { User u=me(); return polls.findByCreatedBy(u).stream().map(p->map(p,u)).toList(); }
    public List<PollResponse> getPollsByStatus(PollStatus s) { return getAllPolls().stream().filter(p->p.getStatus()==s).toList(); }
    public PollResultResponse getPollResults(Long id) { Poll p=find(id); refreshStatus(p); long total=votes.countByPoll(p); List<OptionResult> results=p.getOptions().stream().map(o->{long count=votes.countByPollAndSelectedOption(p,o);return OptionResult.builder().option(o).votes(count).percentage(total==0?0:Math.round(count*10000.0/total)/100.0).build();}).toList(); return PollResultResponse.builder().pollId(id).title(p.getTitle()).status(p.getStatus()).totalVotes(total).results(results).build(); }
    public PollDashboardStatsResponse getDashboardStats() { List<PollResponse> all=getAllPolls(); return PollDashboardStatsResponse.builder().totalPolls(all.size()).totalVotes(all.stream().mapToLong(PollResponse::getTotalVotes).sum()).activePolls(all.stream().filter(p->p.getStatus()==PollStatus.ACTIVE).count()).closedPolls(all.stream().filter(p->p.getStatus()==PollStatus.CLOSED).count()).build(); }
    private Poll find(Long id){return polls.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Poll not found."));}
    private void creator(Poll p,User u){if(!p.getCreatedBy().getId().equals(u.getId()))throw new ApiException(HttpStatus.FORBIDDEN,"You are not authorized to modify this poll.");}
    private boolean sameLocation(String a,String b){return a!=null&&b!=null&&a.trim().equalsIgnoreCase(b.trim());}
}
