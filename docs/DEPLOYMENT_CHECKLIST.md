# 📋 Staging Deployment Checklist

**Objective**: Deploy BookStore to Docker Compose staging in ~8-10 minutes  
**Target**: Anyone on the team can follow this and reproduce the deployment

---

## **PRE-DEPLOYMENT**

- [ ] Docker Desktop is running (Windows/Mac) or Docker daemon (Linux)
- [ ] Git repository cloned to: `d:\Univer\Nam_3\HKII\CNVAR\DA_BookStore\BookStore\NgPhucs2375-DA_BookStore`
- [ ] Terminal/CMD is open in the repository root

---

## **PHASE 1: BUILD** (~2-3 minutes)

**Step 1.1** – Build JAR artifact
```bash
mvnw.cmd -q -DskipTests clean package
```
- [ ] Command completes without errors
- [ ] Verify artifact: `dir /b target\*.jar` → shows `BookStore-0.0.1-SNAPSHOT.jar`

**Step 1.2** – Validate Docker Compose config
```bash
docker compose config > nul 2>&1 && echo "✅ Valid" || echo "❌ Error"
```
- [ ] Output: `✅ Valid`

---

## **PHASE 2: INFRASTRUCTURE** (~1-2 minutes)

**Step 2.1** – Clean slate (stop and remove existing containers)
```bash
docker compose down --volumes
```
- [ ] All containers stopped and removed

**Step 2.2** – Start the stack
```bash
docker compose up -d --force-recreate
```
- [ ] Command completes

**Step 2.3** – Wait for SQL Server to be healthy
```bash
timeout /t 30
```
- [ ] Wait 30 seconds (SQL Server healthcheck takes 20-40s)

**Step 2.4** – Verify all containers running
```bash
docker compose ps
```
- [ ] All 5 containers show status `Up`:
  - `bookom-app-1`
  - `bookom-app-2`
  - `bookom-app-3`
  - `bookom-mssql`
  - `bookom-nginx-lb`

---

## **PHASE 3: DATABASE SETUP** (~15-20 seconds)

**Step 3.1** – Create BookstoreDB
```bash
docker exec bookom-mssql /opt/mssql-tools18/bin/sqlcmd ^
  -S localhost -U sa -P "BookomStaging!123" -C -h -1 -W ^
  -Q "IF DB_ID('BookstoreDB') IS NULL CREATE DATABASE BookstoreDB; SELECT name FROM sys.databases WHERE name='BookstoreDB';"
```
- [ ] Output: `BookstoreDB` (confirmation that database exists)

---

## **PHASE 4: APPLICATION STARTUP** (~2-3 minutes)

**Step 4.1** – Restart app replicas (to connect to database)
```bash
docker compose up -d bookom-app-1 bookom-app-2 bookom-app-3
```
- [ ] Command completes

**Step 4.2** – Wait for Tomcat to start
```bash
timeout /t 10
```
- [ ] Wait 10 seconds

**Step 4.3** – Verify app started successfully
```bash
docker compose logs --tail=20 bookom-app-1 | findstr "Started BookStoreApplication"
```
- [ ] Output contains: `Started BookStoreApplication in XX.XXX seconds`

---

## **PHASE 5: VERIFICATION** (~1-2 minutes)

**Step 5.1** – All containers healthy
```bash
docker compose ps
```
- [ ] All 5 containers: `Up` status

**Step 5.2** – Test HTTP endpoint
```bash
curl -I http://localhost
```
- [ ] Response: `HTTP/1.1 200 OK` (or similar 2xx status)
- [ ] Headers visible (no timeouts or connection refused)

**Step 5.3** – Verify notification queue worker
```bash
docker compose logs --tail=30 bookom-app-1 | findstr "notification_delivery"
```
- [ ] Output shows Hibernate SELECT queries to `notification_delivery` table
- [ ] Confirms queue worker is polling

**Step 5.4** – Verify database migrations applied
```bash
docker exec bookom-mssql /opt/mssql-tools18/bin/sqlcmd ^
  -S localhost -U sa -P "BookomStaging!123" -C ^
  -Q "SELECT COUNT(*) FROM BookstoreDB.sys.tables"
```
- [ ] Output: A number > 0 (Flyway migrations successfully created tables)

---

## **✅ DEPLOYMENT COMPLETE**

All checks passed? Your staging environment is ready!

| Component | Status |
|-----------|--------|
| Docker Stack | ✅ Running |
| SQL Server Database | ✅ Created & Initialized |
| Spring Boot Replicas | ✅ Started |
| Nginx Load Balancer | ✅ Routing Traffic |
| Notification Queue | ✅ Polling |
| HTTP Endpoint | ✅ Responding (200) |

---

## **🔧 TROUBLESHOOTING**

| Problem | Solution |
|---------|----------|
| `docker compose ps` shows nothing | Ensure Docker is running; check `docker --version` |
| SQL Server still "starting" after 40s | Normal on slow machines; wait up to 60s total |
| App containers "Exited (1)" | Database not created yet; run Phase 3 database creation |
| `HTTP 502 Bad Gateway` | Apps still starting (JPA + Flyway take 1-2 min); wait and retry |
| `HTTP 502` persists after 3 min | Check logs: `docker compose logs --tail=100 bookom-app-1` |

---

## **📋 QUICK REFERENCE: COMMON COMMANDS**

```bash
# View app logs
docker compose logs --tail=50 bookom-app-1

# View load balancer logs
docker compose logs --tail=50 bookom-nginx-lb

# View database server logs
docker compose logs --tail=50 bookom-mssql

# Stop all containers
docker compose down

# Stop and remove volumes (clean slate)
docker compose down --volumes

# Query notification queue status
docker exec bookom-mssql /opt/mssql-tools18/bin/sqlcmd ^
  -S localhost -U sa -P "BookomStaging!123" -C ^
  -Q "SELECT status, COUNT(*) AS count FROM BookstoreDB.dbo.notification_delivery GROUP BY status"

# Watch logs in real-time
docker compose logs -f bookom-app-1

# Check container resource usage
docker stats

# List all databases
docker exec bookom-mssql /opt/mssql-tools18/bin/sqlcmd ^
  -S localhost -U sa -P "BookomStaging!123" -C ^
  -Q "SELECT name FROM sys.databases"
```

---

## **⏱️ TIME BREAKDOWN**

| Phase | Duration | Notes |
|-------|----------|-------|
| Build | 2-3 min | Compiling Java code |
| Infrastructure | 1-2 min | Pulling images + starting containers |
| Database | 15-20 sec | Creating schema |
| Application | 2-3 min | Spring Boot startup + Flyway migrations |
| Verification | 1-2 min | Testing endpoints |
| **TOTAL** | **~8-10 min** | From clean state to production-ready |

---

## **📝 NOTES FOR TEAM**

1. **First Time?** Run all phases in order (1-5)
2. **Redeploying?** You can skip Phase 1 if JAR is already built; start from Phase 2
3. **Database already exists?** Skip the database creation in Step 3.1
4. **Need to debug?** Enable verbose logging:
   ```bash
   docker compose logs -f bookom-app-1 | findstr "ERROR\|WARN"
   ```
5. **Want to check notifications in DB?**
   ```bash
   docker exec bookom-mssql /opt/mssql-tools18/bin/sqlcmd ^
     -S localhost -U sa -P "BookomStaging!123" -C ^
     -Q "SELECT TOP 10 id, status, created_at FROM BookstoreDB.dbo.notification_delivery ORDER BY created_at DESC"
   ```

---

**Last Updated**: May 7, 2026  
**Status**: ✅ Verified & Production-Ready  
**See Also**: [STAGING_DEPLOYMENT_GUIDE.md](STAGING_DEPLOYMENT_GUIDE.md), [README_NOTIFICATION.md](README_NOTIFICATION.md)
