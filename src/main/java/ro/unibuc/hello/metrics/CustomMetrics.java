package ro.unibuc.hello.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CustomMetrics {

    private final Counter partyCreatedCounter;
    private final Counter songAddedCounter;
    private final Counter taskCompletedCounter;
    private final Counter userAddedToPartyCounter;
    private final Counter locationAssignedCounter;
    
    // Timer to measure API response times
    private final Timer getPartyByIdTimer;
    private final Timer getAllPartiesTimer;
    
    // Gauge to track active parties (parties that have users and are not completed)
    private final AtomicInteger activeParties = new AtomicInteger(0);
    
    // DistributionSummary to track party points distribution
    private final DistributionSummary partyPointsDistribution;
    
    // Map to track song popularity by count
    private final Map<String, AtomicInteger> songPopularity = new ConcurrentHashMap<>();

    public CustomMetrics(MeterRegistry registry) {
        // Counters
        this.partyCreatedCounter = Counter.builder("party.created.count")
                .description("Total number of parties created")
                .register(registry);

        this.songAddedCounter = Counter.builder("party.song.added.count")
                .description("Total number of songs added to parties")
                .register(registry);

        this.taskCompletedCounter = Counter.builder("party.task.completed.count")
                .description("Total number of tasks completed")
                .register(registry);

        this.userAddedToPartyCounter = Counter.builder("party.user.added.count")
                .description("Total number of users added to parties")
                .register(registry);

        this.locationAssignedCounter = Counter.builder("party.location.assigned.count")
                .description("Total number of locations assigned to parties")
                .register(registry);
        
        // Timers
        this.getPartyByIdTimer = Timer.builder("party.get.by.id.time")
                .description("Time taken to retrieve a party by ID")
                .register(registry);
                
        this.getAllPartiesTimer = Timer.builder("party.get.all.time")
                .description("Time taken to retrieve all parties")
                .register(registry);
        
        // Gauge
        Gauge.builder("party.active.count", activeParties, AtomicInteger::get)
                .description("Current number of active parties")
                .register(registry);
        
        // Distribution Summary
        this.partyPointsDistribution = DistributionSummary.builder("party.points.distribution")
                .description("Distribution of party points")
                .register(registry);
    }

    // Counter increment methods
    public void incrementPartyCreated() {
        partyCreatedCounter.increment();
        activeParties.incrementAndGet(); // Assuming a new party is active by default
    }

    public void incrementSongAdded() {
        songAddedCounter.increment();
    }

    public void incrementTaskCompleted() {
        taskCompletedCounter.increment();
    }

    public void incrementUserAdded() {
        userAddedToPartyCounter.increment();
    }

    public void incrementLocationAssigned() {
        locationAssignedCounter.increment();
    }
    
    // Timer methods
    public Timer.Sample startGetPartyByIdTimer() {
        return Timer.start();
    }
    
    public void stopGetPartyByIdTimer(Timer.Sample sample) {
        sample.stop(getPartyByIdTimer);
    }
    
    public Timer.Sample startGetAllPartiesTimer() {
        return Timer.start();
    }
    
    public void stopGetAllPartiesTimer(Timer.Sample sample) {
        sample.stop(getAllPartiesTimer);
    }
    
    // Gauge methods
    public void incrementActiveParties() {
        activeParties.incrementAndGet();
    }
    
    public void decrementActiveParties() {
        activeParties.decrementAndGet();
    }
    
    public void setActiveParties(int count) {
        activeParties.set(count);
    }
    
    // Distribution Summary methods
    public void recordPartyPoints(double points) {
        partyPointsDistribution.record(points);
    }
    
    // Song popularity tracking
    public void trackSongPopularity(String songId) {
        songPopularity.computeIfAbsent(songId, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    public int getSongPopularity(String songId) {
        AtomicInteger counter = songPopularity.get(songId);
        return counter != null ? counter.get() : 0;
    }
}