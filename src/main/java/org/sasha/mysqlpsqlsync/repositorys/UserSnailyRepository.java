package org.sasha.mysqlpsqlsync.repositorys;


import java.util.*;
import org.sasha.mysqlpsqlsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;


@Repository
public interface UserSnailyRepository extends JpaRepository<UserSnaily,Long> {
    List<UserSnaily> findAll();

}
