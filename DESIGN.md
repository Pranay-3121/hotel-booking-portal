# Design Document

This document explains how the hotel booking portal is designed and why.

## 1. Architecture

```
React (Vite, :5173)  --HTTP/JSON-->  Spring Boot REST API (:8080)  --JPA/JDBC-->  MySQL 8
```

- **Frontend (React):** a single-page app with four screens: search and hotel list, hotel details with booking form, confirmation, and manage booking. All server communication goes through one small module (`src/api.js`) that turns the API's error responses into friendly messages. In development, Vite proxies `/api` to the backend.
- **Backend (Spring Boot):** a layered design.
  - `controller` - HTTP only: parses requests, validates input (`@Valid`), returns DTOs.
  - `service` - business rules and transactions (`BookingService`, `HotelService`).
  - `service/BookingRules` - pure static functions for pricing, date validation, and availability maths. They have no database or Spring dependencies, so they are trivial to unit test.
  - `repository` - Spring Data JPA interfaces.
  - `entity` / `dto` - JPA entities are never returned directly; responses use immutable record DTOs.
  - `exception` - one `GlobalExceptionHandler` converts every failure into the same JSON error shape.
- **Database (MySQL):** the source of truth for hotels, per-night room inventory, and bookings. Integrity rules (unique references, check constraints, foreign keys) are enforced in the database as well as in code.

## 2. Database design

Three tables (see `db/schema.sql`):

**hotels** - name, location, description, amenities (comma-separated text), `price_per_night`, `total_rooms`.

**room_inventory** - one row per hotel per night: `hotel_id`, `stay_date`, `total_rooms`, `booked_rooms`.
- `UNIQUE (hotel_id, stay_date)` so a night can only exist once per hotel.
- `CHECK (booked_rooms >= 0 AND booked_rooms <= total_rooms)` so the database itself refuses overbooking, even if application code has a bug.

**bookings** - `reference` (unique), `hotel_id`, guest name and email, `check_in`, `check_out`, `nights`, `price_per_night`, `total_amount`, `status` (`CONFIRMED` or `CANCELLED`), `created_at`, `cancelled_at`.
- `UNIQUE (reference)`.
- `CHECK (check_out > check_in)`.
- Foreign keys to `hotels`, and an index on `guest_email` for lookups.
- The price per night and the total are **copied onto the booking** so later price changes never alter an existing booking.

Relationships: one hotel has many inventory rows and many bookings. A booking references its hotel; its nights are the inventory rows from `check_in` (inclusive) to `check_out` (exclusive).

## 3. API design

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/hotels` | List/search by `location`; with `checkIn` and `checkOut` it also returns `availableRooms`, `nights` and `totalAmount` per hotel |
| GET | `/api/hotels/{id}` | Hotel details, with availability and price for the given dates |
| GET | `/api/hotels/{id}/alternatives` | Nearby available stays (surprise feature) |
| POST | `/api/bookings` | Create a booking; returns `201` with the booking and its reference |
| GET | `/api/bookings/{reference}?email=` | Retrieve a booking |
| POST | `/api/bookings/{reference}/cancel` | Cancel a booking; email in the JSON body |

**Errors** always look like this:

```json
{
  "timestamp": "...",
  "status": 409,
  "code": "SOLD_OUT",
  "message": "Sorry, no rooms are available for the selected dates. Try different dates.",
  "fieldErrors": { "guestEmail": "Enter a valid email address" }
}
```

`fieldErrors` only appears for validation failures. Status codes: `400` for invalid input or dates, `404` for unknown hotels or bookings, `409` for conflicts (sold out, already cancelled), `500` for unexpected errors (logged server-side, with a generic message to the client).

## 4. Booking logic

- **Date validation:** check-in cannot be in the past (today is allowed), check-out must be after check-in, and a stay is limited to 30 nights.
- **Overlapping stays:** a stay occupies the nights from check-in **up to but not including** the check-out date (a half-open range). Two guests can therefore share a room on the same calendar day when one checks out and the other checks in. Each night is tracked separately in `room_inventory`.
- **Pricing:** calculated only on the backend as `price_per_night x nights`, rounded to 2 decimals with `BigDecimal`. The frontend displays the figure the backend returns and never computes the price itself.
- **Availability:** the rooms free for a whole stay equal the **minimum** free rooms across all its nights. If any single night is full, the stay cannot be booked. If any night has no inventory row (outside the 180-day window), the request is rejected with a clear message.
- **Booking creation:** inside one transaction, the service locks the stay's inventory rows, checks availability, increments `booked_rooms` on each night, and saves the booking with a unique reference. If anything fails, the whole transaction rolls back and nothing is reserved.
- **Booking reference:** `BK-` followed by 8 characters from an unambiguous alphabet (no `0/O`, `1/I`), generated with `SecureRandom`, with a uniqueness check and a database unique constraint.
- **Retrieval and cancellation:** both require the reference and the email. A wrong email returns the same "not found" message as a wrong reference, so the API doesn't reveal which references exist. Cancelling decrements `booked_rooms` on every night of the stay, marks the booking `CANCELLED` and records `cancelled_at`. Cancelling twice, or cancelling a stay that has already started, is rejected.

## 5. Design decisions and trade-offs

- **Per-night inventory table vs. counting overlapping bookings.** Counting overlaps with a date-range query is the "purer" model, but it makes a race-free "last room" check harder. A per-night counter gives simple reads, a simple cancel (decrement), and rows to lock. The cost is that inventory rows must be generated ahead of time (the seed script creates 180 days) and a hotel's room count changes need care.
- **Pessimistic locking vs. optimistic locking.** For a booking system, conflicts on the last room are the exact case that must never fail silently. Pessimistic locking is easier to reason about and explain, and contention is low at this scale. Optimistic locking with retries would scale better under heavy contention but adds retry logic.
- **Business rules in a pure class (`BookingRules`).** This keeps rules out of controllers and makes them testable without a database.
- **DTO records instead of exposing entities.** Avoids leaking internal fields and keeps the API stable if the schema changes.
- **Schema as SQL scripts, `ddl-auto=none`.** The schema is explicit and reviewable, and constraints live in SQL. The trade-off is no automatic migrations (see limitations).
- **Injected `Clock`.** "Today" is injectable so tests can freeze the date instead of depending on the real clock.
- **Amenities as a comma-separated column.** Simple for a single-category demo. A separate `amenities` table would be needed for filtering by amenity.

## 6. Simultaneous bookings (two users, one last room)

`BookingService.create` runs in a single transaction and loads the stay's inventory rows with `SELECT ... FOR UPDATE` (JPA `PESSIMISTIC_WRITE`), ordered by `stay_date`.

If two users try to take the last room at the same moment, the first transaction locks the night rows. The second transaction **waits** on those rows. When the first commits, the second reads the updated counts, sees no free room, and fails cleanly with the `SOLD_OUT` error. Rows are always locked in date order, which prevents two overlapping stays from deadlocking each other. The `CHECK (booked_rooms <= total_rooms)` constraint is a final safety net at the database level.

Cancellation uses the same locking so a cancel and a booking cannot corrupt the counts.

Unit tests cover the sequential version of this rule (the last room can be booked once but not twice). A true concurrent test would need an integration test with two threads against a real MySQL instance, which I did not add.

## 7. Surprise feature: nearby available dates

**What it does:** when a hotel is sold out for the dates a guest chose, the booking panel suggests up to three alternative stays of the **same length** within 7 days before or after, closest first, each with its total price. Clicking one switches the dates and updates the price so the guest can book immediately.

**Why I chose it:** "Sold out" is where most guests abandon a booking. Offering a concrete next step turns a dead end into a possible sale, and it directly reuses the availability model.

**How it works:**
- `GET /api/hotels/{id}/alternatives` loads the per-night inventory for the whole search window in **one query**, rather than one query per candidate date.
- A pure function, `BookingRules.findAlternatives`, slides a window over that data. A candidate is valid only if every night has an inventory row with a free room and the stay does not start in the past. Candidates are ordered by distance from the requested check-in; on a tie a later start comes first.
- The React component `AlternativeDates` shows the options. If the suggestions request fails, it hides itself rather than blocking the page.
- Suggestions are only advice. The booking itself still goes through the normal locked availability check, so a stale suggestion cannot cause an overbooking.
- Covered by unit tests for ordering, the result limit, never suggesting past dates, sold-out hotels, and missing inventory.

## 8. Limitations and improvements

What is incomplete or simplified:
- No authentication, payments, admin panel or cloud deployment (not required).
- No confirmation email; the guest relies on the reference shown on screen.
- Schema changes are plain SQL scripts, with no migration tool.
- Inventory is generated for 180 days from the day the seed script is run and is not extended automatically.
- One room category per hotel, and the number of guests is not modelled.
- No pagination on the hotel list.
- No frontend automated tests, and no concurrent integration test for the locking behaviour.
- The booking reference uniqueness check is "check then insert"; in the extremely unlikely event of a collision the unique constraint would surface as a generic error instead of a retry.

What I would do with more time:
1. Use Flyway or Liquibase for migrations, and a scheduled job to extend inventory.
2. Add an integration test using Testcontainers (real MySQL) with two threads racing for the last room.
3. Send a confirmation email with a cancel link.
4. Add frontend tests (React Testing Library) for the booking form and error states.
5. Add multiple room types, guest counts, and filters (price range, amenities).
6. Add pagination and caching for the hotel list.
7. Move amenities to their own table and add rate limiting to the lookup endpoints.
