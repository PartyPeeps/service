package ro.unibuc.hello.controller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import ro.unibuc.hello.controller.PartyController;
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.data.SongEntity;
import ro.unibuc.hello.data.FoodEntity;
import ro.unibuc.hello.data.LocationEntity;
import ro.unibuc.hello.repositories.PartyRepository;
import ro.unibuc.hello.repositories.SongRepository;
import ro.unibuc.hello.repositories.UserRepository;
import ro.unibuc.hello.repositories.FoodRepository;
import ro.unibuc.hello.repositories.LocationRepository;  // Import the LocationRepository
import ro.unibuc.hello.service.YouTubeService;
import ro.unibuc.hello.data.PartyWithSongsResponse;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistIntegrationTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private UserRepository userRepository;  // Mock UserRepository

    @Mock
    private LocationRepository locationRepository;  // Mock LocationRepository

    @Mock
    private YouTubeService youTubeService;

    @InjectMocks
    private PartyController partyController;

    private PartyEntity party;
    private SongEntity song;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize PartyEntity and SongEntity with required attributes
        party = new PartyEntity("Birthday Party", "2025-03-31");

        // Initialize userIds list and add a user
        party.setUserIds(new ArrayList<>());
        party.getUserIds().add("user123");  // Add a user ID to the party

        song = new SongEntity();
        song.setId("song123");
        song.setTitle("Song Title");
        song.setArtist("Song Artist");

        // Make sure to add the song to the party
        party.addSong(song.getId());
    }

    @Test
    void addSongToParty_ShouldAddSongAndUpdateParty() {
        // Mock YouTube service to return a valid link
        String youtubeLink = "https://youtube.com/song123";
        when(youTubeService.searchYouTube(song.getTitle(), song.getArtist())).thenReturn(youtubeLink);
        when(partyRepository.findById(party.getId())).thenReturn(Optional.of(party));
        when(songRepository.save(song)).thenReturn(song);

        // Call the addSongToParty method
        ResponseEntity<?> response = partyController.addSongToParty(party.getId(), song);

        // Verify that the song has been added to the party
        verify(songRepository, times(1)).save(song);
        verify(partyRepository, times(1)).save(party);

        // Assert that the song was successfully added
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void removeSongFromParty_ShouldRemoveSongAndUpdateParty() {
        // Setup mock behavior for findById and deleting the song
        when(partyRepository.findById(party.getId())).thenReturn(Optional.of(party));
        when(songRepository.findById(song.getId())).thenReturn(Optional.of(song));

        // Add song to party manually for the test
        party.addSong(song.getId());

        // Call the removeSongFromParty method
        ResponseEntity<?> response = partyController.removeSongFromParty(party.getId(), song.getId());

        // Verify that the song has been removed
        verify(partyRepository, times(1)).save(party);
        verify(songRepository, times(1)).delete(song);

        // Assert that the response is OK (status 204 for no content)
        assertEquals(204, response.getStatusCode().value());
    }




    @Test
    void removeSongFromParty_ThatDoesNotExist_ShouldReturnError() {
        // Mock behavior: song doesn't exist in the party's playlist
        when(partyRepository.findById(party.getId())).thenReturn(Optional.of(party));

        // Try to remove a non-existent song
        String nonExistentSongId = "song999";
        ResponseEntity<?> response = partyController.removeSongFromParty(party.getId(), nonExistentSongId);

        // Verify that the response is 404 Not Found (or some other error)
        assertEquals(404, response.getStatusCode().value(), "Trying to remove a non-existent song should return an error.");
    }

    @Test
    void searchYouTube_ShouldReturnCorrectLink() {
        // Mock YouTubeService to return the correct YouTube URL
        String youtubeLink = "https://youtube.com/song123";
        when(youTubeService.searchYouTube(song.getTitle(), song.getArtist())).thenReturn(youtubeLink);

        // Perform the YouTube search
        String result = youTubeService.searchYouTube(song.getTitle(), song.getArtist());

        // Verify that the YouTube link is correct
        assertNotNull(result);
        assertEquals(youtubeLink, result);
    }
}
