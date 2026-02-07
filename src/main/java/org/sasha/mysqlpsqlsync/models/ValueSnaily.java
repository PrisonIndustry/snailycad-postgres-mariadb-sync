package org.sasha.mysqlpsqlsync.models;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "Value", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueSnaily {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "isDefault")
    private Boolean isDefault;

    @Column(name = "createdAt", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "position")
    private Integer position;

    @Column(name = "isDisabled")
    private Boolean isDisabled;
}
