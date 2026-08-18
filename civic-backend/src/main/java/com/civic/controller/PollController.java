package com.civic.controller;

import com.civic.dto.PollDashboardStatsResponse;
import com.civic.dto.PollRequest;
import com.civic.dto.PollResponse;
import com.civic.dto.PollResultResponse;
import com.civic.enums.PollStatus;
import com.civic.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    // Create Poll
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('CITIZEN', 'OFFICIAL')")
    @PostMapping
    public ResponseEntity<PollResponse> createPoll(
            @Valid @RequestBody PollRequest request) {

        return new ResponseEntity<>(
                pollService.createPoll(request),
                HttpStatus.CREATED
        );
    }

    // Get All Polls / Filter by Status
    @GetMapping
    public ResponseEntity<List<PollResponse>> getPolls(
            @RequestParam(required = false) PollStatus status) {

        if (status != null) {
            return ResponseEntity.ok(
                    pollService.getPollsByStatus(status)
            );
        }

        return ResponseEntity.ok(
                pollService.getAllPolls()
        );
    }

    // Poll Stats
    @GetMapping("/stats")
    public ResponseEntity<PollDashboardStatsResponse> getDashboardStats() {

        return ResponseEntity.ok(
                pollService.getDashboardStats()
        );
    }

    // My Polls
    @GetMapping("/my-polls")
    public ResponseEntity<List<PollResponse>> getMyPolls() {

        return ResponseEntity.ok(
                pollService.getMyPolls()
        );
    }

    // Get Poll By Id
    @GetMapping("/{id}")
    public ResponseEntity<PollResponse> getPollById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                pollService.getPollById(id)
        );
    }

    // Update Poll
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('CITIZEN', 'OFFICIAL')")
    @PutMapping("/{id}")
    public ResponseEntity<PollResponse> updatePoll(
            @PathVariable Long id,
            @Valid @RequestBody PollRequest request) {

        return ResponseEntity.ok(
                pollService.updatePoll(id, request)
        );
    }

    // Delete Poll
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('CITIZEN', 'OFFICIAL')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePoll(
            @PathVariable Long id) {

        pollService.deletePoll(id);

        return ResponseEntity.ok(
                "Poll deleted successfully."
        );
    }

    // Poll Results
    @GetMapping("/{id}/results")
    public ResponseEntity<PollResultResponse> getResults(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                pollService.getPollResults(id)
        );
    }
}
