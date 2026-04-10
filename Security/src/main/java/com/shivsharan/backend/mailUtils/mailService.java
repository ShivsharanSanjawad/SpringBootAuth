package com.shivsharan.backend.mailUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;

@Service
public class mailService {

    private static final Logger logger = LoggerFactory.getLogger(mailService.class);

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Autowired
    public JavaMailSender mailSender ;

    public boolean sendMail(String to, String subject, String body) {
        if (!mailEnabled) {
            logger.info("Mail disabled; skipping send to {}", to);
            return true;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send mail", e);
            return false;
        }
    }

    public boolean sendOTP(String mail, int otp) {
        if (!mailEnabled) {
            logger.info("Mail disabled; skipping OTP send to {}", mail);
            return true;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(mail);
            helper.setSubject("OTP For Authentication");
            helper.setText(getHTML(otp), true);
            addInlineWelcomeImageIfPresent(helper);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send OTP email", e);
            return false;
        }
    }

    public boolean sendWelcomeEmail(String mail, String username) {
        String subject = "Welcome to Security";
        String body = "Hi " + username + ",\n\nYour account is ready. You can now log in and complete OTP verification.\n\nThanks,\nSecurity Team";
        return sendMail(mail, subject, body);
    }

    public String getHTML(int otp) throws IOException {
        ClassPathResource templateResource = new ClassPathResource("templates/OTPTemplate.html");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(templateResource.getInputStream(), StandardCharsets.UTF_8))) {
            String html = reader.lines().collect(Collectors.joining());
            return html.replace("${OTP}", Integer.toString(otp));
        }
    }

    private void addInlineWelcomeImageIfPresent(MimeMessageHelper helper) {
        try {
            ClassPathResource imageResource = new ClassPathResource("static/welcome.jpg");
            if (imageResource.exists()) {
                helper.addInline("welcome", imageResource);
            } else {
                logger.warn("welcome.jpg not found on classpath, skipping inline image");
            }
        } catch (Exception e) {
            logger.warn("Failed to attach inline image", e);
        }
    }
}
