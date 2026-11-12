USE hotel_booking;

DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS room_inventory;
DROP TABLE IF EXISTS hotels;

CREATE TABLE hotels (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(150)  NOT NULL,
  location        VARCHAR(100)  NOT NULL,
  description     TEXT          NOT NULL,
  amenities       VARCHAR(500)  NOT NULL,
  price_per_night DECIMAL(10,2) NOT NULL,
  total_rooms     INT           NOT NULL,
  CONSTRAINT chk_hotel_price CHECK (price_per_night > 0),
  CONSTRAINT chk_hotel_rooms CHECK (total_rooms > 0),
  INDEX idx_hotels_location (location)
);

-- One row per hotel per night: the unit of availability tracking
CREATE TABLE room_inventory (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  hotel_id     BIGINT NOT NULL,
  stay_date    DATE   NOT NULL,
  total_rooms  INT    NOT NULL,
  booked_rooms INT    NOT NULL DEFAULT 0,
  CONSTRAINT uq_inventory_hotel_date UNIQUE (hotel_id, stay_date),
  CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
  CONSTRAINT chk_inventory_booked CHECK (booked_rooms >= 0 AND booked_rooms <= total_rooms)
);

CREATE TABLE bookings (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  reference       VARCHAR(20)   NOT NULL,
  hotel_id        BIGINT        NOT NULL,
  guest_name      VARCHAR(100)  NOT NULL,
  guest_email     VARCHAR(150)  NOT NULL,
  check_in        DATE          NOT NULL,
  check_out       DATE          NOT NULL,
  nights          INT           NOT NULL,
  price_per_night DECIMAL(10,2) NOT NULL,
  total_amount    DECIMAL(12,2) NOT NULL,
  status          VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED',
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancelled_at    DATETIME      NULL,
  CONSTRAINT uq_booking_reference UNIQUE (reference),
  CONSTRAINT fk_booking_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id),
  CONSTRAINT chk_booking_dates CHECK (check_out > check_in),
  INDEX idx_booking_email (guest_email)
);