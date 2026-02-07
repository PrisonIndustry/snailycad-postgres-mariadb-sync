package org.sasha.mysqlpsqlsync.repositorys;

import java.util.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;


@Repository
public interface RegisteredVehicleSnailyRepository extends JpaRepository<RegisteredVehicleSnaily, String> {
    Optional<RegisteredVehicleSnaily> findById(String id);
    List<RegisteredVehicleSnaily> findAll();
}
