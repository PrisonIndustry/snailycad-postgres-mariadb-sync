package org.sasha.mysqlpsqlsync.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "owned_vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFiveM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Besitzer (z.B. User-ID oder Identifier)
     */
    @Column(name = "owner")
    private String owner;

    @Column(name = "plate")
    private String plate;

    /**
     * Fahrzeug-Spawnname (z.B. "comoda")
     */
    @Column(name = "vehicle")
    private String vehicle;

    /**
     * car / boat / air
     */
    @Column(name = "type")
    private String type;

    @Column(name = "job")
    private String job;

    /**
     * 0 = draußen, 1 = eingelagert
     */
    @Column(name = "stored")
    private Integer stored;

    @Column(name = "parking")
    private String parking;

    @Column(name = "pound")
    private String pound;

    @Column(name = "category")
    private String category;

    @Column(name = "name")
    private String name;

    @Column(name = "image")
    private String image;

    @Column(name="snailycadid")
    private String snailycadid;
}
