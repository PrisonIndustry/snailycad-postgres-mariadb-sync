package org.sasha.mysqlpsqlsync;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.sasha.mysqlpsqlsync.job.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.jdbc.core.*;
import org.springframework.test.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncJobUnitTest {

    @Mock
    JdbcTemplate mysqlJdbcTemplate;

    @Mock
    JdbcTemplate postgresJdbcTemplate;

    @InjectMocks
    SyncJob syncJob;

    @BeforeEach
    void setUp() {
        syncJob = new SyncJob(mysqlJdbcTemplate, postgresJdbcTemplate);
        ReflectionTestUtils.setField(syncJob, "maleGenderId", "gender-m");
        ReflectionTestUtils.setField(syncJob, "femaleGenderId", "gender-f");
        ReflectionTestUtils.setField(syncJob, "driverLicenseId", "dl-1");
        ReflectionTestUtils.setField(syncJob, "ethnicityId", "eth-unknown");
    }

    @Test
    void syncMissingSnailyCitizens_doesNothing_whenNoUsersWithoutSnailyId() {
        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NULL OR snailycadid = ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(Collections.emptyList());

        syncJob.syncMissingSnailyCitizens();

        verify(mysqlJdbcTemplate, never())
                .update(eq("UPDATE users SET snailycadid = ? WHERE id = ?"), any(), any());

        verify(postgresJdbcTemplate, never()).update(
                contains("INSERT INTO \"Citizen\""),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any()
        );
    }

    @Test
    void syncMissingSnailyCitizens_insertsCitizenAndUpdatesUser_whenUserWithoutSnailyExists() {
        UserFiveM fivem = new UserFiveM();
        fivem.setId(1L);
        fivem.setIdentifier("steam:123");
        fivem.setFirstname("Max");
        fivem.setLastname("Mustermann");
        fivem.setDateofbirth("01.01.2000");
        fivem.setSkin("{\"hair_color_1\": 0, \"eye_color\": 2}");
        fivem.setHeight(180);
        fivem.setPhoneNumber("123456");
        fivem.setIsDead(false);
        fivem.setSex("m");
        fivem.setMetadataJson("{\"foo\": \"bar\"}");

        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NULL OR snailycadid = ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(List.of(fivem));

        when(postgresJdbcTemplate.update(
                contains("INSERT INTO \"Citizen\""),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any())
        ).thenReturn(1);

        when(mysqlJdbcTemplate.update(
                eq("UPDATE users SET snailycadid = ? WHERE id = ?"),
                any(), any())
        ).thenReturn(1);

        syncJob.syncMissingSnailyCitizens();

        verify(postgresJdbcTemplate, times(1)).update(
                contains("INSERT INTO \"Citizen\""),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any()
        );

        verify(mysqlJdbcTemplate, times(1))
                .update(eq("UPDATE users SET snailycadid = ? WHERE id = ?"),
                        any(), eq(1L));
    }

    @Test
    void syncChangedCitizenData_doesNothing_whenNoUsersWithSnailyId() {
        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NOT NULL AND snailycadid <> ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(Collections.emptyList());

        syncJob.syncChangedCitizenData();

        verify(postgresJdbcTemplate, never()).update(
                contains("UPDATE \"Citizen\""),
                any(), any(), any(), any(), any());
    }

    @Test
    void syncChangedCitizenData_updatesNameAndHairColor_whenChangedInFiveM() {
        UserFiveM fivem = new UserFiveM();
        fivem.setId(1L);
        fivem.setSnailycadid("citizen-1");
        fivem.setFirstname("Neuer");
        fivem.setLastname("Name");
        fivem.setSkin("{\"hair_color_1\": 1}");
        fivem.setSex("m");

        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NOT NULL AND snailycadid <> ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(List.of(fivem));

        UserSnaily citizen = new UserSnaily();
        citizen.setId("citizen-1");
        citizen.setName("Alter");
        citizen.setSurname("Name");
        citizen.setHairColor("black (#1c1f21)");

        when(postgresJdbcTemplate.queryForObject(
                anyString(),
                any(BeanPropertyRowMapper.class),
                eq("citizen-1"))
        ).thenReturn(citizen);

        when(postgresJdbcTemplate.update(
                contains("UPDATE \"Citizen\""),
                any(), any(), any(), any(), any())
        ).thenReturn(1);

        syncJob.syncChangedCitizenData();

        verify(postgresJdbcTemplate, times(1)).update(
                contains("UPDATE \"Citizen\""),
                anyString(),
                anyString(),
                anyString(),
                any(LocalDateTime.class),
                eq("citizen-1"));
    }

    @Test
    void buildCitizenFromFiveM_buildsCitizenCorrectly() throws Exception {
        UserFiveM fivem = new UserFiveM();
        fivem.setFirstname("Max");
        fivem.setLastname("Mustermann");
        fivem.setDateofbirth("01.01.2000");
        fivem.setHeight(180);
        fivem.setPhoneNumber("123456");
        fivem.setIsDead(false);
        fivem.setIdentifier("steam:123");
        fivem.setSex("m");
        fivem.setSkin("{\"hair_color_1\": 0, \"eye_color\": 2}");
        fivem.setMetadataJson("{\"foo\": \"bar\"}");

        Method m = SyncJob.class.getDeclaredMethod("buildCitizenFromFiveM", UserFiveM.class);
        m.setAccessible(true);

        UserSnaily citizen = (UserSnaily) m.invoke(syncJob, fivem);

        assertEquals("Max", citizen.getName());
        assertEquals("Mustermann", citizen.getSurname());
        assertNotNull(citizen.getHairColor());
        assertNotNull(citizen.getEyeColor());
        assertNotNull(citizen.getId());
        assertDoesNotThrow(() -> UUID.fromString(citizen.getId()));
    }

    @Test
    void nullSafe_behavesCorrectly() throws Exception {
        Method m = SyncJob.class.getDeclaredMethod("nullSafe", String.class, String.class);
        m.setAccessible(true);

        assertEquals("default", m.invoke(syncJob, null, "default"));
        assertEquals("default", m.invoke(syncJob, "   ", "default"));
        assertEquals("value", m.invoke(syncJob, "value", "default"));
    }

    @Test
    void parseDateOfBirth_parsesOrFallsBack() throws Exception {
        Method m = SyncJob.class.getDeclaredMethod("parseDateOfBirth", String.class);
        m.setAccessible(true);

        LocalDateTime valid = (LocalDateTime) m.invoke(syncJob, "01.01.2000");
        LocalDateTime invalid = (LocalDateTime) m.invoke(syncJob, "nope");
        LocalDateTime empty = (LocalDateTime) m.invoke(syncJob, "");

        assertEquals(2000, valid.getYear());
        assertEquals(1970, invalid.getYear());
        assertEquals(1970, empty.getYear());
    }
}
