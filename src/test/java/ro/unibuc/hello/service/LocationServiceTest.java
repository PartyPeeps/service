package ro.unibuc.hello.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import ro.unibuc.hello.data.LocationEntity;
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.repositories.FoodRepository;
import ro.unibuc.hello.repositories.LocationRepository;
import ro.unibuc.hello.repositories.PartyRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private Counter locationSetCounter;

    @Mock
    private Counter locationRemovedCounter;

    @Mock
    private Counter locationFilterCounter;

    @Mock
    private MeterRegistry meterRegistry;


    @InjectMocks
    private PartyService partyService;

    private PartyEntity testParty;
    private LocationEntity location1;
    private LocationEntity location2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testParty = new PartyEntity("Birthday Bash", "2025-05-15");
        testParty.setId("party123");
        testParty.setPartyPoints(100);

        location1 = new LocationEntity("Club X", "123 Main St", 200, 4.5, 50);
        location1.setId("loc1");

        location2 = new LocationEntity("Lounge Y", "456 Side St", 150, 4.0, 120);
        location2.setId("loc2");

        // Inițializăm Counters din MeterRegistry
        when(meterRegistry.counter("party.location.set")).thenReturn(locationSetCounter);
        when(meterRegistry.counter("party.location.removed")).thenReturn(locationRemovedCounter);
        when(meterRegistry.counter("party.location.filtered")).thenReturn(locationFilterCounter);


        // Creăm explicit service-ul cu toți parametrii
        partyService = new PartyService(partyRepository, null, null, foodRepository, locationRepository, meterRegistry);

    }

    @Test
    void testGetAvailableLocationsForParty() {
        when(partyRepository.findById("party123")).thenReturn(Optional.of(testParty));
        when(locationRepository.findAll()).thenReturn(Arrays.asList(location1, location2));

        List<LocationEntity> result = partyService.getAvailableLocationsForParty("party123", 4.0, 180.0, 100);

        assertEquals(1, result.size());
        assertEquals("loc1", result.get(0).getId());
    }

    @Test
    void testGetAvailableLocationsForParty_NoMatchingLocations() {
        when(partyRepository.findById("party123")).thenReturn(Optional.of(testParty));
        when(locationRepository.findAll()).thenReturn(Arrays.asList(location1, location2));

        List<LocationEntity> result = partyService.getAvailableLocationsForParty("party123", 5.0, 100.0, 50);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAddLocationToParty() {
        when(partyRepository.findById("party123")).thenReturn(Optional.of(testParty));
        when(locationRepository.findById("loc1")).thenReturn(Optional.of(location1));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(testParty);

        PartyEntity updatedParty = partyService.addLocationToParty("party123", "loc1");

        assertNotNull(updatedParty);
        assertEquals("loc1", updatedParty.getLocationId());
    }

    @Test
    void testAddLocationToParty_LocationNotFound() {
        when(partyRepository.findById("party123")).thenReturn(Optional.of(testParty));
        when(locationRepository.findById("loc1")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> partyService.addLocationToParty("party123", "loc1"));
    }

    @Test
    void testAddLocationToParty_LocationAlreadyAdded() {
        // Setează locația deja adăugată
        testParty.setLocationId("loc1");

        // Setup mock-uri pentru repo-uri
        when(partyRepository.findById("party123")).thenReturn(Optional.of(testParty));
        when(locationRepository.findById("loc1")).thenReturn(Optional.of(location1));

        // Apelăm metoda și verificăm că nu a fost modificată locația
        PartyEntity updatedParty = partyService.addLocationToParty("party123", "loc1");

        assertNotNull(updatedParty);  // Verificăm că party este valid
        assertEquals("loc1", updatedParty.getLocationId());  // Verificăm că locația este corectă

        // Verificăm că metoda save nu a fost invocată
        verify(partyRepository, times(0)).save(any(PartyEntity.class)); 
    }

    @Test
    void testRemoveLocationFromParty() {
        testParty.setLocationId("loc1");
        when(partyRepository.findById("party123")).thenReturn(Optional.of(testParty));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(testParty);

        PartyEntity updatedParty = partyService.removeLocationFromParty("party123");

        assertNotNull(updatedParty);
        assertNull(updatedParty.getLocationId());
    }
}
