# StayEasy - Hotel Booking Portal

A small full-stack hotel booking portal built for the Thomas Cook developer internship assessment.

- **Backend:** Java 17, Spring Boot, Spring Data JPA, Bean Validation
- **Frontend:** React (Vite) with React Router
- **Database:** MySQL 8

Guests can browse and search hotels, pick dates, see the total price, book a room, receive a unique booking reference, and look up or cancel a booking using the reference plus their email. The surprise feature suggests nearby available dates when a hotel is sold out.

See [DESIGN.md](DESIGN.md) for architecture, database design, API design, booking logic, concurrency handling, and trade-offs.

## Screenshots

Screenshots of the main flow are in [`docs/screenshots`](docs/screenshots).

| Step | Screenshot |
|------|-----------|
| Search and hotel list | `docs/screenshots/01-search.png` |
| Hotel details and price summary | `docs/screenshots/02-hotel-details.png` |
| Booking confirmation | `docs/screenshots/03-confirmation.png` |
| Manage / cancel booking | `docs/screenshots/04-manage.png` |
| Surprise feature (nearby dates) | `docs/screenshots/05-nearby-dates.png` |

## Prerequisites

- JDK 17 or newer
- Node.js 18 or newer (with npm)
- MySQL 8.x running locally on port 3306
- Git

Maven does not need to be installed separately; the project includes the Maven wrapper (`mvnw`).

## Run it locally

### 1. Create the database and an app user

Run this in MySQL Workbench or the `mysql` command line, choosing your own password:

```sql
CREATE DATABASE hotel_booking CHARACTER SET utf8mb4;
CREATE USER 'hotel_app'@'localhost' IDENTIFIED BY 'your-password-here';
GRANT ALL PRIVILEGES ON hotel_booking.* TO 'hotel_app'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Create the tables and load sample data

Run the two scripts in this order (in Workbench: *File > Open SQL Script*, then the lightning-bolt button):

1. `db/schema.sql` - creates the tables and constraints (it drops and recreates them, so it is safe to re-run)
2. `db/seed.sql` - inserts 6 sample hotels and creates room inventory for the next 180 days starting from the day you run it

Quick check:

```sql
SELECT COUNT(*) FROM hotels;          -- 6
SELECT COUNT(*) FROM room_inventory;  -- 1080
```

> The seed script generates inventory starting from the current date. If you come back to the project weeks later, re-run `schema.sql` and `seed.sql` to refresh the bookable window.

### 3. Configure the backend connection

Credentials are not stored in the repository. In `backend/src/main/resources/`, copy the example file:

```
application-local.properties.example  ->  application-local.properties
```

and put your real database password in the new file:

```properties
spring.datasource.username=hotel_app
spring.datasource.password=your-password-here
```

`application-local.properties` is git-ignored.

### 4. Start the backend

```bash
cd backend
./mvnw spring-boot:run        # on Windows PowerShell: .\mvnw spring-boot:run
```

Wait for `Started BackendApplication`. The API runs on http://localhost:8080.

### 5. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` requests to the backend, so no CORS setup is needed during development.

## Run the tests

```bash
cd backend
./mvnw test
```

The unit tests need no database. They cover price calculation, date validation, availability rules, the "last room" scenario, cancellation releasing availability, and the nearby-dates logic. Spring's default context-load test (`BackendApplicationTests`) does connect to MySQL, so keep MySQL running and the local properties file in place.

## API at a glance

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/hotels?location=&checkIn=&checkOut=` | List/search hotels, with availability and total price when dates are given |
| GET | `/api/hotels/{id}?checkIn=&checkOut=` | Hotel details and availability for the dates |
| GET | `/api/hotels/{id}/alternatives?checkIn=&checkOut=` | Surprise feature: nearby available stays |
| POST | `/api/bookings` | Create a booking |
| GET | `/api/bookings/{reference}?email=` | Retrieve a booking |
| POST | `/api/bookings/{reference}/cancel` | Cancel a booking (body: `{"email": "..."}`) |

Dates use the format `yyyy-MM-dd`. All errors share one JSON shape: `timestamp`, `status`, `code`, `message`, and optional `fieldErrors`.

## Time spent and unfinished items

- **Approximate time spent:** `11 hours`
- **Unfinished / not done:** see "Limitations and improvements" in [DESIGN.md](DESIGN.md). In short: no frontend automated tests, no database migration tool (plain SQL scripts), no confirmation emails, no pagination.

## AI assistance disclosure

I used Claude (Anthropic's AI assistant) significantly throughout this project, for: planning the approach, setting up the tooling, drafting and debugging the backend (entities, services, controllers, error handling), the unit tests, the React frontend, the surprise feature, and drafting these documents. I ran the application, tested the flows myself, fixed the issues that came up, and I am prepared to explain and discuss all of the code and design decisions in the interview.
