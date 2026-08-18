package com.civic.dto;
import lombok.*; import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder public class NotificationResponse { private Long id; private String title,message,link; private LocalDateTime createdAt; }
