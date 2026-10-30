package org.iHarwood;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
/**
 * Configures MongoDB manually so it only activates when app.history.enabled=true.
 * Boot Mongo auto-configurations are excluded in application.properties,
 * keeping headless/no-Mongo deployments completely unaffected.
 */
@Configuration
@ConditionalOnProperty(name = "app.history.enabled", havingValue = "true")
@EnableMongoRepositories(basePackages = "org.iHarwood")
public class MongoConfig {
    private final String uri;
    private final String database;
    public MongoConfig(
            @Value("${spring.data.mongodb.uri}") String uri,
            @Value("${spring.data.mongodb.database:moonphase}") String database) {
        this.uri = uri;
        this.database = database;
    }
    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
