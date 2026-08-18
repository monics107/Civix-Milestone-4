package com.civic.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "notifications") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 500) private String message;
    @Column(length = 200) private String link;
    @Builder.Default @Column(name = "is_read", nullable = false) private boolean read = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
