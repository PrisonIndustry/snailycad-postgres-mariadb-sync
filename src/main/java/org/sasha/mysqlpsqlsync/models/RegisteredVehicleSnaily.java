package org.sasha.mysqlpsqlsync.models;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "RegisteredVehicle", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredVehicleSnaily {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "userId")
    private String userId;

    @Column(name = "citizenId")
    private String citizenId;

    @Column(name = "vinNumber")
    private String vinNumber;

    @Column(name = "plate")
    private String plate;

    @Column(name = "modelId")
    private String modelId;

    @Column(name = "color")
    private String color;

    @Column(name = "createdAt")
    private OffsetDateTime createdAt;

    @Column(name = "registrationStatusId")
    private String registrationStatusId;

    @Column(name = "reportedStolen")
    private Boolean reportedStolen;

    @Column(name = "impounded")
    private Boolean impounded;

    @Column(name = "updatedAt")
    private OffsetDateTime updatedAt;

    @Column(name = "insuranceStatusId")
    private String insuranceStatusId;

    @Column(name = "inspectionStatus")
    private String inspectionStatus;

    @Column(name = "taxStatus")
    private String taxStatus;

    @Column(name = "appearance")
    private String appearance;

    @Column(name = "imageId")
    private String imageId;
}
