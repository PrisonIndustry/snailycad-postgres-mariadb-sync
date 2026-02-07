package org.sasha.mysqlpsqlsync.repositorys;

import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

@Repository
public interface VehicleValueSnailyRepository extends JpaRepository<VehicleValueSnaily, String> {
    VehicleValueSnaily findByHash(String hash);
}
