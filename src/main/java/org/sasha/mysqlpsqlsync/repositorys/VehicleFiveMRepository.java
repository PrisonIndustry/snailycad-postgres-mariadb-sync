package org.sasha.mysqlpsqlsync.repositorys;

import java.util.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;


@Repository
public interface VehicleFiveMRepository extends JpaRepository<VehicleFiveM, Long> {
    Optional<VehicleFiveM> findById(Long id);
    List<VehicleFiveM> findAll();
}
