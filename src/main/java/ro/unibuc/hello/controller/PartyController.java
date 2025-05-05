package ro.unibuc.hello.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.unibuc.hello.data.*;
import ro.unibuc.hello.dto.ErrorResponse;
import ro.unibuc.hello.metrics.CustomMetrics;
import ro.unibuc.hello.repositories.*;
import ro.unibuc.hello.service.PartyService;
import ro.unibuc.hello.service.YouTubeService;

import java.util.*;

@RestController
@RequestMapping("/parties")
public class PartyController {

    private static final Logger logger = LoggerFactory.getLogger(PartyController.class);

    private final PartyRepository partyRepository;
    private final TaskRepository taskRepository;
    private final PartyService partyService;
    private final SongRepository songRepository;
    private final YouTubeService youTubeService;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final LocationRepository locationRepository;
    private final CustomMetrics customMetrics;

    public PartyController(PartyRepository partyRepository,
                           TaskRepository taskRepository,
                           PartyService partyService,
                           SongRepository songRepository,
                           YouTubeService youTubeService,
                           UserRepository userRepository,
                           FoodRepository foodRepository,
                           LocationRepository locationRepository,
                           CustomMetrics customMetrics) {

        this.partyRepository = partyRepository;
        this.taskRepository = taskRepository;
        this.partyService = partyService;
        this.songRepository = songRepository;
        this.youTubeService = youTubeService;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.locationRepository = locationRepository;
        this.customMetrics = customMetrics;
    }

    @GetMapping
    public List<PartyWithSongsResponse> getAllParties() {
        customMetrics.incrementGetAllParties(); // METRICĂ

        List<PartyEntity> parties = partyRepository.findAll();
        List<PartyWithSongsResponse> partyResponses = new ArrayList<>();

        for (PartyEntity party : parties) {
            List<String> songNames = new ArrayList<>();
            for (String songId : party.getPlaylistIds()) {
                songRepository.findById(songId).ifPresent(song ->
                        songNames.add(song.getTitle() + " - " + song.getArtist() + " (" + song.getPath() + ")"));
            }

            List<String> userNames = new ArrayList<>();
            for (String userId : party.getUserIds()) {
                userRepository.findById(userId).ifPresent(user -> userNames.add(user.getName()));
            }

            List<String> foodNames = new ArrayList<>();
            for (String foodId : party.getFoodIds()) {
                foodRepository.findById(foodId).ifPresent(food -> foodNames.add(food.getName()));
            }

            List<String> taskDescriptions = new ArrayList<>();
            for (String taskId : party.getTaskIds()) {
                taskRepository.findById(taskId).ifPresent(task -> taskDescriptions.add(task.getDescription()));
            }

            String locationName = party.getLocationId() != null ?
                    locationRepository.findById(party.getLocationId()).map(LocationEntity::getName).orElse("Unknown Location")
                    : "Unknown Location";

            partyResponses.add(new PartyWithSongsResponse(
                    party.getId(), party.getName(), party.getDate(),
                    locationName, foodNames, userNames,
                    songNames, taskDescriptions, party.getPartyPoints()));
        }

        return partyResponses;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyWithSongsResponse> getPartyById(@PathVariable String id) {
        customMetrics.incrementGetPartyById(); // METRICĂ

        Optional<PartyEntity> partyOptional = partyRepository.findById(id);
        if (partyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        PartyEntity party = partyOptional.get();

        List<String> songNames = new ArrayList<>();
        for (String songId : party.getPlaylistIds()) {
            songRepository.findById(songId).ifPresent(song ->
                    songNames.add(song.getTitle() + " - " + song.getArtist() + " (" + song.getPath() + ")"));
        }

        List<String> userNames = new ArrayList<>();
        for (String userId : party.getUserIds()) {
            userRepository.findById(userId).ifPresent(user -> userNames.add(user.getName()));
        }

        List<String> foodNames = new ArrayList<>();
        for (String foodId : party.getFoodIds()) {
            foodRepository.findById(foodId).ifPresent(food -> foodNames.add(food.getName()));
        }

        List<String> taskDescriptions = new ArrayList<>();
        for (String taskId : party.getTaskIds()) {
            taskRepository.findById(taskId).ifPresent(task -> taskDescriptions.add(task.getDescription()));
        }

        String locationName = party.getLocationId() != null ?
                locationRepository.findById(party.getLocationId()).map(LocationEntity::getName).orElse("Unknown Location")
                : "Unknown Location";

        PartyWithSongsResponse response = new PartyWithSongsResponse(
                party.getId(), party.getName(), party.getDate(),
                locationName, foodNames, userNames,
                songNames, taskDescriptions, party.getPartyPoints());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{partyId}/songs")
    public ResponseEntity<?> addSongToParty(@PathVariable String partyId, @RequestBody SongEntity song) {
        customMetrics.incrementAddSong(); // METRICĂ

        Optional<PartyEntity> partyOptional = partyRepository.findById(partyId);
        if (partyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Party not found");
        }

        String youtubeLink = youTubeService.searchYouTube(song.getTitle(), song.getArtist());
        if (youtubeLink == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not find song on YouTube");
        }

        song.setPath(youtubeLink);
        SongEntity savedSong = songRepository.save(song);

        PartyEntity party = partyOptional.get();
        party.addSong(savedSong.getId());
        partyRepository.save(party);

        return ResponseEntity.ok(savedSong);
    }

    @PostMapping("/{partyId}/addUser/{userId}")
    public ResponseEntity<?> addUserToParty(@PathVariable String partyId, @PathVariable String userId) {
        customMetrics.incrementAddUser(); // METRICĂ

        try {
            PartyEntity updatedParty = partyService.addUserToParty(partyId, userId);
            if (updatedParty != null) {
                return ResponseEntity.ok(updatedParty);
            } else {
                return ResponseEntity.status(400).body("Petrecerea sau utilizatorul nu există sau utilizatorul este deja adăugat.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Eroare internă: " + e.getMessage());
        }
    }

    @PostMapping("/{partyId}/user/{userId}/completeTask/{taskId}")
    public ResponseEntity<PartyEntity> completeTaskAndUpdatePartyPoints(
            @PathVariable String partyId,
            @PathVariable String userId,
            @PathVariable String taskId) {

        customMetrics.incrementCompleteTask(); // METRICĂ

        PartyEntity updatedParty = partyService.updatePartyPointsAfterTaskCompletion(partyId, userId, taskId);
        return updatedParty != null ? ResponseEntity.ok(updatedParty) : ResponseEntity.status(404).body(null);
    }

    // Restul metodelor (create, update, delete etc.) rămân neschimbate

    // Exemplu: create party
    @PostMapping
    public PartyEntity createParty(@RequestBody PartyEntity party) {
        return partyRepository.save(party);
    }

    @PutMapping("/{id}")
    public PartyEntity updateParty(@PathVariable String id, @RequestBody PartyEntity updatedParty) {
        updatedParty.setId(id);
        return partyRepository.save(updatedParty);
    }

    @DeleteMapping("/{id}")
    public void deleteParty(@PathVariable String id) {
        partyRepository.deleteById(id);
    }

    // Alte metode (add/remove food/location/task) rămân neschimbate
}
