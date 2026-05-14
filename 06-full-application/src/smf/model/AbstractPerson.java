package smf.model;

import smf.model.structures.Person;
import java.awt.Image;

/**
 * SMF Library - AbstractPerson
 *
 * Implements all Person fields in one place.
 * Customer, Employee, and Supplier extend this class
 * so they inherit all Person fields without duplicating them.
 *
 * In the original smf-lib, Customerx, Employeex, and Supplierx
 * each had their own copy of ALL the Person fields — roughly
 * 25 fields duplicated three times. Any change had to be made
 * in three places.
 *
 * AbstractPerson fixes that: define the fields once, inherit everywhere.
 *
 * Original concept by: stark (Person interface)
 * Modernized for Java 8 — AbstractPerson base class introduced
 */
public abstract class AbstractPerson implements Person {

    // --- Identity ---
    private String name                = "";
    private String fullName            = "";
    private String dateOfBirth         = "";
    private String nationality         = "";
    private String gender              = "";
    private int    age                 = 0;
    private String nic                 = "";
    private long   socialSecurityNumber = 0;

    // --- Address ---
    private String address    = "";
    private String country    = "";
    private String countryId  = "";
    private String state      = "";
    private String stateId    = "";
    private String city       = "";
    private String cityId     = "";
    private String province   = "";
    private String provinceId = "";
    private String zipCode    = "";

    // --- Contact ---
    private String   email              = "";
    private String   homePhoneNumber    = "";
    private String   handPhoneNumber    = "";
    private String   officePhoneNumber  = "";
    private String   telCode            = "";
    private String   telAreaCode        = "";
    private String   telCountryCode     = "";
    private String   faxCode            = "";
    private String   faxAreaCode        = "";
    private String[] contactNumbers     = {};

    // --- Extra ---
    private Image  photo = null;
    private String note  = "";

    // -------------------------------------------------------------------------
    // Person implementation
    // -------------------------------------------------------------------------

    @Override public void setName(String v)               { this.name = v; }
    @Override public void setFullName(String v)           { this.fullName = v; }
    @Override public void setDOB(String v)                { this.dateOfBirth = v; }
    @Override public void setNationality(String v)        { this.nationality = v; }
    @Override public void setGender(String v)             { this.gender = v; }
    @Override public void setAge(int v)                   { this.age = v; }
    @Override public void setNIC(String v)                { this.nic = v; }
    @Override public void setSocialSecurityNumber(long v) { this.socialSecurityNumber = v; }

    @Override public String getName()               { return name; }
    @Override public String getFullName()           { return fullName; }
    @Override public String getDOB()                { return dateOfBirth; }
    @Override public String getNationality()        { return nationality; }
    @Override public String getGender()             { return gender; }
    @Override public int    getAge()                { return age; }
    @Override public String getNIC()                { return nic; }
    @Override public long   getSocialSecurityNumber(){ return socialSecurityNumber; }

    @Override public void setAddress(String v)    { this.address = v; }
    @Override public void setCountry(String v)    { this.country = v; }
    @Override public void setCountryId(String v)  { this.countryId = v; }
    @Override public void setState(String v)      { this.state = v; }
    @Override public void setStateId(String v)    { this.stateId = v; }
    @Override public void setCity(String v)       { this.city = v; }
    @Override public void setCityId(String v)     { this.cityId = v; }
    @Override public void setProvince(String v)   { this.province = v; }
    @Override public void setProvinceId(String v) { this.provinceId = v; }
    @Override public void setZipCode(String v)    { this.zipCode = v; }

    @Override public String getAddress()    { return address; }
    @Override public String getCountry()    { return country; }
    @Override public String getCountryId()  { return countryId; }
    @Override public String getState()      { return state; }
    @Override public String getStateId()    { return stateId; }
    @Override public String getCity()       { return city; }
    @Override public String getCityId()     { return cityId; }
    @Override public String getProvince()   { return province; }
    @Override public String getProvinceId() { return provinceId; }
    @Override public String getZipCode()    { return zipCode; }

    @Override public void setEmail(String v)             { this.email = v; }
    @Override public void setHomePhoneNumber(String v)   { this.homePhoneNumber = v; }
    @Override public void setHandPhoneNumber(String v)   { this.handPhoneNumber = v; }
    @Override public void setOfficePhoneNumber(String v) { this.officePhoneNumber = v; }
    @Override public void setTelCode(String v)           { this.telCode = v; }
    @Override public void setTelAreaCode(String v)       { this.telAreaCode = v; }
    @Override public void setTelCountryCode(String v)    { this.telCountryCode = v; }
    @Override public void setFaxCode(String v)           { this.faxCode = v; }
    @Override public void setFaxAreaCode(String v)       { this.faxAreaCode = v; }
    @Override public void setContactNumbers(String[] v)  { this.contactNumbers = v; }

    @Override public String   getEmail()             { return email; }
    @Override public String   getHomePhoneNumber()   { return homePhoneNumber; }
    @Override public String   getHandPhoneNumber()   { return handPhoneNumber; }
    @Override public String   getOfficePhoneNumber() { return officePhoneNumber; }
    @Override public String   getTelCode()           { return telCode; }
    @Override public String   getTelAreaCode()       { return telAreaCode; }
    @Override public String   getTelCountryCode()    { return telCountryCode; }
    @Override public String   getFaxCode()           { return faxCode; }
    @Override public String   getFaxAreaCode()       { return faxAreaCode; }
    @Override public String[] getContactNumbers()    { return contactNumbers; }

    @Override public void  setPhoto(Image v) { this.photo = v; }
    @Override public void  setNote(String v) { this.note = v; }
    @Override public Image getPhoto()        { return photo; }
    @Override public String getNote()        { return note; }

    /**
     * Convenience: build a full address string.
     */
    public String getFullAddress() {
        return address + ", " + city + ", " + state + ", " + country + " " + zipCode;
    }

    /**
     * Convenience: build a display name (full name if set, otherwise name).
     */
    public String getDisplayName() {
        return fullName.isEmpty() ? name : fullName;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
             + "[" + name + ", " + email + "]";
    }
}
