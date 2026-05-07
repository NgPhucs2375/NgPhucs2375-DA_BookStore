# Notification Real-time Implementation Guide

**Version**: 1.0 (Production MVP)  
**Last Updated**: May 7, 2026  
**Status**: 75% Complete, Ready for Staging  

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [Implementation Phases](#implementation-phases)
4. [Code Examples & Integration Points](#code-examples)
5. [Operational Knowledge](#operational-knowledge)
6. [Troubleshooting & Maintenance](#troubleshooting)
7. [Next Steps (Roadmap)](#roadmap)

---

## Quick Start

### Prerequisites
- Java 17+
- Spring Boot 3.x
- SQL Server (or compatible)
- Maven 3.8+

### Enable Notification System

**Step 1**: Apply migration V7
```bash
# Flyway automatically applies on next app startup
# Checks: target/classes/db/migration/V7__create_notification_delivery_table.sql
```

**Step 2**: Restart Spring Boot application
```bash
mvn spring-boot:run
```

**Step 3**: Test notification creation
```bash
# Create admin user first (must have ROLE_ADMIN)
# Then call:
curl -X POST http://localhost:8080/api/notifications/admin \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": null,
    "type": "SYSTEM_ANNOUNCEMENT",
    "title": "System Maintenance",
    "message": "Scheduled maintenance 2-3 AM",
    "priority": "HIGH"
  }'
```

**Step 4**: Subscribe to real-time notifications
```javascript
// Browser side
const eventSource = new EventSource('/api/notifications/me/subscribe', {
  headers: { 'Authorization': 'Bearer ' + token }
});

eventSource.addEventListener('notification', (e) => {
  const data = JSON.parse(e.data);
  console.log('Notification received:', data);
  // Update UI: show toast, badge, etc.
});
```

---

## Architecture Overview

### System Components (DB-Backed Queue)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. EVENT SOURCE                                             │
│    OrderService.updateSubOrderStatusForSeller()             │
│    (When seller changes sub-order status)                   │
└────────────────┬────────────────────────────────────────────┘
                 │ calls
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. NOTIFICATION SERVICE                                     │
│    - Validates request                                      │
│    - Persists Notification to DB (synchronous)             │
│    - Calls deliveryQueue.enqueue() (fire-and-forget)       │
└────────────────┬────────────────────────────────────────────┘
                 │
    ┌────────────┴──────────────┐
    │                           │
    ▼ saveNotification()        ▼ enqueue()
┌─────────────────┐      ┌──────────────────────────────┐
│ DB: Table       │      │ DB-Backed Queue              │
│ notifications   │      │ - Create notification_delivery
│                 │      │   record (status=PENDING)
│ (Persistent)    │      │ - Set next_retry_at=NOW
│                 │      │   (send immediately)
└────────┬────────┘      └──────────┬───────────────────┘
         │                          │
         │                          │
         │                    Background Worker
         │                    (runs every 1s)
         │                    @Scheduled(fixedDelay=1000)
         │                    │
         │                    ├─ Query: SELECT * FROM
         │                    │  notification_delivery
         │                    │  WHERE status='PENDING'
         │                    │  AND next_retry_at <= NOW
         │                    │
         │                    ├─ For each task:
         │                    │  - Try send via SSE
         │                    │  - If success: mark SENT
         │                    │  - If fail: backoff + retry
         │                    │  - After 5 attempts: DROP
         │                    │
         │                    └─ Loop again in 1s
         │                         │
         │                         ▼
         │             ┌────────────────────────┐
         │             │ SSE Service            │
         │             │ - emitter.send(event)  │
         │             │ - Multi-tab support    │
         │             └────────┬───────────────┘
         │                      │
         │                      ▼
         │         User's Browser (EventSource)
         │         receives real-time event
         │
         └─────────────────────┐
                              │
                      User can also
                      fetch via polling:
                      GET /api/notifications/me
                      (Shows all notifications
                       from Table: notifications)
```

### Key Entities

#### 1. Notification (Application Event)
```java
@Entity
@Table(name = "notifications")
class Notification {
    Long id;              // PK
    User user;           // FK (NOT NULL, cascade delete)
    NotificationType type;  // ORDER_CREATED, ORDER_STATUS_CHANGED, etc.
    String title;         // "Order #123 Confirmed"
    String message;       // Detailed message
    String payloadJson;   // {"orderId": 123, "subOrderId": 456, "status": "CONFIRMED"}
    Boolean isRead;       // Default: false
    NotificationPriority priority;  // LOW, NORMAL, HIGH, URGENT
    LocalDateTime createdAt;  // When event happened
    LocalDateTime readAt;     // When user marked read (NULL until read)
}
```

**Purpose**: Persists application events for all users. Acts as source of truth.

**Lifecycle**:
- Created immediately when event happens (OrderService.updateSubOrderStatusForSeller())
- Read status updated when user clicks mark-read endpoint
- Never deleted (only cascade on User deletion)

---

#### 2. NotificationDelivery (Delivery Task)
```java
@Entity
@Table(name = "notification_delivery")
class NotificationDelivery {
    Long id;                              // PK
    Notification notification;            // FK (cascade delete)
    String channel;                       // "SSE", "EMAIL", "PUSH"
    DeliveryStatus status;                // PENDING, SENT, FAILED, DROPPED
    LocalDateTime sentAt;                 // When successfully delivered
    String lastError;                     // Exception message if failed
    Integer attemptCount;                 // Number of retry attempts
    LocalDateTime nextRetryAt;            // When to retry (NULL if terminal state)
    LocalDateTime createdAt;              // When enqueued
    LocalDateTime updatedAt;              // Last modified (tracks retry history)
}

enum DeliveryStatus {
    PENDING,  // Waiting to send or retry
    SENT,     // Successfully delivered
    FAILED,   // Failed after max retries, reviewable
    DROPPED   // Abandoned, same as FAILED
}
```

**Purpose**: Tracks delivery attempt status. DB-backed queue for retry coordination.

**Lifecycle**:
- Created when Notification is created (via NotificationService.createNotification())
- Polled by queue worker every 1 second
- Updated with retry attempts + backoff times
- Marked SENT or DROPPED when terminal

---

### Data Flow Example: Order Status Change

```
Timeline (in seconds):
T=0s
  │
  ├─ Seller calls: PATCH /api/orders/sub-orders/456/status
  │
  ├─ OrderService.updateSubOrderStatusForSeller():
  │    - Save sub-order status change to DB
  │    - Commit DB transaction
  │    - Fire event: call notificationService.createNotification()
  │
  ├─ NotificationService.createNotification():
  │    - Create Notification entity (user_id=buyer.id, type=SUB_ORDER_STATUS_CHANGED)
  │    - Save to notifications table (NOW)
  │    - deliveryQueue.enqueue(notification, "SSE")
  │
  └─ NotificationDelivery created (status=PENDING, nextRetryAt=NOW, attemptCount=0)
  
T=1s
  │
  ├─ Queue worker wakes up (@Scheduled fixedDelay=1000)
  │
  ├─ Query: SELECT * FROM notification_delivery
  │  WHERE status='PENDING' AND next_retry_at <= NOW LIMIT 100
  │  Result: [NotificationDelivery(id=1, attemptCount=0, status=PENDING)]
  │
  ├─ processDeliveryTask():
  │    - Get buyer user ID from notification
  │    - Call sseService.sendEventToUser(buyerId, "notification", notification)
  │
  ├─ SSE Service:
  │    - Lookup Map<buyerId, Set<SseEmitter>>
  │    - For each emitter in Set:
  │       - emitter.send(SseEmitter.event().name("notification").data(json))
  │
  └─ If browser is open:
     │
     ├─ EventSource listener fires:
     │  es.addEventListener("notification", (e) => {
     │    const data = JSON.parse(e.data);
     │    console.log("Order status updated!", data);
     │  })
     │
     └─ UI updates immediately (show toast, update badge)
  
T=2s (if browser was offline at T=1s)
  │
  ├─ Queue worker wakes up again
  │
  ├─ Same delivery task still PENDING
  │    (next_retry_at=NOW+2s was set at previous failure)
  │
  └─ Retry send attempt
     ├─ Success: mark SENT, sentAt=NOW, done
     ├─ Failure: attemptCount=1, nextRetryAt=NOW+4s (exponential backoff)
```

---

## Implementation Phases

### Phase 1 (COMPLETED ✅): MVP - In-Memory Queue

**What was built**:
- Notification entity + migrations
- CRUD API endpoints
- SSE real-time infrastructure
- In-memory queue with retry/backoff

**Limitations**:
- Queue lost on app restart
- Single-node only
- No audit trail

---

### Phase 2 (CURRENT): Production-Ready - DB-Backed Queue

**What's implemented**:
- NotificationDelivery entity + V7 migration
- NotificationDeliveryRepository with optimized queries
- Refactored queue worker to read/write DB
- Comprehensive logging + error tracking

**When to deploy**: Now (ready for staging)

**How to verify**:
```bash
# 1. Check migration ran
SELECT * FROM notification_delivery;  # Should return empty (no notifications yet)

# 2. Create admin notification
POST /api/notifications/admin
Body: {"userId": null, "type": "SYSTEM_ANNOUNCEMENT", "title": "Test", "message": "Test"}

# 3. Check delivery records created
SELECT COUNT(*) FROM notification_delivery WHERE status='PENDING';
# Should show N records (one per user)

# 4. Wait 1 second for queue worker
SELECT COUNT(*) FROM notification_delivery WHERE status='SENT';
# Should show N records (all delivered if users online)
```

---

### Phase 3 (FUTURE): Distributed Queue + Scaling

**What to add**:
- Multiple app instances process same queue (no conflicts)
- Distributed lock on delivery row (SELECT...FOR UPDATE)
- Heartbeat/keepalive for SSE (avoid proxy timeouts)

**Implementation**:
- Add DB row-level lock in queue worker
- Or use Redis distributed lock (more complex)

**When**: When scaling to 2+ app instances

---

### Phase 4 (FUTURE): Observability & Monitoring

**What to add**:
- Metrics: queue depth, retry rate, success %
- Structured logging with correlation IDs
- Dashboard for ops (Grafana + Prometheus)

**Metrics to track**:
```sql
-- Queue health
SELECT 
  status, 
  COUNT(*) as count
FROM notification_delivery
WHERE created_at >= DATEADD(hour, -1, SYSUTCDATETIME())
GROUP BY status;
-- Expected: mostly SENT, few PENDING, rare DROPPED

-- Delivery latency
SELECT 
  AVG(DATEDIFF(ms, n.created_at, nd.sent_at)) as avg_latency_ms
FROM notification n
JOIN notification_delivery nd ON n.id = nd.notification_id
WHERE nd.status = 'SENT'
  AND nd.sent_at >= DATEADD(hour, -1, SYSUTCDATETIME());
-- Expected: < 5000 ms (5 seconds)

-- Failure rate
SELECT 
  CAST(100.0 * COUNT(*) FILTER (WHERE status='DROPPED') 
       / COUNT(*) AS DECIMAL(5,2)) as failure_pct
FROM notification_delivery
WHERE created_at >= DATEADD(day, -1, SYSUTCDATETIME());
-- Expected: < 1%
```

---

## Code Examples & Integration Points

### Example 1: Integrate with Business Event

**Scenario**: When a shop approval status changes, notify shop owner

**File**: `src/main/java/com/example/bookstore/service/ShopService.java`

```java
@Service
@RequiredArgsConstructor
public class ShopService {
    private final ShopRepository shopRepo;
    private final NotificationService notificationService;
    
    /**
     * Approve or reject shop application
     */
    @Transactional
    public void approveShop(Long shopId, boolean approve) {
        Shop shop = shopRepo.findById(shopId).orElseThrow();
        shop.setStatus(approve ? APPROVED : REJECTED);
        shop.setApprovedAt(LocalDateTime.now());
        shopRepo.save(shop);
        
        // Fire notification event
        NotificationCreateRequest req = NotificationCreateRequest.builder()
            .type(SHOP_APPROVAL_UPDATED)
            .title(approve ? "Shop Approved!" : "Shop Application Rejected")
            .message(approve 
                ? "Your shop has been approved and is now live!"
                : "Your shop application was rejected. Please review and reapply.")
            .payloadJson(JsonUtils.toJson(new ShopApprovalPayload(
                shopId,
                shop.getOwner().getId(),
                approve
            )))
            .priority(HIGH)
            .build();
        
        // Send to shop owner (userId = shop owner ID)
        notificationService.createNotification(
            SYSTEM_USER_ID,        // admin creating on behalf of system
            shop.getOwner().getId(), // target user
            req
        );
        // Background: queue worker picks up in next second
    }
}
```

### Example 2: Subscribe to Real-time Events (Frontend)

**File**: `src/main/resources/static/js/notifications.js`

```javascript
// Initialize SSE subscription on page load
function initNotifications() {
    const token = localStorage.getItem('jwt_token');
    
    // Create EventSource with JWT in URL or headers
    const eventSource = new EventSource(
        '/api/notifications/me/subscribe',
        {
            // Headers not supported by EventSource, so JWT in URL
            // OR use fetch API: https://...?token=JWT
        }
    );
    
    // Fallback: include token in Authorization header via custom interceptor
    // Spring Security filter will extract from request context
    
    eventSource.addEventListener('notification', (e) => {
        const notification = JSON.parse(e.data);
        
        // Update UI
        showNotificationToast(notification);
        updateBadgeCount();
        
        // Optionally: play sound
        playNotificationSound();
        
        // Log for analytics
        console.log('Notification received', notification);
    });
    
    eventSource.addEventListener('error', (e) => {
        if (eventSource.readyState === EventSource.CLOSED) {
            console.warn('SSE connection closed');
            // Fallback: switch to polling GET /api/notifications/me every 30s
            setTimeout(pollNotifications, 30000);
        }
    });
}

// Polling fallback (if SSE not available)
async function pollNotifications() {
    const response = await fetch('/api/notifications/me?size=10', {
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('jwt_token')
        }
    });
    const data = await response.json();
    
    // Update UI with new notifications
    updateNotificationList(data.items);
}

// Mark notification as read when clicked
async function markAsRead(notificationId) {
    await fetch(`/api/notifications/me/${notificationId}/read`, {
        method: 'PATCH',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('jwt_token')
        }
    });
    // UI updated via SSE or polling
}
```

### Example 3: Query Delivery Status (Ops Dashboard)

**File**: Admin dashboard or cron job

```java
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {
    
    private final NotificationDeliveryRepository deliveryRepo;
    
    /**
     * Dashboard: show delivery health metrics
     * GET /api/admin/notifications/health
     */
    @GetMapping("/health")
    public DeliveryHealthResponse getDeliveryHealth() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        long sent = deliveryRepo.countByChannelAndStatusAndCreatedAtAfter(
            "SSE", 
            SENT, 
            oneHourAgo
        );
        
        long failed = deliveryRepo.countByChannelAndStatusAndCreatedAtAfter(
            "SSE",
            FAILED,
            oneHourAgo
        );
        
        long dropped = deliveryRepo.countByChannelAndStatusAndCreatedAtAfter(
            "SSE",
            DROPPED,
            oneHourAgo
        );
        
        long total = sent + failed + dropped;
        double successRate = total > 0 ? (100.0 * sent / total) : 0;
        
        return DeliveryHealthResponse.builder()
            .sent(sent)
            .failed(failed)
            .dropped(dropped)
            .successRate(successRate)
            .timeWindowHours(1)
            .healthStatus(successRate >= 95 ? "HEALTHY" : "DEGRADED")
            .build();
    }
    
    /**
     * Dashboard: show stuck deliveries (potential worker crash)
     * GET /api/admin/notifications/stuck
     */
    @GetMapping("/stuck")
    public List<NotificationDelivery> getStuckDeliveries() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        return deliveryRepo.findStuckDeliveries(thirtyMinutesAgo);
    }
    
    /**
     * Operational tool: manually retry failed deliveries
     * POST /api/admin/notifications/retry?channel=EMAIL&since=2024-05-01
     */
    @PostMapping("/retry")
    public void retryFailedDeliveries(
        @RequestParam String channel,
        @RequestParam(required = false) String since
    ) {
        LocalDateTime fromTime = since != null 
            ? LocalDateTime.parse(since)
            : LocalDateTime.now().minusHours(1);
        
        deliveryRepo.markForRetry(channel, fromTime);
        // Next queue worker cycle will retry all marked records
    }
}
```

---

## Operational Knowledge

### 1. Monitoring Checklist

**Daily**:
```sql
-- Check delivery success rate (should be > 95%)
SELECT 
  status, 
  COUNT(*) as count
FROM notification_delivery
WHERE created_at >= DATEADD(day, -1, SYSUTCDATETIME())
GROUP BY status;

-- Check for stuck deliveries (should be empty)
SELECT * FROM notification_delivery
WHERE status='PENDING' 
  AND updated_at < DATEADD(hour, -1, SYSUTCDATETIME());
```

**Weekly**:
```sql
-- Check notification table growth
SELECT 
  COUNT(*) as total_notifications,
  COUNT(DISTINCT user_id) as unique_users
FROM notifications
WHERE created_at >= DATEADD(day, -7, SYSUTCDATETIME());

-- Check if any users are hoarding old unread notifications
SELECT 
  user_id,
  COUNT(*) as unread_count
FROM notifications
WHERE is_read = 0
GROUP BY user_id
ORDER BY unread_count DESC
LIMIT 10;
```

**Monthly**:
```sql
-- Archive old read notifications (older than 90 days)
DELETE FROM notification_delivery
WHERE created_at <= DATEADD(day, -90, SYSUTCDATETIME())
  AND status IN ('SENT', 'DROPPED');

-- Vacuum indexes
DBCC DBREINDEX (notification_delivery);
```

---

### 2. Troubleshooting Guide

#### Problem: SSE Connection Closes After 30 Seconds

**Symptom**: Client receives messages for 30s, then EventSource closes

**Root Cause**: Reverse proxy (nginx/LB) timeout or network timeout

**Solution**:
```nginx
# nginx.conf
upstream api {
    server localhost:8080;
}

server {
    location /api/notifications/me/subscribe {
        proxy_pass http://api;
        proxy_http_version 1.1;
        
        # SSE requires persistent connection
        proxy_set_header Connection "";
        proxy_set_header X-Accel-Buffering no;
        
        # Long timeout for SSE (don't close after 30s)
        proxy_read_timeout 3600s;
        proxy_connect_timeout 7d;
    }
}
```

#### Problem: Some Notifications Never Delivered

**Symptom**: Notification created but never appears in user UI

**Root Cause**: Queue worker crashed or user offline

**Debug Steps**:
```sql
-- Find the notification
SELECT * FROM notifications WHERE id = 123;

-- Check delivery attempts
SELECT * FROM notification_delivery 
WHERE notification_id = 123
ORDER BY created_at DESC;

-- If status = PENDING and updated_at very old → worker crashed
-- If status = DROPPED → exceeded 5 retries, user was offline too long

-- Solution: manually mark for retry
UPDATE notification_delivery
SET status = 'PENDING', 
    attempt_count = 0, 
    next_retry_at = SYSUTCDATETIME()
WHERE notification_id = 123;
```

#### Problem: Queue Depth Growing (Not Processing)

**Symptom**: `SELECT COUNT(*) FROM notification_delivery WHERE status='PENDING'` shows increasing numbers

**Root Cause**: 
1. Queue worker thread crashed
2. SSE service broken
3. Too many deliveries (overwhelming the worker)

**Debug**:
```bash
# Check app logs
tail -f logs/application.log | grep NotificationDeliveryQueue

# Look for:
# - "[ERROR] Queue worker crashed: ..."
# - "[ERROR] Processing delivery task ... failed"
# - "[WARN] Notification ... delivery failed, retry"

# Check if worker is running
jps | grep BookStore  # should show running process
```

**Fix**:
1. Restart app: `systemctl restart bookstore-service`
2. Check SSE service health
3. Reduce BATCH_SIZE if overwhelming (ProcessQueue.BATCH_SIZE = 50 instead of 100)

---

### 3. Performance Tuning

#### Reduce Latency (Speed Up Delivery)

```java
// In NotificationDeliveryQueue.java

// Option 1: Reduce poll interval (more CPU)
@Scheduled(fixedDelay = 500, initialDelay = 1000)  // 500ms instead of 1000ms
public void processQueue() { ... }

// Option 2: Process more tasks per cycle (longer transactions)
private static final int BATCH_SIZE = 200;  // 200 instead of 100
```

#### Handle High Volume (Millions of Notifications)

```java
// Problem: Broadcast to 100,000 users = 100k DB inserts

// Solution 1: Batch insert in chunks
private void saveBatch(List<Notification> notifications) {
    for (int i = 0; i < notifications.size(); i += 1000) {
        List<Notification> batch = notifications.subList(i, i + 1000);
        notificationRepository.saveAll(batch);
    }
}

// Solution 2: Async processing (non-blocking)
@Async
public void createBroadcastNotificationAsync(NotificationCreateRequest req) {
    List<User> users = userRepository.findAll();
    for (User user : users) {
        Notification n = saveNotification(user, req);
        deliveryQueue.enqueue(n, "SSE");
    }
}
```

#### Archive Old Records (Keep DB Fast)

```sql
-- Schedule monthly (2 AM)
DELETE FROM notification_delivery
WHERE created_at < DATEADD(month, -3, SYSUTCDATETIME())
  AND status != 'PENDING';

-- Rebuild indexes
DBCC DBREINDEX (notifications);
```

---

### 4. Alert Configuration

Setup alerts in your monitoring system (Datadog, New Relic, CloudWatch):

```yaml
Alerts:
  - name: "SSE Delivery Success Rate"
    condition: "success_rate < 95%"
    severity: "WARNING"
    action: "Page SRE if sustained for 5 minutes"
    
  - name: "Queue Depth Growing"
    condition: "pending_count > 10000"
    severity: "CRITICAL"
    action: "Page on-call engineer immediately"
    
  - name: "Stuck Deliveries"
    condition: "updated_at < now() - 30 minutes AND status='PENDING'"
    severity: "CRITICAL"
    action: "Alert SRE to check queue worker logs"
    
  - name: "High Retry Rate"
    condition: "retry_count_per_min > 100"
    severity: "WARNING"
    action: "Check user connectivity, SSE stability"
```

---

## Troubleshooting & Maintenance

### Scenario 1: User Never Gets a Notification

**Investigation Steps**:

```java
// Step 1: Did notification get created in DB?
Notification n = notificationRepository.findById(123L);
if (n == null) {
    // Problem: NotificationService didn't save
    // Check OrderService logs for exception in fire-and-forget
    return "Notification was never created";
}

// Step 2: Did delivery task get enqueued?
List<NotificationDelivery> tasks = deliveryRepository.findByNotificationId(123L);
if (tasks.isEmpty()) {
    // Problem: enqueue() wasn't called
    // Check NotificationService.createNotification() logic
    return "Delivery task was not created";
}

// Step 3: Was delivery attempt made?
for (NotificationDelivery task : tasks) {
    System.out.println("Status: " + task.getStatus());  // PENDING/SENT/DROPPED
    System.out.println("Attempts: " + task.getAttemptCount());
    System.out.println("Last Error: " + task.getLastError());
}

// Diagnosis:
// - status=SENT → SSE sent successfully, UI issue?
// - status=PENDING, updated_at very old → worker crashed
// - status=DROPPED, attempts=5 → user was offline, gave up
```

### Scenario 2: Queue Worker Seems Frozen

**Check Health**:

```bash
# 1. Is app running?
curl http://localhost:8080/actuator/health

# 2. Are there pending tasks?
SELECT COUNT(*) FROM notification_delivery WHERE status='PENDING';

# 3. How old are they?
SELECT MIN(updated_at) FROM notification_delivery WHERE status='PENDING';

# 4. Check logs for exceptions
tail -100 logs/application.log | grep -i "exception\|error"

# 5. Check thread dump
jstack $(pgrep -f BookStore) | grep -i "NotificationDeliveryQueue"
```

**Restart Worker**:
```bash
systemctl restart bookstore-service
# Or if running locally:
Ctrl+C and restart
```

---

## Roadmap

### Next 1-2 Sprints (Production Readiness)

- [x] **Phase 1**: DB-backed queue (DONE)
- [ ] **Phase 2a**: Distributed lock for multi-instance (SELECT...FOR UPDATE)
- [ ] **Phase 2b**: Heartbeat/keepalive for SSE (HTTP comment every 30s)
- [ ] **Phase 3**: Observability dashboard (Grafana panels)

### Long-term (Nice-to-have)

- [ ] Email/SMS fallback channels (if SSE down)
- [ ] Message broker integration (RabbitMQ, Kafka for true multi-tenant)
- [ ] Batch broadcast optimization (handle 1M+ users)
- [ ] Dead-letter queue with replay capability
- [ ] Idempotency keys (prevent duplicate sends)

---

## Summary

You now have:
✅ Production-ready persistent notification system  
✅ DB-backed queue for reliability  
✅ Real-time SSE delivery with exponential backoff retry  
✅ Comprehensive operational knowledge  
✅ Debugging & troubleshooting guide  

**Next Steps**:
1. Deploy to staging environment
2. Load test: simulate 100 concurrent users
3. Monitor delivery success rate for 24 hours
4. Implement Phase 2 (distributed lock) before multi-instance production
5. Set up monitoring alerts (see Operational Knowledge section)

---

**Questions?** Check architecture doc: [NOTIFICATION_REALTIME_ARCHITECTURE.md](./NOTIFICATION_REALTIME_ARCHITECTURE.md)
