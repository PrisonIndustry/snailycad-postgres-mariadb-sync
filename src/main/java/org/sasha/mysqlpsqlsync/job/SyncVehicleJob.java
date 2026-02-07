package org.sasha.mysqlpsqlsync.job;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.sasha.mysqlpsqlsync.repositorys.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

@Component
public class SyncVehicleJob {

    @Value("${other.registrationstatus}")
    private String registrationStatusId;

    @Value("${other.insurancestatus}")
    private String insuranceStatusId;

    // optional override (wenn leer, wird other.* genutzt)
    @Value("${vehiclestatus:}")
    private String vehicleStatus;

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate mysqlJdbcTemplate;

    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;

    private final VehicleValueSnailyRepository vehicleValueRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SyncVehicleJob.class);

    private final BeanPropertyRowMapper<VehicleFiveM> vehicleRowMapper =
            new BeanPropertyRowMapper<>(VehicleFiveM.class);

    public SyncVehicleJob(
            @Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate,
            @Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbcTemplate,
            VehicleValueSnailyRepository vehicleValueRepository
    ) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
        this.postgresJdbcTemplate = postgresJdbcTemplate;
        this.vehicleValueRepository = vehicleValueRepository;
    }

    /**
     * GTA-V Farben 0..86 (Primary Color)
     */
    private static final Map<Integer, String> GTA_VEHICLE_COLORS = Map.ofEntries(
            Map.entry(0, "Black"),
            Map.entry(1, "Graphite"),
            Map.entry(2, "Black Steel"),
            Map.entry(3, "Dark Silver"),
            Map.entry(4, "Silver"),
            Map.entry(5, "Blue Silver"),
            Map.entry(6, "Rolled Steel"),
            Map.entry(7, "Shadow Silver"),
            Map.entry(8, "Stone Silver"),
            Map.entry(9, "Midnight Silver"),
            Map.entry(10, "Cast Iron Silver"),
            Map.entry(11, "Anthracite Black"),
            Map.entry(12, "Matte Black"),
            Map.entry(13, "Matte Gray"),
            Map.entry(14, "Matte Light Gray"),
            Map.entry(15, "Util Black"),
            Map.entry(16, "Util Black Poly"),
            Map.entry(17, "Util Dark Silver"),
            Map.entry(18, "Util Silver"),
            Map.entry(19, "Util Gun Metal"),
            Map.entry(20, "Util Shadow Silver"),
            Map.entry(21, "Worn Black"),
            Map.entry(22, "Worn Graphite"),
            Map.entry(23, "Worn Silver Gray"),
            Map.entry(24, "Worn Silver"),
            Map.entry(25, "Worn Blue Silver"),
            Map.entry(26, "Worn Shadow Silver"),
            Map.entry(27, "Red"),
            Map.entry(28, "Torino Red"),
            Map.entry(29, "Formula Red"),
            Map.entry(30, "Blaze Red"),
            Map.entry(31, "Grace Red"),
            Map.entry(32, "Garnet Red"),
            Map.entry(33, "Sunset Red"),
            Map.entry(34, "Cabernet Red"),
            Map.entry(35, "Wine Red"),
            Map.entry(36, "Candy Red"),
            Map.entry(37, "Hot Pink"),
            Map.entry(38, "Pfister Pink"),
            Map.entry(39, "Salmon Pink"),
            Map.entry(40, "Sunrise Orange"),
            Map.entry(41, "Orange"),
            Map.entry(42, "Bright Orange"),
            Map.entry(43, "Gold"),
            Map.entry(44, "Bronze Yellow"),
            Map.entry(45, "Yellow"),
            Map.entry(46, "Race Yellow"),
            Map.entry(47, "Dew Yellow"),
            Map.entry(48, "Dark Green"),
            Map.entry(49, "Racing Green"),
            Map.entry(50, "Sea Green"),
            Map.entry(51, "Olive Green"),
            Map.entry(52, "Bright Green"),
            Map.entry(53, "Gasoline Green"),
            Map.entry(54, "Midnight Blue"),
            Map.entry(55, "Galaxy Blue"),
            Map.entry(56, "Dark Blue"),
            Map.entry(57, "Saxon Blue"),
            Map.entry(58, "Blue"),
            Map.entry(59, "Mariner Blue"),
            Map.entry(60, "Harbor Blue"),
            Map.entry(61, "Diamond Blue"),
            Map.entry(62, "Surf Blue"),
            Map.entry(63, "Nautical Blue"),
            Map.entry(64, "Racing Blue"),
            Map.entry(65, "Ultra Blue"),
            Map.entry(66, "Light Blue"),
            Map.entry(67, "Chocolate Brown"),
            Map.entry(68, "Bison Brown"),
            Map.entry(69, "Creek Brown"),
            Map.entry(70, "Feltzer Brown"),
            Map.entry(71, "Maple Brown"),
            Map.entry(72, "Beechwood Brown"),
            Map.entry(73, "Sienna Brown"),
            Map.entry(74, "Saddle Brown"),
            Map.entry(75, "Moss Brown"),
            Map.entry(76, "Woodbeech Brown"),
            Map.entry(77, "Straw Brown"),
            Map.entry(78, "Sandy Brown"),
            Map.entry(79, "Bleached Brown"),
            Map.entry(80, "Schafter Purple"),
            Map.entry(81, "Spinnaker Purple"),
            Map.entry(82, "Midnight Purple"),
            Map.entry(83, "Bright Purple"),
            Map.entry(84, "Cream"),
            Map.entry(85, "Ice White"),
            Map.entry(86, "Frost White")
    );

    /**
     * Alle 2 Minuten: neue Fahrzeuge anlegen + Änderungen übernehmen (über snailycadid)
     */
    @Scheduled(fixedDelayString = "${sync.interval.ms:120000}")
    public void syncVehiclesInsertAndUpdate() {
        log.info("Start FiveM -> SnailyCAD RegisteredVehicle Sync (insert + update by snailycadid) ...");

        try {
            List<VehicleFiveM> allVehicles = mysqlJdbcTemplate.query("SELECT * FROM owned_vehicles", vehicleRowMapper);

            if (allVehicles.isEmpty()) {
                log.info("No FiveM vehicles found.");
                return;
            }

            for (VehicleFiveM vehicle : allVehicles) {
                String plate = safeTrim(vehicle.getPlate());
                try {
                    // Baue gewünschten Zustand
                    RegisteredVehicleSnaily desired = buildDesiredRegisteredVehicle(vehicle);
                    if (desired == null) continue;

                    // snailycadid kommt aus owned_vehicles
                    String snailycadid = safeTrim(vehicle.getSnailycadid()); // <-- WICHTIG: dein Getter

                    if (snailycadid != null) {
                        // UPDATE in Postgres anhand snailycadid, nur wenn sich Werte geändert haben
                        desired.setId(snailycadid);

                        boolean updated = updateRegisteredVehicleIfChanged(desired);
                        if (updated) {
                            log.info("Updated RegisteredVehicle id={} (plate={})", snailycadid, desired.getPlate());
                        }
                        continue;
                    }

                    // Kein snailycadid -> INSERT + zurückschreiben nach MySQL über plate
                    if (plate == null) {
                        log.warn("Skipping vehicle - plate is null and snailycadid is null (can't map back)");
                        continue;
                    }

                    String newId = UUID.randomUUID().toString();
                    desired.setId(newId);

                    insertRegisteredVehicle(desired);
                    updateOwnedVehiclesSnailyCadIdByPlate(plate, newId);

                    log.info("Inserted RegisteredVehicle id={} for plate={} and set owned_vehicles.snailycadid", newId, plate);

                } catch (Exception e) {
                    log.error("Error syncing FiveM vehicle plate={}: {}", plate, e.getMessage(), e);
                }
            }

            log.info("FiveM -> SnailyCAD Sync completed.");

        } catch (Exception e) {
            log.error("Error during vehicle sync: {}", e.getMessage(), e);
        }
    }

    /**
     * Baut gewünschten Zustand aus FiveM-Daten.
     */
    private RegisteredVehicleSnaily buildDesiredRegisteredVehicle(VehicleFiveM vehicle) {
        try {
            RegisteredVehicleSnaily v = new RegisteredVehicleSnaily();

            // citizenId aus MySQL users.snailycadid
            String citizenId = getCitizenIdByOwnerFromMySqlUsers(vehicle.getOwner());
            if (citizenId == null) {
                log.warn("Skipping vehicle {} - no citizenId mapping for owner {}", vehicle.getPlate(), vehicle.getOwner());
                return null;
            }
            v.setCitizenId(citizenId);

            // userId nicht setzbar
            v.setUserId(null);

            // Plate
            v.setPlate(vehicle.getPlate());

            // JSON -> modelId + color
            if (vehicle.getVehicle() != null && !vehicle.getVehicle().isEmpty()) {
                JsonNode rootNode = objectMapper.readTree(vehicle.getVehicle());

                if (rootNode.has("model")) {
                    String hash = rootNode.get("model").asText();
                    v.setModelId(getModelIdByHash(hash));
                }

                if (rootNode.has("color1")) {
                    String colorIdText = rootNode.get("color1").asText();
                    v.setColor(mapGtaColorIdToName(colorIdText));
                }
            }

            if (v.getModelId() == null) {
                log.warn("Skipping vehicle {} - no modelId mapping found", vehicle.getPlate());
                return null;
            }

            // Defaults
            v.setVinNumber(generateVinNumber());
            v.setReportedStolen(false);
            v.setImpounded(false);
            v.setInspectionStatus(null);
            v.setTaxStatus(null);
            v.setAppearance(null);
            v.setImageId(null);

            // StatusIds
            String regId = (vehicleStatus != null && !vehicleStatus.isBlank()) ? vehicleStatus : registrationStatusId;
            String insId = (vehicleStatus != null && !vehicleStatus.isBlank()) ? vehicleStatus : insuranceStatusId;
            v.setRegistrationStatusId(regId);
            v.setInsuranceStatusId(insId);

            v.setCreatedAt(OffsetDateTime.now());
            v.setUpdatedAt(OffsetDateTime.now());

            return v;

        } catch (Exception e) {
            log.error("Error building desired RegisteredVehicle from FiveM: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * INSERT in Postgres
     */
    private void insertRegisteredVehicle(RegisteredVehicleSnaily v) {
        String sql =
                "INSERT INTO \"RegisteredVehicle\" " +
                        "(\"id\", \"userId\", \"citizenId\", \"vinNumber\", \"plate\", \"modelId\", \"color\", " +
                        "\"createdAt\", \"registrationStatusId\", \"reportedStolen\", \"impounded\", \"updatedAt\", " +
                        "\"insuranceStatusId\", \"inspectionStatus\", \"taxStatus\", \"appearance\", \"imageId\") " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        Object createdAt = v.getCreatedAt() != null ? java.sql.Timestamp.from(v.getCreatedAt().toInstant()) : null;
        Object updatedAt = v.getUpdatedAt() != null ? java.sql.Timestamp.from(v.getUpdatedAt().toInstant()) : null;

        postgresJdbcTemplate.update(
                sql,
                v.getId(),
                null,
                v.getCitizenId(),
                v.getVinNumber(),
                v.getPlate(),
                v.getModelId(),
                v.getColor(),
                createdAt,
                v.getRegistrationStatusId(),
                v.getReportedStolen(),
                v.getImpounded(),
                updatedAt,
                v.getInsuranceStatusId(),
                v.getInspectionStatus(),
                v.getTaxStatus(),
                v.getAppearance(),
                v.getImageId()
        );
    }

    /**
     * UPDATE in Postgres nur wenn geändert (Vergleich über snailycadid).
     */
    private boolean updateRegisteredVehicleIfChanged(RegisteredVehicleSnaily desired) {
        String sql =
                "UPDATE \"RegisteredVehicle\" SET " +
                        "\"plate\" = ?, " +
                        "\"color\" = ?, " +
                        "\"modelId\" = ?, " +
                        "\"citizenId\" = ?, " +
                        "\"updatedAt\" = ? " +
                        "WHERE \"id\" = ? AND ( " +
                        "COALESCE(\"plate\", '') <> COALESCE(?, '') OR " +
                        "COALESCE(\"color\", '') <> COALESCE(?, '') OR " +
                        "COALESCE(\"modelId\", '') <> COALESCE(?, '') OR " +
                        "COALESCE(\"citizenId\", '') <> COALESCE(?, '') " +
                        ")";

        Object updatedAt = java.sql.Timestamp.from(OffsetDateTime.now().toInstant());

        int changed = postgresJdbcTemplate.update(
                sql,
                desired.getPlate(),
                desired.getColor(),
                desired.getModelId(),
                desired.getCitizenId(),
                updatedAt,
                desired.getId(),
                desired.getPlate(),
                desired.getColor(),
                desired.getModelId(),
                desired.getCitizenId()
        );

        return changed > 0;
    }

    /**
     * MySQL: owned_vehicles.snailycadid setzen (ohne ID-Spalte!)
     * -> Update über plate.
     */
    private void updateOwnedVehiclesSnailyCadIdByPlate(String plate, String snailycadid) {
        String sql = "UPDATE owned_vehicles SET snailycadid = ? WHERE plate = ? LIMIT 1";
        mysqlJdbcTemplate.update(sql, snailycadid, plate);
    }

    /**
     * MySQL: owner (identifier) -> users.snailycadid (= CitizenId)
     */
    private String getCitizenIdByOwnerFromMySqlUsers(String owner) {
        try {
            String sql = "SELECT snailycadid FROM users WHERE identifier = ? LIMIT 1";
            List<String> ids = mysqlJdbcTemplate.queryForList(sql, String.class, owner);
            if (ids.isEmpty() || ids.get(0) == null || ids.get(0).isBlank()) return null;
            return ids.get(0);
        } catch (Exception e) {
            log.error("Error getting citizenId for owner {} from MySQL users: {}", owner, e.getMessage(), e);
            return null;
        }
    }

    private String mapGtaColorIdToName(String colorIdText) {
        if (colorIdText == null || colorIdText.isBlank()) return null;

        try {
            int id = Integer.parseInt(colorIdText);
            return GTA_VEHICLE_COLORS.getOrDefault(id, "Unknown(" + id + ")");
        } catch (NumberFormatException e) {
            return colorIdText;
        }
    }

    /**
     * ModelId Lookup via VehicleValue.hash inkl. unsigned fallback
     */
    private String getModelIdByHash(String hashFromJson) {
        try {
            VehicleValueSnaily vv = vehicleValueRepository.findByHash(hashFromJson);
            if (vv != null) return vv.getId();

            if (hashFromJson != null && hashFromJson.matches("-?\\d+")) {
                long signed = Long.parseLong(hashFromJson);
                long unsigned = signed & 0xFFFFFFFFL;
                String unsignedStr = Long.toString(unsigned);

                VehicleValueSnaily vv2 = vehicleValueRepository.findByHash(unsignedStr);
                if (vv2 != null) return vv2.getId();
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting VehicleValue for hash {}: {}", hashFromJson, e.getMessage(), e);
            return null;
        }
    }

    private String generateVinNumber() {
        return "VIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
