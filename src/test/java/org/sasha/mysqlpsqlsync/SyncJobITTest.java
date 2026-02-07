package org.sasha.mysqlpsqlsync;

import java.util.*;
import org.junit.jupiter.api.*;
import org.sasha.mysqlpsqlsync.job.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.jdbc.core.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SyncJobITTest {

    @MockBean(name = "mysqlJdbcTemplate")
    JdbcTemplate mysqlJdbcTemplate;

    @MockBean(name = "postgresJdbcTemplate")
    JdbcTemplate postgresJdbcTemplate;

    @Autowired
    SyncJob syncJob;



    @Test
    void syncMissingSnailyCitizens_runsAndQueriesInContext() {
        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NULL OR snailycadid = ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(Collections.emptyList());

        syncJob.syncMissingSnailyCitizens();

        verify(mysqlJdbcTemplate, times(1))
                .query(eq("SELECT * FROM users WHERE snailycadid IS NULL OR snailycadid = ''"),
                        any(BeanPropertyRowMapper.class));
    }

    @Test
    void syncMissingSnailyCitizens_createsCitizenAndUpdatesUser_inContext() {
        UserFiveM userFiveM = new UserFiveM();
        userFiveM.setId(1L);
        userFiveM.setIdentifier("steam:123");
        userFiveM.setFirstname("Max");
        userFiveM.setLastname("Mustermann");
        userFiveM.setDateofbirth("01.01.2000");
        userFiveM.setSkin("{\"hair_color_1\": 0, \"eye_color\": 2}");
        userFiveM.setHeight(180);
        userFiveM.setPhoneNumber("123456");
        userFiveM.setIsDead(false);
        userFiveM.setSex("m");

        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NULL OR snailycadid = ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(List.of(userFiveM));

        when(postgresJdbcTemplate.queryForObject(
                eq("SELECT id FROM \"User\" WHERE \"steamIdentifier\" = ? OR \"fivemIdentifier\" = ? LIMIT 1"),
                eq(String.class),
                any(), any())
        ).thenReturn("user-123");

        when(postgresJdbcTemplate.update(
                startsWith("INSERT INTO \"Citizen\""),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any(), any())
        ).thenReturn(1);

        when(mysqlJdbcTemplate.update(eq("UPDATE users SET snailycadid = ? WHERE id = ?"), any(), any()))
                .thenReturn(1);

        syncJob.syncMissingSnailyCitizens();

        verify(postgresJdbcTemplate, times(1))
                .update(startsWith("INSERT INTO \"Citizen\""),
                        any(), any(), any(), any(), any(),
                        any(), any(), any(), any(), any(),
                        any(), any(), any(), any(), any(),
                        any(), any(), any(), any());

        verify(mysqlJdbcTemplate, times(1))
                .update(eq("UPDATE users SET snailycadid = ? WHERE id = ?"), any(), eq(1L));
    }



    @Test
    void syncChangedCitizenData_runsAndQueriesInContext() {
        when(mysqlJdbcTemplate.query(
                eq("SELECT * FROM users WHERE snailycadid IS NOT NULL AND snailycadid <> ''"),
                any(BeanPropertyRowMapper.class))
        ).thenReturn(Collections.emptyList());

        syncJob.syncChangedCitizenData();

        verify(mysqlJdbcTemplate, atLeastOnce())
                .query(eq("SELECT * FROM users WHERE snailycadid IS NOT NULL AND snailycadid <> ''"),
                        any(BeanPropertyRowMapper.class));
    }

    @Test
    void syncChangedCitizenData_updatesCitizen_inContext() {
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
                startsWith("SELECT *"),
                any(BeanPropertyRowMapper.class),
                eq("citizen-1"))
        ).thenReturn(citizen);

        when(postgresJdbcTemplate.update(
                startsWith("UPDATE \"Citizen\""),
                any(), any(), any(), any(), any())
        ).thenReturn(1);

        syncJob.syncChangedCitizenData();

        verify(postgresJdbcTemplate, times(1))
                .update(startsWith("UPDATE \"Citizen\""),
                        any(), any(), any(), any(), eq("citizen-1"));
    }
}
