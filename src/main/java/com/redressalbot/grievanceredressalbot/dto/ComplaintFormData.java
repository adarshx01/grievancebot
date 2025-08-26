package com.redressalbot.grievanceredressalbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComplaintFormData {
    private PersonalInformation personalInformation;
    private ContactDetails contactDetails;
    private IncidentInformation incidentInformation;
    private List<Document> documents;
    private String summary;
    private boolean submitForm;
    private boolean mandatoryInfoQueried;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInformation {
        private FullName fullName;
        private String dateOfBirth;
        private String gender;
        private String adhaarNumber;
        private String maritalStatus;
        private List<Dependent> dependents;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FullName {
            private String firstName;
            private String middleName;
            private String lastName;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Dependent {
            private String name;
            private String relationship;
            private Integer age;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactDetails {
        private List<Phone> phones;
        private List<String> emails;
        private Address address;
        private List<EmergencyContact> emergencyContacts;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Phone {
            private String type;
            private String number;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Address {
            private String street;
            private String landmark;
            private String city;
            private String district;
            private String state;
            private String postalCode;
            private String country;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EmergencyContact {
            private String name;
            private String relationship;
            private String phone;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentInformation {
        private String incidentDate;
        private String incidentTime;
        private IncidentLocation incidentLocation;
        private String incidentType;
        private String detailedDescription;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class IncidentLocation {
            private String placeName;
            private String address;
            private Float latitude;
            private Float longitude;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private String documentName;
        private String documentType;
        private String fileUrl;
    }
}