USE hotel_booking;

INSERT INTO hotels (name, location, description, amenities, price_per_night, total_rooms) VALUES
('Sea Breeze Residency', 'Mumbai', 'Comfortable sea-facing rooms minutes from Marine Drive and the business district.', 'WiFi,Breakfast,Air Conditioning,Gym', 6500.00, 10),
('Palm Grove Resort', 'Goa', 'Beachside resort with a pool, spa and garden restaurant.', 'WiFi,Pool,Spa,Beach Access,Breakfast', 8200.00, 8),
('Pink City Haveli', 'Jaipur', 'Restored heritage haveli with courtyard dining and rooftop views of the fort.', 'WiFi,Breakfast,Rooftop Restaurant,Cultural Shows', 5400.00, 6),
('Lakeview Palace Inn', 'Udaipur', 'Rooms overlooking Lake Pichola, close to the City Palace.', 'WiFi,Lake View,Breakfast,Airport Pickup', 9100.00, 5),
('Garden City Suites', 'Bengaluru', 'Modern business hotel near tech parks with co-working spaces.', 'WiFi,Co-working,Gym,Breakfast,Parking', 4800.00, 12),
('Himalayan Retreat', 'Manali', 'Cosy mountain lodge with valley views and a bonfire evening every day.', 'WiFi,Heater,Breakfast,Bonfire,Parking', 3900.00, 2);

-- Create inventory for the next 180 days for every hotel
SET SESSION cte_max_recursion_depth = 1000;

INSERT INTO room_inventory (hotel_id, stay_date, total_rooms, booked_rooms)
WITH RECURSIVE days AS (
  SELECT CURDATE() AS d
  UNION ALL
  SELECT d + INTERVAL 1 DAY FROM days WHERE d < CURDATE() + INTERVAL 179 DAY
)
SELECT h.id, days.d, h.total_rooms, 0
FROM hotels h CROSS JOIN days;