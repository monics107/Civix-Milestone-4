package com.civic.controller;

import com.civic.dto.MonthlyReportResponse;
import com.civic.entity.*;
import com.civic.repository.*;
import lombok.RequiredArgsConstructor;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.context.SecurityContextHolder;


@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PetitionRepository petitions;
    private final PollRepository polls;
    private final SignatureRepository signatures;
    private final VoteRepository votes;
    private final UserRepository users;


    // ============================================================
    // MONTHLY REPORT
    // ============================================================

    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(
            @RequestParam int year,
            @RequestParam int month) {

        LocalDateTime start =
                LocalDateTime.of(year, month, 1, 0, 0);

        LocalDateTime end =
                start.plusMonths(1);

        List<Petition> petitionList =
                scopePetitions();

        List<Poll> pollList =
                scopePolls();

        long totalPetitions =
                petitionList.stream()
                        .filter(p ->
                                between(
                                        p.getCreatedAt(),
                                        start,
                                        end
                                )
                        )
                        .count();

        long totalSignatures =
                signatures.findAll()
                        .stream()
                        .filter(s ->
                                between(
                                        s.getSignedAt(),
                                        start,
                                        end
                                )
                        )
                        .count();

        long totalPolls =
                pollList.stream()
                        .filter(p ->
                                between(
                                        p.getCreatedAt(),
                                        start,
                                        end
                                )
                        )
                        .count();

        long totalVotes =
                votes.findAll()
                        .stream()
                        .filter(v ->
                                between(
                                        v.getVotedAt(),
                                        start,
                                        end
                                )
                        )
                        .count();

        long activePetitions =
                petitionList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PetitionStatus.ACTIVE
                        )
                        .count();

        long underReviewPetitions =
                petitionList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PetitionStatus.UNDER_REVIEW
                        )
                        .count();

        long approvedPetitions =
                petitionList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PetitionStatus.APPROVED
                        )
                        .count();

        long rejectedPetitions =
                petitionList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PetitionStatus.REJECTED
                        )
                        .count();

        long closedPetitions =
                petitionList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PetitionStatus.CLOSED
                        )
                        .count();

        long activePolls =
                pollList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PollStatus.ACTIVE
                        )
                        .count();

        long closedPolls =
                pollList.stream()
                        .filter(p ->
                                p.getStatus() ==
                                com.civic.enums.PollStatus.CLOSED
                        )
                        .count();

        long activeEngagement =
                activePetitions + activePolls;


        return MonthlyReportResponse.builder()
                .year(year)
                .month(month)
                .monthName(
                        Month.of(month)
                                .getDisplayName(
                                        TextStyle.FULL,
                                        Locale.ENGLISH
                                )
                )
                .totalPetitions(totalPetitions)
                .totalSignatures(totalSignatures)
                .totalPolls(totalPolls)
                .totalVotes(totalVotes)
                .activePetitions(activePetitions)
                .underReviewPetitions(underReviewPetitions)
                .approvedPetitions(approvedPetitions)
                .rejectedPetitions(rejectedPetitions)
                .closedPetitions(closedPetitions)
                .activePolls(activePolls)
                .closedPolls(closedPolls)
                .activeEngagement(activeEngagement)
                .build();
    }


    // ============================================================
    // PDF EXPORT
    // ============================================================

    @GetMapping("/monthly/export/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam int year,
            @RequestParam int month) throws Exception {

        MonthlyReportResponse report =
                monthly(year, month);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        Document document =
                new Document(PageSize.A4.rotate(), 28, 28, 30, 32);

        PdfWriter.getInstance(
                document,
                output
        );

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 21, Color.WHITE);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(222, 240, 255));
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(19, 41, 86));
        Font metricValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 19, new Color(16, 35, 78));
        Font metricLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(93, 112, 151));

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3.4f, 1.4f});
        PdfPCell brand = cell("CIVIX  |  Reports & Analytics", titleFont, new Color(10, 62, 130), Element.ALIGN_LEFT, 15);
        brand.setPaddingBottom(5);
        header.addCell(brand);
        header.addCell(cell(report.getMonthName() + " " + year, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE), new Color(0, 151, 159), Element.ALIGN_CENTER, 15));
        PdfPCell subtitle = cell("Monthly civic engagement summary for petitions and public polls", subtitleFont, new Color(10, 62, 130), Element.ALIGN_LEFT, 12);
        subtitle.setColspan(2);
        subtitle.setPaddingTop(0);
        subtitle.setPaddingBottom(14);
        header.addCell(subtitle);
        document.add(header);
        document.add(Chunk.NEWLINE);

        PdfPTable metrics = new PdfPTable(4);
        metrics.setWidthPercentage(100);
        metrics.setSpacingAfter(17);
        metrics.setWidths(new float[]{1, 1, 1, 1});
        addMetric(metrics, "TOTAL PETITIONS", report.getTotalPetitions(), new Color(231, 240, 255), new Color(21, 99, 230), metricValueFont, metricLabelFont);
        addMetric(metrics, "ACTIVE PETITIONS", report.getActivePetitions(), new Color(228, 248, 239), new Color(19, 184, 123), metricValueFont, metricLabelFont);
        addMetric(metrics, "RESOLVED PETITIONS", report.getApprovedPetitions() + report.getClosedPetitions(), new Color(247, 232, 255), new Color(149, 23, 217), metricValueFont, metricLabelFont);
        addMetric(metrics, "VOTES CAST", report.getTotalVotes(), new Color(255, 239, 229), new Color(253, 116, 29), metricValueFont, metricLabelFont);
        document.add(metrics);

        PdfPTable overview = new PdfPTable(2);
        overview.setWidthPercentage(100);
        overview.setSpacingAfter(17);
        overview.setWidths(new float[]{1, 1});
        addOverview(overview, "Poll participation", report.getTotalPolls(), "Total polls | " + report.getActivePolls() + " active", new Color(230, 239, 255));
        addOverview(overview, "Petition support", report.getActiveEngagement(), "Active petitions and polls", new Color(255, 231, 233));
        document.add(overview);

        document.add(new Paragraph("Petition status breakdown", sectionFont));
        PdfPTable statusTable = new PdfPTable(5);
        statusTable.setWidthPercentage(100);
        statusTable.setSpacingBefore(7);
        statusTable.setSpacingAfter(18);
        statusTable.setWidths(new float[]{1, 1, 1, 1, 1});
        addStatus(statusTable, "ACTIVE", report.getActivePetitions(), new Color(25, 118, 233));
        addStatus(statusTable, "UNDER REVIEW", report.getUnderReviewPetitions(), new Color(255, 173, 18));
        addStatus(statusTable, "APPROVED", report.getApprovedPetitions(), new Color(22, 175, 105));
        addStatus(statusTable, "REJECTED", report.getRejectedPetitions(), new Color(238, 61, 72));
        addStatus(statusTable, "CLOSED", report.getClosedPetitions(), new Color(133, 144, 167));
        document.add(statusTable);


        // --------------------------------------------------------
        // PETITION DETAILS
        // --------------------------------------------------------

        document.add(new Paragraph("Petition details", sectionFont));

        PdfPTable petitionTable =
                new PdfPTable(8);
        petitionTable.setWidthPercentage(100);
        petitionTable.setSpacingBefore(7);
        petitionTable.setSpacingAfter(18);
        petitionTable.setWidths(new float[]{2.25f, 1.2f, 1.05f, .75f, .85f, 1.25f, 1.35f, 1.45f});

        String[] petitionHeaders = {
                "Title",
                "Department",
                "Status",
                "Progress",
                "Signatures",
                "Responsible official",
                "Expected completion",
                "Pending work"
        };

        for (String columnHeader : petitionHeaders) {
            petitionTable.addCell(tableHeader(columnHeader));
        }


        for (Petition petition : scopePetitions()) {

            petitionTable.addCell(tableCell(safeString(petition.getTitle())));
            petitionTable.addCell(tableCell(petition.getDepartment() == null ? "" : petition.getDepartment()));
            petitionTable.addCell(statusCell(petition.getStatus() == null ? "" : petition.getStatus().name()));
            petitionTable.addCell(tableCell((petition.getProgressPercent() == null ? 0 : petition.getProgressPercent()) + "%"));
            petitionTable.addCell(tableCell(String.valueOf(petition.getCurrentSignatures()), Element.ALIGN_RIGHT));
            petitionTable.addCell(tableCell(safeString(petition.getResponsiblePerson())));
            petitionTable.addCell(tableCell(petition.getExpectedCompletionAt() == null ? "" : petition.getExpectedCompletionAt().toLocalDate().toString()));
            petitionTable.addCell(tableCell(safeString(petition.getPendingWork())));
        }

        document.add(petitionTable);


        // --------------------------------------------------------
        // POLL DETAILS
        // --------------------------------------------------------

        document.add(new Paragraph("Poll details", sectionFont));

        PdfPTable pollTable = new PdfPTable(5);
        pollTable.setWidthPercentage(100);
        pollTable.setSpacingBefore(7);
        pollTable.setWidths(new float[]{2.6f, 1.5f, 1.25f, .85f, 1.25f});

        String[] pollHeaders = {
                "Title",
                "Department",
                "Status",
                "Votes",
                "Close Date"
        };

        for (String columnHeader : pollHeaders) {
            pollTable.addCell(tableHeader(columnHeader));
        }


        for (Poll poll : scopePolls()) {

            pollTable.addCell(tableCell(safeString(poll.getTitle())));
            pollTable.addCell(tableCell(poll.getDepartment() == null ? "" : poll.getDepartment()));
            pollTable.addCell(statusCell(poll.getStatus() == null ? "" : poll.getStatus().name()));
            pollTable.addCell(tableCell(String.valueOf(votes.countByPoll(poll)), Element.ALIGN_RIGHT));
            pollTable.addCell(tableCell(poll.getCloseDate() == null ? "" : poll.getCloseDate().toString()));
        }

        document.add(pollTable);

        Paragraph footer = new Paragraph("Generated by Civix - Monthly civic engagement report", FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(112, 128, 163)));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(14);
        document.add(footer);
        document.close();


        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=civix-report-"
                                + year
                                + "-"
                                + month
                                + ".pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(output.toByteArray());
    }


    // ============================================================
    // EXCEL EXPORT
    // ============================================================

    @GetMapping("/monthly/export/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam int year,
            @RequestParam int month) throws Exception {

        MonthlyReportResponse report =
                monthly(year, month);

        XSSFWorkbook workbook =
                new XSSFWorkbook();


        // --------------------------------------------------------
        // SUMMARY SHEET
        // --------------------------------------------------------

        XSSFSheet summary =
                workbook.createSheet("Summary");

        String[][] rows = {
                {"Metric", "Value"},
                {
                        "Total Petitions",
                        String.valueOf(
                                report.getTotalPetitions()
                        )
                },
                {
                        "Total Signatures",
                        String.valueOf(
                                report.getTotalSignatures()
                        )
                },
                {
                        "Total Polls",
                        String.valueOf(
                                report.getTotalPolls()
                        )
                },
                {
                        "Total Votes",
                        String.valueOf(
                                report.getTotalVotes()
                        )
                },
                {
                        "Active Engagement",
                        String.valueOf(
                                report.getActiveEngagement()
                        )
                },
                {
                        "Active Petitions",
                        String.valueOf(
                                report.getActivePetitions()
                        )
                },
                {
                        "Under Review Petitions",
                        String.valueOf(
                                report.getUnderReviewPetitions()
                        )
                },
                {
                        "Approved Petitions",
                        String.valueOf(
                                report.getApprovedPetitions()
                        )
                },
                {
                        "Rejected Petitions",
                        String.valueOf(
                                report.getRejectedPetitions()
                        )
                },
                {
                        "Closed Petitions",
                        String.valueOf(
                                report.getClosedPetitions()
                        )
                },
                {
                        "Active Polls",
                        String.valueOf(
                                report.getActivePolls()
                        )
                },
                {
                        "Closed Polls",
                        String.valueOf(
                                report.getClosedPolls()
                        )
                }
        };


        for (int i = 0; i < rows.length; i++) {

            var row =
                    summary.createRow(i);

            row.createCell(0)
                    .setCellValue(rows[i][0]);

            row.createCell(1)
                    .setCellValue(rows[i][1]);
        }


        // --------------------------------------------------------
        // PETITIONS SHEET
        // --------------------------------------------------------

        XSSFSheet petitionSheet =
                workbook.createSheet("Petitions");

        String[] petitionHeaders = {
                "Title",
                "Department",
                "Status",
                "Progress",
                "Signatures",
                "Responsible Official",
                "Expected Completion",
                "Pending Work"
        };


        for (int i = 0;
             i < petitionHeaders.length;
             i++) {

            petitionSheet
                    .createRow(0)
                    .createCell(i)
                    .setCellValue(
                            petitionHeaders[i]
                    );
        }


        int petitionRow = 1;


        for (Petition petition :
                scopePetitions()) {

            var row =
                    petitionSheet
                            .createRow(
                                    petitionRow++
                            );

            row.createCell(0)
                    .setCellValue(
                            safeString(
                                    petition.getTitle()
                            )
                    );

            row.createCell(1)
                    .setCellValue(
                            petition.getDepartment() == null
                                    ? ""
                                    : petition.getDepartment()
                    );

            row.createCell(2)
                    .setCellValue(
                            petition.getStatus() == null
                                    ? ""
                                    : petition
                                            .getStatus()
                                            .name()
                    );

            row.createCell(3)
                    .setCellValue(
                            (
                                    petition
                                            .getProgressPercent()
                                            == null
                                            ? 0
                                            : petition
                                                    .getProgressPercent()
                            )
                                    + "%"
                    );

            row.createCell(4)
                    .setCellValue(
                            petition
                                    .getCurrentSignatures()
                    );

            row.createCell(5)
                    .setCellValue(
                            petition
                                    .getResponsiblePerson()
                                    == null
                                    ? ""
                                    : petition
                                            .getResponsiblePerson()
                    );

            row.createCell(6)
                    .setCellValue(
                            petition
                                    .getExpectedCompletionAt()
                                    == null
                                    ? ""
                                    : petition
                                            .getExpectedCompletionAt()
                                            .toString()
                    );

            row.createCell(7)
                    .setCellValue(
                            petition
                                    .getPendingWork()
                                    == null
                                    ? ""
                                    : petition
                                            .getPendingWork()
                    );
        }


        // --------------------------------------------------------
        // POLLS SHEET
        // --------------------------------------------------------

        XSSFSheet pollSheet =
                workbook.createSheet("Polls");

        String[] pollHeaders = {
                "Title",
                "Department",
                "Status",
                "Votes",
                "Close Date"
        };


        for (int i = 0;
             i < pollHeaders.length;
             i++) {

            pollSheet
                    .createRow(0)
                    .createCell(i)
                    .setCellValue(
                            pollHeaders[i]
                    );
        }


        int pollRow = 1;


        for (Poll poll :
                scopePolls()) {

            var row =
                    pollSheet.createRow(
                            pollRow++
                    );

            row.createCell(0)
                    .setCellValue(
                            safeString(
                                    poll.getTitle()
                            )
                    );

            row.createCell(1)
                    .setCellValue(
                            poll.getDepartment() == null
                                    ? ""
                                    : poll.getDepartment()
                    );

            row.createCell(2)
                    .setCellValue(
                            poll.getStatus() == null
                                    ? ""
                                    : poll.getStatus().name()
                    );

            row.createCell(3)
                    .setCellValue(
                            votes.countByPoll(poll)
                    );

            row.createCell(4)
                    .setCellValue(
                            poll.getCloseDate() == null
                                    ? ""
                                    : poll.getCloseDate().toString()
                    );
        }


        // --------------------------------------------------------
        // OUTPUT
        // --------------------------------------------------------

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        workbook.write(output);

        workbook.close();


        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=civix-report-"
                                + year
                                + "-"
                                + month
                                + ".xlsx"
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(
                        output.toByteArray()
                );
    }


    // ============================================================
    // DATE HELPER
    // ============================================================

    private PdfPCell cell(String text, Font font, Color background, int alignment, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(padding);
        return cell;
    }

    private void addMetric(PdfPTable table, String label, long value, Color background, Color accent, Font valueFont, Font labelFont) {
        PdfPCell metric = new PdfPCell();
        metric.setBackgroundColor(background);
        metric.setBorderColor(accent);
        metric.setBorderWidthBottom(4);
        metric.setPadding(12);
        Paragraph number = new Paragraph(String.valueOf(value), valueFont);
        number.setSpacingAfter(5);
        metric.addElement(number);
        metric.addElement(new Paragraph(label, labelFont));
        table.addCell(metric);
    }

    private void addOverview(PdfPTable table, String title, long value, String detail, Color background) {
        PdfPCell card = new PdfPCell();
        card.setBackgroundColor(background);
        card.setBorder(Rectangle.NO_BORDER);
        card.setPadding(13);
        card.addElement(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(19, 41, 86))));
        Paragraph number = new Paragraph(String.valueOf(value), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(19, 41, 86)));
        number.setSpacingBefore(5);
        number.setSpacingAfter(2);
        card.addElement(number);
        card.addElement(new Paragraph(detail, FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(93, 112, 151))));
        table.addCell(card);
    }

    private void addStatus(PdfPTable table, String label, long value, Color color) {
        PdfPCell status = new PdfPCell();
        status.setBackgroundColor(new Color(249, 251, 255));
        status.setBorderColor(new Color(224, 232, 245));
        status.setPadding(10);
        Paragraph name = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, color));
        name.setAlignment(Element.ALIGN_CENTER);
        status.addElement(name);
        Paragraph number = new Paragraph(String.valueOf(value), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new Color(19, 41, 86)));
        number.setAlignment(Element.ALIGN_CENTER);
        number.setSpacingBefore(4);
        status.addElement(number);
        table.addCell(status);
    }

    private PdfPCell tableHeader(String text) {
        return cell(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE), new Color(19, 74, 143), Element.ALIGN_LEFT, 7);
    }

    private PdfPCell tableCell(String text) {
        return tableCell(text, Element.ALIGN_LEFT);
    }

    private PdfPCell tableCell(String text, int alignment) {
        PdfPCell cell = cell(text, FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(45, 62, 94)), Color.WHITE, alignment, 7);
        cell.setBorderColor(new Color(225, 232, 244));
        return cell;
    }

    private PdfPCell statusCell(String value) {
        Color color = switch (value) {
            case "ACTIVE" -> new Color(25, 118, 233);
            case "UNDER_REVIEW" -> new Color(204, 127, 0);
            case "APPROVED" -> new Color(22, 145, 83);
            case "REJECTED" -> new Color(208, 41, 51);
            default -> new Color(92, 106, 135);
        };
        PdfPCell cell = tableCell(value.replace('_', ' '));
        cell.setBackgroundColor(new Color(248, 250, 255));
        cell.setPhrase(new Phrase(value.replace('_', ' '), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, color)));
        return cell;
    }

    private boolean between(
            LocalDateTime value,
            LocalDateTime start,
            LocalDateTime end) {

        return value != null
                && !value.isBefore(start)
                && value.isBefore(end);
    }


    // ============================================================
    // PETITION ACCESS SCOPE
    // ============================================================

    private List<Petition> scopePetitions() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                users.findByEmail(email)
                        .orElseThrow();


        if (user.getRole() == Role.OFFICIAL) {

            return petitions.findAll()
                    .stream()
                    .filter(
                            petition ->
                                    user.getDepartment() != null
                                    && petition.getDepartment() != null
                                    && user.getDepartment()
                                            .equalsIgnoreCase(
                                                    petition.getDepartment()
                                            )
                    )
                    .toList();
        }


        return petitions.findAll();
    }


    // ============================================================
    // POLL ACCESS SCOPE
    // ============================================================

    private List<Poll> scopePolls() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                users.findByEmail(email)
                        .orElseThrow();


        if (user.getRole() == Role.OFFICIAL) {

            return polls.findAll()
                    .stream()
                    .filter(
                            poll ->
                                    user.getDepartment() != null
                                    && poll.getDepartment() != null
                                    && user.getDepartment()
                                            .equalsIgnoreCase(
                                                    poll.getDepartment()
                                            )
                    )
                    .toList();
        }


        return polls.findAll();
    }


    // ============================================================
    // STRING HELPER
    // ============================================================

    private String safeString(String value) {

        return value == null
                ? ""
                : value;
    }
}
