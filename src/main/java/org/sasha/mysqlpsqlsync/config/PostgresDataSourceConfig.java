package org.sasha.mysqlpsqlsync.config;

import jakarta.persistence.*;
import java.util.*;
import javax.sql.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.*;
import org.springframework.transaction.*;

@Configuration
@EnableJpaRepositories (
        basePackages = {"org.sasha.mysqlpsqlsync.postgres.repository", "org.sasha.mysqlpsqlsync.repositorys"},
        entityManagerFactoryRef = "postgresEntityManagerFactory",
        transactionManagerRef = "postgresTransactionManager"
)
public class PostgresDataSourceConfig {
    @Bean
    @ConfigurationProperties ("spring.datasource.postgres")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource() {
        return postgresDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "postgresEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean postgresEntityManagerFactory(
            @Qualifier ("postgresDataSource") DataSource dataSource
    ) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("org.sasha.mysqlpsqlsync.postgres.entity", "org.sasha.mysqlpsqlsync.models");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean(name = "postgresTransactionManager")
    public PlatformTransactionManager postgresTransactionManager(
            @Qualifier("postgresEntityManagerFactory") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }
}
