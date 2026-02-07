package org.sasha.mysqlpsqlsync.repositorys;

import java.util.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;


@Repository
public interface UserFiveMRespository extends JpaRepository<UserFiveM,Long> {
    Optional<UserFiveM> findById(Long id);
    List<UserFiveM> findAll();
}
