drop schema if exists reservation cascade;
create schema reservation;
set search_path to reservation;

-- Reservation(sID, age, length, sName, day, cName, rating, cID) was decomposed into 3 relations 
-- using BCNF Decomposition method. Initially decomposed into R1(sID, sName, rating, age) and R2(sID, cID, cName, length, day)
-- R2 was further decomposed into R1'(cID, cName, length) and R2'(sID, cID, day)

-- First decomposed relation R1 renamed as Skipper to represent captain information
CREATE TABLE Skipper(
    -- autoincrement sID as the primary key
    sID serial PRIMARY KEY,
    sName text NOT NULL, 
    -- add a constraint name for debugging meaningful errors
    rating integer CONSTRAINT ratingRange check(rating in (0,1,2,3,4,5)),
    age integer NOT NULL check age > 0
);

-- Relation to represent the craft which was decomposed into R2' from Reservation
CREATE TABLE Craft(
    -- autoincrement cID as the primary key 
    cID serial PRIMARY KEY,
    cName text NOT NULL,
    length float NOT NULL
);

-- Third relation in BCNF Decomposition. Links the Skipper reserving a Craft on a particular day
CREATE TABLE SkipperReservation(
    sID integer REFERENCES Skipper(sID),
    cID integer REFERENCES Craft(cID),
    day DATE NOT NULL,
    -- ensure that a skipper cannot reserve two crafts on the same day or one craft cannot be
    -- reserved by 2 different skippers on the same day
    CONSTRAINT reserve_constraint PRIMARY KEY (sID, cID, day)
);