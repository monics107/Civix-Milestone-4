package com.civic.repository;
import com.civic.entity.Notification; import com.civic.entity.User; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> { List<Notification> findByUserOrderByCreatedAtDesc(User user); }
