package com.TenaMed.email.service;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildPrescriptionReviewedEmail(String link) {
        return String.format("""
                <html>
                  <body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #1f2937;\">
                    <h2 style=\"margin-bottom: 8px;\">Prescription Approved</h2>
                    <p>Your prescription has been successfully reviewed.</p>
                    <p>You can now view available medicines.</p>
                    <p>
                      <a href=\"%s\" style=\"display: inline-block; padding: 10px 16px; background-color: #2563eb; color: #ffffff; text-decoration: none; border-radius: 6px;\">View Prescription</a>
                    </p>
                    <p>If you did not request this, you can ignore this email.</p>
                  </body>
                </html>
                """, link);
    }

    public String buildPrescriptionRejectedEmail(String link, String rejectionReason) {
        String reasonBlock = (rejectionReason == null || rejectionReason.isBlank())
                ? ""
                : "<p><strong>Reason:</strong> " + escapeHtml(rejectionReason) + "</p>";

        return String.format("""
                <html>
                  <body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #1f2937;\">
                    <h2 style=\"margin-bottom: 8px;\">Prescription Rejected</h2>
                    <p>We could not approve your prescription.</p>
                    %s
                    <p>Please re-upload a valid prescription.</p>
                    <p>
                      <a href=\"%s\" style=\"display: inline-block; padding: 10px 16px; background-color: #dc2626; color: #ffffff; text-decoration: none; border-radius: 6px;\">View Details</a>
                    </p>
                  </body>
                </html>
                """, reasonBlock, link);
    }

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

    public String buildPharmacistInvitationEmail(String pharmacyName, String link) {
        return String.format("""
                <html>
                  <body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #1f2937;\">
                    <h2 style=\"margin-bottom: 8px;\">You're invited to join a pharmacy</h2>
                    <p>You have been invited to join <strong>%s</strong> as a pharmacist.</p>
                    <p>
                      Click the link below to continue your onboarding:
                      <a href=\"%s\">Accept Invitation</a>
                    </p>
                    <p>This invitation expires in 24 hours.</p>
                    <p>If you did not expect this email, you can ignore it.</p>
                  </body>
                </html>
                """, pharmacyName, link);
    }

          private String escapeHtml(String value) {
            if (value == null) {
              return "";
            }
            return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
          }
}
