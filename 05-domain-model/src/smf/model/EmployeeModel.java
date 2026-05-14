package smf.model;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.messages.MessageLog;
import smf.model.structures.Employee;
import smf.util.AppUtils;

import java.time.LocalDate;

/**
 * SMF Library - EmployeeModel
 *
 * Complete implementation of Employee (which extends Person).
 * Extends AbstractPerson — all Person fields inherited, not duplicated.
 *
 * Original concept by: stark (Employeex — all Employee methods threw
 * UnsupportedOperationException)
 * Modernized for Java 8 — all methods implemented,
 * java.util.Date replaced with LocalDate
 */
public class EmployeeModel extends AbstractPerson implements Employee {

    private final DataStore   db  = DataStore.getInstance();
    private final MessageLog  log = MessageLog.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();

    private String    employeeId   = "";
    private String    employeeType = "FULL_TIME";
    private String    salaryType   = "MONTHLY";
    private double    salary       = 0.0;
    private LocalDate joinDate     = LocalDate.now();
    private LocalDate leaveDate    = null;

    // -------------------------------------------------------------------------
    // Employee interface implementation — all methods now work
    // -------------------------------------------------------------------------

    @Override public void      setEmployeeId(String id)   { this.employeeId = id; }
    @Override public String    getEmployeeId()             { return employeeId; }

    @Override public void      setEmployeeType(String t)  { this.employeeType = t; }
    @Override public String    getEmployeeType()           { return employeeType; }

    @Override public void      setSalaryType(String t)    { this.salaryType = t; }
    @Override public String    getSalaryType()             { return salaryType; }

    @Override public void      setSalary(double s)        { this.salary = s; }
    @Override public double    getSalary()                 { return salary; }

    @Override public void      setJoinDate(LocalDate d)   { this.joinDate = d; }
    @Override public LocalDate getJoinDate()               { return joinDate; }

    @Override public void      setLeaveDate(LocalDate d)  { this.leaveDate = d; }
    @Override public LocalDate getLeaveDate()              { return leaveDate; }

    // -------------------------------------------------------------------------
    // Database operations
    // -------------------------------------------------------------------------

    public boolean save() {
        boolean ok = db.Insert(
            "INSERT INTO employees (employee_id, name, email, salary_type, salary, join_date, employee_type)",
            new String[]{
                employeeId, getName(), getEmail(),
                salaryType, String.valueOf(salary),
                AppUtils.dateToString(joinDate), employeeType
            }
        );
        if (ok) {
            log.info("[EmployeeModel] Saved: " + getName());
            bus.publish(AppEvent.DB_INSERT);
        } else {
            log.error("[EmployeeModel] Save failed: " + getName());
            bus.publish(AppEvent.DB_ERROR);
        }
        return ok;
    }
}
