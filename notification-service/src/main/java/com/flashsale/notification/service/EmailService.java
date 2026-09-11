package com.flashsale.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.from-email:no-reply@flashsalesystem.com}")
    private String fromEmail;

    /**
     * Sends an HTML transactional email.
     *
     * @param to recipient email address
     * @param subject email subject line
     * @param htmlContent rich HTML body content
     * @return true if successfully accepted by the SMTP relay, false otherwise
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("Preparing to send HTML email to: {}, subject: {}", to, subject);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email successfully dispatched to: {}", to);
            return true;
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send email to: {} with subject: {}", to, subject, ex);
            return false;
        }
    }
}

/*This `EmailService.java` is **clean and appropriate for the notification service**. Its responsibility is correctly limited to composing and sending email. It should not decide *when* an email should be sent—that belongs to `NotificationService`.

## 1. Overall flow

Your notification architecture is becoming:

```text
Kafka Event
    ↓
NotificationService
    ↓
EmailService
    ↓
JavaMailSender
    ↓
SMTP Server
    ↓
Customer Email
```

For example:

```text
payment.completed
       ↓
NotificationService
       ↓
EmailService
       ↓
"Payment Successful"
       ↓
Customer
```

---

## 2. `JavaMailSender`

```java
private final JavaMailSender mailSender;
```

This is Spring's abstraction for sending email.

Your `EmailService` doesn't need to know the low-level SMTP connection details. Those are configured in `application.yml`.

So:

```text
EmailService
     ↓
JavaMailSender
     ↓
Spring Mail configuration
     ↓
SMTP server
```

---

## 3. Sender email

```java
@Value("${app.notification.from-email:no-reply@flashsalesystem.com}")
private String fromEmail;
```

Spring reads:

```yaml
app:
  notification:
    from-email: no-reply@flashsalesystem.com
```

If the property doesn't exist, it uses:

```text
no-reply@flashsalesystem.com
```

This is good because the sender address isn't hard-coded inside the method.

One clarification: configuring `from-email` does **not by itself** prevent SMTP providers from rejecting mail. The SMTP provider's authentication, domain verification, SPF/DKIM/DMARC, sender policy, etc. also matter.

---

# 4. `sendHtmlEmail()`

```java
public boolean sendHtmlEmail(
        String to,
        String subject,
        String htmlContent)
```

This method receives three things:

```text
to          → customer@example.com
subject     → Payment Successful
htmlContent → HTML email body
```

For example:

```java
emailService.sendHtmlEmail(
    "customer@example.com",
    "Payment Successful",
    "<h1>Payment Successful</h1><p>Order ORD-1001 is confirmed.</p>"
);
```

---

# 5. Creating `MimeMessage`

```java
MimeMessage message = mailSender.createMimeMessage();
```

`MimeMessage` supports richer email features than a simple plain-text message, including HTML.

---

## 6. `MimeMessageHelper`

```java
MimeMessageHelper helper = new MimeMessageHelper(
        message,
        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
        StandardCharsets.UTF_8.name()
);
```

This configures the email.

### UTF-8

```java
StandardCharsets.UTF_8.name()
```

allows content such as:

```text
₹
é
ñ
中文
हिंदी
```

to be represented correctly.

### HTML

The actual HTML mode is enabled here:

```java
helper.setText(htmlContent, true);
```

The `true` means:

```text
HTML = yes
```

So:

```html
<h1>Payment Successful</h1>
<p>Thank you for your order.</p>
```

is rendered as HTML rather than displayed as raw tags.

---

# 7. Setting email properties

```java
helper.setFrom(fromEmail);
helper.setTo(to);
helper.setSubject(subject);
helper.setText(htmlContent, true);
```

This creates the basic email:

```text
From:    no-reply@flashsalesystem.com
To:      customer@example.com
Subject: Payment Successful

        HTML BODY
```

All good.

---

# 8. Sending

```java
mailSender.send(message);
```

This hands the message to the configured mail infrastructure.

If the operation succeeds:

```java
return true;
```

If an exception occurs:

```java
return false;
```

This makes it easy for `NotificationService` to update the notification log:

```text
EmailService
     ↓
true
     ↓
NotificationLog = SENT
```

or:

```text
EmailService
     ↓
false
     ↓
NotificationLog = FAILED
```

---

# 9. Exception handling

You catch:

```java
catch (MessagingException | MailException ex)
```

That's appropriate.

`MessagingException` can occur while constructing/configuring the MIME message, while `MailException` covers Spring mail sending failures.

The method then logs the failure and returns `false`.

This means the caller doesn't necessarily need to crash simply because one email couldn't be sent.

---

# 10. Important notification-service consideration ⚠️

Your comment says:

> "Return true if successfully accepted by the SMTP relay"

That's a reasonable description of what `mailSender.send()` generally means from the application perspective, but it **doesn't guarantee that the recipient actually received or read the email**.

There are multiple stages:

```text
Your application
      ↓
SMTP relay accepts
      ↓
Mail provider processes
      ↓
Recipient server accepts
      ↓
Inbox / spam folder
```

So `true` should really be understood as something like:

> **The application successfully handed the message to the configured mail sender without an immediate exception.**

For your notification log, `SENT` or `DISPATCHED` may be more precise than "DELIVERED" if you're not receiving delivery/bounce webhooks.

---

# 11. Synchronous sending — important for your Kafka architecture

The method is synchronous:

```java
mailSender.send(message);
```

That means the calling thread waits for the mail operation to finish.

For example:

```text
Kafka listener
      ↓
NotificationService
      ↓
EmailService
      ↓
SMTP
      ↓
wait...
      ↓
response
```

For a small system that's fine.

But your flash-sale system is designed for high throughput, so you don't want a slow SMTP server to unnecessarily block Kafka consumption.

Your architecture should eventually look more like:

```text
Kafka
  ↓
Notification Consumer
  ↓
Notification processing
  ↓
Email dispatch
  ↓
SMTP
```

with proper retry/backoff and idempotency.

**Don't necessarily make this method asynchronous itself.** The important thing is to design the notification processing layer correctly so Kafka consumption isn't tightly coupled to SMTP latency.

---

# 12. Idempotency belongs outside `EmailService`

This is important given your previous `NotificationLogRepository`.

`EmailService` should simply do:

```text
"Send this email."
```

It should **not** do:

```text
"Has this Kafka event already been processed?"
```

That belongs in:

```text
NotificationService
```

The intended flow is:

```text
Kafka event
     ↓
NotificationService
     ↓
Check NotificationLog
     ↓
Already processed?
   ↙       ↘
 YES        NO
  ↓          ↓
Skip       EmailService
             ↓
           Send
             ↓
        Save log
```

This keeps responsibilities clean.

---

# 13. One security consideration

You're accepting:

```java
String htmlContent
```

directly.

That is fine if **your own NotificationService generates trusted templates**.

But you should not allow arbitrary user-provided HTML to flow directly into this method, because that could result in unwanted HTML/content injection.

For example, prefer:

```text
NotificationService
      ↓
Trusted email template
      ↓
EmailService
```

rather than:

```text
Untrusted user input
      ↓
Raw HTML
      ↓
EmailService
```

---

# Verdict

### `EmailService.java` → ✅ Keep it

```text
JavaMailSender              ✅
HTML email support          ✅
UTF-8                       ✅
Configurable sender         ✅
Exception handling          ✅
Simple return status        ✅
Clean responsibility        ✅
```

### Things to remember for later

```text
1. true ≠ guaranteed recipient delivery
2. SMTP sending is synchronous
3. Retry/idempotency belongs in NotificationService
4. Don't pass untrusted HTML directly
5. Production email needs proper SMTP/domain configuration
```

Your notification service currently has:

```text
notification-service/
├── entity/
│   └── NotificationLog.java
├── repository/
│   └── NotificationLogRepository.java
└── service/
    └── EmailService.java
```

The next file I'd review is **`NotificationLog.java`**, because that will determine whether your `FAILED/SENT` tracking, Kafka-event deduplication, timestamps, and database indexes are correctly designed.
*/
