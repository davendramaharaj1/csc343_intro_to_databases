-- Q5. Flight Hopping

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q5 CASCADE;

CREATE TABLE q5 (
	destination CHAR(3),
	num_flights INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS day CASCADE;
DROP VIEW IF EXISTS n CASCADE;

CREATE VIEW day AS
SELECT day::date as day FROM q5_parameters;
-- can get the given date using: (SELECT day from day)

CREATE VIEW n AS
SELECT n FROM q5_parameters;
-- can get the given number of flights using: (SELECT n from n)

-- HINT: You can answer the question by writing one recursive query below, without any more views.
-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q5

WITH RECURSIVE flightHopping AS(
    (
    -- get all the flights leaving from YYZ with the departure and arrival times for checking < 24 hours
    SELECT inbound as destination, s_dep, s_arv, count(id) AS num_flights
    FROM Flight
    -- ensure outbound airport is YYZ and the date of departure is day FROM day table above
    WHERE outbound ='YYZ' AND CAST(s_dep AS date) = (SELECT day FROM day)
    -- group all flights by the destination and departure/arrival times
    GROUP BY destination, s_dep, s_arv
    )

    UNION ALL

    (
    -- recursive call to the CTE to match the airport destinations from last iteration as
    -- oubound flights in the current iteration
    SELECT flight.inbound as destination, flight.s_dep, flight.s_arv, num_flights + 1
    FROM flightHopping JOIN flight ON flightHopping.destination = flight.outbound
    WHERE num_flights < (SELECT n FROM n) AND
         -- ensure that the next flight being hopped on is scheduled to depart < 24 AFTER arrival  
         (EXTRACT(EPOCH FROM flight.s_dep - flightHopping.s_arv)/3600) < 24 AND
         -- flight departing after arriving should not have left as yet else diff is negative
         (EXTRACT(EPOCH FROM flight.s_dep - flightHopping.s_arv)/3600) > 0
    )
)

SELECT destination, num_flights FROM flightHopping;
















