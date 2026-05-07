# Notification System - Techniques & Knowledge Base

## 🎓 Technologies & Patterns Applied

### 1. Architecture Patterns

#### Layered Architecture
**What**: Separation of concerns (Controller → Service → Repository → Entity)
```
User Request
    ↓
Controller (HTTP mapping, validation)
    ↓
Service (Business logic, transactions)
    ↓
Repository (Data access, queries)
    ↓
Entity (Domain model, ORM mapping)
    ↓
Database
```

**Why Used**:
- Testability: can mock each layer independently
- Maintainability: clear responsibilities
- Scalability: can optimize each layer separately

**Examples in Code**:
- `NotificationController` handles HTTP routing
- `NotificationService` handles business logic
- `NotificationRepository` handles data access
- `Notification`, `NotificationDelivery` entities

---

#### Event-Driven Architecture
**What**: Decouple producers from consumers via events
```
OrderService (Producer)
    ↓ creates event
Notification (Event)
    ↓ consumed by
NotificationDeliveryQueue (Consumer)
```

**Why Used**:
- OrderService doesn't care if notification succeeds
- Notification system can be updated without changing OrderService
- Easier to add new consumers (email, SMS, push)

**Trade-off**:
- Fire-and-forget means notification is best-effort (not guaranteed)
- If you need guaranteed delivery, would need saga pattern (more complex)

---

#### Queue/Backoff Pattern
**What**: Distribute work over time with exponential backoff
```
Event arrives at T=0s
    ↓ (fail)
Retry in 2s (T=2s)
    ↓ (fail)
Retry in 4s (T=6s)
    ↓ (fail)
Retry in 8s (T=14s)
    ↓ (success)
Done
```

**Why Used**:
- Avoids hammering a temporarily-down service
- Gives time for network/browser to recover
- Prevents "thundering herd" (all retries at same time)

**When to Use**:
- External API calls (might be slow)
- Network-dependent delivery (SSE might disconnect)
- Bursty traffic (smooth out peaks)

**Alternatives**:
- Immediate retry: fast but resource-intensive
- Circuit breaker: fail fast, good for persistent failures

---

### 2. Database Patterns

#### Index Strategy
**What**: Optimize query performance via database indexes
```sql
-- Before index: full table scan
SELECT * FROM notification_delivery 
WHERE status='PENDING' AND next_retry_at <= NOW
-- Scans all 1M rows → slow

-- After index on (status, next_retry_at):
CREATE INDEX IX_pending_retry ON notification_delivery(status, next_retry_at)
-- Uses index → fast (O(log n))
```

**Types Used**:
1. **Composite Index** on (status, next_retry_at)
   - For queue worker polling
   - Filter by status first (low cardinality), then time range

2. **Foreign Key Index** on notification_id
   - For audit trail queries
   - Find all delivery attempts for a notification

3. **Partial Index** on (status, created_at) WHERE status='FAILED'
   - For ops dashboard (failed deliveries only)
   - Smaller index = faster scans

**Rule of Thumb**:
- Index if query is slow AND runs frequently
- Don't index INSERT-heavy tables excessively (indexes slow down writes)

---

#### Cascade Delete
**What**: Automatically delete child records when parent deleted
```sql
CONSTRAINT FK_notification_delivery_notification 
FOREIGN KEY (notification_id) 
REFERENCES notifications(id) 
ON DELETE CASCADE
```

**Why Used**:
- Referential integrity: no orphaned delivery records
- Data consistency: if notification deleted, all attempts deleted
- Automatic cleanup: don't need manual cleanup queries

**Alternative**:
- ON DELETE SET NULL: keep child but clear FK (not used here)
- ON DELETE RESTRICT: prevent deletion if children exist (safer but rigid)

---

### 3. Spring Framework Patterns

#### Dependency Injection via Constructor
**What**: Spring provides dependencies automatically
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class NotificationService {
    private final NotificationRepository repo;
    private final NotificationDeliveryQueue queue;
    // Lombok generates constructor:
    // public NotificationService(NotificationRepository repo, NotificationDeliveryQueue queue)
}
```

**Why Used**:
- Testability: can inject mock implementations
- Immutability: dependencies are final (can't accidentally change)
- Clarity: constructor parameters show dependencies upfront

**Alternatives**:
- Field injection: `@Autowired private NotificationRepository repo;` (harder to test)
- Setter injection: `public void setRepo(NotificationRepository repo)` (mutable, less clear)

---

#### Transactional Boundaries
**What**: Group database operations into atomic transactions
```java
@Transactional
public NotificationItemResponse createNotification(...) {
    // Step 1: Save notification
    Notification n = saveNotification(user, req);
    
    // Step 2: Enqueue delivery
    deliveryQueue.enqueue(n, "SSE");
    
    // If exception here, both steps rollback
    return toItemResponse(n);
}
```

**Why Used**:
- Atomicity: either both steps succeed or both rollback
- Consistency: never in partial state
- Prevents data corruption

**Trade-offs**:
- Longer transactions = more locks = more contention
- Should be as short as possible
- Here: only includes DB saves, not SSE delivery

---

#### Scheduled Tasks
**What**: Run background jobs on a schedule
```java
@Scheduled(fixedDelay = 1000, initialDelay = 2000)
public void processQueue() {
    // Runs every 1000ms (1 second)
    // First run: 2000ms after app startup
}
```

**Why Used**:
- Polling-based queue: check DB regularly
- Alternative to message broker: simpler for MVP

**Configuration**:
```java
@Configuration
@EnableScheduling  // Enable @Scheduled annotation
public class AppConfig { }
```

**Advantages**:
- Simple, no external dependencies
- Monitoring: can add metrics to scheduled methods

**Disadvantages**:
- Single-thread: if task takes 2s and interval is 1s, next task waits
- No distribution: all instances do same work (wasteful)
- For production: consider message broker (RabbitMQ, Kafka)

---

### 4. ORM Patterns (JPA/Hibernate)

#### Entity Relationships
**What**: Define relationships between domain objects
```java
@Entity
class Notification {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL)
    private Set<NotificationDelivery> deliveries;
}

@Entity
class NotificationDelivery {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;
}
```

**Lazy Loading** (`FetchType.LAZY`):
- Don't load related objects automatically
- Reduces memory, faster queries
- But beware: LazyInitializationException if accessing outside transaction

**Cascade**:
- `CascadeType.ALL`: delete notification → delete all deliveries
- Avoids orphaned records

---

#### Repository Query Methods
**What**: Spring Data JPA generates queries from method names
```java
// Method name → SQL query
List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
// SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC

Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead, Pageable pageable);
// SELECT * FROM notifications WHERE user_id=? AND is_read=? ORDER BY created_at DESC
```

**Benefits**:
- No SQL to write
- Type-safe
- Automatic pagination

**When to Use @Query**:
- Complex queries (multiple joins)
- Performance-critical queries (need hints)
- Native SQL needed

---

### 5. Concurrency Patterns

#### ConcurrentHashMap
**What**: Thread-safe map for multi-threaded access
```java
private Map<Long, Set<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

// Thread-safe operations
emitterMap.put(userId, new CopyOnWriteArraySet<>());  // Add user
emitterMap.get(userId).add(emitter);                  // Add emitter
emitterMap.get(userId).remove(emitter);               // Remove emitter
```

**Why Used**:
- Multiple threads: queue worker + SSE service both access
- Lock granularity: locks individual bucket, not entire map
- Better performance than `synchronized Map`

**Alternative**:
- `synchronized Map`: coarse-grained lock (entire map locked)
- `HashMap` with manual synchronization: error-prone

---

#### Scheduled Executor Service (Removed in Phase 2)
**Old Code**:
```java
private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

executor.scheduleWithFixedDelay(this::processQueue, 0, 1, TimeUnit.SECONDS);
```

**Why Removed**:
- Manual thread management (error-prone)
- Lost on app restart
- Spring @Scheduled is simpler for MVP

**When to Use**:
- Custom thread pools needed
- Multiple parallel tasks
- Advanced scheduling (e.g., task priorities)

---

### 6. REST API Patterns

#### Pagination
**What**: Return results in pages to handle large datasets
```java
// Request
GET /api/notifications/me?page=0&size=20

// Response
{
  "items": [...],
  "page": 0,
  "size": 20,
  "totalItems": 150,
  "totalPages": 8,
  "hasNext": true
}
```

**Why Used**:
- Bandwidth: don't load all 1M notifications
- Performance: query only N rows
- Scaling: supports UI pagination (show page 1, 2, 3...)

**Implementation**:
```java
Page<Notification> page = repo.findByUserId(userId, 
    PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending())
);
```

---

#### Ownership Check
**What**: Ensure user can only access own data
```java
// WRONG: returns anyone's notifications
Notification n = repo.findById(id);

// CORRECT: returns only if owned by current user
Notification n = repo.findByIdAndUserId(id, currentUserId)
    .orElseThrow(() -> new NotFoundException(...));
```

**Why Used**:
- Security: prevent user A from reading user B's notifications
- Data isolation: multi-tenant safety

**Best Practice**:
- Check at repository layer (closer to data)
- Not just in controller (could be bypassed)

---

### 7. Testing Patterns

#### Integration Test
**What**: Test full flow (HTTP request → DB)
```java
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepository repo;
    
    @Test
    public void testCreateNotification() throws Exception {
        // Act: make HTTP request
        mockMvc.perform(post("/api/notifications/admin")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content(json)
        )
        // Assert: check response
        .andExpect(status().isOk());
        
        // Assert: check DB
        assertThat(repo.findAll()).hasSize(1);
    }
}
```

**Benefits**:
- Tests real behavior
- Catches integration issues (SQL errors, config issues)

**Trade-off**:
- Slower than unit tests (real DB, Spring boot)
- Should be fewer than unit tests

---

## 🛠️ Techniques & Best Practices

### 1. Error Handling

#### Fire-and-Forget with Try-Catch
**Pattern**:
```java
try {
    notificationService.createNotification(...);
} catch (Exception e) {
    logger.error("Failed to send notification", e);
    // Don't throw: let order creation complete anyway
}
```

**When to Use**:
- Best-effort operations (notification, not critical)
- Prevent cascade failures (order should succeed even if notification fails)

**Never Use**:
- For critical operations (payment, order creation)
- If caller needs to know about failure

---

#### Structured Logging
**Pattern**:
```java
logger.info("Notification {} delivered to user {} via {} at attempt {}",
    notificationId,
    userId,
    channel,
    attemptNumber);

logger.error("Notification {} delivery DROPPED after {} attempts",
    notificationId,
    maxAttempts);
```

**Benefits**:
- Structured: easy to grep, parse, aggregate
- Context: includes relevant IDs for debugging
- Levels: INFO (normal), WARN (recoverable), ERROR (critical)

---

### 2. Performance Optimization

#### Batch Operations
**Problem**: Broadcast to 100k users = 100k INSERT statements = slow
```java
// Slow
for (User user : allUsers) {
    notificationRepository.save(createNotification(user, req));  // 1 INSERT each
}

// Fast
List<Notification> batch = new ArrayList<>();
for (User user : allUsers) {
    batch.add(createNotification(user, req));
}
notificationRepository.saveAll(batch);  // 1 batch INSERT
```

**Trade-off**:
- Batch is faster but uses more memory
- Don't batch > 1000 items at once

---

#### Database Connection Pooling
**How Spring Handles It**:
- HikariCP by default: maintains pool of reusable connections
- No need to manually manage
- Config: `spring.datasource.hikari.maximum-pool-size=20`

**When Too Slow**:
- Increase pool size (but uses more resources)
- Or optimize queries (add indexes, reduce fetching)

---

### 3. Security Patterns

#### JWT Authentication
**Flow**:
```
User login → Server generates JWT token
User sends token in Authorization header
Server validates token → extract userId → check role
```

**In Notification Code**:
```java
@GetMapping("/me")
public NotificationListResponse getMyNotifications(
    @RequestAttribute("CURRENT_USER_ID") Long userId
) {
    // userId extracted by JwtAuthenticationFilter from JWT
    return notificationService.getMyNotifications(userId, ...);
}
```

**Security Consideration**:
- Token should expire (default 1 hour)
- Always use HTTPS (token could be stolen on HTTP)
- Never log token

---

#### Role-Based Access Control (RBAC)
**Pattern**:
```java
@PostMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // Only admins
public NotificationItemResponse createNotification(...) {
    return notificationService.createNotification(...);
}
```

**Alternatives**:
- `hasRole('ADMIN')`: exact match
- `hasAnyRole('ADMIN', 'MODERATOR')`: any of list
- `@PostAuthorize`: check after execution (less common)

---

## 📚 Knowledge Requirements by Role

### Backend Developer
- [ ] Spring Boot fundamentals (services, repositories, entities)
- [ ] JPA/Hibernate ORM
- [ ] SQL query optimization (indexes, explains plans)
- [ ] REST API design (HTTP methods, status codes)
- [ ] Transactional semantics (ACID, isolation levels)
- [ ] Concurrency (locks, atomicity)

**Reading**: Spring documentation, JPA guide, SQL tuning

---

### Frontend Developer
- [ ] EventSource API (SSE protocol)
- [ ] JavaScript async patterns (callbacks, promises, async/await)
- [ ] Error handling and retries
- [ ] UI state management (notifications toast/badge)
- [ ] Browser dev tools (Network tab, Console logs)

**Reading**: MDN EventSource docs, HTTP spec

---

### DevOps / SRE
- [ ] Database monitoring (query performance, connection pools)
- [ ] Application logs (structured logging, aggregation)
- [ ] Alerting (thresholds, on-call rotation)
- [ ] Capacity planning (queue depth, memory usage)
- [ ] Deployment strategies (blue-green, rolling updates)

**Reading**: SQL Server DMVs, Prometheus metrics, log parsing

---

### QA / Testing
- [ ] Integration test frameworks (Spring Boot Test)
- [ ] Load testing tools (Locust, JMeter, Apache Bench)
- [ ] Chaos engineering (kill process, simulate network failures)
- [ ] Database introspection (SELECT queries, explain plans)
- [ ] Bug reproduction (logs, database state, repro steps)

**Reading**: Spring Boot Test docs, load testing best practices

---

## 🎯 Key Takeaways

### Architectural Decisions
1. **DB-Backed Queue** over in-memory for resilience
   - Pro: survives restarts, auditable
   - Con: slightly higher latency

2. **Exponential Backoff** for retries
   - Pro: doesn't hammer failing service
   - Con: slower recovery if issue is temporary

3. **Fire-and-Forget** event handling
   - Pro: order creation returns quickly
   - Con: notification delivery not guaranteed

4. **Fan-out Broadcasting** for schema simplicity
   - Pro: simpler queries, better security
   - Con: more DB inserts for large broadcasts

### Technical Patterns
1. **Layered Architecture**: clear separation of concerns
2. **Event-Driven**: decouple components
3. **Scheduled Tasks**: simple polling for MVP
4. **Pagination**: handle large datasets
5. **Ownership Checks**: security at data layer

### Operational Practices
1. **Monitor**: queue depth, success rate, latency
2. **Alert**: on anomalies (>10k pending, <95% success)
3. **Log**: structured logs with context
4. **Investigate**: via DB queries (delivery audit trail)
5. **Tune**: indexes, batch sizes, connection pools

---

## 🚀 Further Learning

### Recommended Resources
1. **Spring Documentation**: https://spring.io/projects/spring-boot
2. **Hibernate Guide**: https://hibernate.org/orm/documentation/
3. **Database Design**: "Designing Data-Intensive Applications" by Martin Kleppmann
4. **Message Queues**: RabbitMQ, Kafka tutorials (for future distributed phase)
5. **Monitoring**: Prometheus, Grafana guides

### Practice Exercises
1. Add email channel to notification delivery
2. Implement idempotency key to prevent duplicates
3. Add distributed lock for multi-instance queue
4. Create Grafana dashboard for metrics
5. Implement heartbeat for SSE (keep-alive)

---

**Last Updated**: May 7, 2026  
**For Questions**: Review relevant documentation or contact platform team
