-- Q3. North and South Connections

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q3 CASCADE;

CREATE TABLE q3 (
    outbound VARCHAR(30),
    inbound VARCHAR(30),
    direct INT,
    one_con INT,
    two_con INT,
    earliest timestamp
);

-- Do this for each of the views that define your intermediate steps. QUERY 3 
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS city_combos CASCADE;
DROP VIEW IF EXISTS two_con_flights CASCADE;
DROP VIEW IF EXISTS one_con_flights CASCADE;
DROP VIEW IF EXISTS direct_flights CASCADE;
DROP VIEW IF EXISTS relevant_flights CASCADE;


-- Define views for your intermediate steps here:
-- First create a relation with all the required information from Airport and flight 
CREATE VIEW relevant_flights as 
SELECT a1.city as outbound, a2.city as inbound ,a1.country as country_out, a2.country as country_in, flight.s_dep as departure, flight.s_arv as arrival
FROM Airport a1, Airport a2, flight
WHERE a1.code = flight.outbound and a2.code = flight.inbound and flight.outbound != flight.inbound;

-- Then find out all the information for the direct flights that are available ONLY from CANADA and the US 
CREATE VIEW direct_flights as 
SELECT outbound, inbound, arrival as direct_arrival
FROM relevant_flights
WHERE CAST(departure as date) = '2021-04-30' and CAST(arrival as date) = '2021-04-30' and 
      ((country_out = 'Canada' and country_in = 'USA') or (country_out = 'USA' and country_in = 'Canada')); 

-- Then determine all the flights with only one connection
CREATE VIEW one_con_flights as
SELECT r1.outbound, r2.inbound, r2.arrival as one_con_arrival
FROM relevant_flights r1, relevant_flights r2
WHERE CAST(r1.departure as date) = '2021-04-30' and CAST(r1.arrival as date) = '2021-04-30' and 
      CAST(r2.departure as date) = '2021-04-30' and CAST(r2.arrival as date) = '2021-04-30' and 
      CAST (r2.departure AS time) >= (CAST (r1.arrival AS time) + interval '30 minutes') and
      ((r1.country_out = 'Canada' and r2.country_in = 'USA') or (r1.country_out = 'USA' and r2.country_in = 'Canada')) and
      r1.inbound = r2.outbound and
      r1.outbound != r2.inbound; 

-- Then determine all the flights with two connections
CREATE VIEW two_con_flights as
SELECT r1.outbound, r2.inbound, r2.arrival as two_con_arrival
FROM relevant_flights r1, relevant_flights r2, relevant_flights r3
WHERE CAST(r1.departure as date) = '2021-04-30' and CAST(r1.arrival as date) = '2021-04-30' and 
      CAST(r2.departure as date) = '2021-04-30' and CAST(r2.arrival as date) = '2021-04-30' and 
      CAST(r3.departure as date) = '2021-04-30' and CAST(r3.arrival as date) = '2021-04-30' and 
      CAST (r2.departure AS time) >= (CAST (r1.arrival AS time) + interval '30 minutes') and
      CAST (r3.departure AS time) >= (CAST (r2.arrival AS time) + interval '30 minutes') and
      ((r1.country_out = 'Canada' and r2.country_in = 'USA') or (r1.country_out = 'USA' and r2.country_in = 'Canada')) and
      r1.inbound = r2.outbound and
      r2.inbound = r3.outbound and
      r1.outbound != r3.inbound; 

-- Get all combinations of all the cities within the USA and Canada
CREATE VIEW city_combos as
SELECT a1.city as outbound, a2.city as inbound
FROM airport a1 JOIN airport a2 ON a1.city != a2.city and 
    ((a1.country = 'Canada' and a2.country = 'USA') or (a1.country = 'USA' and a2.country = 'Canada')); 

-- Now for the final answer, we must join all three of the prior views we just found. 
CREATE VIEW answer as
SELECT c1.outbound, c1.inbound, count(distinct direct_arrival) as direct, count(distinct one_con_arrival) as one_con, count(two_con_arrival) as two_con,
    CASE 
        -- When all of the times do not exist
		WHEN min(direct_arrival) is NULL and min(one_con_arrival) is NULL and min(two_con_arrival) is NULL THEN NULL
        -- Check only if one of the times exist
        WHEN min(one_con_arrival) is NULL and min(two_con_arrival) is NULL THEN min(direct_arrival)
		WHEN min(direct_arrival) is NULL and min(two_con_arrival) is NULL THEN min(one_con_arrival)
		WHEN min(direct_arrival) is NULL and min(one_con_arrival) is NULL THEN min(two_con_arrival)
        -- Check for two_con_arrival time is always null but compare the direct_arrival times and the one_con_arrival times
		WHEN min(one_con_arrival) >= min(direct_arrival) and min(two_con_arrival) is NULL THEN min(direct_arrival)
		WHEN min(direct_arrival) >= min(one_con_arrival) and min(two_con_arrival) is NULL THEN min(one_con_arrival)
		-- Check for when one_con_arrival time is always null compare the direct_arrival times and the two_con_arrival times
		WHEN min(two_con_arrival) >= min(direct_arrival) and min(one_con_arrival) is NULL THEN min(direct_arrival)
		WHEN min(direct_arrival) >= min(two_con_arrival) and min(one_con_arrival) is NULL THEN min(two_con_arrival)
        -- Check for when direct_arrival time is null and compare the single and double connecting arrival times 
		WHEN min(two_con_arrival) >= min(one_con_arrival) and min(direct_arrival) is NULL THEN min(one_con_arrival)
		WHEN min(one_con_arrival) >= min(two_con_arrival) and min(direct_arrival) is NULL THEN min(two_con_arrival)
        -- Check for when neither of the arrival times are null and we must compare all three with one another
		WHEN min(one_con_arrival) >= min(direct_arrival) and min(two_con_arrival) >= min(direct_arrival) THEN min(direct_arrival)
		WHEN min(direct_arrival) >= min(one_con_arrival) and min(two_con_arrival) >= min(one_con_arrival) THEN min(one_con_arrival)
		WHEN min(direct_arrival) >= min(two_con_arrival) and min(one_con_arrival) >= min(two_con_arrival) THEN min(two_con_arrival)	
	END
    as earliest
FROM city_combos c1 LEFT JOIN direct_flights d1 ON c1.outbound = d1.outbound and c1.inbound = d1.inbound
     LEFT JOIN one_con_flights o1 ON c1.outbound = o1.outbound and c1.inbound = o1.inbound
     LEFT JOIN two_con_flights t1 ON c1.outbound = t1.outbound and c1.inbound = t1.inbound
GROUP BY c1.outbound, c1.inbound; 


-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q3
select * from answer; 
