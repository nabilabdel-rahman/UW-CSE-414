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
        // create the caregiver
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
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
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
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
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement("SELECT A.Username, V.Name, V.Doses " +
                    "FROM Availabilities as A, Vaccines as V WHERE A.Time = '" + d +
                    "' ORDER BY A.username");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                System.out.println("Caregiver name: " + resultSet.getString(1) + " Vaccine name: " +
                        resultSet.getString(2) + " Vaccine doses: " + resultSet.getInt(3));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        String vaccine = tokens[2];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        Date d = Date.valueOf(date);
        String caregiverName = null;
        int doses = 0;
        int appointmentID = 0;
        try {
            PreparedStatement check = con.prepareStatement("SELECT A.Username, V.Doses FROM " +
                    "Availabilities AS A, Vaccines AS V WHERE A.Time = '" + d + "' AND V.Name = '" +
                    vaccine + "' ORDER BY A.Username DESC");
            ResultSet possibilities = check.executeQuery();
            while (possibilities.next()) {
                caregiverName = possibilities.getString(1);
                doses = possibilities.getInt(2);
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
            return;
        }
        if (caregiverName == null) {
            System.out.println("No Caregiver is available!");
            return;
        }
        if (doses <= 0) {
            System.out.println("Not enough available doses!");
            return;
        }
        try {
            PreparedStatement newID = con.prepareStatement("SELECT A.Appointment_id FROM " +
                    "Appointments AS A ORDER BY A.Appointment_ID");
            ResultSet appointmentList = newID.executeQuery();
            while (appointmentList.next() && appointmentID == appointmentList.getInt(1)) {
                appointmentID += 1;
            }
            PreparedStatement removal = con.prepareStatement("DELETE FROM Availabilities " +
                "WHERE Time = '" + d + "' AND Username = '" + caregiverName + "'");
            removal.executeUpdate();
            PreparedStatement schedule = con.prepareStatement("INSERT INTO Appointments VALUES " +
                    "(" + appointmentID + ", '" + caregiverName + "', '" + vaccine + "', '" +
                    d + "', '" + currentPatient.getUsername() + "')");
            schedule.executeUpdate();
            PreparedStatement update = con.prepareStatement("UPDATE Vaccines SET Doses = (DOSES " +
                    "- 1) WHERE Name = '" + vaccine + "'");
            update.executeUpdate();
            System.out.println("Appointment ID: {" + appointmentID + "}, Caregiver username: {" +
                    caregiverName + "}");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
            return;
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
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String appointmentID = tokens[1];
        try {
            PreparedStatement check = con.prepareStatement("SELECT Caregiver_username, " +
                    "Patient_username FROM Appointments WHERE Appointment_id = " + appointmentID);
            ResultSet names = check.executeQuery();
            String caregiver = null;
            String patient = null;
            while (names.next()) {
                caregiver = names.getString(1);
                patient = names.getString(2);
            }
            if (currentCaregiver != null) {
                if (!(caregiver.equals(currentCaregiver.getUsername()))) {
                    System.out.println("Please try again!");
                    return;
                }
            }
            if (currentPatient != null) {
                if (!(patient.equals(currentPatient.getUsername()))) {
                    System.out.println("Please try again!");
                    return;
                }
            }
            Date date = null;
            String vaccine = null;
            PreparedStatement statement = con.prepareStatement("SELECT Vaccine_name, Time FROM " +
                    "Appointments WHERE Appointment_id = " + appointmentID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                vaccine = resultSet.getString(1);
                date = resultSet.getDate(2);
            }
            if (caregiver == null || vaccine == null || date == null || patient == null) {
                System.out.println("Please try again!");
                return;
            }
            PreparedStatement removal = con.prepareStatement("DELETE FROM Appointments WHERE " +
                    "Appointment_id = " + appointmentID);
            removal.executeUpdate();
            PreparedStatement updateOne = con.prepareStatement("INSERT INTO Availabilities VALUES" +
                    " ('" + date + "', '" + caregiver + "')");
            updateOne.executeUpdate();
            PreparedStatement updateTwo = con.prepareStatement("UPDATE Vaccines SET Doses = " +
                    "(DOSES + 1) WHERE Name = '" + vaccine + "'");
            updateTwo.executeUpdate();
            System.out.println("Appointment successfully canceled!");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
            return;
        } finally {
            cm.closeConnection();
        }
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
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if (currentCaregiver != null) {
            try {
                PreparedStatement statement = con.prepareStatement("SELECT A.Appointment_id, " +
                        "A.Vaccine_name, A.Time, A.Patient_username FROM Appointments AS A WHERE " +
                        "A.Caregiver_username = '" +currentCaregiver.getUsername() + "' ORDER BY " +
                        "A.Appointment_ID");
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println("Appointment ID: " + resultSet.getLong(1) + " Vaccine name: "
                    + resultSet.getString(2) + " Date: " + resultSet.getDate(3) +
                    " Patient name: " + resultSet.getString(4));
                }
            } catch (SQLException e){
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
            return;
        }
        if (currentPatient != null) {
            try {
                PreparedStatement statement = con.prepareStatement("SELECT A.Appointment_id, " +
                        "A.Vaccine_name, A.Time, A.Caregiver_username FROM Appointments AS A WHERE " +
                        "A.Patient_username = '" + currentPatient.getUsername() + "' ORDER BY " +
                        "A.Appointment_ID");
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println("Appointment ID: " + resultSet.getLong(1) + " Vaccine name: "
                            + resultSet.getString(2) + " Date: " + resultSet.getDate(3) +
                            " Caregiver name: " + resultSet.getString(4));
                }
            } catch (SQLException e){
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
            return;
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
    }
}