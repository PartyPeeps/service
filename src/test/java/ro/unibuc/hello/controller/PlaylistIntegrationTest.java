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
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.data.SongEntity;
import ro.unibuc.hello.repositories.PartyRepository;
import ro.unibuc.hello.repositories.SongRepository;
import ro.unibuc.hello.service.YouTubeService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class PlaylistIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withSharding();


    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://host.docker.internal:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private YouTubeService youTubeService;

    private PartyEntity party;
    private SongEntity song;

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();
        partyRepository.deleteAll();

        song = new SongEntity();
        song.setTitle("Song Title");
        song.setArtist("Song Artist");
        song.setPath("https://youtube.com/song123");
        song = songRepository.save(song);

        party = new PartyEntity("Birthday Party", "2025-03-31");
        party.setUserIds(Collections.singletonList("user123"));
        party.setPlaylistIds(Collections.singletonList(song.getId()));
        party = partyRepository.save(party);
    }

    @Test
    @Order(1)
    void addSongToParty_ShouldAddSongAndUpdateParty() throws Exception {
        SongEntity newSong = new SongEntity();
        newSong.setTitle("New Song");
        newSong.setArtist("New Artist");

        mockMvc.perform(post("/parties/" + party.getId() + "/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newSong)))
                .andExpect(status().isOk());

        PartyEntity updatedParty = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(updatedParty.getPlaylistIds()).hasSize(2);
    }

    @Test
    @Order(2)
    void removeSongFromParty_ShouldRemoveSongAndUpdateParty() throws Exception {
        mockMvc.perform(delete("/parties/" + party.getId() + "/songs/" + song.getId()))
                .andExpect(status().isNoContent());

        PartyEntity updatedParty = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(updatedParty.getPlaylistIds()).doesNotContain(song.getId());
    }

    @Test
    @Order(3)
    void removeSongFromParty_ThatDoesNotExist_ShouldReturnError() throws Exception {
        mockMvc.perform(delete("/parties/" + party.getId() + "/songs/song999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    void searchYouTube_ShouldReturnCorrectLink() {
        String result = youTubeService.searchYouTube(song.getTitle(), song.getArtist());
        assertThat(result).isEqualTo("https://www.youtube.com/watch?v=DS-raAyMxl4");
    }
}
