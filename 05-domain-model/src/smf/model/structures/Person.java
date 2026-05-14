package smf.model.structures;

import java.awt.Image;

/**
 * SMF Library - Person
 *
 * The foundation of the domain model hierarchy.
 * Customer, Employee, and Supplier all extend Person
 * so they all share these common fields without duplicating code.
 *
 * This is the key design insight of the original smf-lib:
 * define the contract once here, implement once in AbstractPerson,
 * and every domain class inherits it automatically.
 *
 * Original concept by: stark
 * Modernized for Java 8 — method naming conventions cleaned up
 */
public interface Person {

    // --- Identity ---
    void setName(String name);
    void setFullName(String fullName);
    void setDOB(String dateOfBirth);        // format: yyyy-MM-dd
    void setNationality(String nationality);
    void setGender(String gender);
    void setAge(int age);
    void setNIC(String nic);                // National Identity Card number
    void setSocialSecurityNumber(long ssn);

    String getName();
    String getFullName();
    String getDOB();
    String getNationality();
    String getGender();
    int    getAge();
    String getNIC();
    long   getSocialSecurityNumber();

    // --- Address ---
    void setAddress(String address);
    void setCountry(String country);
    void setCountryId(String countryId);
    void setState(String state);
    void setStateId(String stateId);
    void setCity(String city);
    void setCityId(String cityId);
    void setProvince(String province);
    void setProvinceId(String provinceId);
    void setZipCode(String zipCode);

    String getAddress();
    String getCountry();
    String getCountryId();
    String getState();
    String getStateId();
    String getCity();
    String getCityId();
    String getProvince();
    String getProvinceId();
    String getZipCode();

    // --- Contact ---
    void setEmail(String email);
    void setHomePhoneNumber(String number);
    void setHandPhoneNumber(String number);   // mobile
    void setOfficePhoneNumber(String number);
    void setTelCode(String code);
    void setTelAreaCode(String areaCode);
    void setTelCountryCode(String countryCode);
    void setFaxCode(String code);
    void setFaxAreaCode(String areaCode);
    void setContactNumbers(String[] numbers);

    String   getEmail();
    String   getHomePhoneNumber();
    String   getHandPhoneNumber();
    String   getOfficePhoneNumber();
    String   getTelCode();
    String   getTelAreaCode();
    String   getTelCountryCode();
    String   getFaxCode();
    String   getFaxAreaCode();
    String[] getContactNumbers();

    // --- Extra ---
    void   setPhoto(Image photo);
    void   setNote(String note);
    Image  getPhoto();
    String getNote();
}
