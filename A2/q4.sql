-- Q4. Plane Capacity Histogram

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q4 CASCADE;

CREATE TABLE q4 (
	airline CHAR(2),
	tail_number CHAR(5),
	very_low INT,
	low INT,
	fair INT,
	normal INT,
	high INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS answer CASCADE;
DROP VIEW IF EXISTS count_values CASCADE;
DROP VIEW IF EXISTS percent_full CASCADE;
DROP VIEW IF EXISTS num_passengers CASCADE;
DROP VIEW IF EXISTS plane_capacity CASCADE;

-- Define views for your intermediate steps here:
-- Create a view that has the capacity of the place (id is flight_id)
CREATE VIEW plane_capacity AS 
SELECT id, (capacity_economy + capacity_business + capacity_first) as plane_capacity, plane.airline, plane.tail_number
FROM plane JOIN flight ON plane.airline = flight.airline and plane.tail_number = flight.plane; 

-- Create a view that holds the number of passengers in each flight that departed 
CREATE VIEW num_passengers AS 
SELECT departure.flight_id as id, count(booking.pass_id) as total_passengers
FROM departure LEFT JOIN booking ON departure.flight_id = booking.flight_id
GROUP BY departure.flight_id;  

-- Create a view for all the numerical capacity in percentage 
CREATE VIEW percent_full AS 
SELECT DISTINCT plane_capacity.id, airline, tail_number,
    CASE 
		WHEN (total_passengers::decimal/ plane_capacity::decimal) >= 0.0 
			and (total_passengers::decimal/ plane_capacity::decimal) < 0.2 THEN 'very low'
		WHEN (total_passengers::decimal/ plane_capacity::decimal) >= 0.2 
			and (total_passengers::decimal/ plane_capacity::decimal) < 0.4 THEN 'low'
		WHEN (total_passengers::decimal/ plane_capacity::decimal) >= 0.4 
			and (total_passengers::decimal/ plane_capacity::decimal) < 0.6 THEN 'fair'
		WHEN (total_passengers::decimal/ plane_capacity::decimal) >= 0.6 
			and (total_passengers::decimal/ plane_capacity::decimal) < 0.8 THEN 'normal'
		WHEN (total_passengers::decimal/ plane_capacity::decimal) >= 0.8 THEN 'high'	
	END
    as capacity_labels
FROM plane_capacity JOIN num_passengers ON plane_capacity.id = num_passengers.id; 

-- Create a view that will convert each rating to a numerical value for counting them 
CREATE VIEW count_values as 
SELECT plane_capacity.airline as airline, plane_capacity.tail_number as tail_number, 
	-- very low capacity
	CASE 
		WHEN capacity_labels = 'very low' THEN 1
		ELSE 0
	END AS very_low,
	-- low capacity
	CASE 
		WHEN capacity_labels = 'low' THEN 1
		ELSE 0
	END AS low,
	-- fair capacity
	CASE
		WHEN capacity_labels = 'fair' THEN 1
		ELSE 0
	END AS fair,
	-- normal capacity
	CASE
		WHEN capacity_labels = 'normal' THEN 1
		ELSE 0
	END AS normal,
	-- High capacity
	CASE
		WHEN capacity_labels = 'high' THEN 1
		ELSE 0
	END AS high
FROM plane_capacity LEFT JOIN percent_full ON plane_capacity.id = percent_full.id and
	plane_capacity.airline = percent_full.airline and 
	plane_capacity.tail_number = percent_full.tail_number;

-- Final answer for the plane capacities 
CREATE VIEW answer as
SELECT airline, tail_number, sum(very_low) as very_low, sum(low) as low, sum(fair) as fair, sum(normal) as normal, sum(high) as high
FROM count_values
GROUP BY airline, tail_number; 

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q4
SELECT * FROM answer; 
