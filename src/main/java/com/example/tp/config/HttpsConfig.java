package com.example.tp.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Exercice 5 : l'appli tourne en HTTPS sur 8443, on ajoute un port HTTP 8080
// qui redirige vers HTTPS. Avec app.https.redirect-http=false le HTTP est juste refuse.
@Configuration
@ConditionalOnProperty(name = "app.https.redirect-http", havingValue = "true")
public class HttpsConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> redirectionHttps(
            @Value("${app.https.http-port}") int httpPort,
            @Value("${server.port}") int httpsPort) {
        return factory -> {
            // oblige Tomcat a passer par un canal chiffre sur toutes les URLs
            factory.addContextCustomizers(context -> {
                SecurityConstraint contrainte = new SecurityConstraint();
                contrainte.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                contrainte.addCollection(collection);
                context.addConstraint(contrainte);
            });

            Connector http = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            http.setScheme("http");
            http.setPort(httpPort);
            http.setSecure(false);
            http.setRedirectPort(httpsPort);
            factory.addAdditionalTomcatConnectors(http);
        };
    }
}
