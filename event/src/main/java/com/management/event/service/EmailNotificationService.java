package com.management.event.service;

import com.management.event.entity.Letter;
import com.management.event.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Best-effort email notifications. If mail isn't configured or sending fails,
 * we only log and never fail the main business transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.notifications.email.enabled:false}")
    private boolean enabled;

    @Value("${app.notifications.email.from:}")
    private String from;

    public void notifyApproverAssigned(Letter letter, User approver) {
        if (letter == null || approver == null) return;
        String subject = "New letter awaiting your approval";
        String body = """
                Hello %s,

                A letter titled "%s" is waiting for your approval.
                Letter ID: %s
                Requested by: %s (%s)

                Please log in to review it.
                """.formatted(
                nullToEmpty(approver.getUserName()),
                nullToEmpty(letter.getTitle()),
                String.valueOf(letter.getId()),
                letter.getUser() != null ? nullToEmpty(letter.getUser().getUserName()) : "",
                letter.getUser() != null ? nullToEmpty(letter.getUser().getRegNumber()) : ""
        );
        send(approver.getEmail(), subject, body);
    }

    public void notifyRequesterApproved(Letter letter, User actedBy) {
        if (letter == null || letter.getUser() == null) return;
        String subject = "Your letter was approved";
        String body = """
                Hello %s,

                Your letter titled "%s" has been approved.
                Letter ID: %s
                Approved by: %s (%s)

                Please log in to view the status.
                """.formatted(
                nullToEmpty(letter.getUser().getUserName()),
                nullToEmpty(letter.getTitle()),
                String.valueOf(letter.getId()),
                actedBy != null ? nullToEmpty(actedBy.getUserName()) : "",
                actedBy != null ? nullToEmpty(actedBy.getRegNumber()) : ""
        );
        send(letter.getUser().getEmail(), subject, body);
    }

    public void notifyRequesterRejected(Letter letter, User actedBy, String reason) {
        if (letter == null || letter.getUser() == null) return;
        String subject = "Your letter was rejected";
        String body = """
                Hello %s,

                Your letter titled "%s" has been rejected.
                Letter ID: %s
                Rejected by: %s (%s)
                Reason: %s

                Please log in to view details.
                """.formatted(
                nullToEmpty(letter.getUser().getUserName()),
                nullToEmpty(letter.getTitle()),
                String.valueOf(letter.getId()),
                actedBy != null ? nullToEmpty(actedBy.getUserName()) : "",
                actedBy != null ? nullToEmpty(actedBy.getRegNumber()) : "",
                reason == null ? "" : reason
        );
        send(letter.getUser().getEmail(), subject, body);
    }

    private void send(String to, String subject, String text) {
        if (!enabled) return;
        if (!StringUtils.hasText(to)) return;

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) return;

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject == null ? "" : subject);
            msg.setText(text == null ? "" : text);
            if (StringUtils.hasText(from)) {
                msg.setFrom(from);
            }
            sender.send(msg);
        } catch (Exception e) {
            log.warn("Email send failed (to={}, subject={}): {}", to, subject, e.toString());
        }
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}

