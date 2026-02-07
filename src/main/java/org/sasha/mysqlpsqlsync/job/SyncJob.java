package org.sasha.mysqlpsqlsync.job;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

@Component
public class SyncJob {

    @Value("${gender.male}")
    private String maleGenderId;

    @Value("${gender.female}")
    private String femaleGenderId;

    @Value("${other.driverlicense}")
    private String driverLicenseId;

    @Value("${other.ethnicity}")
    private String ethnicityId;

    @Qualifier("mysqlJdbcTemplate")
    JdbcTemplate mysqlJdbcTemplate;

    @Qualifier("postgresJdbcTemplate")
    JdbcTemplate postgresJdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(SyncJob.class);

    private final BeanPropertyRowMapper<UserFiveM> userFiveMRowMapper =
            new BeanPropertyRowMapper<>(UserFiveM.class);

    private final BeanPropertyRowMapper<UserSnaily> userSnailyRowMapper =
            new BeanPropertyRowMapper<>(UserSnaily.class);

    private static final String uknowString = "Unknown";

    public SyncJob(
            @Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate,
            @Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbcTemplate
    ) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
        this.postgresJdbcTemplate = postgresJdbcTemplate;
    }

    /**
     * Periodic sync: create missing SnailyCAD citizens for FiveM users
     * (Interval is configured via sync.interval.ms in application.yaml)
     */
    @Scheduled(fixedDelayString = "${sync.interval.ms:120000}")
    public void syncMissingSnailyCitizens() {
        log.info("Start FiveM -> SnailyCAD Sync ...");

        List<UserFiveM> usersWithoutSnaily = loadUsersWithoutSnailyId();

        if (usersWithoutSnaily.isEmpty()) {
            log.warn("No FiveM users found without snailycadid.");
            return;
        }

        for (UserFiveM user : usersWithoutSnaily) {
            try {
                log.info("Process FiveM-User id=" + user.getId()
                        + " identifier=" + user.getIdentifier());

                UserSnaily citizen = buildCitizenFromFiveM(user);

                insertCitizen(citizen);

                updateUserWithSnailyId(user.getId(), citizen.getId());

                log.info("Citizen " + citizen.getId()
                        + " created for FiveM users " + user.getId());

            } catch (Exception e) {
                log.error("Error syncing FiveM user id=" + user.getId()
                        + ": " + e.getMessage());
            }
        }

        log.info("FiveM -> SnailyCAD Sync completed.");
    }

    /**
     * Periodic sync: update already linked citizens (e.g. name and hair color)
     */
    @Scheduled(fixedDelayString = "${sync.update.interval.ms:300000}")
    public void syncChangedCitizenData() {
        log.info("Start FiveM -> SnailyCAD Update Sync (Name/Hair Color) ...");

        List<UserFiveM> usersWithSnaily = loadUsersWithSnailyId();

        if (usersWithSnaily.isEmpty()) {
            log.info("No FiveM users with linked snailycadid found.");
            return;
        }

        for (UserFiveM fivemUser : usersWithSnaily) {
            String citizenId = fivemUser.getSnailycadid();
            if (citizenId == null || citizenId.isBlank()) {
                continue;
            }

            try {
                UserSnaily citizen = loadCitizenById(citizenId);
                if (citizen == null) {
                    if (log.isInfoEnabled()) {
                        log.info("No citizen for snailycadid=" + citizenId + " found.");
                    }
                    continue;
                }

                JsonNode skinNode = parseSkinJson(fivemUser.getSkin());
                int hairColorIndex = skinNode.path("hair_color_1").asInt(-1);
                String expectedHairColor = resolveHairColorName(hairColorIndex);

                String expectedName = nullSafe(fivemUser.getFirstname(), "Unknown");
                String expectedSurname = nullSafe(fivemUser.getLastname(), uknowString);

                boolean nameChanged =
                        !Objects.equals(citizen.getName(), expectedName)
                                || !Objects.equals(citizen.getSurname(), expectedSurname);

                boolean hairChanged =
                        !Objects.equals(citizen.getHairColor(), expectedHairColor);

                if (!nameChanged && !hairChanged) {
                    continue;
                }

                updateCitizenNameAndHairColor(
                        citizenId,
                        nameChanged ? expectedName : citizen.getName(),
                        nameChanged ? expectedSurname : citizen.getSurname(),
                        hairChanged ? expectedHairColor : citizen.getHairColor()
                );

                if (log.isInfoEnabled()) {
                    log.info("Citizen " + citizenId + " updated. " +
                            (nameChanged ? "Name " : "") +
                            (hairChanged ? "Haircolor " : ""));
                }

            } catch (Exception e) {
                log.error("Error during update sync for FiveM users id=" + fivemUser.getId()
                        + ": " + e.getMessage());
                log.error(e.getMessage());
            }
        }

        log.info("FiveM -> SnailyCAD update sync complete..");
    }

    private List<UserFiveM> loadUsersWithoutSnailyId() {
        String sql = "SELECT * FROM users WHERE snailycadid IS NULL OR snailycadid = ''";
        return mysqlJdbcTemplate.query(sql, userFiveMRowMapper);
    }

    private List<UserFiveM> loadUsersWithSnailyId() {
        String sql = "SELECT * FROM users WHERE snailycadid IS NOT NULL AND snailycadid <> ''";
        return mysqlJdbcTemplate.query(sql, userFiveMRowMapper);
    }

    private void updateUserWithSnailyId(Long userId, String snailyId) {
        String sql = "UPDATE users SET snailycadid = ? WHERE id = ?";
        mysqlJdbcTemplate.update(sql, snailyId, userId);
    }

    private UserSnaily loadCitizenById(String citizenId) {
        try {
            String sql = """
                SELECT *
                FROM "Citizen"
                WHERE id = ?
                """;
            return postgresJdbcTemplate.queryForObject(sql, userSnailyRowMapper, citizenId);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error loading Citizen " + citizenId + ": " + e.getMessage());
            }
            return null;
        }
    }

    private void updateCitizenNameAndHairColor(String citizenId,
                                               String name,
                                               String surname,
                                               String hairColor) {
        String sql = """
                UPDATE "Citizen"
                SET name = ?,
                    surname = ?,
                    "hairColor" = ?,
                    "updatedAt" = ?
                WHERE id = ?
                """;

        postgresJdbcTemplate.update(
                sql,
                name,
                surname,
                hairColor,
                LocalDateTime.now(),
                citizenId
        );
    }

    private UserSnaily buildCitizenFromFiveM(UserFiveM fivem) throws Exception {
        UserSnaily citizen = new UserSnaily();

        citizen.setId(UUID.randomUUID().toString());

        citizen.setName(nullSafe(fivem.getFirstname(), uknowString));
        citizen.setSurname(nullSafe(fivem.getLastname(), uknowString));

        citizen.setDateOfBirth(parseDateOfBirth(fivem.getDateofbirth()));

        JsonNode skinNode = parseSkinJson(fivem.getSkin());

        int hairColorIndex = skinNode.path("hair_color_1").asInt(-1);
        int eyeColorIndex = skinNode.path("eye_color").asInt(-1);
        int sexIndex = skinNode.path("sex").asInt(-1);

        citizen.setHairColor(resolveHairColorName(hairColorIndex));
        citizen.setEyeColor(resolveEyeColorName(eyeColorIndex));

        citizen.setAddress("Unknow");
        citizen.setHeight(fivem.getHeight() != null ? fivem.getHeight().toString() : "0");
        citizen.setWeight("0");

        citizen.setPhoneNumber(fivem.getPhoneNumber());
        citizen.setDead(Boolean.TRUE.equals(fivem.getIsDead()));
        citizen.setMissing(false);
        citizen.setArrested(false);

        LocalDateTime now = LocalDateTime.now();
        citizen.setCreatedAt(now);
        citizen.setUpdatedAt(now);

        citizen.setGenderId(getGenderID(fivem));
        citizen.setEthnicityId(ethnicityId);

        citizen.setDriversLicenseId(driverLicenseId);
        citizen.setWeaponLicenseId(null);
        citizen.setPilotLicenseId(null);
        citizen.setWaterLicenseId(null);
        citizen.setCcwId(null);

        citizen.setNote("Automatically synchronized from FiveM");
        citizen.setAppearance(fivem.getSkin());
        citizen.setAdditionalInfo(fivem.getMetadataJson());

        return citizen;
    }

    private String nullSafe(String s, String d) {
        return (s == null || s.isBlank()) ? d : s;
    }

    private LocalDateTime parseDateOfBirth(String dob) {
        if (dob == null || dob.isBlank()) {
            return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate ld = LocalDate.parse(dob, fmt);
            return ld.atStartOfDay();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("DOB could not be read: " + dob);
            }
            return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
    }

    private JsonNode parseSkinJson(String skin) throws Exception {
        if (skin == null || skin.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(skin);
    }

    private String getGenderID(UserFiveM fivem) {
        String sex = fivem.getSex();
        if ("m".equalsIgnoreCase(sex)) {
            return maleGenderId;
        } else if ("f".equalsIgnoreCase(sex)) {
            return femaleGenderId;
        }
        return null; // return a default if you want
    }

    private String resolveEyeColorName(int index) {
        if (log.isInfoEnabled()) {
            log.info("Color ID Eye: " + index);
        }
        return switch (index) {
            case 0 -> "black";
            case 1 -> "very light blue/green";
            case 2 -> "dark blue";
            case 3 -> "brown";
            case 4 -> "darker brown";
            case 5 -> "light brown";
            case 6 -> "blue";
            case 7 -> "light blue";
            case 8 -> "pink";
            case 9 -> "yellow";
            case 10 -> "purple";
            case 11 -> "black";
            case 12 -> "dark green";
            case 13 -> "light brown";
            case 14 -> "yellow/black pattern";
            case 15 -> "light colored spiral pattern";
            case 16 -> "shiny red";
            case 17 -> "shiny half blue/half red";
            case 18 -> "half black/half light blue";
            case 19 -> "white/red perimter";
            case 20 -> "green snake";
            case 21 -> "red snake";
            case 22 -> "dark blue snake";
            case 23 -> "dark yellow";
            case 24 -> "bright yellow";
            case 25 -> "all black";
            case 26 -> "red small pupil";
            case 27 -> "devil blue/black";
            case 28 -> "white small pupil";
            case 29 -> "glossed over";
            default -> uknowString;
        };
    }

    private String resolveHairColorName(int index) {
        switch (index) {
            case 0:  return "black (#1c1f21)";
            case 1:  return "black/dark gray (#272a2c)";
            case 2:  return "dark ash brown (#312e2c)";
            case 3:  return "dark brown (#35261c)";
            case 4:  return "brown (#4b321f)";
            case 5:  return "medium brown (#5c3b24)";
            case 6:  return "warm brown (#6d4c35)";
            case 7:  return "soft brown (#6b503b)";
            case 8:  return "light brown (#765c45)";
            case 9:  return "light warm brown (#7f684e)";
            case 10: return "dirty blond (#99815d)";
            case 11: return "dark blond (#a79369)";
            case 12: return "blond (#af9c70)";
            case 13: return "gold blond (#bba063)";
            case 14: return "rich blond (#d6b97b)";
            case 15: return "light blond (#dac38e)";
            case 16: return "dark ash brown (#9f7f59)";
            case 17: return "brown chestnut (#845039)";
            case 18: return "dark ginger brown (#682b1f)";
            case 19: return "dark red brown (#61120c)";
            case 20: return "deep red (#640f0a)";
            case 21: return "red (#7c140f)";
            case 22: return "bright copper red (#a02e19)";
            case 23: return "warm orange red (#b64b28)";
            case 24: return "copper (#a2502f)";
            case 25: return "ginger (#aa4e2b)";
            case 26: return "dark gray (#626262)";
            case 27: return "gray (#808080)";
            case 28: return "light gray (#aaaaaa)";
            case 29: return "silver (#c5c5c5)";
            case 30: return "deep violet (#463955)";
            case 31: return "purple (#5a3f6b)";
            case 32: return "bright purple (#763c76)";
            case 33: return "hot pink (#ed74e3)";
            case 34: return "magenta (#eb4b93)";
            case 35: return "light pink (#f299bc)";
            case 36: return "aqua (#04959e)";
            case 37: return "teal (#025f86)";
            case 38: return "dark blue (#023974)";
            case 39: return "green (#3fa16a)";
            case 40: return "dark green (#217c61)";
            case 41: return "deep green (#185c55)";
            case 42: return "lime green (#b6c034)";
            case 43: return "yellow-green (#70a90b)";
            case 44: return "bright green (#439d13)";
            case 45: return "golden blond (#dcb857)";
            case 46: return "yellow gold (#e5b103)";
            case 47: return "gold orange (#e69102)";
            case 48: return "orange (#f28831)";
            case 49: return "light orange (#fb8057)";
            case 50: return "peach (#e28b58)";
            case 51: return "rust (#d1593c)";
            case 52: return "dark red (#ce3120)";
            case 53: return "blood red (#ad0903)";
            case 54: return "deep crimson (#880302)";
            case 55: return "dark charcoal (#1f1814)";
            case 56: return "charcoal brown (#291f19)";
            default: return uknowString;
        }
    }

    private String getEthnicityIdFromSkin(JsonNode skinNode) {
        try {
            String sql = "SELECT id FROM \"Value\" WHERE \"type\" = 'ETHNICITY' AND \"value\" = 'UNKNOWN' LIMIT 1";
            return postgresJdbcTemplate.queryForObject(sql, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String getSnailyUserIdForIdentifier(String identifier) {
        try {
            String sql = "SELECT id FROM \"User\" WHERE \"steamIdentifier\" = ? OR \"fivemIdentifier\" = ? LIMIT 1";
            return postgresJdbcTemplate.queryForObject(sql, String.class, identifier, identifier);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("No Snaily user for identifier " + identifier);
            }
            return null;
        }
    }

    /**
     * Insert citizen into SnailyCAD (PostgreSQL)
     */
    private void insertCitizen(UserSnaily c) {
        String sql = """
            INSERT INTO "Citizen"
            (id, "userId", name, surname, "dateOfBirth",
             "genderId", "ethnicityId",
             "hairColor", "eyeColor",
             address, height, weight,
             "phoneNumber", dead, missing,
             "createdAt", "updatedAt",
             appearance, "additionalInfo")
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        postgresJdbcTemplate.update(sql,
                c.getId(),
                c.getUserId(),
                c.getName(),
                c.getSurname(),
                c.getDateOfBirth(),
                c.getGenderId(),
                c.getEthnicityId(),
                c.getHairColor(),
                c.getEyeColor(),
                c.getAddress(),
                c.getHeight(),
                c.getWeight(),
                c.getPhoneNumber(),
                c.getDead(),
                c.getMissing(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getAppearance(),
                c.getAdditionalInfo()
        );
    }
}
