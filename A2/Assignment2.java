/* 
 * This code is provided solely for the personal and private use of students 
 * taking the CSC343H course at the University of Toronto. Copying for purposes 
 * other than this use is expressly prohibited. All forms of distribution of 
 * this code, including but not limited to public repositories on GitHub, 
 * GitLab, Bitbucket, or any other online platform, whether as given or with 
 * any changes, are expressly prohibited. 
*/

import java.sql.*;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.lang.*;

public class Assignment2 {
   /////////
   // DO NOT MODIFY THE VARIABLE NAMES BELOW.

   // A connection to the database
   Connection connection;

   // Can use if you wish: seat letters
   List<String> seatLetters = Arrays.asList("A", "B", "C", "D", "E", "F");

   // private int executeUpdate;

   Assignment2() throws SQLException {
      try {
         Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      }
   }

   /**
    * Connects and sets the search path.
    *
    * Establishes a connection to be used for this session, assigning it to the
    * instance variable 'connection'. In addition, sets the search path to
    * 'air_travel, public'.
    *
    * @param url      the url for the database
    * @param username the username to connect to the database
    * @param password the password to connect to the database
    * @return true if connecting is successful, false otherwise
    */
   public boolean connectDB(String URL, String username, String password) {
      // Implement this method!
      try {
         /* return a connection object to the database server */
         connection = DriverManager.getConnection(URL, username, password);

         /* string query to set the search path to the air_travel database */
         String set_path = "SET search_path TO air_travel, public";

         PreparedStatement path_stat = connection.prepareStatement(set_path);
         path_stat.executeUpdate();
         return true;
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return false;
   }

   /**
    * Closes the database connection.
    *
    * @return true if the closing was successful, false otherwise
    */
   public boolean disconnectDB() {
      // Implement this method!
      try {
         connection.close();
         return true;
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return false;
   }

   /* ======================= Airline-related methods ======================= */

   /**
    * Attempts to book a flight for a passenger in a particular seat class. Does so
    * by inserting a row into the Booking table.
    *
    * Read handout for information on how seats are booked. Returns false if seat
    * can't be booked, or if passenger or flight cannot be found.
    *
    * 
    * @param passID    id of the passenger
    * @param flightID  id of the flight
    * @param seatClass the class of the seat (economy, business, or first)
    * @return true if the booking was successful, false otherwise.
    */
   public boolean bookSeat(int passID, int flightID, String seatClass) {
      // Implement this method!

      /**
       * check if the passenger exists in the passenger table using the id attribute
       */
      try {
         /** attempt to get a tuple from passengers with id = passID */
         PreparedStatement checkPassengerExists = connection
               .prepareStatement("SELECT * FROM Passenger WHERE Passenger.id = ?");
         /** set the passenger id to be passID */
         checkPassengerExists.setInt(1, passID);
         /** Execute query to return the ResultSet Object */
         ResultSet passengerInfo = checkPassengerExists.executeQuery();
         /** check if passengerInfo has anything */
         if (!passengerInfo.next()) {
            return false; /** passengerInfo is empty */
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

      /** check if the flight exists in the flight table */
      try {
         /** create query statement to retrieve tuple for flight id = flightID */
         PreparedStatement checkFlightExists = connection.prepareStatement("SELECT * FROM Flight WHERE Flight.id = ?");
         /** set the flight.id = flightID */
         checkFlightExists.setInt(1, flightID);
         /** Execute query to return ResultSet flight flighID */
         ResultSet flightInfo = checkFlightExists.executeQuery();
         /** check if flightInfo is empty */
         if (!flightInfo.next()) {
            return false; /** flightInfo is empty */
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }

      /** attempt to book passenger passID on flight flightID with class seatClass */
      try {
         // get a relation with the capacities and bookings per class for this flight
         PreparedStatement getCapBookings = connection.prepareStatement(
               "SELECT Flight.id, Plane.capacity_economy, Plane.capacity_business, Plane.capacity_first, "
                     + "SUM(CASE seat_class WHEN 'economy' THEN 1 else 0 END) AS economy_count, "
                     + "SUM(CASE seat_class WHEN 'business' THEN 1 else 0 END) AS business_count, "
                     + "SUM(CASE seat_class WHEN 'first' THEN 1 else 0 END) AS first_count "
                     + "FROM Flight JOIN Plane ON Flight.airline = Plane.airline AND Flight.plane = Plane.tail_number "
                     + "LEFT JOIN Booking ON Flight.id = Booking.flight_id " + "WHERE Flight.id = ? "
                     + "GROUP BY Flight.id, Plane.capacity_economy, Plane.capacity_business, Plane.capacity_first");

         /** set the seatClass in preparedstatement */
         getCapBookings.setInt(1, flightID);

         /**
          * Evaluate the query to get the bookings and capacities per class per flight
          */
         ResultSet flightInfo = getCapBookings.executeQuery();
         flightInfo.next();

         /** get the price of the future booking */
         PreparedStatement getflightPrice = connection.prepareStatement("SELECT * FROM price WHERE flight_id = ?");
         getflightPrice.setInt(1, flightID);
         ResultSet price_tuple = getflightPrice.executeQuery();
         price_tuple.next();
         int seat_price = price_tuple.getInt(seatClass);

         /** get the starting row numbers for each class */
         int first = 1;
         int business = (int) Math.ceil((flightInfo.getInt("capacity_first")) / 6.0) + 1;
         int economy = (int) Math.ceil((flightInfo.getInt("capacity_first")) / 6.0)
               + (int) Math.ceil((flightInfo.getInt("capacity_business")) / 6.0) + 1;

         /** attempt to book the passenger by inserting info into booking table */
         if (seatClass == "first") {
            /** check if first class if already full */
            if (flightInfo.getInt("capacity_first") - flightInfo.getInt("first_count") <= 0) {
               return false;
            } else {
               PreparedStatement bookFirst = connection.prepareStatement(
                     "INSERT INTO Booking VALUES((SELECT max(id) FROM Booking) + 1, ?, ?, ?, ?, ?::seat_class, ?, ?)");

               /** set the appropriate values */
               bookFirst.setInt(1, passID);
               bookFirst.setInt(2, flightID);
               bookFirst.setTimestamp(3, getCurrentTimeStamp());
               bookFirst.setInt(4, seat_price);
               bookFirst.setString(5, seatClass);

               /** get the row number of the booking */
               int first_row = first + (int) Math.floor(flightInfo.getInt("first_count") / 6.0);

               /** get the offset, which determines the seat in row */
               int first_offset = flightInfo.getInt("first_count") % 6;

               /** set the remaining values in bookFirst statement */
               bookFirst.setInt(6, first_row);
               bookFirst.setString(7, seatLetters.get(first_offset));

               /** execute update to Booking */
               bookFirst.executeUpdate();

               return true;
            }
         } else if (seatClass == "business") {
            /** check if business clas is full */
            if (flightInfo.getInt("capacity_business") - flightInfo.getInt("business_count") <= 0) {
               return false;
            } else {
               PreparedStatement bookBusiness = connection.prepareStatement(
                     "INSERT INTO Booking VALUES((SELECT max(id) FROM Booking) + 1, ?, ?, ?, ?, ?::seat_class, ?, ?)");

               /** set the appropriate values */
               bookBusiness.setInt(1, passID);
               bookBusiness.setInt(2, flightID);
               bookBusiness.setTimestamp(3, getCurrentTimeStamp());
               bookBusiness.setInt(4, seat_price);
               bookBusiness.setString(5, seatClass);
               /** get the row number of the booking */
               int bus_row = business + (int) Math.floor(flightInfo.getInt("business_count") / 6.0);

               /** get the offset, which determines the seat in row */
               int bus_offset = flightInfo.getInt("business_count") % 6;

               /** set the remaining values in bookFirst statement */
               bookBusiness.setInt(6, bus_row);
               bookBusiness.setString(7, seatLetters.get(bus_offset));

               /** execute update to Booking */
               bookBusiness.executeUpdate();

               return true;
            }
         } else if (seatClass == "economy") {
            /** check if booking is being attempted beyond an overbooked economy class */
            if (flightInfo.getInt("capacity_economy") - flightInfo.getInt("economy_count") <= -10) {
               return false;
            }

            /** insert economy class booking for passID on flightID */
            PreparedStatement bookEconomy = connection.prepareStatement(
                  "INSERT INTO Booking VALUES((SELECT max(id) FROM Booking) + 1, ?, ?, ?, ?, ?::seat_class, ?, ?)");

            /** set the appropriare values */
            bookEconomy.setInt(1, passID);
            bookEconomy.setInt(2, flightID);
            bookEconomy.setTimestamp(3, getCurrentTimeStamp());
            bookEconomy.setInt(4, seat_price);
            bookEconomy.setString(5, seatClass);

            /** check if there are any seats left to book without overbooking */
            if (flightInfo.getInt("capacity_economy") - flightInfo.getInt("economy_count") > 0) {

               /** get the row number for booking based on current arrangements */
               int econ_row = economy + (int) Math.floor(flightInfo.getInt("economy_count") / 6.0);

               /** get the offset, which determines the seat in the row */
               int econ_offset = flightInfo.getInt("economy_count") % 6;

               /** set the remaining values in bookEconomy */
               bookEconomy.setInt(6, econ_row);
               bookEconomy.setString(7, seatLetters.get(econ_offset));
            } else {
               bookEconomy.setNull(6, Types.NULL);
               bookEconomy.setNull(7, Types.NULL);
            }
            /** execute update to Booking */
            bookEconomy.executeUpdate();
            return true;
         }

      } catch (SQLException e) {
         e.printStackTrace();
      }

      return false;
   }

   /**
    * Attempts to upgrade overbooked economy passengers to business class or first
    * class (in that order until each seat class is filled). Does so by altering
    * the database records for the bookings such that the seat and seat_class are
    * updated if an upgrade can be processed.
    *
    * Upgrades should happen in order of earliest booking timestamp first.
    *
    * If economy passengers are left over without a seat (i.e. more than 10
    * overbooked passengers or not enough higher class seats), remove their
    * bookings from the database.
    * 
    * @param flightID The flight to upgrade passengers in.
    * @return the number of passengers upgraded, or -1 if an error occured.
    */
   public int upgrade(int flightID) {
      // Implement this method!

      try {
         /*
          * First create a query to find all overbooked passengers and have null values
          * for their seats
          */
         PreparedStatement null_passengers = connection
               .prepareStatement("SELECT pass_id, id " + "FROM Booking " + "WHERE (row IS NULL and letter IS NULL) "
                     + "and seat_class = 'economy' and flight_id = ? " + "ORDER BY datetime");
         /** set the flight.id = flightID */
         null_passengers.setInt(1, flightID);
         ResultSet overbooked_passengers = null_passengers.executeQuery();

         /* Then a query that finds the capacities of first class and business class */
         PreparedStatement capacities = connection.prepareStatement(
               "SELECT capacity_economy, capacity_business, capacity_first " + "FROM flight JOIN plane ON "
                     + "flight.plane = plane.tail_number and flight.airline = plane.airline " + "WHERE flight.id =  ?");
         /** set the flight.id = flightID */
         capacities.setInt(1, flightID);
         ResultSet plane_capacities = capacities.executeQuery();
         plane_capacities.next();

         /*
          * Then Determine the Number of booked seats in first class for the flight
          * (should give us one tupple)
          */
         PreparedStatement booked_first = connection
               .prepareStatement("SELECT max(row) as max_row, count(*) as num_booked_first " + "FROM booking "
                     + "WHERE seat_class ='first' " + "and flight_id =  ?");
         /** set the flight.id = flightID */
         booked_first.setInt(1, flightID);
         ResultSet first_class_booked = booked_first.executeQuery();
         first_class_booked.next();

         /*
          * Then Determine the Number of booked seats in business class for the flight
          * (should give us one tupple)
          */
         PreparedStatement booked_business = connection
               .prepareStatement("SELECT max(row) as max_row, count(*) as num_booked_business " + "FROM booking "
                     + "WHERE seat_class = 'business' " + "and flight_id =  ?");
         /** set the flight.id = flightID */
         booked_business.setInt(1, flightID);
         ResultSet bus_class_booked = booked_business.executeQuery();
         bus_class_booked.next();

         /*
          * Determine how many seats are available for upgrading in business class and
          * first class
          */
         int empty_business_seats = plane_capacities.getInt("capacity_business")- bus_class_booked.getInt("num_booked_business");
         int empty_first_seats = plane_capacities.getInt("capacity_first")- first_class_booked.getInt("num_booked_first");

         /** Determine the starting row for first class and business class */
         int first = 1;
         int business = (int) Math.ceil((plane_capacities.getInt("capacity_first"))/6.0) + 1;

         /** Get the number of passengers booked in business and first class */
         int num_business = bus_class_booked.getInt("num_booked_business");
         int num_first = first_class_booked.getInt("num_booked_first");

         /* Determine the max letter value for first class and business class */

         /* Get the first row value from each of the classes */
         // int row_first_class = first_class_booked.getInt("max_row");

         /* Counter for the number of people upgraded */
         int upgraded_pass = 0;

         while (overbooked_passengers.next()) {
            if (empty_business_seats > 0) {
               PreparedStatement update_business = connection.prepareStatement(
                     "UPDATE booking " + "SET seat_class = 'business', row = ?, letter = ? " + "WHERE id = ?");

               /* Get the offset of the business class seat letter */
               // int offset_business = bus_class_booked.getInt("num_booked_business") % 6;
               int offset_business = num_business % 6;
               /* Get the row off the business class */
               // int row_bus_class = bus_class_booked.getInt("max_row");
               // int row_bus_class = business + (int) Math.floor((bus_class_booked.getInt("num_booked_business")) / 6.0);
               int row_bus_class = business + (int) Math.floor((num_business) / 6.0);

               /* Set the values in the query above to update the bookings table */
               // if (offset_business == 0) {
               //    row_bus_class++;
               // }

               update_business.setInt(1, row_bus_class);
               update_business.setString(2, seatLetters.get(offset_business));
               update_business.setInt(3, overbooked_passengers.getInt("id"));
               update_business.executeUpdate();

               empty_business_seats--;
               upgraded_pass++;
               num_business++;
            }

            else if (empty_first_seats > 0) {
               PreparedStatement update_first = connection.prepareStatement(
                     "UPDATE booking " + "SET seat_class = 'first', row = ?, letter = ? " + "WHERE id = ?");

               /* Get the offset of the business class seat letter */
               // int offset_first = first_class_booked.getInt("num_booked_first") % 6;
               int offset_first = num_first % 6;

               /* Get the row off the business class */
               // int row_first_class = bus_class_booked.getInt("max_row");
               // int row_first_class = first + (int) Math.floor((first_class_booked.getInt("num_booked_first")) / 6.0);
               int row_first_class = first + (int) Math.floor(num_first / 6.0);

               /* Set the values in the query above to update the bookings table */
               // if (offset_first == 0) {
               //    row_first_class++;
               // }

               update_first.setInt(1, row_first_class);
               update_first.setString(2, seatLetters.get(offset_first));
               update_first.setInt(3, overbooked_passengers.getInt("id"));
               update_first.executeUpdate();

               empty_first_seats--;
               upgraded_pass++;
               num_first++;

            }
            /*
             * Remove passenger from booking if there is no more empty first class or
             * business class seats
             */
            else {
               PreparedStatement remove_pass = connection.prepareStatement("DELETE FROM booking " + "WHERE id = ?");

               /* Set the Values */
               remove_pass.setInt(1, overbooked_passengers.getInt("id"));
               remove_pass.executeUpdate();

            }
         }
         return upgraded_pass;

      } catch (SQLException e) {
         e.printStackTrace();
      }

      return -1;

   }

   /* ----------------------- Helper functions below ------------------------- */

   // A helpful function for adding a timestamp to new bookings.
   // Example of setting a timestamp in a PreparedStatement:
   // ps.setTimestamp(1, getCurrentTimeStamp());

   /**
    * Returns a SQL Timestamp object of the current time.
    * 
    * @return Timestamp of current time.
    */
   private java.sql.Timestamp getCurrentTimeStamp() {
      java.util.Date now = new java.util.Date();
      return new java.sql.Timestamp(now.getTime());
   }

   // Add more helper functions below if desired.

   /* ----------------------- Main method below ------------------------- */

   public static void main(String[] args) {
      // You can put testing code in here. It will not affect our autotester.
      System.out.println("Running the code!");
      try {
         /** create an assignment2 object */
         Assignment2 assignment2 = new Assignment2();
         System.out.println("Created Assignment 2 objects\n");

         /** connect to the database */
         System.out.println("Connecting to Database\n");
         boolean connect = assignment2.connectDB("jdbc:postgresql://localhost:5432/csc343h-seunari1", "seunari1", "");
         System.out.println(connect);

         /**
          * TEST 1 add Bob Ross to flight 10
          */
         // boolean ret = assignment2.bookSeat(7, 10, "economy");
         // System.out.println(ret);

         /**
          * TEST 2 upgrade flight 10 Expected : Jaqueline (PassID 6) upgraded to Business
          * 2B Bob Ross(PassID 7) cannot be upgraded, therefore, removed Return 1
          */
         // int ret = assignment2.upgrade(10);
         // System.out.println(ret);

         /**
          * TEST 3 Add 12 passengers from Bob Ross (7) to Bruce Banner (18) to Flight 6
          * Economy Class Expected: Seats should be filled from 25A, 25B....26F Returns
          * 12 true boolean from 12 bookings
          */
         // boolean ret1 = assignment2.bookSeat(7, 6, "economy");
         // System.out.println(ret1);
         // boolean ret2 = assignment2.bookSeat(8, 6, "economy");
         // System.out.println(ret2);
         // boolean ret3 = assignment2.bookSeat(9, 6, "economy");
         // System.out.println(ret3);
         // boolean ret4 = assignment2.bookSeat(10, 6, "economy");
         // System.out.println(ret4);
         // boolean ret5 = assignment2.bookSeat(11, 6, "economy");
         // System.out.println(ret5);
         // boolean ret6 = assignment2.bookSeat(12, 6, "economy");
         // System.out.println(ret6);
         // boolean ret7 = assignment2.bookSeat(13, 6, "economy");
         // System.out.println(ret7);
         // boolean ret8 = assignment2.bookSeat(14, 6, "economy");
         // System.out.println(ret8);
         // boolean ret9 = assignment2.bookSeat(15, 6, "economy");
         // System.out.println(ret9);
         // boolean ret10 = assignment2.bookSeat(16, 6, "economy");
         // System.out.println(ret10);
         // boolean ret11 = assignment2.bookSeat(17, 6, "economy");
         // System.out.println(ret11);
         // boolean ret12 = assignment2.bookSeat(18, 6, "economy");
         // System.out.println(ret12);

         /**
          * TEST 4 Upgrade all passengers from TEST 3 from economy to business TRICK:
          * There should not be anyone to upgrade in the first RETURN: upgraded should be
          * 0
          */
         // int ret = assignment2.upgrade(6);
         // System.out.println(ret);

         /** TEST 5 Add 23 passengers to flight 11
          *  Flight 11 capacity for economy is 13 
          *  Adding 23 people means 13 are assigned seats from 4A, 4B, 4C.....6A
          *  The remaining 10 shall have Nulls
          */
         // boolean ret1 = assignment2.bookSeat(7, 11, "economy");
         // System.out.println(ret1);
         // boolean ret2 = assignment2.bookSeat(8, 11, "economy");
         // System.out.println(ret2);
         // boolean ret3 = assignment2.bookSeat(9, 11, "economy");
         // System.out.println(ret3);
         // boolean ret4 = assignment2.bookSeat(10, 11, "economy");
         // System.out.println(ret4);
         // boolean ret5 = assignment2.bookSeat(11, 11, "economy");
         // System.out.println(ret5);
         // boolean ret6 = assignment2.bookSeat(12, 11, "economy");
         // System.out.println(ret6);
         // boolean ret7 = assignment2.bookSeat(13, 11, "economy");
         // System.out.println(ret7);
         // boolean ret8 = assignment2.bookSeat(14, 11, "economy");
         // System.out.println(ret8);
         // boolean ret9 = assignment2.bookSeat(15, 11, "economy");
         // System.out.println(ret9);
         // boolean ret10 = assignment2.bookSeat(16, 11, "economy");
         // System.out.println(ret10);
         // boolean ret11 = assignment2.bookSeat(17, 11, "economy");
         // System.out.println(ret11);
         // boolean ret12 = assignment2.bookSeat(18, 11, "economy");
         // System.out.println(ret12);
         // boolean ret13 = assignment2.bookSeat(19, 11, "economy");
         // System.out.println(ret13);
         // boolean ret14 = assignment2.bookSeat(20, 11, "economy");
         // System.out.println(ret14);
         // boolean ret15 = assignment2.bookSeat(21, 11, "economy");
         // System.out.println(ret15);
         // boolean ret16 = assignment2.bookSeat(22, 11, "economy");
         // System.out.println(ret16);
         // boolean ret17 = assignment2.bookSeat(23, 11, "economy");
         // System.out.println(ret17);
         // boolean ret18 = assignment2.bookSeat(24, 11, "economy");
         // System.out.println(ret18);
         // boolean ret19 = assignment2.bookSeat(25, 11, "economy");
         // System.out.println(ret19);
         // boolean ret20 = assignment2.bookSeat(26, 11, "economy");
         // System.out.println(ret20);
         // boolean ret21 = assignment2.bookSeat(27, 11, "economy");
         // System.out.println(ret21);
         // boolean ret22 = assignment2.bookSeat(28, 11, "economy");
         // System.out.println(ret22);
         // boolean ret23 = assignment2.bookSeat(29, 11, "economy");
         // System.out.println(ret23);

         /**
          * TEST 6: Attempt to upgrade flight 11, continuing from 
          * TEST 5. 
          EXPECTED: Last 10 Nulls should be upgraded to business class 
          on seats 2A, 2B, 2C, 2D, 2E, 2F, 3A
          and the remaining 3 should be upgraded to first class on seats
          1A, 1B, 1C
          EXPECTED OUTPUT: 5 (upgrades)
         //  */
         int ret = assignment2.upgrade(11);
         System.out.println(ret);

         /** disconnect from the database */
         boolean disconnect = assignment2.disconnectDB();
         System.out.println(disconnect);

      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

}