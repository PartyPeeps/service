package ro.unibuc.hello.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.annotation.Timed;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@Service
public class CustomMetricsService {

    private final MeterRegistry meterRegistry;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public CustomMetricsService(MeterRegistry meterRegistry, MongoTemplate mongoTemplate) {
        this.meterRegistry = meterRegistry;
        this.mongoTemplate = mongoTemplate;
    }

    // Availability
    @Scheduled(fixedRate = 10000)
    public void reportAvailability() {
        boolean isMongoUp = false;
        try {
            isMongoUp = mongoTemplate.getDb().getName() != null;
        } catch (Exception e) {
            isMongoUp = false;
        }
        meterRegistry.gauge("app_availability", isMongoUp ? 1 : 0);
    }

    // Quality: increment error counter manually
    public void handleError(Exception e) {
        meterRegistry.counter("app_error_count").increment();
    }
}
