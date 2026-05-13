 # Notification Real-time Architecture & Implementation Guide

## 1. Overview & Architecture

### 1.1 System Design
```
┌─────────────────────────────────────────────────────────────────┐
│ Notification Real-time System Architecture                      │
└─────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ OrderService │ (when seller updates sub-order status)
    │ (Hook Event) │
    └────────┬─────┘
             │ createNotification()
             ▼
    ┌──────────────────────────────┐
    │ NotificationService          │
    │ - Validate & persist to DB   │
    │ - Save to notifications tbl  │
    │ - Enqueue delivery task      │
    └────────┬─────────────────────┘
             │
             ├─────────────────────────────────────────┐
             │                                         │
             ▼                                         ▼
    ┌──────────────────────────┐      ┌─────────────────────────┐
    │ NotificationDeliveryQueue│      │ Notifications Table (DB)│
    │ (Worker: poll & retry)  │      │ - id, user_id, type,    │
    │ - Read from DB queue    │      │   title, created_at,    │
    │ - Retry logic (backoff) │      │   is_read, etc.         │
    │ - Log attempt/result    │      │                         │
    └────────┬─────────────────┘      └─────────────────────────┘
             │
             │ sendEventToUser/sendEventToAll
             ▼
    ┌──────────────────────────┐
    │ NotificationSseService   │
    │ (SSE Emitters per user)  │
    └────────┬─────────────────┘
             │
             ├──────────┬──────────┬──────────┐
             │          │          │          │
    ┌────────▼────┐ ┌──▼────────┐┌─▼───────┐┌▼────────┐
    │ Browser Tab │ │ Mobile    ││ Another ││ Offline │
    │ (Connected) │ │ App       ││ Tab     ││ Queue   │
    └─────────────┘ └───────────┘└─────────┘└────────┘
```

### 1.2 Data Flow: Order Status Change → Notification Delivery

1. **Event Trigger**: Seller calls PATCH `/api/orders/sub-orders/{id}/status`
   - OrderController validates ownership & role
   - OrderService.updateSubOrderStatusForSeller() runs inside @Transactional

2. **Persist Event**: NotificationService.createNotification() is called (fire-and-forget)
   - Validates request (type, title not empty)
   - For single user: save 1 Notification record
   - For broadcast (userId=null): fan-out save N records (1 per user)
   - Each record saves with is_read=false, created_at=now

3. **Enqueue Delivery**: NotificationDeliveryQueue.enqueue() queues for sending
   - Creates DeliveryTask with attempt=0, nextAttemptAt=now
   - Added to in-memory or DB queue

4. **Process Queue** (background worker runs every 1 second):
   - Poll queue for tasks where nextAttemptAt <= now
   - Try send via NotificationSseService (broadcast or single user)
   - If success: remove from queue + log success
   - If fail: increment attempts, calculate backoff, reschedule
   - If attempts >= MAX: drop from queue + log failure

5. **Send to Client**:
   - SSE service maintains Map<userId, Set<SseEmitter>>
   - For each connected user, sends event via emitter.send()
   - Event name = "notification", data = NotificationItemResponse (JSON)

6. **Client-side** (not in this scope but mentioned for context):
   - JavaScript listens: es.addEventListener('notification', (e) => ...)
   - Updates UI: badge count, notification list, sound alert
   - Can also fetch via GET /api/notifications/me (polling fallback)

---

## 2. Key Components & Responsibilities

### 2.1 NotificationService (Core Business Logic)
**Location**: `src/main/java/com/example/bookstore/service/NotificationService.java`
**Responsibility**: 
- Validate incoming notification requests
- Persist notifications to DB
- Enqueue for delivery
- Handle bulk operations (broadcast = fan-out)

**Key Methods**:
- `createNotification(creatorUserId, targetUserId, req)`: Main entry point
  - If targetUserId != null: 1 notification to 1 user
  - If targetUserId == null: fan-out to all users (broadcast)
  - Always persists FIRST, then enqueues
  - Why persist first? Ensures DB is source of truth even if SSE fails

### 2.2 NotificationDeliveryQueue (Retry & Backoff Strategy)
**Location**: `src/main/java/com/example/bookstore/sse/NotificationDeliveryQueue.java`
**Responsibility**:
- Manage in-memory queue of delivery tasks
- Background worker (scheduled every 1 second)
- Implement exponential backoff retry (2s, 4s, 8s, 16s, 32s)
- Log failures after MAX_ATTEMPTS=5

**Why exponential backoff?**
- Avoids hammering a temporarily-down client
- If client reconnects in 2s, we retry; if down for 30s+, we give up
- Balances responsiveness vs. resource waste

**Note**: Current implementation is in-memory. Production upgrade will move to DB.

### 2.3 NotificationSseService (Real-time Delivery)
**Location**: `src/main/java/com/example/bookstore/sse/NotificationSseService.java`
**Responsibility**:
- Register/unregister SSE emitters per user
- Send events to connected users
- Handle multi-tab: one user can have multiple emitters

**Why multi-tab support?**
- User opens app in 2 browser tabs → 2 emitters registered
- Notification sent to both tabs simultaneously
- Improves user experience (sees notification everywhere)

### 2.4 NotificationController (REST API)
**Location**: `src/main/java/com/example/bookstore/controller/NotificationController.java`
**Responsibility**:
- User endpoints: GET /api/notifications/me, PATCH mark-read
- Admin endpoints: POST /api/notifications/admin (create + broadcast)
- SSE endpoint: GET /api/notifications/me/subscribe

**Security**:
- All `/notifications/**` requires authenticated user
- Only ADMIN can POST /admin (enforced by SecurityConfig)
- Ownership check in service layer (ensure user can only access own notifications)

---

## 3. Techniques & Patterns Applied

### 3.1 At-Least-Once Delivery Pattern
**What**: Notification might be sent multiple times (not exactly-once)
**Why**: Harder to achieve exactly-once in distributed systems; at-least-once is simpler + sufficient
**Implementation**:
- Store notification ID in DB first (immutable)
- When retry sends same notification again, client sees same ID
- Client-side deduplication by ID (if needed in future)

### 3.2 Outbox/Inbox Pattern (Future Upgrade)
**What**: Store delivery status/retry info in `notification_delivery` table
**Why**: 
- Survives app restart (current queue is lost)
- Enables multi-instance coordination (via polling or message broker)
- Audit trail: who sent, when, success/fail, retry count
**Future**: Will replace in-memory queue with DB-backed worker

### 3.3 Fan-out Broadcasting
**What**: For broadcast (userId=null), create 1 notification record per user
**Why**:
- Schema constraint: notifications.user_id is NOT NULL
- Simpler to query "get all unread for user X"
- Trade-off: more DB inserts, but simpler queries + security model
**Alternative**: Separate broadcast table (would require separate schema)

### 3.4 Fire-and-Forget Event Handling
**What**: OrderService.updateSubOrderStatusForSeller() does NOT wait for notification creation
**Why**:
- Keeps order update fast (user sees response immediately)
- Notification is best-effort (doesn't block business logic)
- Wrapped in try-catch to prevent cascading failure
**Implementation**: Notification creation happens async (via queue), not inside order transaction

### 3.5 Exponential Backoff with Jitter (Future)
**Current**: Simple backoff: 2s, 4s, 8s, 16s, 32s (no jitter)
**Future**: Add jitter to prevent "thundering herd" when many tasks fail simultaneously
**Formula**: `backoff_ms = BASE * (1 << attempts) + random(0, BASE)`

### 3.6 Multi-tab SSE Connection Management
**What**: ConcurrentHashMap<userId, Set<SseEmitter>>
**Why**: 
- One user can open app in multiple tabs
- Each tab = separate SSE connection = separate emitter
- All emitters for same user get same notification
**Trade-off**: More memory (1 per open tab), but better UX

---

## 4. Security Considerations

### 4.1 Ownership Check
- In NotificationService: check that `findByIdAndUserId()` returns result
- Prevents user A from reading user B's notifications
- Enforced at service layer (not controller), closer to data

### 4.2 Role-Based Access
- /api/notifications/admin/* requires ROLE_ADMIN
- Defined in SecurityConfig.java
- Only admins can create system announcements or broadcast

### 4.3 No Token Leakage
- SSE is not RESTful; returns SseEmitter not JSON
- Browser SSE() call sends Cookies + Authorization header automatically
- Server extracts CURRENT_USER_ID from JWT in filter

---

## 5. Data Schema

### 5.1 Notifications Table (V6 migration)
```sql
CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type NVARCHAR(50) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    message NVARCHAR(MAX) NULL,
    payload_json NVARCHAR(MAX) NULL,
    is_read BIT NOT NULL DEFAULT 0,
    priority NVARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    read_at DATETIME2 NULL,
    
    CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast query: list unread notifications
CREATE INDEX IX_notifications_user_is_read_created_at 
    ON notifications (user_id, is_read, created_at DESC);
```

### 5.2 Notification_Delivery Table (V7 migration - coming)
```sql
CREATE TABLE notification_delivery (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    channel NVARCHAR(50) NOT NULL, -- 'SSE' or 'EMAIL' or 'PUSH'
    status NVARCHAR(20) NOT NULL, -- 'PENDING', 'SENT', 'FAILED', 'DROPPED'
    sent_at DATETIME2 NULL,
    last_error NVARCHAR(500) NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    
    CONSTRAINT FK_notif_delivery_notif FOREIGN KEY (notification_id) 
        REFERENCES notifications(id) ON DELETE CASCADE
);

-- Index for workers to find pending tasks
CREATE INDEX IX_notification_delivery_status_retry 
    ON notification_delivery (status, next_retry_at) 
    WHERE status = 'PENDING';
```

---

## 6. Production Readiness Checklist

### Now (MVP - 75% done)
- [x] Persistence: notifications table with full CRUD
- [x] Real-time: SSE endpoint, multi-tab support
- [x] Queue: in-memory retry with exponential backoff
- [x] Hook: OrderService → NotificationService integration
- [x] API: list, mark-read, unread-count, subscribe, admin create
- [x] Security: JWT ownership, role-based access
- [x] Compile: passes Maven build

### Must-Have for Production (25% remaining)
- [ ] Delivery tracking: notification_delivery table (V7)
- [ ] DB-backed queue: NotificationDeliveryQueue reads/writes to DB
- [ ] Metrics & logging: attempt count, error tracking, correlation ID
- [ ] Multi-instance support: shared DB queue (no single-node assumption)
- [ ] Idempotency key: business key to prevent duplicate sends

### Nice-to-Have (Future iterations)
- [ ] Heartbeat: periodic SSE keepalive (avoid proxy timeouts)
- [ ] Dead-letter queue: manual replay of failed deliveries
- [ ] Batch broadcast: chunking for large fan-outs (avoid timeout)
- [ ] Fallback channels: email, push notifications if SSE down
- [ ] Analytics: delivery rate, latency percentiles, retention

---

## 7. Environment & Dependencies

### 7.1 Required Technologies
- **Java**: 17+ (using records, virtual threads ready)
- **Spring Boot**: 3.0+ (virtual threads support)
- **Spring Data JPA**: for repository queries
- **Spring Web**: for SSE @Controller
- **SQL Server**: notifications table with DATETIME2
- **Build**: Maven 3.8+

### 7.2 Key Libraries
- **Lombok**: @RequiredArgsConstructor, @Builder for boilerplate
- **Jackson**: JSON serialization of notifications to SSE client

### 7.3 Architectural Patterns
- **Layered Architecture**: Controller → Service → Repository → Entity
- **Transactional Boundary**: Service methods marked @Transactional
- **Dependency Injection**: Spring @RequiredArgsConstructor injects via constructor
- **Async Task Queue**: Background worker thread for delivery retry

---

## 8. Knowledge Required to Understand & Operate

### 8.1 For Developers
1. **Spring Framework**
   - @Service, @Repository, @Transactional
   - DI via constructor injection
   - @RequestMapping, @GetMapping, @PostMapping

2. **JPA/Hibernate**
   - Entity mapping, @ManyToOne, @OneToMany
   - Repository query methods
   - @Query custom JPQL/native SQL

3. **SQL Server Specifics**
   - DATETIME2 timezone handling
   - IDENTITY auto-increment columns
   - Indexes for query performance

4. **Real-time Web**
   - Server-Sent Events (SSE) protocol
   - Browser EventSource API
   - Connection lifecycle (onCompletion, onTimeout, onError)

5. **Concurrency**
   - ConcurrentHashMap for thread-safe map
   - ScheduledExecutorService for background tasks
   - Synchronized blocks for critical sections

6. **Testing**
   - Unit test: mock NotificationRepository, verify persistence
   - Integration test: @SpringBootTest, test SSE handshake
   - Load test: simulate N users subscribing + sending notifications

### 8.2 For DevOps/SRE
1. **Monitoring**
   - Queue depth: how many deliveries pending?
   - Retry rate: how many failures/retries per minute?
   - Latency: time from event → DB persist → SSE delivery
   - Connection count: how many SSE emitters active?

2. **Alerting**
   - Queue depth > threshold → risk of lost deliveries
   - Retry rate spike → client connectivity issues
   - Dead-letter count growing → tasks dropped after 5 retries

3. **Database Tuning**
   - Ensure index on (user_id, is_read, created_at) exists
   - Monitor notifications table growth (1000s per day? 1000s per second?)
   - Archive old read notifications quarterly

4. **Scaling**
   - Shared DB queue means no single-instance assumption
   - Multiple app instances can process queue concurrently
   - Use DB row-level locking to avoid duplicate delivery

---

## 9. Flow Diagrams

### 9.1 Happy Path: Seller Updates Sub-order → Buyer Gets Notification

```
Seller Browser                  Backend (Spring)                   Buyer Browser
─────────────────              ────────────────────                ────────────────

PATCH /api/orders/
  sub-orders/{id}/status
         │
         └──────────────────────────────────────────────────────────────┐
                                                                        │
                                  OrderService.updateSubOrderStatusForSeller()
                                  ├─ Save new status to DB
                                  ├─ Commit transaction
                                  ├─ Try: notificationService.createNotification()
                                  │  ├─ Validate request
                                  │  ├─ Save to notifications table
                                  │  └─ deliveryQueue.enqueue()
                                  │     ├─ Add to queue
                                  │     └─ Background worker picks up (in 1 sec)
                                  │        ├─ Call notificationSseService.sendEventToUser()
                                  │        ├─ Get emitter from Map<buyerId, Set<SseEmitter>>
                                  │        └─ emitter.send(event)
                                  │
                                  └─ Return 200 OK
         │
         └──────────────────────────────────────────────────────────────┐
                                                                        │
                                  SSE event arrives
                                  ├─ event.name = "notification"
                                  └─ event.data = {
                                       "id": 123,
                                       "type": "SUB_ORDER_STATUS_CHANGED",
                                       "title": "...",
                                       "message": "...",
                                       "isRead": false
                                     }
                                                                        │
                                                              ES listener
                                                              ├─ Update badge
                                                              ├─ Show toast
                                                              └─ Play sound
```

### 9.2 Error Handling: Retry + Backoff

```
Queue Worker                 SSE Service              Buyer (offline)
────────────────            ───────────              ─────────────────

Task: send notification
├─ Attempt 1 at T=0
│  └─ User offline → IOException
│     └─ Mark failed, backoff = 2s
│
├─ (1 sec passes, queue worker runs again)
│  └─ Attempt 2 at T=2
│     └─ Still offline → IOException
│        └─ backoff = 4s
│
├─ (2 sec passes)
│  └─ Attempt 3 at T=6
│     └─ Still offline → IOException
│        └─ backoff = 8s
│
├─ (8 sec passes, T=14)
│  └─ Attempt 4 at T=14
│     └─ Buyer reconnects! ✓ Send succeeds
│        └─ Remove from queue
│           (Notification already in DB, so not lost)
```

---

## 10. Next Steps (Implementation Roadmap)

### Phase 1 (This sprint): Add Delivery Tracking
- Create V7 migration: notification_delivery table
- Create NotificationDelivery entity
- Update NotificationService: log attempts to DB
- Add admin endpoint to view delivery status

### Phase 2 (Next sprint): DB-Backed Queue
- Refactor NotificationDeliveryQueue to read from DB
- Replace in-memory queue with DB queries
- Enables multi-instance deployment

### Phase 3: Observability
- Add metrics: queue depth, retry rate, success rate
- Add correlation ID for tracing
- Dashboard for ops to monitor delivery health

### Phase 4: Resilience & Scale
- Implement heartbeat SSE (keep connection alive)
- Add dead-letter table for manual replay
- Batch broadcast for large fan-outs

---

## 11. References & Further Reading

- **Spring SSE**: https://spring.io/guides/gs/sse-reactive-client/
- **Server-Sent Events**: https://html.spec.whatwg.org/multipage/server-sent-events.html
- **Exponential Backoff**: https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
- **At-Least-Once Delivery**: https://www.confluent.io/blog/exactly-once-semantics-are-not-exactly-the-same/
- **Database Indexing**: https://use-the-index-luke.com/

---

## Document Maintained By
Development Team - BookStore Multi-vendor Platform  
Last Updated: May 7, 2026  
Version: 1.0 (MVP)
