package com.databasebackuputility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableScheduling
public class DatabaseBackupUtilityApplication {


    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DatabaseBackupUtilityApplication.class);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);

        System.exit(SpringApplication.exit(app.run(args)));
//        SpringApplication.run(DatabaseBackupUtilityApplication.class, args);
    }

}
