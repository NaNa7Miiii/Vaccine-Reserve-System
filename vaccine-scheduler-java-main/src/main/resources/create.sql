CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
)

CREATE TABLE Appointment (
    ID int,
    AppointmentTime date,
    Username_C varchar(255) REFERENCES Caregivers(Username) ON UPDATE CASCADE ON DELETE CASCADE,
    Username_P varchar(255) REFERENCES Patients(Username) ON UPDATE CASCADE ON DELETE CASCADE,
    Name_V varchar(255) REFERENCES Vaccines(Name) ON UPDATE CASCADE ON DELETE CASCADE,
    PRIMARY KEY (ID, Username_C, Username_P, Name_V)
)