package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ro.unibuc.hello.data.LocationEntity;
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.repositories.LocationRepository;
import ro.unibuc.hello.repositories.PartyRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class LocationControllerIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withSharding();

    @BeforeAll
    public static void setUp() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://localhost:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private LocationRepository locationRepository;

    private PartyEntity party;
    private LocationEntity location;

    @BeforeEach
    public void cleanUpAndAddTestData() {
        partyRepository.deleteAll();
        locationRepository.deleteAll();

        party = new PartyEntity("Test Party", "2025-01-01");
        party = partyRepository.save(party);

        location = new LocationEntity("Club X", "123 Street", 100.0, 4.5, 50);
        location = locationRepository.save(location);
    }

    @Test
    public void testAddLocationToParty() throws Exception {
        mockMvc.perform(put("/parties/" + party.getId() + "/location/" + location.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(location.getId()));
    }

    @Test
    public void testRemoveLocationFromParty() throws Exception {
        party.setLocationId(location.getId());
        partyRepository.save(party);

        mockMvc.perform(delete("/parties/" + party.getId() + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").doesNotExist());
    }

    @Test
    public void testGetAvailableLocationsForParty() throws Exception {
        mockMvc.perform(get("/parties/" + party.getId() + "/locations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(location.getId()));
    }

    @Test
    public void testGetAvailableLocationsForParty_NoResults() throws Exception {
        LocationEntity lowRatedLocation = new LocationEntity("Low Rated Location", "456 Street", 120.0, 4.0, 60);
        locationRepository.save(lowRatedLocation);

        mockMvc.perform(get("/parties/" + party.getId() + "/locations").param("minRating", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void testAddLocationToParty_LocationNotFound() throws Exception {
        String invalidLocationId = "invalid-location-id";

        mockMvc.perform(put("/parties/" + party.getId() + "/location/" + invalidLocationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Location not found"));
    }




}
