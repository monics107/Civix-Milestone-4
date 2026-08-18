package com.civic.service.impl;
import com.civic.dto.NotificationResponse; import com.civic.entity.*; import com.civic.exception.ApiException; import com.civic.repository.*; import com.civic.service.NotificationService; import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Service; import java.util.List;
@Service @RequiredArgsConstructor public class NotificationServiceImpl implements NotificationService {
 private final NotificationRepository notifications; private final UserRepository users;
 private User me(){return users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Authenticated user not found."));}
 public void notify(User user,String title,String message,String link){notifications.save(Notification.builder().user(user).title(title).message(message).link(link).build());}
 public List<NotificationResponse> mine(){return notifications.findByUserOrderByCreatedAtDesc(me()).stream().map(n->NotificationResponse.builder().id(n.getId()).title(n.getTitle()).message(n.getMessage()).link(n.getLink()).createdAt(n.getCreatedAt()).build()).toList();}
 public void consume(Long id){Notification n=notifications.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Notification not found."));if(!n.getUser().getId().equals(me().getId()))throw new ApiException(HttpStatus.FORBIDDEN,"Not allowed.");notifications.delete(n);}
 public void notifyOfficials(String location,String department,String title,String message,String link){users.findByRoleOrderByIdDesc(Role.OFFICIAL).stream().filter(u->u.isActive()&&u.isVerified()&&((location!=null&&location.equalsIgnoreCase(u.getLocation()))||(department!=null&&department.equalsIgnoreCase(u.getDepartment())))).forEach(u->notify(u,title,message,link));}
}
