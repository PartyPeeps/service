package ro.unibuc.hello.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockitoAnnotations;

import ro.unibuc.hello.data.FoodEntity;
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.data.TaskEntity;
import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.repositories.PartyRepository;
import ro.unibuc.hello.repositories.TaskRepository;
import ro.unibuc.hello.repositories.UserRepository;
import ro.unibuc.hello.repositories.FoodRepository;
import ro.unibuc.hello.repositories.LocationRepository;
import ro.unibuc.hello.service.PartyService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ro.unibuc.hello.data.*;
import ro.unibuc.hello.repositories.*;
import ro.unibuc.hello.service.PartyService;

@ExtendWith(MockitoExtension.class)
public class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private FoodRepository foodRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private PartyService partyService;
    private PartyEntity party;
    private UserEntity user;
    private TaskEntity task;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        partyService = new PartyService(
            partyRepository,
            userRepository,
            taskRepository,
            foodRepository,
            locationRepository,
            meterRegistry
        );

        // Initialize test data
        user = new UserEntity("user1", "john.doe@example.com", "password123");
        
        party = new PartyEntity("Party Name", "2025-12-31", "user1");
        party.setId("party1");
        party.setPartyPoints(50);
        party.setUserIds(new ArrayList<>(List.of("user1")));
        party.setFoodIds(new ArrayList<>());
        partyService.addUserToParty("party1", "user1");
    }

    @Test
    void test_addUserToParty() {
        // Setup
        when(partyRepository.findById("party1")).thenReturn(Optional.of(party));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        
        // Test
        PartyEntity result = partyService.addUserToParty("party1", "user2");
        
        // Verify
        assertNotNull(result);
        assertTrue(result.getUserIds().contains("user2"));
        assertEquals(1, meterRegistry.counter("party.users.added").count());
    }

    @Test
    void test_updatePartyPointsAfterTaskCompletion() {
        // Setup
        task = new TaskEntity("task1", "Description of task1", 100, "party1", "user1");
        task.setCompleted(true);
        
        when(partyRepository.findById("party1")).thenReturn(Optional.of(party));
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        
        // Test
        PartyEntity result = partyService.updatePartyPointsAfterTaskCompletion("party1", "user1", "task1");
        
        // Verify
        assertNotNull(result);
        assertEquals(150, result.getPartyPoints());
        assertEquals(1, meterRegistry.counter("party.tasks.completed").count());
    }

    @Test
    void test_getPartiesForUser() {
        // Setup
        when(partyRepository.findByUserIdsContaining("user1")).thenReturn(List.of(party));
        
        // Test
        List<PartyEntity> result = partyService.getPartiesForUser("user1");
        
        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("party1", result.get(0).getId());
    }

    @Test
    void test_addFoodToParty() {
        // Setup
        when(partyRepository.findById("party1")).thenReturn(Optional.of(party));
        when(foodRepository.findById("food1")).thenReturn(Optional.of(new FoodEntity()));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        
        // Test
        PartyEntity result = partyService.addFoodToParty("party1", "food1");
        
        // Verify
        assertNotNull(result);
        assertEquals(1, meterRegistry.counter("party.food.added").count());
    }

    @Test
    void test_removeFoodFromParty() {
        // Setup
        party.getFoodIds().add("food1");
        when(partyRepository.findById("party1")).thenReturn(Optional.of(party));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        
        // Test
        PartyEntity result = partyService.removeFoodFromParty("party1", "food1");
        
        // Verify
        assertNotNull(result);
        assertEquals(1, meterRegistry.counter("party.food.removed").count());
    }
}