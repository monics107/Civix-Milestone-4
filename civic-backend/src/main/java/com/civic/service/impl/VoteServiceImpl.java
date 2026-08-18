package com.civic.service.impl;

import com.civic.dto.VoteRequest; import com.civic.dto.VoteResponse; import com.civic.entity.*; import com.civic.enums.PollStatus;
import com.civic.exception.ApiException; import com.civic.repository.*; import com.civic.service.VoteService;
import lombok.RequiredArgsConstructor; import org.springframework.dao.DataIntegrityViolationException; import org.springframework.http.HttpStatus; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {
    private final VoteRepository voteRepository; private final PollRepository pollRepository; private final UserRepository userRepository; private final com.civic.service.NotificationService notifications;
    @Override @Transactional
    public VoteResponse castVote(Long pollId, VoteRequest request) {
        User user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Authenticated user not found."));
        Poll poll = pollRepository.findById(pollId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Poll not found."));
        LocalDateTime now=LocalDateTime.now(); PollStatus status = now.isAfter(poll.getCloseDate()) ? PollStatus.CLOSED : PollStatus.ACTIVE;
        if (poll.getStatus()!=status) {poll.setStatus(status);pollRepository.save(poll);}
        if (!sameLocation(user.getLocation(),poll.getTargetLocation())) throw new ApiException(HttpStatus.FORBIDDEN,"You are not eligible to vote on this poll because it is targeted to another location.");
        if (poll.getCreatedBy().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"You created this poll and are not eligible to vote.");
        if (status != PollStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "This poll is currently closed.");
        if (!poll.getOptions().contains(request.getSelectedOption())) throw new ApiException(HttpStatus.BAD_REQUEST,"Selected option does not exist in this poll.");
        if (voteRepository.existsByPollAndUser(poll,user)) throw new ApiException(HttpStatus.CONFLICT,"You have already voted on this poll.");
        try { Vote saved=voteRepository.saveAndFlush(Vote.builder().poll(poll).user(user).selectedOption(request.getSelectedOption()).build()); notifications.notify(poll.getCreatedBy(),"New vote on your poll",user.getName() + " voted on your poll \"" + poll.getTitle() + "\".","/polls/" + poll.getId() + "/results"); return VoteResponse.builder().message("Vote submitted successfully.").selectedOption(saved.getSelectedOption()).votedAt(saved.getVotedAt()).build(); }
        catch (DataIntegrityViolationException ex) { throw new ApiException(HttpStatus.CONFLICT,"You have already voted on this poll."); }
    }
    private boolean sameLocation(String a,String b){return a!=null&&b!=null&&a.trim().equalsIgnoreCase(b.trim());}
}
