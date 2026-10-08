# Kisan Bandhu · किसान बंधु

**Unified B2B SaaS platform that automates APMC mandi weighbridge operations** — SIH idea submission by *Team Victus*.

> "From gate congestion to seamless procurement: redefining the APMC ecosystem."

Farmers arrive at APMC mandi gates unpredictably, causing 4–8 hour vehicle queues, idle weighbridges and opaque manual slot allocation — and marginal farmers with only feature phones are excluded from digital systems entirely. Kisan Bandhu replaces the queue with:

1. **Web PWA slot booking** (smartphones) — 15-minute weighbridge slots, instant digital tokens, SMS/OTP alerts.
2. **Toll-Free IVR voice assistant** (feature phones) — a Hindi/English voice menu that books slots, reports tokens and reads market prices over a phone call.
3. **Zero-Idle Trap Engine** — a scheduler that marks no-shows, auto-reschedules them into the next free slot, and instantly back-fills freed slots from a waitlist.
4. **Live admin dashboard** — hourly throughput, no-show rate, slot board, SMS outbox and event log.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Data JPA/Hibernate, Spring Security + JWT |
| Database | MySQL 8.4 (Aiven Cloud profile) — H2 for local/dev |
| Frontend | Vanilla HTML/CSS/JS **Progressive Web App** (offline via service worker) |
| Voice | Pluggable Toll-Free IVR webhook (Exotel / Twilio / Knowlarity-ready) |
| Messaging | Mock SMS gateway (drop-in MSG91/Twilio adapter), OTP with expiry + attempt limits |
| Data | Agmarknet / e-NAM / OGD price sources (sync enabled by config flag) |

## Quick start

```bash
cd backend
mvn spring-boot:run          # http://localhost:8080
```

First run seeds demo data (H2 file at `backend/data/`):

| Role | Phone | Password |
|---|---|---|
| Farmer | `9876543210` | `farmer123` |
| Mandi Admin | `9999999999` | `admin123` |

Run tests (booking conflicts, no-show engine, IVR flow, OTP):

```bash
mvn test
```

Build a production jar:

```bash
mvn -DskipTests package
java -jar target/kisan-bandhu-backend-0.1.0.jar
```

### MySQL / Aiven (production profile)

```bash
export KB_DB_HOST=your-aiven-host.aivencloud.com
export KB_DB_PORT=18834            # Aiven MySQL port (SSL)
export KB_DB_NAME=kisanbandhu
export KB_DB_USER=avnadmin
export KB_DB_PASSWORD=...
export KB_JWT_SECRET=<64+ random chars>
java -jar target/kisan-bandhu-backend-0.1.0.jar --spring.profiles.active=mysql
```

## How it works

### Slot model

- Each mandi has active **weighbridges** and operating hours (default 08:00–17:00, 15-min slots).
- The availability grid is computed virtually; a `Slot` row is only created on booking, so a booking occupies exactly one bridge/time cell.
- Guards: one active slot per farmer per day, no double-booking of a bridge/time, bookings only for future slots inside mandi hours.

### Zero-Idle Trap Engine (`service/ZeroIdleEngine.java`)

Runs every **30 seconds**:

1. **No-show sweep** — booked slots past `graceMinutes` (default 20) become `NO_SHOW`, then:
   - **auto-reschedule**: the engine grabs the next free bridge/time today, creates a new token linked via `rescheduledFrom`, and SMSes the farmer; or
   - if the day is over, the farmer is auto-joined into **tomorrow's waitlist**.
2. **Gap back-fill** — any freed/cancelled slot inside the next 2 hours is immediately offered to the oldest waitlist entry (promoted + SMS).
3. **SMS reminders** — upcoming slots get a single "reach now" reminder.

### IVR voice flow

```
/api/ivr/incoming  { phone, node, digits }  →  { node, prompt, action, options, data }
```

State machine: `welcome` (language) → `menu_en`/`menu_hi` → `1` book · `2` token status · `3` market prices · `4` waitlist · `0` exit. The demo phone on the home page drives this same endpoint. In production the telephony provider's webhook points here and TTS plays `prompt`.

Unknown callers are auto-provisioned as farmers so a feature-phone user can book without ever touching a website.

### Auth & OTP

- `POST /api/auth/register`, `POST /api/auth/login` → JWT (72 h), roles `FARMER` / `ADMIN`.
- `POST /api/otp/request`, `POST /api/otp/verify` — 6-digit codes, 5-min expiry, 5 attempts; delivered through the SMS service (mock logs to console + `sms_outbox`, visible in the admin dashboard).

## API map

```
Public
  GET  /api/public/mandis
  GET  /api/public/availability?mandiId=&date=
  GET  /api/market/prices?commodity=
  POST /api/auth/register | /api/auth/login
  POST /api/otp/request | /api/otp/verify
  POST /api/ivr/incoming            GET /api/ivr/config

Farmer (Bearer JWT)
  POST /api/slots/book              POST /api/slots/{id}/cancel
  POST /api/slots/checkin           GET  /api/slots/mine
  POST /api/slots/waitlist          GET  /api/slots/waitlist/mine
  GET  /api/auth/me

Admin (role ADMIN)
  GET  /api/admin/dashboard?mandiId=&date=
  GET  /api/admin/slots?mandiId=&date=
  POST /api/admin/slots/{id}/checkout
  GET  /api/admin/sms               POST /api/admin/market/sync
```

## Project layout

```
kisan-bandhu/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/kisanbandhu/
│       ├── config/        Security, CORS, demo data seeder
│       ├── domain/        User, Mandi, Weighbridge, Slot, Waitlist, OTP, SMS, MarketPrice, AuditEvent
│       ├── repo/          Spring Data JPA repositories
│       ├── dto/           Request/response records
│       ├── security/      JWT issue/parse + auth filter
│       ├── service/       Auth, OTP, SMS, Slot, ZeroIdleEngine, IVR, Market, Dashboard, Audit
│       └── web/           REST controllers + error handler
│   └── src/main/resources/static/   PWA (index.html, app.js, styles.css, manifest, sw.js, icons)
│   └── src/test/java/     End-to-end flow tests (5 scenarios)
└── docs/                  Proposal notes
```

## Roadmap hooks

- **SMS**: implement `SmsService.deliver()` with MSG91/Twilio credentials (`kisan.sms.provider`).
- **Agmarknet**: set `kisan.agmarknet.enabled=true` and `kisan.agmarknet.base-url` to go live; otherwise seeded/cached prices are served.
- **IVR**: point Exotel/Twilio/Knowlarity webhook at `/api/ivr/incoming` and feed `prompt` into TTS; DTMF digits arrive as `digits`.
- **Scale**: the stateless JWT design + MySQL (Aiven) supports multi-mandi SaaS onboarding — add a `mandi_admin` role scoped per mandi row.
