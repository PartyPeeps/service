package ro.unibuc.hello.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CustomMetrics {

    private final Counter getAllPartiesCounter;
    private final Timer getAllPartiesTimer;
    private final Counter addUserCounter;
    private final Counter songsAddedCounter;
    private final Counter partyCreatedCounter;
    private final Counter getPartyByIdCounter;
    private final Counter completeTaskCounter;

    public CustomMetrics(MeterRegistry registry) {
        this.getAllPartiesCounter = registry.counter("parties_get_all_total");
        this.getAllPartiesTimer = registry.timer("parties_get_all_duration");
        this.addUserCounter = registry.counter("parties_add_user_total");
        this.songsAddedCounter = registry.counter("parties_songs_added_total");
        this.partyCreatedCounter = registry.counter("parties_created_total");
        this.getPartyByIdCounter = registry.counter("parties_get_by_id_total");
        this.completeTaskCounter = registry.counter("parties_task_completed_total");
    }

    public void countGetAllParties() {
        getAllPartiesCounter.increment();
    }

    public Timer getGetAllPartiesTimer() {
        return getAllPartiesTimer;
    }

    public void countAddUser() {
        addUserCounter.increment();
    }

    public void countSongAdded() {
        songsAddedCounter.increment();
    }

    public void countPartyCreated() {
        partyCreatedCounter.increment();
    }

    public void incrementGetPartyById() {
        getPartyByIdCounter.increment();
    }

    public void incrementAddUser() {
        addUserCounter.increment(); // reuse
    }

    public void incrementCompleteTask() {
        completeTaskCounter.increment();
    }

    public void incrementAddSong() {
        songsAddedCounter.increment(); // reuse
    }

    public void incrementGetAllParties() {
        getAllPartiesCounter.increment(); // reuse
    }
}
