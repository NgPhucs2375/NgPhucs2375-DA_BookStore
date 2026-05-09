# Notification System - Flow Diagrams

## 1. Notification Creation & Delivery Workflow

```mermaid
sequenceDiagram
    participant Seller as Seller<br/>(Browser)
    participant API as OrderService<br/>(Spring)
    participant Service as NotificationService<br/>(Spring)
    participant DB as Database<br/>(SQL Server)
    participant Queue as NotificationDeliveryQueue<br/>(Worker)
    participant SSE as NotificationSseService<br/>(Emitter)
    participant Buyer as Buyer<br/>(Browser)
    
    Seller->>API: PATCH /api/orders/sub-orders/{id}/status
    activate API
    API->>DB: Save sub-order status
    API->>DB: Commit transaction
    
    API->>Service: createNotification(buyerId, request)
    deactivate API
    
    activate Service
    Service->>DB: INSERT INTO notifications (user_id, type, title, ...)
    Service->>DB: INSERT INTO notification_delivery (status='PENDING', next_retry_at=NOW)
    Service->>Service: return NotificationItemResponse
    deactivate Service
    
    Note over Queue: Background Worker<br/>wakes up every 1 second
    
    Queue->>DB: SELECT * FROM notification_delivery<br/>WHERE status='PENDING' AND next_retry_at <= NOW
    activate Queue
    Queue->>SSE: sendEventToUser(buyerId, "notification", data)
    
    activate SSE
    SSE->>SSE: Get emitter for buyerId
    SSE->>Buyer: emitter.send(SseEmitter.event())
    deactivate SSE
    
    Buyer->>Buyer: EventSource listener fires
    Buyer->>Buyer: Update UI (show toast, update badge)
    
    Queue->>DB: UPDATE notification_delivery SET status='SENT', sent_at=NOW
    deactivate Queue
```

---

## 2. Retry Logic & Exponential Backoff

```mermaid
flowchart TD
    A["DeliveryTask Status<br/>status=PENDING<br/>next_retry_at <= NOW"] -->|Queue Worker<br/>polls every 1s| B["Attempt Send via SSE"]
    
    B -->|Success| C["✅ Mark SENT<br/>status='SENT'<br/>sent_at=NOW<br/>next_retry_at=NULL"]
    
    B -->|Fail<br/>attempt < 5| D["Calculate Backoff<br/>attempts++"]
    D --> E["Backoff Schedule<br/>Attempt 1: 2s<br/>Attempt 2: 4s<br/>Attempt 3: 8s<br/>Attempt 4: 16s<br/>Attempt 5: 32s"]
    E --> F["Reschedule<br/>next_retry_at = NOW + backoff<br/>status='PENDING'<br/>lastError=exception"]
    F --> G["Save to DB<br/>Wait for next poll"]
    G -->|1s later| B
    
    B -->|Fail<br/>attempt >= 5| H["❌ Give Up<br/>status='DROPPED'<br/>attempt_count=5<br/>next_retry_at=NULL"]
    
    C --> I["Terminal State<br/>No more processing"]
    H --> I
    
    style A fill:#fff3cd
    style C fill:#d4edda
    style H fill:#f8d7da
    style I fill:#e2e3e5
```

---

## 3. SSE Multi-Tab Connection Management

```mermaid
graph TD
    A["Notification Event<br/>sseService.sendEventToUser<br/>buyerId=123"]
    
    A --> B["Get Set of Emitters<br/>Map.get(123)"]
    
    B --> C["Multiple Tabs/Windows<br/>of Same User"]
    
    C --> D1["Tab 1<br/>Emitter 1<br/>Connected"]
    C --> D2["Tab 2<br/>Emitter 2<br/>Connected"]
    C --> D3["Tab 3<br/>Emitter 3<br/>TIMEOUT"]
    
    D1 --> E["emitter.send<br/>✅ Success"]
    D2 --> F["emitter.send<br/>✅ Success"]
    D3 --> G["IOException<br/>⚠️ Removed from Set"]
    
    E --> H["EventSource<br/>listener fires"]
    F --> H
    
    H --> I["Multiple UIs Update<br/>Synchronized"]
    
    style A fill:#e3f2fd
    style I fill:#d4edda
    style G fill:#ffe0b2
```

---

## 4. DB-Backed Queue Architecture

```mermaid
graph LR
    A["notification_delivery<br/>Table (DB)"]
    
    A -->|Every 1 second| B["Queue Worker<br/>@Scheduled<br/>fixedDelay=1000ms"]
    
    B -->|Query| C["findPendingRetries<br/>status='PENDING'<br/>next_retry_at <= NOW<br/>LIMIT 100"]
    
    C --> D{For Each Task}
    
    D -->|Try Send| E["SSE Delivery"]
    E -->|Success| F["UPDATE<br/>status='SENT'<br/>sent_at=NOW"]
    E -->|Failure| G["Check Attempts<br/>< MAX=5?"]
    
    G -->|Yes| H["Increment Counter<br/>Calculate Backoff<br/>next_retry_at=NOW+backoff"]
    G -->|No| I["UPDATE<br/>status='DROPPED'"]
    
    H --> J["UPDATE DB<br/>Keep PENDING"]
    J --> K["Next cycle<br/>will retry"]
    
    F --> L["✅ Terminal State"]
    I --> L
    
    style A fill:#fff3cd
    style B fill:#e3f2fd
    style L fill:#d4edda
    style K fill:#e8daef
```

---

## 5. Fan-out Broadcast Flow

```mermaid
flowchart TD
    A["Admin: POST /api/notifications/admin<br/>userId=null<br/>broadcast=true"]
    
    A -->|NotificationService| B["For each user in database"]
    
    B --> C["Create Notification<br/>user_id=user.id<br/>type, title, message"]
    
    C --> D["Save to<br/>notifications table"]
    
    D --> E["Enqueue Delivery<br/>notification_delivery record<br/>status=PENDING"]
    
    E --> F["N Delivery Tasks<br/>in Queue"]
    
    F -->|Queue Worker| G["Process Each Task<br/>in batches of 100"]
    
    G -->|Attempt 1| H1["Send to User 1"]
    G -->|Attempt 2| H2["Send to User 2"]
    G -->|Attempt 3| H3["...send to User N"]
    
    H1 -->|Success| I["✅ User 1 sees on Tab 1<br/>✅ User 1 sees on Tab 2"]
    H2 -->|Fail| J["⚠️ Retry in 2s"]
    
    style A fill:#cfe9ff
    style F fill:#fff3cd
    style I fill:#d4edda
    style J fill:#ffe0b2
```

---

## 6. Error Handling & Recovery

```mermaid
graph TD
    A["Delivery Task<br/>PENDING"]
    
    A -->|Attempt| B{SSE Send<br/>Result?}
    
    B -->|Exception<br/>User Offline| C["IOException"]
    B -->|Connection<br/>Closed| D["SSE Emitter<br/>Closed"]
    B -->|Network<br/>Error| E["TimeoutException"]
    B -->|Success| F["✅ SENT"]
    
    C --> G["attemptCount++<br/>lastError=message"]
    D --> G
    E --> G
    
    G --> H{Retry?<br/>attemptCount<br/>< 5?}
    
    H -->|Yes| I["Calculate Backoff<br/>next_retry_at=<br/>NOW + exponential"]
    H -->|No| J["❌ DROPPED<br/>Abandoned"]
    
    I --> K["UPDATE DB<br/>status=PENDING<br/>nextRetryAt=future"]
    
    K --> L["Wait 1 second"]
    L -->|Next Cycle| A
    
    J --> M["Terminal State<br/>Logged for review"]
    F --> M
    
    style F fill:#d4edda
    style J fill:#f8d7da
    style M fill:#e2e3e5
```

---

## 7. SSE Connection Lifecycle

```mermaid
sequenceDiagram
    participant Client as Browser<br/>JavaScript
    participant Spring as Spring<br/>SSE Controller
    participant Queue as Queue<br/>Worker
    participant Repo as NotificationRepository
    
    Client->>Spring: GET /api/notifications/me/subscribe
    activate Spring
    Spring->>Spring: Create SseEmitter
    Spring->>Spring: Register in Map<userId, Set<Emitter>>
    
    Spring-->>Client: Start SSE Stream<br/>(keep-alive)
    deactivate Spring
    
    Note over Client,Spring: Connection stays open<br/>Ready to receive events
    
    Queue->>Repo: Query pending deliveries
    Queue->>Spring: sendEventToUser(userId, event)
    
    activate Spring
    Spring->>Spring: Get emitter from Map
    Spring->>Spring: emitter.send(event)
    Spring-->>Client: 📨 Event Data
    deactivate Spring
    
    Client->>Client: EventSource listener<br/>fires 'notification' event
    
    Note over Client: UI Updates<br/>Show toast, badge, sound
    
    Client->>Client: Connection stays alive<br/>Waiting for more events
    
    Client->>Client: ⚠️ Connection timeout<br/>after 30 min (nginx config)
    
    Client->>Client: JavaScript<br/>eventSource.close()
    
    Spring->>Spring: onCompletion() triggered<br/>Remove from Map
    
    Note over Client,Spring: Connection closed<br/>Map.remove(userId)
```

---

## 8. Monitoring & Operations Dashboard

```mermaid
graph TD
    A["Notification Operations<br/>Dashboard"]
    
    A -->|Query 1| B["Last 1 Hour<br/>Success Rate"]
    B --> B1["SELECT COUNT(*)<br/>WHERE status IN<br/>SENT/FAILED/DROPPED"]
    B1 --> C["Display %<br/>🟢 Healthy>95%<br/>🟡 Degraded 90-95%<br/>🔴 Down<90%"]
    
    A -->|Query 2| D["Queue Depth"]
    D --> D1["SELECT COUNT(*)<br/>WHERE status=PENDING<br/>AND next_retry_at<=NOW"]
    D1 --> E["Alert if > 10,000<br/>📈 Indicates backlog"]
    
    A -->|Query 3| F["Stuck Deliveries"]
    F --> F1["SELECT *<br/>WHERE updated_at<br/>< NOW-30min<br/>AND status=PENDING"]
    F1 --> G["Alert if found<br/>⚠️ Worker may crash"]
    
    A -->|Query 4| H["Failed Deliveries<br/>Last 24H"]
    H --> H1["SELECT * WHERE<br/>status=FAILED<br/>OR status=DROPPED"]
    H1 --> I["Manual Investigation<br/>Last error messages"]
    
    A -->|Action| J["Retry Failed<br/>Deliveries"]
    J --> J1["UPDATE SET<br/>status=PENDING<br/>attemptCount=0"]
    
    style C fill:#d4edda
    style E fill:#fff3cd
    style G fill:#f8d7da
```

---

## 9. Performance Characteristics

```mermaid
graph LR
    A["Performance Metrics"]
    
    A --> B["Latency"]
    B --> B1["Event Created → DB: <100ms"]
    B --> B2["DB → SSE Delivery: 1-5 seconds<br/>depends on queue worker cycle"]
    B --> B3["SSE → Browser: <100ms<br/>network dependent"]
    
    A --> C["Throughput"]
    C --> C1["Single Worker: ~1000 msg/sec<br/>batch 100, poll every 1 sec"]
    C --> C2["Multi-Instance: N*1000 msg/sec<br/>with distributed lock"]
    
    A --> D["Storage"]
    D --> D1["notifications table<br/>grows ~1000/day (typical)"]
    D --> D2["notification_delivery table<br/>auto-cleanup every 90 days"]
    
    A --> E["Memory"]
    E --> E1["In-memory emitter map<br/>~1KB per open connection<br/>100K users = 100MB"]
    
    style B1 fill:#d4edda
    style C1 fill:#fff3cd
    style D2 fill:#e8daef
```

---

## 10. Deployment Checklist

```mermaid
graph TD
    A["Pre-Deployment<br/>Checklist"]
    
    A --> B["Database"]
    B --> B1["✅ V7 migration created<br/>notification_delivery table"]
    B --> B2["✅ Indexes created<br/>IX_notification_delivery_pending_retry<br/>IX_notification_delivery_by_notification"]
    
    A --> C["Application"]
    C --> C1["✅ NotificationDelivery entity<br/>NotificationDeliveryRepository"]
    C --> C2["✅ Queue worker refactored<br/>reads from DB, not in-memory"]
    C --> C3["✅ Tests pass<br/>mvn compile"]
    
    A --> D["Configuration"]
    D --> D1["✅ SecurityConfig updated<br/>auth rules for /api/notifications/**"]
    D --> D2["✅ @Scheduled enabled<br/>@EnableScheduling in config"]
    
    A --> E["Monitoring"]
    E --> E1["✅ Alerts configured<br/>success rate, queue depth"]
    E --> E2["✅ Dashboard created<br/>health checks, failed deliveries"]
    
    A --> F["Testing"]
    F --> F1["✅ Integration test<br/>create notification → verify delivery"]
    F --> F2["✅ Load test<br/>100 concurrent users"]
    F --> F3["✅ Failover test<br/>kill worker, verify recovery"]
    
    style B1 fill:#d4edda
    style C3 fill:#d4edda
    style F3 fill:#d4edda
```

---

## Legend & Symbols

| Symbol | Meaning |
|--------|---------|
| ✅ | Success, Task completed |
| ❌ | Failed, Dropped, Terminal failure |
| ⚠️ | Warning, Potential issue |
| 📨 | Message/Event transmitted |
| 🟢 | Healthy |
| 🟡 | Degraded |
| 🔴 | Critical/Down |

---

**Usage**: 
- Use these diagrams in documentation, wiki, or training materials
- Update periodically as architecture evolves
- Reference specific diagrams when discussing issues
