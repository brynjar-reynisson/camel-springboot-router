package com.breynisson.router;

import com.breynisson.router.jdbc.DatabaseAdapter;
import org.springframework.boot.SpringApplication;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        DatabaseAdapter.init();
        SpringApplication.run(SpringBootApplication.class, args);
    }

}
