package ro.unibuc.hello.controller;
import io.micrometer.core.annotation.Timed;


import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.data.PartyWithSongsResponse;
import ro.unibuc.hello.data.SongEntity;
import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.repositories.PartyRepository;
import ro.unibuc.hello.dto.ErrorResponse;
import ro.unibuc.hello.metrics.CustomMetrics;


import ro.unibuc.hello.service.PartyService;
import ro.unibuc.hello.data.LocationEntity;
import ro.unibuc.hello.data.FoodEntity;

import ro.unibuc.hello.repositories.TaskRepository;
import ro.unibuc.hello.data.TaskEntity;
import ro.unibuc.hello.repositories.SongRepository;
import ro.unibuc.hello.service.YouTubeService;
import ro.unibuc.hello.repositories.UserRepository;
import ro.unibuc.hello.repositories.FoodRepository;
import ro.unibuc.hello.repositories.LocationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final CustomMetrics metrics; // Add the metrics field

    public PartyController(PartyRepository partyRepository, TaskRepository taskRepository, PartyService partyService,
                       SongRepository songRepository, YouTubeService youTubeService,
                       UserRepository userRepository, FoodRepository foodRepository, 
                       LocationRepository locationRepository, CustomMetrics metrics) { // Add metrics to constructor
        this.partyRepository = partyRepository;
        this.taskRepository = taskRepository;
        this.partyService = partyService;
        this.songRepository = songRepository;
        this.youTubeService = youTubeService;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.locationRepository = locationRepository;
        this.metrics = metrics; // Initialize metrics
    }

    @GetMapping
    public List<PartyWithSongsResponse> getAllParties() {
        // Start timing the operation
        Timer.Sample timer = metrics.startGetAllPartiesTimer();
        
        try {
            List<PartyEntity> parties = partyRepository.findAll();
            List<PartyWithSongsResponse> partyResponses = new ArrayList<>();
            
            // Count active parties
            int activeCount = 0;

            for (PartyEntity party : parties) {
                // Count active parties (has users)
                if (!party.getUserIds().isEmpty()) {
                    activeCount++;
                }
                
                // Record party points distribution
                metrics.recordPartyPoints(party.getPartyPoints());
                
                // Fetch Song Names
                List<String> songNames = new ArrayList<>();
                for (String songId : party.getPlaylistIds()) {
                    songRepository.findById(songId).ifPresent(song ->
                        songNames.add(song.getTitle() + " - " + song.getArtist() + " (" + song.getPath() + ")")
                    );
                }

                // Fetch User Names
                List<String> userNames = new ArrayList<>();
                for (String userId : party.getUserIds()) {
                    userRepository.findById(userId).ifPresent(user ->
                        userNames.add(user.getName())
                    );
                }

                // Fetch Food Names
                List<String> foodNames = new ArrayList<>();
                for (String foodId : party.getFoodIds()) {
                    foodRepository.findById(foodId).ifPresent(food ->
                        foodNames.add(food.getName())
                    );
                }

                // Fetch Task Descriptions
                List<String> taskDescriptions = new ArrayList<>();
                for (String taskId : party.getTaskIds()) {
                    taskRepository.findById(taskId).ifPresent(task ->
                        taskDescriptions.add(task.getDescription())
                    );
                }

                // Fetch Location Name
                String locationName = party.getLocationId() != null ?
                    locationRepository.findById(party.getLocationId()).map(LocationEntity::getName).orElse("Unknown Location")
                    : "Unknown Location";

                // Create the custom response object
                PartyWithSongsResponse response = new PartyWithSongsResponse(
                    party.getId(),
                    party.getName(),
                    party.getDate(),
                    locationName,
                    foodNames,
                    userNames,
                    songNames,
                    taskDescriptions,
                    party.getPartyPoints()
                );

                partyResponses.add(response);
            }
            
            // Update active parties gauge
            metrics.setActiveParties(activeCount);
            
            return partyResponses;
        } finally {
            // Stop timing the operation
            metrics.stopGetAllPartiesTimer(timer);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyWithSongsResponse> getPartyById(@PathVariable String id) {
        // Start timing the operation
        Timer.Sample timer = metrics.startGetPartyByIdTimer();
        
        try {
            Optional<PartyEntity> partyOptional = partyRepository.findById(id);
            if (partyOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            PartyEntity party = partyOptional.get();
            
            // Record party points for distribution summary
            metrics.recordPartyPoints(party.getPartyPoints());

            // Fetch song details for the party and concatenate title, artist, and path
            List<String> songNames = new ArrayList<>();
            for (String songId : party.getPlaylistIds()) {
                Optional<SongEntity> songOptional = songRepository.findById(songId);
                songOptional.ifPresent(song -> {
                    String songDetails = song.getTitle() + " - " + song.getArtist() + " (" + song.getPath() + ")";
                    songNames.add(songDetails);
                    
                    // Track song popularity
                    metrics.trackSongPopularity(songId);
                });
            }

            // Fetch user names for the party
            List<String> userNames = new ArrayList<>();
            for (String userId : party.getUserIds()) {
                Optional<UserEntity> userOptional = userRepository.findById(userId);
                userOptional.ifPresent(user -> userNames.add(user.getName()));
            }

            // Fetch location name for the party
            String locationName = "";
            Optional<LocationEntity> locationOptional = locationRepository.findById(party.getLocationId());
            if (locationOptional.isPresent()) {
                locationName = locationOptional.get().getName();
            }

            // Fetch food names for the party
            List<String> foodNames = new ArrayList<>();
            for (String foodId : party.getFoodIds()) {
                Optional<FoodEntity> foodOptional = foodRepository.findById(foodId);
                foodOptional.ifPresent(food -> foodNames.add(food.getName()));
            }

            // Fetch task descriptions for the party
            List<String> taskDescriptions = new ArrayList<>();
            for (String taskId : party.getTaskIds()) {
                Optional<TaskEntity> taskOptional = taskRepository.findById(taskId);
                taskOptional.ifPresent(task -> taskDescriptions.add(task.getDescription()));
            }

            // Create the custom response object
            PartyWithSongsResponse response = new PartyWithSongsResponse(
                    party.getId(),
                    party.getName(),
                    party.getDate(),
                    locationName,
                    foodNames,
                    userNames,
                    songNames,
                    taskDescriptions,
                    party.getPartyPoints());

            return ResponseEntity.ok(response);
        } finally {
            // Stop timing the operation
            metrics.stopGetPartyByIdTimer(timer);
        }
    }

    @GetMapping("/{partyId}/tasks")
    public List<TaskEntity> getTasksByParty(@PathVariable String partyId) {
        return taskRepository.findByPartyId(partyId);
    }

    @PostMapping
    public PartyEntity createParty(@RequestBody PartyEntity party) {
        PartyEntity savedParty = partyRepository.save(party);
        // Increment the party created counter
        metrics.incrementPartyCreated();
        // Record initial party points
        metrics.recordPartyPoints(party.getPartyPoints());
        
        logger.info("New party created: {}", savedParty.getName());
        return savedParty;
    }

    @PutMapping("/{id}")
    public PartyEntity updateParty(@PathVariable String id, @RequestBody PartyEntity updatedParty) {
        updatedParty.setId(id);
        PartyEntity savedParty = partyRepository.save(updatedParty);
        // Record updated party points
        metrics.recordPartyPoints(savedParty.getPartyPoints());
        return savedParty;
    }

    @DeleteMapping("/{id}")
    public void deleteParty(@PathVariable String id) {
        // Decrease active parties count
        Optional<PartyEntity> partyOptional = partyRepository.findById(id);
        if (partyOptional.isPresent() && !partyOptional.get().getUserIds().isEmpty()) {
            metrics.decrementActiveParties();
        }
        
        partyRepository.deleteById(id);
    }

    @PutMapping("/{partyId}/foods/{foodId}")
    public ResponseEntity<PartyEntity> addFoodToParty(@PathVariable String partyId, @PathVariable String foodId) {
        PartyEntity updatedParty = partyService.addFoodToParty(partyId, foodId);
        return updatedParty != null ? ResponseEntity.ok(updatedParty) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{partyId}/location/{locationId}")
    public ResponseEntity<Object> addLocationToParty(@PathVariable String partyId,
                                                    @PathVariable String locationId) {
        try {
            PartyEntity updatedParty = partyService.addLocationToParty(partyId, locationId);
            if (updatedParty != null) {
                // Increment the location assigned counter
                metrics.incrementLocationAssigned();
                logger.info("Location assigned to party: {}", partyId);
                return ResponseEntity.ok(updatedParty);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse("Location not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/{id}/locations")
    public List<LocationEntity> getAvailableLocationsForParty(
            @PathVariable String id,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer maxPoints) {

        return partyService.getAvailableLocationsForParty(id, minRating, maxPrice, maxPoints);
    }

    @GetMapping("/{id}/foods")
    public List<FoodEntity> getAvailableFoodsForParty(
            @PathVariable String id,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer maxPoints) {

        return partyService.getAvailableFoodsForParty(id, minRating, maxPrice, maxPoints);
    }

    @PostMapping("/{partyId}/addUser/{userId}")
    public ResponseEntity<?> addUserToParty(
            @PathVariable String partyId,
            @PathVariable String userId) {
        try {
            PartyEntity updatedParty = partyService.addUserToParty(partyId, userId);

            if (updatedParty != null) {
                // Increment the user added counter
                metrics.incrementUserAdded();
                
                // If this is the first user, increment active parties
                if (updatedParty.getUserIds().size() == 1) {
                    metrics.incrementActiveParties();
                }
                
                logger.info("User {} added to party {}", userId, partyId);
                return ResponseEntity.ok(updatedParty);
            } else {
                return ResponseEntity.status(400)
                        .body("Petrecerea sau utilizatorul nu există sau utilizatorul este deja adăugat.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Eroare internă a serverului: " + e.getMessage());
        }
    }

    @PostMapping("/{partyId}/user/{userId}/completeTask/{taskId}")
    public ResponseEntity<PartyEntity> completeTaskAndUpdatePartyPoints(
            @PathVariable String partyId,
            @PathVariable String userId,
            @PathVariable String taskId) {

        // Call the method from PartyService to update party points
        PartyEntity updatedParty = partyService.updatePartyPointsAfterTaskCompletion(partyId, userId, taskId);

        // If the party was successfully updated
        if (updatedParty != null) {
            // Increment the task completed counter
            metrics.incrementTaskCompleted();
            
            // Record the updated party points in the distribution summary
            metrics.recordPartyPoints(updatedParty.getPartyPoints());
            
            logger.info("Task {} completed by user {} in party {}", taskId, userId, partyId);
            return ResponseEntity.ok(updatedParty);
        } else {
            // If the party doesn't exist or the user isn't at the party
            return ResponseEntity.status(404).body(null);
        }
    }

    @PostMapping("/{partyId}/songs")
    public ResponseEntity<?> addSongToParty(@PathVariable String partyId, @RequestBody SongEntity song) {
        Optional<PartyEntity> partyOptional = partyRepository.findById(partyId);
        if (partyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Party not found");
        }

        // Fetch YouTube link
        String youtubeLink = youTubeService.searchYouTube(song.getTitle(), song.getArtist());
        if (youtubeLink == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not find song on YouTube");
        }

        // Save song with YouTube path
        song.setPath(youtubeLink);
        SongEntity savedSong = songRepository.save(song);

        // Add song ID to party
        PartyEntity party = partyOptional.get();
        party.addSong(savedSong.getId());
        partyRepository.save(party);

        // Increment the song added counter
        metrics.incrementSongAdded();
        
        // Track song popularity
        metrics.trackSongPopularity(savedSong.getId());
        
        logger.info("Song added to party: {} - {}", song.getTitle(), song.getArtist());

        return ResponseEntity.ok(savedSong);
    }

    @DeleteMapping("/{partyId}/songs/{songId}")
    public ResponseEntity<?> removeSongFromParty(@PathVariable String partyId, @PathVariable String songId) {
        Optional<PartyEntity> partyOptional = partyRepository.findById(partyId);
        if (partyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Party not found");
        }

        PartyEntity party = partyOptional.get();
        if (!party.getPlaylistIds().contains(songId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Song not found in the party playlist");
        }

        Optional<SongEntity> songOptional = songRepository.findById(songId);
        if (songOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Song not found");
        }

        party.removeSong(songId);
        partyRepository.save(party);
        songRepository.delete(songOptional.get());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{partyId}/tasks")
    public ResponseEntity<PartyEntity> addTaskToParty(@PathVariable String partyId, @RequestBody TaskEntity newTask) {
        // Find the party by ID
        Optional<PartyEntity> partyOptional = partyRepository.findById(partyId);
        
        // Check if the party exists
        if (partyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        
        // Create the new task and save it to the task repository
        TaskEntity savedTask = taskRepository.save(newTask);

        // Retrieve the party entity
        PartyEntity party = partyOptional.get();
        
        // Add the task ID to the party's task list
        party.addTask(savedTask.getId());

        // Add the task points to the party's total points
        party.setPartyPoints(party.getPartyPoints() + savedTask.getPoints());
        PartyEntity updatedParty = partyRepository.save(party);
        
        // Record the updated party points
        metrics.recordPartyPoints(updatedParty.getPartyPoints());

        return ResponseEntity.ok(updatedParty);
    }

    @DeleteMapping("/{partyId}/location")
    public ResponseEntity<PartyEntity> removeLocationFromParty(@PathVariable String partyId) {
        PartyEntity updatedParty = partyService.removeLocationFromParty(partyId);
        return updatedParty != null ? ResponseEntity.ok(updatedParty) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{partyId}/foods/{foodId}")
    public ResponseEntity<PartyEntity> removeFoodFromParty(@PathVariable String partyId, @PathVariable String foodId) {
        PartyEntity updatedParty = partyService.removeFoodFromParty(partyId, foodId);
        return updatedParty != null ? ResponseEntity.ok(updatedParty) : ResponseEntity.notFound().build();
    }
}