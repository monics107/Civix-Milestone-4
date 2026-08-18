package com.civic.controller;
import com.civic.dto.NotificationResponse; import com.civic.service.NotificationService; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor public class NotificationController { private final NotificationService notifications; @GetMapping public List<NotificationResponse> mine(){return notifications.mine();} @DeleteMapping("/{id}") public void consume(@PathVariable Long id){notifications.consume(id);} }
