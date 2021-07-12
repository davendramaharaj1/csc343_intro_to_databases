-- Q2. Refunds!

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q2 CASCADE;


CREATE TABLE q2 (
    airline CHAR(2),
    name VARCHAR(50),
    year CHAR(4),
    seat_class seat_class,
    refund REAL
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS Refunds CASCADE;
DROP VIEW IF EXISTS BookingRefunds CASCADE;
DROP VIEW IF EXISTS BookingRefundInfo CASCADE;
DROP VIEW IF EXISTS LabelledRefundFlights CASCADE;
DROP VIEW IF EXISTS RefundFlightsCountries CASCADE;
DROP VIEW IF EXISTS InboundRefundFlights CASCADE;
DROP VIEW IF EXISTS RefundFlights CASCADE;
DROP VIEW IF EXISTS AllFlights CASCADE;
DROP VIEW IF EXISTS DepartureFlights CASCADE;

-- Define views for your intermediate steps here:

-- get all departure time of actual flights with the departure delay
CREATE VIEW DepartureFlights AS
SELECT flight.id as flight_id, airline, inbound, outbound, s_dep, s_arv, departure.datetime as actual_departure, 
        EXTRACT(YEAR from departure.datetime) as year, EXTRACT(HOUR FROM departure.datetime - s_dep) as departure_delay
FROM flight JOIN departure ON flight.id = departure.flight_id;

-- get arrival times of all flights 
CREATE VIEW AllFlights AS
SELECT DepartureFlights.flight_id, airline, inbound, outbound, s_dep, s_arv, actual_departure, Arrival.datetime as actual_arrival,
        year, departure_delay, EXTRACT(HOUR FROM Arrival.datetime - s_arv) as arrival_delay
FROM DepartureFlights JOIN Arrival ON DepartureFlights.flight_id = Arrival.flight_id;

-- get the flight to be refunded
CREATE VIEW RefundFlights AS
SELECT flight_id, airline, inbound, outbound, year, departure_delay, arrival_delay
FROM AllFlights
WHERE departure_delay >= 5 AND arrival_delay > CAST(departure_delay/2 AS REAL);

-- get the countries of inbound airports
CREATE VIEW InboundRefundFlights AS
SELECT flight_id, airline, inbound, outbound, airport.country as in_country, year,
        departure_delay, arrival_delay
FROM RefundFlights JOIN airport ON RefundFlights.inbound = airport.code;

-- get the coutnries of outbound airports
CREATE VIEW RefundFlightsCountries AS
SELECT flight_id, airline, inbound, outbound, in_country, airport.country as out_country,
        year, departure_delay, arrival_delay
FROM InboundRefundFlights JOIN airport ON InboundRefundFlights.outbound = airport.code;

-- label each refund flight as domestic or international
CREATE VIEW LabelledRefundFlights AS
SELECT flight_id, airline, inbound, outbound, in_country, out_country, year, departure_delay,
        CASE
            WHEN in_country = out_country THEN 'DOMESTIC'
            ELSE 'INTERNATIONAL'
        END Flight_Type
FROM RefundFlightsCountries;

-- get the booking with the refund flights to acquire refund information with the airline name
CREATE VIEW BookingRefundInfo AS
SELECT Booking.flight_id, airline, Airline.name as airline_name, Booking.price as price, Booking.seat_class as seat_class, 
        year, departure_delay, flight_type
FROM LabelledRefundFlights JOIN Booking ON LabelledRefundFlights.flight_id = Booking.flight_id
    JOIN Airline ON LabelledRefundFlights.airline = Airline.code;

--get the refunds per booking for airlines where refund depends on the flight type
CREATE VIEW BookingRefunds AS
SELECT flight_id, airline, airline_name, year, seat_class, price, 
        (CASE
            WHEN flight_type = 'DOMESTIC' AND departure_delay >= 5 AND departure_delay < 10 THEN 0.35 * price
            WHEN flight_type = 'DOMESTIC' AND departure_delay >= 10  THEN 0.5 * price
            WHEN flight_type = 'INTERNATIONAL' AND departure_delay >= 8 AND departure_delay < 12 THEN 0.35 * price
            WHEN flight_type = 'INTERNATIONAL' AND departure_delay >= 12 THEN 0.5 * price
        END) refund_price
FROM BookingRefundInfo;

-- show the refunds per flight on a particular year for a certain seat class
CREATE VIEW Refunds AS
SELECT airline, airline_name, year, seat_class, sum(refund_price) as refund
FROM BookingRefunds
GROUP BY airline, airline_name, year, seat_class;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q2
SELECT airline, airline_name as name, year, seat_class, refund FROM Refunds;