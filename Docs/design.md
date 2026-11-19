# PayTrack - Technical Design Document (Phase 1)

| | |
|---|---|
| **Document ID** | LV-JAVA-PAYTRACK-P1-DESIGN |
| **Version** | 1.0 |
| **Issue date** | 15 Nov 2026 |
| **Author** | Ultra Tech |
| **Audience** | Mentor, reviewers |
| **Related** | PayTrack PRD Phase 1 (LV-JAVA-PAYTRACK-P1) |

---

## 1. Architecture

PayTrack is a **multi-tenant SaaS platform** for wholesale distributors. Each distributor is a tenant. Data is isolated by `tenant_id` at the database level, enforced by Spring Security + JWT at the application level.

### 1.1 Layered view

```
+-----------------------------------------------------------------+
|                        HTTP / JSON (REST)                        |
+-----------------------------------------------------------------+
|  JwtAuthenticationFilter  ->  extracts tenantId from Bearer token|
|  SecurityConfig           ->  @PreAuthorize role checks          |
+-----------------------------------------------------------------+
|  Controllers (thin)  ->  validation, HTTP semantics              |
+-----------------------------------------------------------------+
|  Services (transactional)  ->  business logic                    |
|    - all queries filtered by TenantContext.get()                 |
+-----------------------------------------------------------------+
|  Repositories (Spring Data JPA)                                  |
+-----------------------------------------------------------------+
|  PostgreSQL 16   +   Flyway migrations                           |
+-----------------------------------------------------------------+
```

### 1.2 Tenant isolation - chosen strategy

**Approach: discriminator column (`tenant_id` on every business table).**

Why this approach:
- The PRD (page 1, "Technical Design") explicitly says: *"Every business entity such as invoices, customers and payments is associated with a tenant identifier. All queries are filtered using the tenant ID."*
- Simpler operations than schema-per-tenant or database-per-tenant for Phase 1 (9,000 invoices/month/tenant is small).
- No Hibernate filter complexity yet - filtering is centralized in service methods (`InvoiceService.listForCurrentTenant()`), so a single code review audit is enough.

Central enforcement:
- `TenantContext` (ThreadLocal) is populated by `JwtAuthenticationFilter` from the JWT claim `tenantId`.
- Services call `TenantContext.get()` - never accept a `tenantId` parameter from the client.
- Repositories use `findAllByTenantId(UUID)` / `findByIdAndTenantId(UUID, Long)`.

**Anti-patterns explicitly forbidden in this codebase:**
- NO: `repository.findById(id)` then `if (!inv.tenantId.equals(...))` - race conditions and leaks.
- NO: Passing `tenantId` as a request parameter.
- NO: Using a Hibernate `@Filter` without a `TenantContext` lifecycle test.

### 1.3 Authentication flow

```
Client                Backend
  | POST /api/v1/auth/login {username, password}
  +---------------------------------------------->
  |                            AuthenticationManager.authenticate(...)
  |                            -> AppUserDetails (id, tenantId, role)
  |                            -> JwtService.issue(userId, tenantId, role)
  | 200 {accessToken, tokenType, expiresInMs}
  <----------------------------------------------
  | GET /api/v1/invoices  (Authorization: Bearer <jwt>)
  +---------------------------------------------->
  |                            JwtAuthenticationFilter
  |                            -> parse JWT -> TenantContext.set(tenantId)
  |                            -> SecurityContextHolder.setAuthentication(...)
  | 200 [invoices for that tenant]
  <----------------------------------------------
```

JWT claims: `sub` (userId), `tenantId` (UUID), `role` (string), `iat`, `exp`.
HS256, secret >= 32 chars, read from `JWT_SECRET` env var.

### 1.4 Module map

| Module | Package | Owner (per PRD 8) |
|---|---|---|
| Identity: auth, tenants, users, roles | `com.paytrack.config`, `com.paytrack.security` | Engineer A |
| Customers, invoices, credit notes | `com.paytrack.model`, `com.paytrack.service` | Engineer B |
| Tax & numbering engine, invoice PDF | (Phase 1 same module) | Engineer C |
| Reports, notifications, settings | Phase 2+ | Engineer D |

---

## 2. Data Model

### 2.1 Logical model (Phase 1)

```
              +--------------+
              |   Tenant     |
              | id UUID PK   |
              | name         |
              +------+-------+
                     | 1
                     |
                     | N
              +------v-------+          +--------------+
              |  AppUser     |          |   Invoice    |
              | id BIGSERIAL |          | id BIGSERIAL |
              | tenant_id FK |          | tenant_id FK |
              | username UQ  |          | customer     |
              | password_hash|          | amount NUMERIC|
              | role         |          | status       |
              | enabled      |          +--------------+
              +--------------+
```

### 2.2 Physical DDL (Flyway `V1__init_tenant_user_invoice.sql`)

```sql
CREATE TABLE tenant (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenant(id),
    username       VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(30)  NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_username UNIQUE (username)
);

CREATE INDEX idx_app_user_tenant ON app_user(tenant_id);

CREATE TABLE invoice (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    customer    VARCHAR(255) NOT NULL,
    amount      NUMERIC(15,2) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_tenant ON invoice(tenant_id);
```

**Design decisions:**
- `UUID` for `tenant.id` -> no enumeration leak, suitable for external URLs and JWT claims.
- `BIGSERIAL` for `app_user.id` and `invoice.id` -> compact, sortable, cheap joins.
- `username` is **globally unique**. Compromise for Phase 1: a user provides only `{username, password}` at login, without a `tenantCode`. Phase 2 will add `tenantCode` and drop the global uniqueness.
- `password_hash` stores `{bcrypt}$2a$10$...` (DelegatingPasswordEncoder format).
- All money columns are `NUMERIC(15,2)`, never `FLOAT` / `DOUBLE` (see 3 Rounding Rule).

### 2.3 Seed data (Flyway `V2`, `V3`)

| Tenant | UUID | User | Role |
|---|---|---|---|
| Meridian Supplies | `11111111-1111-1111-1111-111111111111` | `alice` | FINANCE_MANAGER |
| Acme Distributors | `22222222-2222-2222-2222-222222222222` | `bob` | FINANCE_MANAGER |

Both users: password `Passw0rd!`, hash `{bcrypt}$2a$10$t5ZwzoxsFvmr7krvMLzh/OFXaJSsdwh/n4TaGVkUG3RdB4lu86jyC`.

Invoices:
- Tenant A: `A-Customer-1` (1000.00), `A-Customer-2` (2500.00)
- Tenant B: `B-Customer-1` (9999.00)

---

## 3. API Contract

Base path: `/api/v1`. Content-type: `application/json`. Auth: `Authorization: Bearer <jwt>`.

### 3.1 `POST /api/v1/auth/login`

Public. No auth required.

**Request:**
```json
{ "username": "alice", "password": "Passw0rd!" }
```

**Success - 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 3600000
}
```

**Errors:**
- `401 Unauthorized` - bad credentials.
- `400 Bad Request` - missing fields (bean validation).

### 3.2 `GET /api/v1/invoices`

Authenticated. Returns invoices **for the current tenant only**.

**Success - 200:**
```json
[
  { "id": 1, "tenantId": "11111111-1111-1111-1111-111111111111",
    "customer": "A-Customer-1", "amount": 1000.00, "status": "ISSUED" },
  { "id": 2, "tenantId": "11111111-1111-1111-1111-111111111111",
    "customer": "A-Customer-2", "amount": 2500.00, "status": "ISSUED" }
]
```

### 3.3 `GET /api/v1/invoices/{id}`

Authenticated. Returns the invoice if `invoice.tenant_id == TenantContext.get()`. Otherwise **404** (not 403 - we do not leak existence of other tenants' invoices).

**Success - 200:** same shape as list item.

**Errors:**
- `401` - no/invalid Bearer token.
- `404` - invoice not found **or** belongs to another tenant.

### 3.4 `GET /actuator/health`

Public. Returns `{"status":"UP"}` when app + DB are reachable.

### 3.5 Error format (uniform, per NFR-05)

```json
{
  "timestamp": "2026-11-19T06:38:52.123Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/v1/invoices/3",
  "message": "Invoice 3 not found"
}
```

---

## 4. Tax Rule (GST)

Phase 1 invoice = sum of lines, each line has quantity, unit price, discount%, tax rate, place of supply.

### 4.1 Line-level calculation

For each line `i`:

```
discounted_base_i  = round(quantity_i * unit_price_i * (1 - discount_pct_i / 100), 2)
tax_amount_i       = round(discounted_base_i * tax_rate_i / 100, 2)
line_total_i       = discounted_base_i + tax_amount_i
```

### 4.2 Place-of-supply rule (India GST)

Let `seller_state` = distributor's state, `buyer_state` = customer's shipping state.

- If `buyer_state == seller_state` -> **CGST + SGST**, each at `tax_rate / 2`.
- If `buyer_state != seller_state` -> **IGST** at full `tax_rate`.

Example (same state, 18% GST on 1000):
```
CGST = round(1000 * 9 / 100, 2) = 90.00
SGST = round(1000 * 9 / 100, 2) = 90.00
Total = 1000 + 90 + 90 = 1180.00
```

Example (inter-state, 18% IGST on 1000):
```
IGST  = round(1000 * 18 / 100, 2) = 180.00
Total = 1000 + 180 = 1180.00
```

### 4.3 Invoice total

```
subtotal    = SUM(discounted_base_i)
tax_total   = SUM(tax_amount_i)
grand_total = subtotal + tax_total
```

**Storage:** all amounts stored as `NUMERIC(15,2)` (BigDecimal in Java), rounding at each line, **then** sum the rounded values. Never round after summing.

---

## 5. Rounding Rule (manually verifiable)

### 5.1 The rule

**HALF_UP to 2 decimal places at each calculation step.** Not at the end. This is the only rule used in Phase 1.

Implementation: `BigDecimal.setScale(2, RoundingMode.HALF_UP)`.

### 5.2 Manual verification examples

| Input | Raw | Rounded (HALF_UP, 2 dp) |
|---|---|---|
| `0.005` | 0.005 | **0.01** |
| `0.004` | 0.004 | **0.00** |
| `10.555` | 10.555 | **10.56** |
| `10.554` | 10.554 | **10.55** |
| `1000 * 9 / 100` | 90.0 | **90.00** |
| `1234.567 * 1.18` | 1456.78806 | **1456.79** |
| `-0.005` | -0.005 | **-0.01** |

### 5.3 Why HALF_UP (and not HALF_EVEN)

- HALF_UP is the norm in Indian GST invoicing and what an accountant would compute by hand.
- HALF_EVEN (banker's rounding) minimizes long-run bias but produces surprises (`0.005 -> 0.00`) that are hard to reconcile with a human.

### 5.4 Edge cases explicitly covered

| Case | Treatment |
|---|---|
| Quantity is fractional (e.g. 2.5 kg) | Multiply as `BigDecimal`, round only the **line total**, not intermediate qty * price |
| Discount is a % (e.g. 12.5%) | `base * (1 - disc/100)` rounded to 2 dp |
| Negative line (return) | `RoundingMode.HALF_UP` is symmetric on negatives (-0.005 -> -0.01) |
| Currency | Always INR; if multi-currency is added in Phase 2, rounding stays per-currency but the currency code travels with every amount (per PRD "Engineering rules") |

### 5.5 Conformance test (must pass)

```java
assertThat(round(new BigDecimal("0.005"))).isEqualByComparingTo("0.01");
assertThat(round(new BigDecimal("0.004"))).isEqualByComparingTo("0.00");
assertThat(round(new BigDecimal("1234.567"))).isEqualByComparingTo("1234.57");
```

---

## 6. Numbering Rule (invoice numbers)

### 6.1 Format

```
<FY-prefix>/<series>/<sequence>
```

Example for FY 2026-27, series `INV`, invoice #42:
```
2026-27/INV/00042
```

| Part | Meaning |
|---|---|
| `2026-27` | Financial year (April 1 - March 31, India) |
| `INV` | Series code - configurable per tenant, default `INV` |
| `00042` | 5-digit zero-padded sequence, scoped to `(tenant, FY, series)` |

### 6.2 Rules

1. **No gaps.** The number is allocated inside the same transaction that inserts the invoice.
2. **No duplicates.** Enforced by a `UNIQUE (tenant_id, financial_year, series, sequence_number)` constraint on the numbering table.
3. **Concurrency-safe.** The allocator locks the series row (`SELECT ... FOR UPDATE`) before incrementing.
4. **Never reused.** A voided invoice keeps its number; a new invoice gets the next number. Corrections are made with a **credit note**, not by editing the invoice (per FR-1.06).
5. **Resets annually.** Sequence starts at 1 on April 1.
6. **Immutable.** Once issued, `invoiceNumber` is part of the issued record and cannot change.

### 6.3 Numbering table (Phase 1)

```sql
CREATE TABLE invoice_series (
    tenant_id       UUID NOT NULL REFERENCES tenant(id),
    financial_year  VARCHAR(10) NOT NULL,
    series_code     VARCHAR(10) NOT NULL,
    next_number     BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (tenant_id, financial_year, series_code)
);
```

Allocator (pseudo-SQL):

```sql
BEGIN;
SELECT next_number FROM invoice_series
 WHERE tenant_id = :t AND financial_year = :fy AND series_code = :s
 FOR UPDATE;
UPDATE invoice_series SET next_number = next_number + 1
 WHERE tenant_id = :t AND financial_year = :fy AND series_code = :s;
INSERT INTO invoice (..., invoice_number) VALUES (..., :formatted);
COMMIT;
```

### 6.4 Manual verification

| Action | Expected `invoiceNumber` |
|---|---|
| Tenant A issues its first invoice in FY 2026-27 | `2026-27/INV/00001` |
| Tenant A issues its second | `2026-27/INV/00002` |
| Tenant B issues its first | `2026-27/INV/00001` (independent series) |
| Tenant A voids invoice 2 | `2026-27/INV/00002` is kept; next is `2026-27/INV/00003` |
| Next FY starts | first invoice is `2027-28/INV/00001` |

### 6.5 Conformance test (must pass)

Fire 20 parallel `issueInvoice()` calls for the same tenant/FY/series. Assert:
- 20 distinct `invoiceNumber` values,
- consecutively numbered `00001` to `00020`,
- no gap,
- 0 duplicates.

This is exactly **AC-1.2** from the PRD.

---

## 7. Permission Matrix (5 roles x all actions)

Roles from FR-1.01: `OWNER`, `FINANCE_MANAGER`, `ACCOUNTANT`, `COLLECTOR`, `READ_ONLY`.

Legend: Y = allowed, N = denied, R = read-only, - = not applicable

| Action | OWNER | FINANCE_MANAGER | ACCOUNTANT | COLLECTOR | READ_ONLY |
|---|:---:|:---:|:---:|:---:|:---:|
| **Organisation & users** | | | | | |
| Create user | Y | N | N | N | N |
| Update user | Y | N | N | N | N |
| Deactivate user | Y | N | N | N | N |
| Assign role | Y | N | N | N | N |
| View users | Y | Y | R | R | R |
| Configure organisation (GSTIN, address, logo) | Y | N | N | N | N |
| View audit log | Y | R | N | N | R |
| **Customers** | | | | | |
| Create customer | Y | Y | N | N | N |
| Update customer | Y | Y | N | N | N |
| View customer | Y | Y | Y | Y | R |
| **Invoices** | | | | | |
| Create draft invoice | Y | Y | N | N | N |
| Edit draft invoice | Y | Y | N | N | N |
| Issue invoice | Y | Y | N | N | N |
| Void issued invoice (via credit note) | Y | Y | N | N | N |
| Download invoice PDF | Y | Y | Y | Y | R |
| View invoices (all, current tenant) | Y | Y | Y | Y | R |
| **Payments** | | | | | |
| Record payment | Y | N | Y | N | N |
| Reconcile payment -> invoice | Y | N | Y | N | N |
| View payments | Y | Y | Y | Y | R |
| **Collections** | | | | | |
| View overdue receivables | Y | Y | Y | Y | R |
| Log collection action | Y | N | N | Y | N |
| Send reminder | Y | N | N | Y | N |
| View collection portfolio | Y | Y | Y | Y | R |
| **Reporting** | | | | | |
| View dashboard | Y | Y | R | R | R |
| Export financial report | Y | Y | N | N | N |
| View KPIs / DSO | Y | Y | R | N | R |

### Enforcement

- Annotation-based on controller methods:
  - `@PreAuthorize("hasRole('OWNER')")`
  - `@PreAuthorize("hasAnyRole('OWNER','FINANCE_MANAGER')")`
  - `@PreAuthorize("hasAnyRole('OWNER','FINANCE_MANAGER','ACCOUNTANT','COLLECTOR','READ_ONLY')")`
- Every protected endpoint has **two tests**: one with an allowed role (expect 200/201) and one with a denied role (expect 403). See NFR-02.
- The matrix above is the single source of truth. Code review checks every `@PreAuthorize` against this table.

---

## 8. Risks & Mitigations

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Rounding differences between lines and totals | High | Medium | HALF_UP per line; conformance test in 5.5; reviewer checks 3 invoices by hand (AC-1.3) |
| R2 | Gaps in invoice numbers after failed transaction | Medium | High | Allocate number inside the issuing transaction with `SELECT ... FOR UPDATE` on the series row (6.3); conformance test 20-parallel (AC-1.2) |
| R3 | Tenant leak via forgotten filter | Medium | Critical | `TenantContext` populated only by JWT filter; all services go through `findByTenantId`; two automated isolation tests (AC-1.1) |
| R4 | Weak `JWT_SECRET` in prod | Medium | High | Enforce >= 32 chars; env var, never committed; `.env.example` documents it |
| R5 | Flyway checksum mismatch after hotfix | Medium | Medium | Rule: never edit an applied migration; use `flyway repair` + new `Vn` file |
| R6 | One team member falls behind | Medium | Medium | Raise in weekly check-in; mentor notified (PRD 11) |
| R7 | Concurrent invoice issuance race | Low | High | `SELECT ... FOR UPDATE` + unique constraint on series row |
| R8 | Time zone bugs on `expiresInMs` | Low | Low | JWT uses UTC epoch seconds; `Date` on server in UTC |

---

## 9. Out of scope (Phase 1)

Per PRD 4.3: general-ledger accounting, GST return filing, inventory, payroll.

---

## 10. Appendix - Current implementation status

| Requirement | Status | Where |
|---|---|---|
| FR-1.01 Organisation model | done | `Tenant`, `AppUser`, `Role` enum |
| FR-1.02 Customers | not started | - |
| FR-1.03 Invoices | partial | `Invoice` entity + seed |
| FR-1.04 Tax engine | not started | - |
| FR-1.05 Numbering | not started | Design here (6) |
| FR-1.06 Lifecycle | partial - `status` field only | - |
| FR-1.07 Documents (PDF) | not started | - |
| FR-1.08 Client-specific | not started | - |
| NFR-01 Tenancy | done | `TenantContext`, isolation tests |
| NFR-02 Access control | partial - basic | `hasRole` in `SecurityConfig` |
| NFR-03 Money correctness | design only | 4-5 |
| NFR-04 Data integrity | done | FK, unique, transactions |
| NFR-05 API standard | partial - `/api/v1` only | - |
| NFR-08 Security | done | BCrypt, JWT, env var |
| NFR-09 Audit | not started | - |
| NFR-10 Testing | done | `TenantIsolationTest` (4 tests) |
| NFR-11 Documentation | done | this file |

Legend: done / partial / not started

---

## 11. How to run (for the reviewer)

```bash
docker compose up -d postgres
mvn spring-boot:run
# then in another shell:
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Passw0rd!"}'
```

Tests:
```bash
mvn clean test
# -> Tests run: 4, Failures: 0
```