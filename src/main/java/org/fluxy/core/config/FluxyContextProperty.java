package org.fluxy.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fluxy.context")
@Data
public class FluxyContextProperty {

    private boolean enablePersistence = true;

}
