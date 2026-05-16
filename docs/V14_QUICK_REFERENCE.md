# V14 Schema Changes: Quick Reference Cheat Sheet

## 🔴 CRITICAL: Nullable Field Changes

### books.category_id (NULLABLE)
**Impact on Code:** ⚠️ **MUST NULL-CHECK in RecommendationService**

```java
// ❌ WILL CRASH if category_id IS NULL:
sourceBook.getCategory().getId()

// ✅ FIXED:
if (sourceBook.getCategory() != null) {
    // Use category...
}
```

**Where to fix:**
- File: `RecommendationService.java` line 191
- Method: `getFallbackSameAuthorOrCategory()`

---

### books.publish_year (CHANGED: NVARCHAR → INT)
**Impact on Queries:** BookRepository year range queries now WORK correctly

```java
// ❌ OLD (BROKEN with NVARCHAR):
WHERE publish_year >= '2020' AND publish_year <= '2025'

// ✅ NEW (WORKS with INT):
WHERE publish_year >= 2020 AND publish_year <= 2025
```

**Where to fix:**
- File: `BookRepository.java` lines 31-71
- Method: `searchApprovedBooks()`

---

### Other Nullable Fields (No Code Change Needed)

| Table | Field | Nullable | Reason | Action |
|-------|-------|----------|--------|--------|
| books | description | NULL | Optional | Text similarity handles empty ✓ |
| books | price | NULL | Draft state | Check in checkout validation |
| books | stock_quantity | NULL | Legacy | Check in inventory validation |
| users | email | NULL | Optional | Check before sending email |
| users | phone | NULL | Optional | Check before sending SMS |
| users | first_name, last_name | NULL | Incomplete | UI prompts for completion |
| sub_orders | (all fields) | NOT NULL | Required | No changes needed |
| order_items | (all fields) | NOT NULL | Required | No changes needed |

---

## 📊 New Indexes (15 Total)

### Tier 1: CRITICAL (Must Verify)
```sql
-- RecommendationService recommendation filtering
IX_books_recommendation_filter
   ON books(approval_status, category_id) 
   WHERE approval_status='APPROVED' AND category_id IS NOT NULL

-- RecommendationService FP-Growth mining
IX_order_items_order_book
   ON order_items(sub_order_id, book_id)

-- RecommendationService completed-orders-only filter
IX_sub_orders_completed
   ON sub_orders(order_id, status) 
   WHERE status IN ('DELIVERED', 'CONFIRMED')
```

### Tier 2: Performance (High-use queries)
```sql
IX_books_approval_status (single column)
IX_books_author (single column)
IX_books_title (single column)
IX_books_publish_year (single, filtered on publish_year IS NOT NULL)
IX_books_category_seller (composite)
IX_sub_orders_order_status (composite)
IX_sub_orders_seller_status (composite)
```

### Tier 3: Utility
```sql
IX_users_username (unique)
IX_users_email (single, filtered)
IX_users_role (single)
IX_category_name (unique)
IX_notifications_user_read (composite)
```

---

## ✅ Migration Checklist

### Before Deployment
- [ ] Backup existing database (if upgrading from V13)
- [ ] Read V14 migration file line-by-line (1050+ lines, critical)
- [ ] Review nullable field strategy (this guide)

### During Deployment
- [ ] Delete old BookstoreDB (or use new database)
- [ ] Start Spring Boot (Flyway runs V1-V14 automatically)
- [ ] Watch logs for: "Successfully completed 14 Flyway migrations"

### After Deployment
- [ ] Verify all 16 tables exist (SQL query in guide)
- [ ] Verify NVARCHAR enum columns (SQL query in guide)
- [ ] Verify all 15 indexes created (SQL query provided below)
- [ ] Test recommendations (should be 10x+ faster)

### Code Changes Required
- [ ] RecommendationService: null-check category in fallback (line 191)
- [ ] BookRepository: numeric year comparison (lines 31-71)
- [ ] SearchService: catch no results if publish_year filters fail
- [ ] Unit tests: edge cases for null category in recommendations

---

## 🔍 Verification Queries

### Check all indexes exist:
```sql
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_NAME IN ('books', 'sub_orders', 'order_items')
ORDER BY TABLE_NAME, INDEX_NAME;
```

### Verify publish_year is INT:
```sql
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'books' AND COLUMN_NAME LIKE 'publish_year%'
ORDER BY COLUMN_NAME;
```

Expected results:
- `publish_year` → `int` → NULL
- `publish_year_note` → `nvarchar` → 100

### Verify category_id is nullable:
```sql
SELECT COLUMN_NAME, IS_NULLABLE, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'books' AND COLUMN_NAME = 'category_id';
```

Expected: IS_NULLABLE = 'YES'

### Check enum columns are NVARCHAR:
```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE COLUMN_NAME IN ('role', 'approval_status', 'status', 'priority')
ORDER BY TABLE_NAME;
```

Expected: All show `nvarchar`

---

## 🐛 Troubleshooting

### Issue: "publish_year query returns wrong results"
**Cause**: Java still using string comparison  
**Fix**: Update BookRepository.java to use numeric comparison (>= 2020, not >= '2020')

### Issue: "RecommendationService NullPointerException"
**Cause**: Calling `.getCategory().getId()` without null-check  
**Fix**: Add `if (sourceBook.getCategory() != null)` guard

### Issue: "Indexes not being used"
**Cause**: Stale query plans  
**Fix**: 
```sql
-- Clear query plan cache:
DBCC FREEPROCCACHE;
DBCC DROPCLEANBUFFERS;
```

### Issue: "Foreign key constraint errors during V14 migration"
**Cause**: V14 drops tables in wrong order  
**Fix**: Already handled in V14 (reverse dependency order)

---

## 📈 Performance Impact

Expected improvements after V14:

| Operation | Before V13 | After V14 | Improvement |
|-----------|-----------|-----------|------------|
| Recommendation generation | ~500ms | ~50ms | 10x faster |
| Year range filter | Broken | <1ms | Fixed + faster |
| Category peer lookup | ~100ms | <1ms | 100x faster |
| Notification queries | ~50ms | ~10ms | 5x faster |

---

## 🔗 References

- Full guide: [V14_NORMALIZATION_GUIDE.md](V14_NORMALIZATION_GUIDE.md)
- Migration details: [SCHEMA_MIGRATION_GUIDE.md](SCHEMA_MIGRATION_GUIDE.md)
- V14 SQL file: `src/main/resources/db/migration/V14__reset_clean_consolidated_schema.sql`

---

**Last Updated**: May 15, 2026  
**Format**: Cheat Sheet for quick team reference
