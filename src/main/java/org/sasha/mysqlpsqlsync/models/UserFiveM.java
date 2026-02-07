package org.sasha.mysqlpsqlsync.models;

import jakarta.persistence.*;
import java.sql.*;
import lombok.*;


@Entity
@Table(name = "users", schema = "public") // ggf. anpassen
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class  UserFiveM  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "ssn")
    private String ssn;

    // JSON in DB -> als String in Java
    @Column(name = "accounts")
    private String accountsJson;

    // Spalte heißt `group` -> bei Bedarf in DB mit "group" anlegen
    @Column(name = "\"group\"")
    private String userGroup;

    @Column(name = "inventory")
    private String inventoryJson;

    @Column(name = "job")
    private String job;

    @Column(name = "job_grade")
    private Integer jobGrade;

    @Column(name = "loadout")
    private String loadoutJson;

    @Column(name = "metadata")
    private String metadataJson;

    @Column(name = "position")
    private String positionJson;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    // Wenn in DB als TEXT/VARCHAR: einfach String lassen
    @Column(name = "dateofbirth")
    private String dateofbirth;

    @Column(name = "sex")
    private String sex;

    @Column(name = "height")
    private Integer height;

    @Column(name = "skin")
    private String skin;

    @Column(name = "status")
    private String statusJson;

    @Column(name = "is_dead")
    private Boolean isDead;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "last_property")
    private String lastProperty;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "last_seen")
    private Timestamp lastSeen;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "pedmode")
    private String pedmode;

    @Column(name = "pedallowed")
    private Boolean pedallowed;

    @Column(name = "pedcomponents")
    private String pedcomponentsJson;

    @Column(name = "deathcounter")
    private Integer deathcounter;

    @Column(name = "health_state")
    private String healthStateJson;

    @Column(name = "snailycadid")
    private String snailycadid;
}
