package org.fluxy.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EnableJpaRepositories(basePackages = {"org.fluxy.context.persistence.repository"})
@EntityScan(basePackages = {"org.fluxy.context.persistence.entity"})
@ComponentScan(basePackages = {"org.fluxy.context.service"})
public class FluxyContextAutoConfiguration {

    // Averiguar como agregar dinamicamente paquetes al component scan

}

