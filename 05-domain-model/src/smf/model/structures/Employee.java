package smf.model.structures;

import java.time.LocalDate;

/**
 * SMF Library - Employee
 *
 * Extends Person with employee-specific fields.
 * Original concept by: stark — modernized for Java 8
 * (java.util.Date replaced with LocalDate)
 */
public interface Employee extends Person {

    void     setEmployeeId(String id);
    String   getEmployeeId();

    void     setEmployeeType(String type);  // FULL_TIME, PART_TIME, CONTRACT
    String   getEmployeeType();

    void     setSalaryType(String type);    // MONTHLY, HOURLY, ANNUAL
    String   getSalaryType();

    void     setSalary(double salary);
    double   getSalary();

    void     setJoinDate(LocalDate date);
    LocalDate getJoinDate();

    void     setLeaveDate(LocalDate date);
    LocalDate getLeaveDate();
}
