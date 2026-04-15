package com.TenaMed.email.service;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildDoctorInvitationEmail(String hospitalName, String link) {
        return String.format("""
                <html>
                  <body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #1f2937;\">
                    <h2 style=\"margin-bottom: 8px;\">You're invited to join a hospital</h2>
                    <p>You have been invited to join <strong>%s</strong> as a doctor.</p>
                    <p>
                      Click the link below to continue your onboarding:
                      <a href=\"%s\">Accept Invitation</a>
                    </p>
                    <p>This invitation expires in 24 hours.</p>
                    <p>If you did not expect this email, you can ignore it.</p>
                  </body>
                </html>
                """, hospitalName, link);
    }
}
