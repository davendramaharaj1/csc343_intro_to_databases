-- Q1. Airlines

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q1 CASCADE;

CREATE TABLE q1 (
    pass_id INT,
    name VARCHAR(100),
    airlines INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS answer1 CASCADE;
DROP VIEW IF EXISTS answer CASCADE;
DROP VIEW IF EXISTS departedFlights CASCADE;
DROP VIEW IF EXISTS PassengerBookings CASCADE;

-- Define views for your intermediate steps here:
create view PassengerBookings AS
select firstname, surname, pass_id, flight_id
from passenger JOIN booking ON passenger.id = booking.pass_id;

create view departedFlights as 
select flight_id, airline
from departure JOIN flight ON departure.flight_id=flight.id;

create view answer as
select firstname, surname, pass_id, airline
from PassengerBookings LEFT JOIN  departedFlights ON PassengerBookings.flight_id = departedFlights.flight_id; 
-- group by PassengerBookings.pass_id; 

create view answer1 as
select pass_id, concat(firstname,' ', surname) as name, count(distinct airline) AS airlines 
from answer
group by pass_id, firstname, surname;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q1
SELECT pass_id, name, airlines FROM answer1;
