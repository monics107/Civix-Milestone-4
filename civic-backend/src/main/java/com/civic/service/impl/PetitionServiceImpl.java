package com.civic.service.impl;

import com.civic.dto.*;
import com.civic.entity.*;
import com.civic.enums.PetitionStatus;
import com.civic.repository.*;
import com.civic.service.PetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PetitionServiceImpl implements PetitionService {
    private final PetitionRepository repo;
    private final UserRepository users;
    private final SignatureRepository signatures;
    private final PetitionActivityRepository activities;
    private final PetitionReviewRepository reviews;
    private final PetitionResponseLogRepository responseLogs;
    private final com.civic.service.NotificationService notifications;

    private User me() {
        return users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void officialAccess(Petition p, User u) {
        if (u.getRole() == Role.SUPER_ADMIN)
            return;
        if (u.getRole() != Role.OFFICIAL || !u.isVerified() || !u.isActive())
            throw new RuntimeException("Official access required");
        if (p.getDepartment() == null || u.getDepartment() == null
                || !sameDepartment(p.getDepartment(), u.getDepartment()))
            throw new RuntimeException("You are not authorized for this department");
    }

    private PetitionResponse map(Petition p) {
        User u = me();
        return PetitionResponse.builder().id(p.getId()).title(p.getTitle()).description(p.getDescription())
                .category(p.getCategory()).location(p.getLocation()).department(p.getDepartment()).goal(p.getGoal())
                .currentSignatures(p.getCurrentSignatures()).status(p.getStatus()).creatorName(p.getCreator().getName())
                .creatorEmail(p.getCreator().getEmail()).signedByCurrentUser(signatures.existsByPetitionAndUser(p, u))
                .ownedByCurrentUser(p.getCreator().getId().equals(u.getId())).rejectionReason(p.getRejectionReason())
                .proposedSolution(p.getProposedSolution()).actionPlan(p.getActionPlan())
                .responsiblePerson(p.getResponsiblePerson()).responsibleDesignation(p.getResponsibleDesignation())
                .responsibleDepartment(p.getResponsibleDepartment()).workStartAt(p.getWorkStartAt())
                .expectedCompletionAt(p.getExpectedCompletionAt()).completedAt(p.getCompletedAt())
                .completedWork(p.getCompletedWork()).pendingWork(p.getPendingWork()).pendingReason(p.getPendingReason())
                .progressPercent(p.getProgressPercent()).petitionDate(p.getPetitionDate()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }

    public PetitionResponse createPetition(PetitionRequest r) {
        User u = me();
        if (u.getRole() != Role.CITIZEN)
            throw new RuntimeException("Only citizens can create petitions");
        Petition p = Petition.builder().title(r.getTitle()).description(r.getDescription()).category(r.getCategory())
                .location(r.getLocation()).department(r.getDepartment()).goal(r.getGoal()).petitionDate(r.getPetitionDate()).creator(u).build();
        p = repo.save(p);
        notifications.notifyOfficials(p.getLocation(), p.getDepartment(), "New petition created", "A new petition was created: " + p.getTitle(), "/petitions/" + p.getId());
        return map(p);
    }

    public PetitionResponse getPetitionById(Long id) {
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        User u = me();
        if (u.getRole() == Role.OFFICIAL)
            officialAccess(p, u);
        return map(p);
    }

    public Page<PetitionResponse> getAllPetitions(int page, int size, String category, PetitionStatus status) {
        User u = me();
        Pageable pb = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Petition> ps;
        if (u.getRole() == Role.OFFICIAL) {
            if (u.getDepartment() == null)
                throw new RuntimeException("Official department is not assigned");
            List<Petition> matches = repo.findAll().stream()
                    .filter(p -> sameDepartment(p.getDepartment(), u.getDepartment()))
                    .filter(p -> status == null || p.getStatus() == status)
                    .sorted(Comparator.comparing(Petition::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .toList();
            int start = Math.min((int) pb.getOffset(), matches.size());
            int end = Math.min(start + pb.getPageSize(), matches.size());
            ps = new PageImpl<>(matches.subList(start, end), pb, matches.size());
        } else if (category != null && !category.isBlank() && status != null)
            ps = repo.findByCategoryAndStatus(category, status, pb);
        else if (category != null && !category.isBlank())
            ps = repo.findByCategory(category, pb);
        else if (status != null)
            ps = repo.findByStatus(status, pb);
        else
            ps = repo.findAll(pb);
        return ps.map(this::map);
    }

    public Page<PetitionResponse> getMyPetitions(int page, int size) {
        User u = me();
        return repo.findByCreator(u, PageRequest.of(page, size, Sort.by("createdAt").descending())).map(this::map);
    }

    public PetitionResponse updatePetition(Long id, PetitionRequest r) {
        User u = me();
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        if (!p.getCreator().getId().equals(u.getId()))
            throw new RuntimeException("You are not allowed to update this petition");
        if (p.getStatus() != PetitionStatus.ACTIVE)
            throw new RuntimeException("Only active petitions can be edited");
        p.setTitle(r.getTitle());
        p.setDescription(r.getDescription());
        p.setCategory(r.getCategory());
        p.setLocation(r.getLocation());
        p.setDepartment(r.getDepartment());
        p.setGoal(r.getGoal());
        p.setPetitionDate(r.getPetitionDate());
        return map(repo.save(p));
    }

    @Transactional
    public void deletePetition(Long id) {
        User u = me();
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        if (!p.getCreator().getId().equals(u.getId()))
            throw new RuntimeException("You are not allowed to delete this petition");
        signatures.deleteByPetition(p);
        repo.delete(p);
    }

    @Transactional
    public PetitionResponse updateDecision(Long id, OfficialDecisionRequest r) {
        User u = me();
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        officialAccess(p, u);
        if (r.getStatus() == PetitionStatus.REJECTED) {
            if (blank(r.getRejectionReason()) || blank(r.getProposedSolution()))
                throw new RuntimeException("Rejection reason and proposed solution are required");
            p.setRejectionReason(r.getRejectionReason());
            p.setProposedSolution(r.getProposedSolution());
        }
        if (r.getStatus() == PetitionStatus.APPROVED) {
            p.setActionPlan(r.getActionPlan());
            p.setResponsiblePerson(r.getResponsiblePerson());
            p.setResponsibleDesignation(r.getResponsibleDesignation());
            p.setResponsibleDepartment(r.getResponsibleDepartment());
            p.setWorkStartAt(r.getWorkStartAt());
            p.setExpectedCompletionAt(r.getExpectedCompletionAt());
        }
        p.setStatus(r.getStatus());
        repo.save(p);
        notifications.notify(p.getCreator(), "Petition status updated", u.getName() + " updated your petition \"" + p.getTitle() + "\" to " + r.getStatus() + ".", "/petitions/" + p.getId());
        activities.save(PetitionActivity.builder().petition(p).official(u).action(r.getStatus().name())
                .description(r.getComment() != null ? r.getComment() : r.getStatus().name()).build());
        return map(p);
    }

    public PetitionResponse updateProgress(Long id, ProgressUpdateRequest r) {
        User u = me();
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        officialAccess(p, u);
        if (p.getStatus() != PetitionStatus.APPROVED && p.getStatus() != PetitionStatus.UNDER_REVIEW)
            throw new RuntimeException("Petition is not in a work-progress stage");
        p.setProgressPercent(r.getProgressPercent());
        p.setCompletedWork(r.getCompletedWork());
        p.setPendingWork(r.getPendingWork());
        p.setPendingReason(r.getPendingReason());
        p.setExpectedCompletionAt(r.getExpectedCompletionAt());
        repo.save(p);
        activities.save(PetitionActivity.builder().petition(p).official(u).action("PROGRESS_UPDATED")
                .description(r.getDescription()).build());
        return map(p);
    }

    public PetitionResponse complete(Long id) {
        User u = me();
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        officialAccess(p, u);
        p.setProgressPercent(100);
        p.setCompletedAt(java.time.LocalDateTime.now());
        p.setStatus(PetitionStatus.UNDER_REVIEW);
        repo.save(p);
        activities.save(PetitionActivity.builder().petition(p).official(u).action("COMPLETED_FOR_REVIEW")
                .description("Work completed; citizen review requested").build());
        return map(p);
    }

    public PetitionResponse close(Long id) {
        User u = me();
        Petition p = repo.findById(id).orElseThrow(() -> new RuntimeException("Petition not found"));
        officialAccess(p, u);
        long total = reviews.countByPetition(p);
        long positive = reviews.countByPetitionAndRatingGreaterThanEqual(p, 4);
        if (total == 0 || positive * 100.0 / total < 75)
            throw new RuntimeException("Petition cannot be closed until positive reviews reach 75% or more");
        p.setStatus(PetitionStatus.CLOSED);
        repo.save(p);
        notifications.notify(p.getCreator(), "Petition closed", "Your petition \"" + p.getTitle() + "\" has been closed.", "/petitions/" + p.getId());
        activities.save(PetitionActivity.builder().petition(p).official(u).action("CLOSED")
                .description("Closed after meeting 75% positive review threshold").build());
        return map(p);
    }






    
    @Override
    public List<ActivityResponse> getTimeline(Long id) {
        Petition petition = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Petition not found"));
        User user = me();
        if (user.getRole() == Role.OFFICIAL) {
            officialAccess(petition, user);
        }
        if (user.getRole() == Role.CITIZEN && !petition.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to view this petition timeline");
        }
        return activities.findByPetitionOrderByCreatedAtAsc(petition).stream()
                .map(activity -> ActivityResponse.builder()
                        .id(activity.getId())
                        .action(activity.getAction())
                        .description(activity.getDescription())
                        .officialName(activity.getOfficial() == null ? null : activity.getOfficial().getName())
                        .designation(activity.getOfficial() == null ? null : activity.getOfficial().getDesignation())
                        .department(activity.getOfficial() == null ? null : activity.getOfficial().getDepartment())
                        .createdAt(activity.getCreatedAt())
                        .build())
                .toList();
    }








    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean sameDepartment(String petitionDepartment, String officialDepartment) {
        return normalizeDepartment(petitionDepartment).equals(normalizeDepartment(officialDepartment));
    }

    private String normalizeDepartment(String value) {
        return value == null ? "" : value.replace("account_balance", "").trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
