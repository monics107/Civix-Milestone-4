package com.civic.service;
import com.civic.entity.User; import com.civic.dto.NotificationResponse; import java.util.List;
public interface NotificationService { void notify(User user, String title, String message, String link); List<NotificationResponse> mine(); void consume(Long id); void notifyOfficials(String location, String department, String title, String message, String link); }
