package org.sasha.mysqlpsqlsync.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "\"VehicleValue\"", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleValueSnaily {

    @Id
    @Column(name = "\"id\"", nullable = false)
    private String id;

    @Column(name = "\"valueId\"", nullable = false)
    private String valueId;

    @Column(name = "\"hash\"", nullable = false)
    private String hash;
}
