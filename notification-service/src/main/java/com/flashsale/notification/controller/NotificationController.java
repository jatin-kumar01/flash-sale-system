package com.flashsale.notification.controller;

import com.flashsale.common.dto.ApiResponse;
import com.flashsale.notification.entity.NotificationLog;
import com.flashsale.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my-notifications")
    public ResponseEntity<ApiResponse<Page<NotificationLog>>> getMyNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Fetching paginated notifications for user: {}", userId);
        Page<NotificationLog> page = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", page));
    }

    @GetMapping("/order/{orderReference}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getOrderNotifications(
            @PathVariable("orderReference") String orderReference) {

        log.debug("Fetching notification logs for orderReference: {}", orderReference);
        List<NotificationLog> logs = notificationService.getNotificationsByOrderReference(orderReference);
        return ResponseEntity.ok(ApiResponse.success("Order notifications retrieved successfully", logs));
    }
}
/*## Review of `NotificationController.java`

This file is **mostly correct and appropriately thin**. Its job is only to receive HTTP requests, extract the user/order information, call `NotificationService`, and return `ApiResponse`.

### 1. `getMyNotifications()`

```java
@GetMapping("/my-notifications")
public ResponseEntity<ApiResponse<Page<NotificationLog>>> getMyNotifications(
        @RequestHeader("X-User-Id") Long userId,
        @PageableDefault(
            size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable)
```

Request:

```http
GET /api/notifications/my-notifications
X-User-Id: 101
```

The flow is:

```text
Frontend
   ↓
API Gateway
   ↓
X-User-Id = 101
   ↓
NotificationController
   ↓
NotificationService
   ↓
NotificationLogRepository
   ↓
PostgreSQL
```

`Pageable` means you don't load every notification at once.

For example:

```text
page = 0
size = 10
sort = createdAt DESC
```

means:

> Give me the latest 10 notifications.

---

### 2. `getOrderNotifications()`

```java
@GetMapping("/order/{orderReference}")
```

Example:

```http
GET /api/notifications/order/ORD-1001
```

Flow:

```text
Controller
   ↓
NotificationService
   ↓
NotificationLogRepository
   ↓
findByOrderReference("ORD-1001")
```

This is useful for checking the communication history of an order:

```text
ORD-1001
 ├── ORDER_CREATED → SENT
 ├── PAYMENT_SUCCESS → SENT
 └── ORDER_EXPIRED → FAILED
```

---

## ⚠️ Important security issue

This explanation:

> "`X-User-Id` ... scope notifications strictly to the calling user"

is **only true if the API Gateway securely controls this header**.

A client should **not** be allowed to simply send:

```http
X-User-Id: 999
```

and see another user's notifications.

The intended architecture should be:

```text
Client
  ↓ JWT
API Gateway
  ↓ validates JWT
  ↓ extracts user ID
  ↓ sets/overwrites X-User-Id
Notification Service
  ↓
NotificationController
```

So the gateway must **overwrite** the incoming `X-User-Id`, rather than trusting a client-supplied value.

---

## ⚠️ Another security issue: order endpoint

This endpoint:

```java
@GetMapping("/order/{orderReference}")
```

does **not** receive `X-User-Id`.

Therefore, if this endpoint is exposed to normal users, someone could potentially request:

```text
/api/notifications/order/ORD-OTHER-USER
```

and retrieve another customer's notification history.

### Better approach

For customer-facing access, pass the user ID:

```java
@GetMapping("/order/{orderReference}")
public ResponseEntity<ApiResponse<List<NotificationLog>>> getOrderNotifications(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable String orderReference) {

    List<NotificationLog> logs =
            notificationService.getUserOrderNotifications(userId, orderReference);

    return ResponseEntity.ok(
            ApiResponse.success(logs, "Order notifications retrieved successfully")
    );
}
```

Then the repository/service should verify:

```text
orderReference belongs to userId
```

For an **internal admin/support endpoint**, you can instead protect it with an appropriate role.

---

## ⚠️ Entity exposure

The controller returns:

```java
ApiResponse<Page<NotificationLog>>
```

and:

```java
ApiResponse<List<NotificationLog>>
```

That means your **JPA entity is directly exposed as API JSON**.

It works, but I would eventually change it to:

```text
NotificationLog Entity
        ↓
NotificationResponse DTO
        ↓
Controller
        ↓
JSON
```

This prevents internal database fields from accidentally becoming part of your public API.

For the current development stage, however, **you don't need to change it immediately**.

---

## 4. Logging

This is good:

```java
log.debug("Fetching paginated notifications for user: {}", userId);
```

and:

```java
log.debug("Fetching notification logs for orderReference: {}", orderReference);
```

You are not logging email content or sensitive information.

One small consideration: `userId` and `orderReference` are generally fine, but avoid logging email bodies, tokens, payment credentials, etc.

---

## 5. Error handling

The controller doesn't have:

```java
try {
   ...
} catch (...) {
}
```

That's **good**.

Your centralized:

```text
GlobalExceptionHandler
```

should handle service exceptions.

So:

```text
Controller
   ↓
Service throws exception
   ↓
GlobalExceptionHandler
   ↓
ApiResponse error
```

This keeps the controller clean.

---

# Final verdict

| Part                         | Status                         |
| ---------------------------- | ------------------------------ |
| REST mappings                | ✅ Good                         |
| Thin controller              | ✅ Good                         |
| Pagination                   | ✅ Good                         |
| Sorting                      | ✅ Good                         |
| `ApiResponse`                | ✅ Good                         |
| Service delegation           | ✅ Good                         |
| Logging                      | ✅ Good                         |
| `X-User-Id` concept          | ✅ Good, if gateway controls it |
| Order endpoint authorization | ⚠️ Needs attention             |
| Entity directly exposed      | ⚠️ Improve later               |
| Business logic in controller | ✅ None                         |

### Keep this file?

**Yes. No major rewrite is needed.**

The **one important change before calling the notification API production-ready** is to ensure `/order/{orderReference}` cannot expose another user's notification history.

Also, the testing command in your description should use the Maven Wrapper if your project is configured with it on Windows:

```powershell
.\mvnw.cmd clean test-compile -pl notification-service
```

Expected:

```text
BUILD SUCCESS
```

The next file I would review is **`NotificationLog.java`**, because that entity determines the database structure, notification status, timestamps, and—most importantly—the database-level idempotency protection for duplicate Kafka events.
*/