# Schema Migration & Cleanup Guide

## Overview
This document explains how to cleanly reset the database schema from the old fragmented state (V1-V13 with piecemeal fixes) to the consolidated schema (V14+).

---

## Current State

### Problem
- `ddl-auto=update` was enabled, allowing Hibernate to auto-modify schema outside Flyway tracking
- V1 created initial schema with incorrect enum column types (VARCHAR instead of NVARCHAR)
- V4 & V5 were created to fix V1's mistakes (columns realignment)
- Result: Schema drift, migration litter, and ongoing sync issues

### Solution Implemented
1. **Disabled Hibernate DDL**: Changed `spring.jpa.hibernate.ddl-auto=validate` in `application.properties`
2. **Consolidated Migration**: Created `V14__reset_clean_consolidated_schema.sql` to:
   - Drop all existing tables (in correct dependency order)
   - Recreate complete schema with correct types from the start
   - Bake in all fields from V1-V13 upfront (no piecemeal ALTERs)
   - Add strategic indexes for query performance
   - Document design decisions and known constraints

---

## Steps to Migrate to Clean Schema

### Option 1: Fresh Start (Recommended)
If you can delete and recreate the database:

```sql
-- 1. In SQL Server Management Studio, delete old database
DROP DATABASE BookstoreDB;

-- 2. Create new empty database
CREATE DATABASE BookstoreDB;

-- 3. Spring Boot will auto-create schema_version table via Flyway
-- 4. Restart your Spring Boot application
--    → Flyway will run V1 through V14 sequentially
--    → Result: Clean, consolidated schema with all features
```

### Option 2: In-Place Migration (Keep Existing Data)
If you need to preserve data, this is more complex. Contact your DB team, as it requires:
- Exporting data from old tables
- Running V14 (drops and recreates)
- Re-importing data with schema mapping
- Validating referential integrity

---

## Migration Checklist

After running V14, verify the following:

### 1. Schema Validation
```sql
-- Check all critical tables exist
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'dbo' 
ORDER BY TABLE_NAME;
```

Expected tables:
- users, category, user_favorite_categories
- books, carts, cart_items
- orders_master, sub_orders, order_items
- seller_shops
- notifications, notification_delivery
- user_wishlist_books, user_addresses, user_security_events
- distributed_lock

### 2. Column Types Validation
```sql
-- Verify enum columns use NVARCHAR (not VARCHAR or other types)
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE COLUMN_NAME IN ('role', 'approval_status', 'status', 'priority')
ORDER BY TABLE_NAME, COLUMN_NAME;
```

Expected results:
| TABLE_NAME   | COLUMN_NAME         | DATA_TYPE | CHARACTER_MAXIMUM_LENGTH |
|--------------|-------------------|-----------|--------------------------|
| books        | approval_status   | nvarchar  | 20                       |
| notifications| priority          | nvarchar  | 20                       |
| sub_orders   | status            | nvarchar  | 30                       |
| users        | role              | nvarchar  | 20                       |

### 3. Index Validation
```sql
-- Verify performance indexes are in place
SELECT TABLE_NAME, INDEX_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE INDEX_NAME LIKE 'IX_%'
ORDER BY TABLE_NAME, INDEX_NAME;
```

Critical indexes to verify:
- `IX_books_approval_status`
- `IX_books_recommendation_filter` (on approval_status, category_id)
- `IX_order_items_order_book`
- `IX_sub_orders_completed`

### 4. Foreign Key Validation
```sql
-- Check all foreign keys are in place
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'dbo'
ORDER BY TABLE_NAME;
```

### 5. Application Validation (Java)
After migration, restart Spring Boot and verify:
```bash
# Check logs for Flyway migration success
# Search for: "Successfully completed 14 Flyway migrations"

# Log in to UI and verify:
# - Seller shop creation works (no schema errors)
# - Book listing shows approved books (filtered by approval_status)
# - Cart operations work
# - Orders complete without errors
```

---

## Design Decisions in V14

### 1. Enum Columns (NVARCHAR)
**Why NVARCHAR instead of VARCHAR?**
- Hibernate 6.x on SQL Server defaults to NVARCHAR for `@Enumerated(EnumType.STRING)`
- Prevents future V4-type migration nightmares

**Columns affected:**
- `users.role`: NVARCHAR(20), default 'BUYER'
- `books.approval_status`: NVARCHAR(20), default 'PENDING'
- `sub_orders.status`: NVARCHAR(30), default 'PENDING_PAYMENT'
- `notifications.priority`: NVARCHAR(20), default 'NORMAL'

### 2. Nullable Fields (Recommendation Robustness)
**Why these nullable?**
- `books.category_id`: Some books may not fit a category
  - **Impact**: Recommendation fallback logic must null-check before calling `getCategory().getId()`
  - **Fix**: Guard in `RecommendationService.getFallbackSameAuthorOrCategory()`

- `books.description`: Text similarity needs empty handling
  - **Impact**: CosineSimilarityAlgorithm handles blank strings
  - **Fix**: Already implemented with `.isBlank()` check

### 3. publish_year as NVARCHAR (Known Limitation)
**Current implementation:**
- Stored as NVARCHAR(50) to support flexible formats (e.g., "2023", "2023-Q1", "circa 2020")
- Query filters use string comparison (>=, <=) in BookRepository

**Known Issue:**
```sql
-- This query works but is fragile:
-- WHERE publish_year >= '2020' AND publish_year <= '2025'
-- Problem: String comparison breaks if data is inconsistent ("2020-Q1" vs "2020")
```

**Recommendations:**
1. **Short-term**: Add data validation layer to normalize publish_year format
2. **Medium-term**: Create migration to convert to INT with nullable migration_notes field
3. **Future**: Add `publish_date` (DATE) alongside `publish_year` for more flexibility

### 4. Indexes for Performance

**Recommendation Query Indexes:**
- `IX_books_recommendation_filter`: (approval_status, category_id) filtered on APPROVED only
- `IX_order_items_order_book`: (sub_order_id, book_id) for FP-Growth itemset mining
- `IX_sub_orders_completed`: (order_id, status) filtered on DELIVERED or CONFIRMED orders

**Query Pattern Impact:**
- Recommendation service now has indexed data source
- Order-to-basket aggregation benefits from covering index

---

## After Migration: Next Steps

### 1. Update Development Documentation
- [ ] Remove references to "V4/V5 fixes schema" from team docs
- [ ] Update schema documentation to point to V14 as source of truth

### 2. Code Cleanup
- [ ] Verify `RecommendationService` null-safety for category-less books
- [ ] Add test for null category in recommendation fallback
- [ ] Consider adding data validation for publish_year in book service

### 3. Future Migration Policy
- [x] `ddl-auto=update` is now disabled for shared environments
- [ ] All schema changes go through Flyway migrations
- [ ] Dev environments can keep `ddl-auto=validate` or enable locally if needed
- [ ] Code review checklist: "Does this change require a new migration?"

### 4. Testing
- [ ] Regression test: All existing queries still work
- [ ] Recommendation queries: FP-Growth mining still produces results
- [ ] Notification delivery: All channels still functional
- [ ] Order processing: Orders with multiple sellers (sub_orders) still work

---

## Rollback (If Needed)

If V14 fails in production (unlikely, as it's idempotent):

```sql
-- Option 1: Flyway will track that V14 failed; fix and run again
-- Option 2: If data was lost, restore from backup, then run V14

-- To manually verify Flyway state:
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## Common Issues & Troubleshooting

### Issue 1: "Can't create table; foreign key constraint fails"
**Cause**: Table creation order violated FK constraints  
**Fix**: V14 drops tables in reverse dependency order, then creates in correct order  
**Verify**: All parent tables created before child tables

### Issue 2: "Enum value validation failed"
**Cause**: Existing enum values don't match Java @Enum definitions  
**Fix**: V14 uses default constraints; ensure application matches  
**Verify**: Check `approval_status` has only PENDING, APPROVED, REJECTED

### Issue 3: "Performance: Queries still slow"
**Cause**: Indexes not being used  
**Fix**: Check index creation succeeded; verify query plans  
**Command**: 
```sql
SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('books');
```

### Issue 4: "Connection timeout during migration"
**Cause**: V14 lock escalation on large table drops  
**Fix**: Increase connection timeout or run during maintenance window  
**Setting**: In `application.properties`:
```properties
spring.datasource.hikari.connection-timeout=60000
```

---

## Questions or Issues?

If something goes wrong:
1. Check `target/logs/` for Spring Boot migration logs
2. Run schema validation SQL queries above
3. Compare current schema with V14 migration intent
4. Check Flyway metadata table: `SELECT * FROM flyway_schema_history;`

---

**Last Updated**: 2026-05-15  
**Migration Version**: V14  
**Status**: Ready for production ✅
