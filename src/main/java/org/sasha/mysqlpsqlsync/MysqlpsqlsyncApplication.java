package org.sasha.mysqlpsqlsync;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.boot.autoconfigure.orm.jpa.*;
import org.springframework.scheduling.annotation.*;

@EnableScheduling
@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        }
)
public class MysqlpsqlsyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysqlpsqlsyncApplication.class, args);


    }

}
