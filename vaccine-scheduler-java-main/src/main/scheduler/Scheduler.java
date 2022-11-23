package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_Patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the Patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save the patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // check if a user is logged in, can either be patient or caregiver
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];

        // filter the caregivers for the given date
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String caregiverAvail = "SELECT Username FROM Availabilities WHERE Time = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(caregiverAvail);
            statement.setString(1, date);
            ResultSet resultSet = statement.executeQuery();

            // Output the username for the caregivers that are available for the date
            System.out.println("The caregivers that are available for this date are: ");
            while (resultSet.next()) {
                System.out.print(resultSet.getString("Username") + " ");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking date.");
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        // output the number of available doses left for each vaccine
        cm = new ConnectionManager();
        con = cm.createConnection();

        String vaccineLeft = "SELECT * FROM Vaccines;";

        try {
            PreparedStatement statement = con.prepareStatement(vaccineLeft);
            ResultSet resultSet = statement.executeQuery();

            System.out.println(" The number of available does left for each vaccine: ");
            while (resultSet.next()) {
                System.out.print("Name of Vaccine: " + resultSet.getString("Name") + " ");
                System.out.print("Doses Left: " + resultSet.getString("Doses") + " ");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking vaccines.");
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // Patients perform this operation to reserve an appointment
        if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        // Check: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        // Variables setup
        String reserveDate = tokens[1];
        String vaccineName = tokens[2];
        String caregiverName = "";
        int availVaccine = 0;
        Date d = null;

        // Verify the date
        try {
            d = Date.valueOf(reserveDate);
        } catch(IllegalArgumentException e) {
            System.out.println("Having a invalid date");
        }
        if (d == null) {
            System.out.println("Please enter a valid date in the form 'yyyy-mm-dd");
            return;
        }

        // build the connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String vaccineCheck = "SELECT * FROM Vaccines AS V WHERE V.Name = ?;";
        String reserveInfo = "SELECT TOP 1 * FROM Availabilities WHERE Time = ?;";
        try {
            try {
                // conduct a vaccine check of doses availability
                PreparedStatement statement = con.prepareStatement(vaccineCheck);
                statement.setString(1, vaccineName);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    availVaccine = resultSet.getInt("Doses");
                    if (availVaccine <= 0) {
                        System.out.println("Not enough available doses!");
                    }
                } else {
                    System.out.println("Please enter a valid vaccine name");
                    return;
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when checking vaccines.");
                System.out.println("Please try again!");
                e.printStackTrace();
            }
            try {
                // find a caregiver
                PreparedStatement statement = con.prepareStatement(reserveInfo);
                statement.setString(1, reserveDate);
                ResultSet resultSet = statement.executeQuery();

                // choose the caregiver by alphabetical order
                if (resultSet.next()) {
                    caregiverName = resultSet.getString("Username");
                } else {
                    System.out.println("No Caregiver is available!");
                    return;
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when assigning the caregiver.");
                System.out.println("Please try again!");
                e.printStackTrace();
            }
        } finally {
            cm.closeConnection();
        }

        // Output the appointment ID
        // build the connection
        cm = new ConnectionManager();
        con = cm.createConnection();
        int newID = 0;
        int max = -1;

        // Search for the current max ID
        String maxID = "SELECT * FROM Appointment;";
        try {
            PreparedStatement statement = con.prepareStatement(maxID);
            // obtain the maxID
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                max = 0;;
            } else {
                while (resultSet.next()) {
                    newID = resultSet.getInt("ID");
                    if (newID > max) {
                        max = newID;
                    }
                }
                // max + 1 will be the appointment ID
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when setting the appointment ID.");
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        // Insert the newly created appointment
        // Get the current patient name
        String PatientName = currentPatient.getUsername();

        cm = new ConnectionManager();
        con = cm.createConnection();

        String makeappointment = "INSERT INTO Appointment VALUES (?, ?, ?, ?, ?);";
        // Begin Insertion
        try {
            PreparedStatement statement = con.prepareStatement(makeappointment);
            statement.setString(1, Integer.toString(max+1));
            statement.setString(2, reserveDate);
            statement.setString(3, caregiverName);
            statement.setString(4, PatientName);
            statement.setString(5, vaccineName);
            statement.executeUpdate();
            // decrease the vaccine storage by 1
            try {
                Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                vaccine.decreaseAvailableDoses(1);
            } catch (SQLException e) {
                System.out.println("Error occurred when updating vaccine doses.");
                System.out.println("Please try again!");
                e.printStackTrace();
            }
            // remove the availability for the caregiver on that day
            try {
                removeAvailability(d, caregiverName);
            } catch (SQLException e) {
                System.out.println("Error occurred when removing caregiver availability.");
                System.out.println("Please try again!");
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when updating appointments.");
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        System.out.println("Congrats, you have successfully made an appointment!");
        System.out.println("Your appointment ID is: " + Integer.toString(max+1));
        System.out.println("Your assigned caregiver is: " + caregiverName);
    }

    public static void removeAvailability(Date d, String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String decAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(decAvailability);
            statement.setDate(1, d);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        String appointmentID = tokens[1];

        // build the connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // load the appointment info
        String checkAppointment = "SELECT * FROM Appointment WHERE ID = ?;";
        String cancelAppointment = "DELETE FROM Appointment WHERE ID = ?;";
        String increAvailability = "INSERT INTO Availabilities VALUES (?, ?);";
        try {
            PreparedStatement statement = con.prepareStatement(checkAppointment);
            statement.setString(1, appointmentID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String patientName = resultSet.getString("Username_P");
                // caregivers can cancel any appointments, while patients can only cancel their own 
                if ((currentPatient != null && currentPatient.getUsername() == patientName) || (currentCaregiver != null)) {
                    String vaccineName = resultSet.getString("Name_V");
                    String caregiverName = resultSet.getString("Username_C");
                    String time = resultSet.getString("AppointmentTime");
                    // increase the vaccine storage by 1
                    Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                    vaccine.increaseAvailableDoses(1);
                    // add the caregiver back to the availabilities table
                    statement = con.prepareStatement(increAvailability);
                    statement.setString(1, time);
                    statement.setString(2, caregiverName);
                    statement.executeUpdate();
                    // delete the appointment
                    statement = con.prepareStatement(cancelAppointment);
                    statement.setString(1, appointmentID);
                    statement.executeUpdate();
                } else {
                    System.out.println("Sorry, you need permission");
                    return;
                }
            } else {
                System.out.println("The appointment does not exist!");
                return;
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        System.out.println("appointment has been successfully canceled!");
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // check if a user is logged in, can either be patient or caregiver
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        // variables setup
        List<Integer> appointmentID = new ArrayList<>();
        List<String> vaccineName = new ArrayList<>();
        List<Date> date = new ArrayList<>();

        // build the connection
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentCaregiver != null) { //  for caregivers
            List<String> patientName = new ArrayList<>();
            String curName = currentCaregiver.getUsername();
            String AppointmentInfo = "SELECT * FROM Appointment WHERE Username_C = ?;";
            try {
                PreparedStatement statement = con.prepareStatement(AppointmentInfo);
                statement.setString(1, curName);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    appointmentID.add(resultSet.getInt("ID"));
                    vaccineName.add(resultSet.getString("Name_V"));
                    date.add(resultSet.getDate("AppointmentTime"));
                    patientName.add(resultSet.getString("Username_P"));
                }
                for (int i = 0; i < patientName.size(); i++) {
                    System.out.print("Appointment ID: " + appointmentID.get(i) + " ");
                    System.out.print("Vaccine Name: " + vaccineName.get(i) + " ");
                    System.out.print("Date: " + date.get(i) + " ");
                    System.out.println("Patient name: " + patientName.get(i) + " ");
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } else if (currentPatient != null) { // for patients
            List<String> caregiverName = new ArrayList<>();
            String curName = currentPatient.getUsername();
            String AppointmentInfo = "SELECT * FROM Appointment WHERE Username_P = ?;";
            try {
                PreparedStatement statement = con.prepareStatement(AppointmentInfo);
                statement.setString(1, curName);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    appointmentID.add(resultSet.getInt("ID"));
                    vaccineName.add(resultSet.getString("Name_V"));
                    date.add(resultSet.getDate("AppointmentTime"));
                    caregiverName.add(resultSet.getString("Username_C"));
                }
                for (int i = 0; i < caregiverName.size(); i++) {
                    System.out.print("Appointment ID: " + appointmentID.get(i) + " ");
                    System.out.print("Vaccine Name: " + vaccineName.get(i) + " ");
                    System.out.print("Date: " + date.get(i) + " ");
                    System.out.println("Caregiver name: " + caregiverName.get(i) + " ");
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // check if the user is in login status or not
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
        // Note: this method cannot have an error
    }
}
