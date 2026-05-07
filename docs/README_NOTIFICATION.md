# Notification Real-time System - Complete Documentation

## 📚 Documentation Structure

This directory contains comprehensive documentation for the **Notification Real-time System** - a production-ready notification delivery platform with exponential backoff retry, SSE real-time push, and DB-backed queue for resilience.

### Quick Navigation

1. **[NOTIFICATION_REALTIME_ARCHITECTURE.md](./NOTIFICATION_REALTIME_ARCHITECTURE.md)** ← START HERE
   - System design overview
   - Architecture diagrams
   - Technical patterns & techniques
   - Data schema
   - Production readiness checklist

2. **[NOTIFICATION_IMPLEMENTATION_GUIDE.md](./NOTIFICATION_IMPLEMENTATION_GUIDE.md)** ← FOR DEVELOPERS
   - Quick start guide
   - Code examples
   - Integration points
   - Operational procedures
   - Troubleshooting guide

3. **[NOTIFICATION_FLOW_DIAGRAMS.md](./NOTIFICATION_FLOW_DIAGRAMS.md)** ← FOR VISUALIZATION
   - Mermaid flow diagrams
   - Sequence diagrams
   - Process flows
   - Monitoring dashboard mockup

4. **[STAGING_DEPLOYMENT_GUIDE.md](./STAGING_DEPLOYMENT_GUIDE.md)** ← FOR DEPLOYMENT
  - Step-by-step Docker Compose staging deployment
  - Local verification commands
  - Troubleshooting for 502 / DB connection issues

---

## 🎯 What's Been Implemented

### Phase 1 ✅ COMPLETE - MVP Foundation
- [x] Notification entity with multi-enum support (type, priority)
- [x] REST API (list, mark-read, unread-count, subscribe, admin-create)
- [x] SSE real-time delivery with multi-tab support
- [x] Basic retry queue with exponential backoff
- [x] OrderService event hook (auto-notify on order status change)
- [x] Security enforcement (JWT, role-based access)

**Status**: Builds successfully, tested locally

---

### Phase 2 ✅ COMPLETE - Production-Ready Queue
- [x] NotificationDelivery entity for delivery tracking
- [x] V7 migration (notification_delivery table)
- [x] DB-backed queue (replaces in-memory)
- [x] NotificationDeliveryRepository with optimized queries
- [x] Comprehensive logging & error tracking
- [x] Exponential backoff retry (2s, 4s, 8s, 16s, 32s)
- [x] Delivery audit trail for investigation

**Status**: Ready for staging deployment

**Benefits**:
- Survives app restarts
- Multi-instance ready (shared DB queue)
- Manual intervention tools (ops can retry, investigate)
- Full audit trail of all delivery attempts

---

### Phase 3 🔄 NEXT PRIORITY - Distributed Coordination
- [ ] Distributed lock for multi-instance (SELECT...FOR UPDATE in worker)
- [ ] Heartbeat/keepalive for SSE (HTTP comment every 30s)
- [ ] Health check endpoint for worker status
- [ ] Graceful shutdown (process remaining queue on shutdown)

**When**: Before multi-instance production deployment

**Effort**: 2-3 days

---

### Phase 4 📊 FUTURE - Observability
- [ ] Metrics: queue depth, retry rate, success rate, latency percentiles
- [ ] Structured logging with correlation IDs
- [ ] Grafana dashboard for ops
- [ ] Prometheus integration
- [ ] Alerting rules

**When**: 1 month after Phase 2 production

---

## 🏗️ System Architecture (Quick Overview)

```
OrderService (Business Event)
    ↓
NotificationService (Persist + Enqueue)
    ├─ Save to: notifications table (persistent)
    ├─ Enqueue: notification_delivery table (retry queue)
    │
NotificationDeliveryQueue (Background Worker - every 1 second)
    ├─ Query DB: SELECT * WHERE status='PENDING' AND next_retry_at <= NOW
    ├─ For each:
    │  ├─ Try send via SSE
    │  ├─ Success → mark SENT, done
    │  ├─ Fail → reschedule with backoff (2s, 4s, 8s, ...)
    │  ├─ After 5 attempts → mark DROPPED, log
    │
NotificationSseService (Real-time Push)
    ├─ Multi-tab emitter registry: Map<userId, Set<SseEmitter>>
    ├─ Send: emitter.send(SseEmitter.event())
    │
Browser (SSE EventSource)
    ├─ Listen: addEventListener('notification', ...)
    ├─ Update: UI (toast, badge, sound)
    
Fallback: GET /api/notifications/me (polling if SSE down)
```

---

## 📊 Data Model

### notifications table
```
id (PK) | user_id (FK) | type | title | message | payload_json | is_read | priority | created_at | read_at
```
**Purpose**: Application events (persistent, immutable source of truth)

### notification_delivery table
```
id (PK) | notification_id (FK) | channel | status | sent_at | last_error | attempt_count | next_retry_at | created_at | updated_at
```
**Purpose**: Delivery attempts tracking (DB-backed queue for resilience)

---

## 🔑 Key Concepts

### 1. At-Least-Once Delivery
- Notification might be sent 2x (not exactly-once)
- SSE is idempotent (same message twice = no harm)
- Client can deduplicate by notification ID if needed

### 2. Exponential Backoff
```
Attempt 1: T=0s        → if fail → retry in 2s
Attempt 2: T=2s        → if fail → retry in 4s (2+4)
Attempt 3: T=6s        → if fail → retry in 8s (6+8)
Attempt 4: T=14s       → if fail → retry in 16s
Attempt 5: T=30s       → if fail → give up (DROPPED)
Total time: ~62 seconds to confirm permanent failure
```

### 3. Fan-out Broadcasting
For broadcasts (userId=null), creates 1 notification record per user:
- Simpler queries ("get all notifications for user X")
- Better security model (no shared broadcasts)
- Trade-off: more DB inserts

### 4. Fire-and-Forget Event Handling
OrderService doesn't wait for notification to complete:
- Order update returns immediately
- Notification is async, best-effort
- Wrapped in try-catch to prevent cascade failure

---

## 🚀 How to Deploy

### Prerequisites
```bash
# Check Java version
java -version  # Should be 17+

# Check Maven
mvn --version  # Should be 3.8+

# Check DB connectivity
sqlcmd -S <server> -U <user> -P <password> -Q "SELECT 1"
```

### Deployment Steps

**1. Backup database**
```bash
# SQL Server backup
BACKUP DATABASE [BookStore] 
TO DISK = '/var/opt/mssql/backups/bookstore_pre_v7.bak'
```

**2. Deploy code**
```bash
# Build
cd BookStore
mvn clean package -DskipTests

# Copy WAR/JAR to server
scp target/bookstore-1.0.jar user@server:/opt/bookstore/
```

**3. Start application**
```bash
# Flyway automatically applies V7 migration on startup
systemctl restart bookstore-service

# Verify migration ran
sqlcmd -S server -U user -P pwd -Q "SELECT COUNT(*) FROM notification_delivery"
```

**4. Test**
```bash
# Check notification creation
curl -X POST http://localhost:8080/api/notifications/admin \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":null,"type":"SYSTEM_ANNOUNCEMENT","title":"Test","message":"Test"}'

# Should return quickly
# Check DB after 2 seconds:
SELECT COUNT(*) FROM notification_delivery WHERE status='SENT';
```

---

## 🧪 Testing Strategy

### Unit Tests
```bash
mvn test -Dtest=NotificationServiceTest
```

### Integration Tests
```bash
# Create notification, verify delivery
1. POST /api/notifications/admin (create for user 123)
2. Sleep 2 seconds (wait for queue worker)
3. SELECT * FROM notification_delivery WHERE status != 'DROPPED'
4. Assert: should have SENT records
```

### Load Test (Locust)
```python
# Send 100 notifications/sec for 5 minutes
for i in range(100):
    POST /api/notifications/admin

# Monitor:
# - Response time (should stay < 200ms)
# - Queue depth (should stay < 10000)
# - Success rate (should stay > 95%)
```

### Chaos Test
```bash
# Kill queue worker, verify:
1. Notifications still persist to DB
2. Delivery records stuck in PENDING
3. When worker restarts, retries resume

# Kill database, verify:
1. App logs errors (expected)
2. Queue worker doesn't crash
3. When DB recovers, retries resume
```

---

## 📈 Monitoring & Alerts

### Key Metrics (Daily Check)

```sql
-- Delivery success rate (should be > 95%)
SELECT 
  status, 
  COUNT(*) as count,
  CAST(100.0 * COUNT(*) / SUM(COUNT(*)) OVER () AS DECIMAL(5,2)) as pct
FROM notification_delivery
WHERE created_at >= DATEADD(day, -1, SYSUTCDATETIME())
GROUP BY status;

-- Average delivery latency (should be < 5 seconds)
SELECT AVG(DATEDIFF(ms, n.created_at, nd.sent_at)) as avg_latency_ms
FROM notifications n
JOIN notification_delivery nd ON n.id = nd.notification_id
WHERE nd.status='SENT' AND nd.sent_at >= DATEADD(hour, -1, SYSUTCDATETIME());

-- Stuck deliveries (should be 0)
SELECT COUNT(*) FROM notification_delivery
WHERE status='PENDING' AND updated_at < DATEADD(hour, -1, SYSUTCDATETIME());
```

### Alerts to Set Up
- ⚠️ Success rate < 95% for 5 minutes
- 🔴 Queue depth > 10,000 (incoming faster than outgoing)
- 🔴 Stuck deliveries found (worker might be crashed)
- 🔴 Avg latency > 30 seconds

---

## 🔧 Troubleshooting Quick Reference

| Symptom | Cause | Fix |
|---------|-------|-----|
| Notifications not arriving | User offline, SSE timed out | Check delivery_delivery status, manually retry |
| Queue depth growing | Worker crashed or overloaded | Restart app, check logs, increase BATCH_SIZE if needed |
| App crashes on startup | V7 migration failed | Check SQL Server error log, verify migration file |
| SSE closes after 30s | Nginx/LB timeout | Add `proxy_read_timeout 3600s` to nginx config |
| High memory usage | Too many SSE connections | Check emitter map size, close stale connections |

See **[NOTIFICATION_IMPLEMENTATION_GUIDE.md](./NOTIFICATION_IMPLEMENTATION_GUIDE.md#troubleshooting--maintenance)** for detailed troubleshooting.

---

## 📝 Code Change Summary

### New Files Created
```
src/main/java/com/example/bookstore/model/NotificationDelivery.java
src/main/java/com/example/bookstore/repository/NotificationDeliveryRepository.java
src/main/resources/db/migration/V7__create_notification_delivery_table.sql

docs/NOTIFICATION_REALTIME_ARCHITECTURE.md
docs/NOTIFICATION_IMPLEMENTATION_GUIDE.md
docs/NOTIFICATION_FLOW_DIAGRAMS.md
docs/README_NOTIFICATION.md (this file)
```

### Files Modified
```
src/main/java/com/example/bookstore/sse/NotificationDeliveryQueue.java
  - Replaced in-memory queue with DB-backed implementation
  - Added comprehensive comments explaining retry logic
  - Uses NotificationDeliveryRepository for persistence

src/main/java/com/example/bookstore/service/NotificationService.java
  - Updated createNotification() to use new queue signature
  - Added javadoc explaining the flow
  - Comments on broadcast fan-out design
```

---

## 🎓 Knowledge Required (For Team)

### For Developers
- Spring Boot fundamentals (@Service, @Repository, @Transactional)
- JPA/Hibernate entity mapping
- SSE protocol (Server-Sent Events)
- SQL indexing strategy
- Exponential backoff patterns
- Concurrent collections (ConcurrentHashMap)

### For DevOps/SRE
- Database monitoring (SQL Server DMVs)
- Log aggregation & analysis (grep, ELK, etc.)
- Alert configuration (Datadog, PagerDuty, etc.)
- App lifecycle (restart, graceful shutdown)
- Capacity planning (queue depth, memory, CPU)

### For QA/Testing
- Integration test patterns (Spring boot test, @SpringBootTest)
- Load testing tools (Locust, JMeter, LoadRunner)
- Chaos engineering (kill process, network failures)
- Database introspection (select, insert, update)

---

## 📞 Support & Escalation

### For Questions
1. Check relevant documentation file above
2. Search existing code comments
3. Review flow diagrams for visual understanding

### For Issues
1. Check troubleshooting guide
2. Gather logs: `grep NotificationDeliveryQueue logs/*.log`
3. Query database: `SELECT * FROM notification_delivery WHERE status='DROPPED'`
4. Review monitoring dashboard
5. Contact platform team with findings

### For Enhancements
1. Update [NOTIFICATION_IMPLEMENTATION_GUIDE.md](./NOTIFICATION_IMPLEMENTATION_GUIDE.md#roadmap) roadmap
2. Create feature branch: `git checkout -b feat/notification-*`
3. Reference architecture decisions in commit message
4. Update docs after deployment

---

## 📅 Maintenance Schedule

| Frequency | Task | Owner |
|-----------|------|-------|
| Daily | Check delivery success rate | SRE |
| Weekly | Archive old read notifications | DBA |
| Monthly | Review stuck delivery logs, test alerts | DevOps |
| Quarterly | Capacity planning, growth trend analysis | Engineering Lead |
| Annually | Security audit, dependency updates | Security Team |

---

## 🏆 Success Criteria

Your notification system is production-ready when:

- [x] **Persistence**: Notifications saved to DB, survive app restart
- [x] **Real-time**: SSE delivers messages within 5 seconds
- [x] **Resilience**: Exponential backoff retry, at-least-once guarantee
- [x] **Observability**: Queue metrics queryable, delivery audit trail
- [x] **Multi-tab**: User sees notification in all open browser tabs
- [x] **Security**: JWT auth, role-based access, ownership checks
- [x] **Testing**: Unit tests, integration tests, load tests pass
- [ ] **Monitoring**: Alerts configured, dashboard operational (Phase 3)
- [ ] **Multi-instance**: Distributed lock, graceful shutdown (Phase 3)

Current: ✅ 7/9 complete (78%)

---

## 📖 References

### External Resources
- **Spring SSE**: https://spring.io/guides/gs/sse-reactive-client/
- **Server-Sent Events Spec**: https://html.spec.whatwg.org/multipage/server-sent-events.html
- **Exponential Backoff**: https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
- **Database Indexing**: https://use-the-index-luke.com/

### Internal References
- Notification API: `GET /api/notifications/me`, `PATCH /api/notifications/me/{id}/read`, etc.
- Order Service Hook: `OrderService.updateSubOrderStatusForSeller()`
- Auth Config: `SecurityConfig.java` (OAuth2 + JWT)

---

## 📄 Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | May 7, 2026 | Initial documentation (Phase 1+2 complete) | Platform Team |

---

**Last Updated**: May 7, 2026  
**Status**: ✅ Production MVP Ready  
**Maintainer**: Platform Engineering  

For questions or updates, contact: [team email]
