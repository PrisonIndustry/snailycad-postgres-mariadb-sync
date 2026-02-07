package org.sasha.mysqlpsqlsync.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "\"Citizen\"", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSnaily
{


    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "\"userId\"")
    private String userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "surname", nullable = false, length = 255)
    private String surname;

    @Column(name = "\"dateOfBirth\"", nullable = false)
    private java.time.LocalDateTime dateOfBirth;

    @Column(name = "\"genderId\"")
    private String genderId;

    @Column(name = "\"ethnicityId\"")
    private String ethnicityId;

    @Column(name = "\"hairColor\"", nullable = false, length = 255)
    private String hairColor;

    @Column(name = "\"eyeColor\"", nullable = false, length = 255)
    private String eyeColor;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "height", nullable = false, length = 255)
    private String height;

    @Column(name = "weight", nullable = false, length = 255)
    private String weight;

    @Column(name = "\"driversLicenseId\"")
    private String driversLicenseId;

    @Column(name = "\"weaponLicenseId\"")
    private String weaponLicenseId;

    @Column(name = "\"pilotLicenseId\"")
    private String pilotLicenseId;

    @Column(name = "\"ccwId\"")
    private String ccwId;

    @Column(name = "\"imageId\"")
    private String imageId;

    @Column(name = "note")
    private String note;

    @Column(name = "dead")
    private Boolean dead;

    @Column(name = "\"phoneNumber\"")
    private String phoneNumber;

    @Column(name = "\"dateOfDead\"")
    private java.time.LocalDateTime dateOfDead;

    @Column(name = "\"createdAt\"", nullable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "\"updatedAt\"", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @Column(name = "arrested")
    private Boolean arrested;

    @Column(name = "\"socialSecurityNumber\"")
    private String socialSecurityNumber;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "postal", length = 255)
    private String postal;

    @Column(name = "\"waterLicenseId\"")
    private String waterLicenseId;

    @Column(name = "appearance")
    private String appearance;

    @Column(name = "\"additionalInfo\"")
    private String additionalInfo;

    @Column(name = "\"suspendedLicensesId\"")
    private String suspendedLicensesId;

    @Column(name = "\"driversLicenseNumber\"")
    private String driversLicenseNumber;

    @Column(name = "\"pilotLicenseNumber\"")
    private String pilotLicenseNumber;

    @Column(name = "\"waterLicenseNumber\"")
    private String waterLicenseNumber;

    @Column(name = "\"weaponLicenseNumber\"")
    private String weaponLicenseNumber;

    @Column(name = "\"imageBlurData\"")
    private String imageBlurData;

    @Column(name = "\"dateOfMissing\"")
    private java.time.LocalDateTime dateOfMissing;

    @Column(name = "missing")
    private Boolean missing;

}
