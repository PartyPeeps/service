package ro.unibuc.hello.service;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;  // <-- adăugat
import io.micrometer.core.instrument.Counter;
import ro.unibuc.hello.data.FoodEntity;
import ro.unibuc.hello.data.LocationEntity;

import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.data.TaskEntity;
import ro.unibuc.hello.data.UserEntity;  // Importă UserEntity pentru a lucra cu utilizatori
import ro.unibuc.hello.repositories.PartyRepository;

import ro.unibuc.hello.repositories.FoodRepository;
import ro.unibuc.hello.repositories.LocationRepository;

import java.util.ArrayList;

import ro.unibuc.hello.repositories.TaskRepository;
import ro.unibuc.hello.repositories.UserRepository;  // Importă UserRepository
import java.util.ArrayList;  // Importă pentru a inițializa lista dacă e null

import java.util.List;
import java.util.Optional;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final FoodRepository foodRepository;
    private final LocationRepository locationRepository;
    private final MeterRegistry meterRegistry;

    // Metrice
    private final Counter foodAddedCounter;
    private final Counter foodRemovedCounter;
    private final Counter locationSetCounter;
    private final Counter locationRemovedCounter;
    private final Counter foodFilterCounter;
    private final Counter locationFilterCounter;

    // Constructor pentru injectarea dependențelor
    public PartyService(PartyRepository partyRepository, UserRepository userRepository, TaskRepository taskRepository,
                        FoodRepository foodRepository, LocationRepository locationRepository, MeterRegistry meterRegistry) {
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.foodRepository = foodRepository;
        this.locationRepository = locationRepository;
        this.meterRegistry = meterRegistry;

        // Inițializare metrice
        this.foodAddedCounter = meterRegistry.counter("party.food.added");
        this.foodRemovedCounter = meterRegistry.counter("party.food.removed");
        this.locationSetCounter = meterRegistry.counter("party.location.set");
        this.locationRemovedCounter = meterRegistry.counter("party.location.removed");
        this.foodFilterCounter = meterRegistry.counter("party.food.filtered");
        this.locationFilterCounter = meterRegistry.counter("party.location.filtered");
    }

    public PartyEntity addUserToParty(String partyId, String userId) {
        PartyEntity party = partyRepository.findById(partyId).orElse(null);
        
        if (party != null) {
            // Verificăm dacă lista `userIds` este null și o inițializăm
            if (party.getUserIds() == null) {
                party.setUserIds(new ArrayList<>());
            }
    
            // Adăugăm utilizatorul doar dacă nu este deja în listă
            if (!party.getUserIds().contains(userId)) {
                party.getUserIds().add(userId);
                return partyRepository.save(party);  // Salvăm petrecerea actualizată
            }
        }
        return null;
    }
    

    public PartyEntity updatePartyPointsAfterTaskCompletion(String partyId, String userId, String taskId) {
        // Căutăm petrecerea
        PartyEntity partyEntity = partyRepository.findById(partyId).orElse(null);
    
        // Verificăm dacă petrecerea există
        if (partyEntity != null) {
            // Căutăm taskul completat
            TaskEntity taskEntity = taskRepository.findById(taskId).orElse(null);
    
            // Verificăm dacă taskul există și este completat
            if (taskEntity != null && taskEntity.isCompleted()) {
                // Adăugăm punctele taskului la petrecerea respectivă
                int taskPoints = taskEntity.getPoints();
                partyEntity.setPartyPoints(partyEntity.getPartyPoints() + taskPoints);
    
                // Căutăm utilizatorul care a completat taskul
                UserEntity userEntity = userRepository.findById(userId).orElse(null);
    
                // Verificăm dacă utilizatorul există
                if (userEntity != null) {
                    // Adăugăm punctele taskului la utilizator
                    userEntity.setPoints(userEntity.getPoints() + taskPoints);
    
                    // Salvăm utilizatorul actualizat
                    userRepository.save(userEntity);
                }
    
                // Salvăm petrecerea actualizată
                return partyRepository.save(partyEntity);
            }
        }
    
        // Dacă petrecerea, taskul sau utilizatorul nu există, returnăm null
        return null;
    }


    public List<PartyEntity> getAllParties() {
        return partyRepository.findAll();
    }

    public PartyEntity getPartyById(String id) {
        return partyRepository.findById(id).orElse(null);
    }    

    public List<PartyEntity> getPartiesForUser(String userId) {
        return partyRepository.findByUserIdsContaining(userId);
    }

    public PartyEntity createParty(PartyEntity party) {
        return partyRepository.save(party);
    }

    public List<FoodEntity> getAvailableFoodsForParty(String partyId, Double minRating, Double maxPrice, Integer maxPoints) {
        List<FoodEntity> foods = foodRepository.findAll();
        PartyEntity party = partyRepository.findById(partyId).orElse(null);
        int partyPoints = party.getPartyPoints();

        for (FoodEntity food : foods) {
            double discountedPrice = (partyPoints >= food.getDiscountPointsRequired())
                    ? food.getPrice() * 0.85
                    : food.getPrice();
            food.setDiscountedPrice(discountedPrice);
        }

        if (minRating != null || maxPrice != null || maxPoints != null) {
            foodFilterCounter.increment(); // Increment metrică
        }

        if (minRating != null) {
            foods = foods.stream().filter(food -> food.getRating() >= minRating).toList();
        }
        if (maxPrice != null) {
            foods = foods.stream().filter(food -> food.getDiscountedPrice() <= maxPrice).toList();
        }
        if (maxPoints != null) {
            foods = foods.stream().filter(food -> food.getDiscountPointsRequired() <= maxPoints).toList();
        }

        return foods;
    }

    public PartyEntity addFoodToParty(String partyId, String foodId) {
        Optional<PartyEntity> party = partyRepository.findById(partyId);
        if (party.isEmpty()) throw new RuntimeException("Party not found");

        Optional<FoodEntity> food = foodRepository.findById(foodId);
        if (food.isEmpty()) throw new RuntimeException("Food not found");

        party.get().getFoodIds().add(foodId);
        foodAddedCounter.increment(); // Increment metrică

        return partyRepository.save(party.get());
    }
    

    public List<LocationEntity> getAvailableLocationsForParty(String partyId, Double minRating, Double maxPrice, Integer maxPoints) {
        PartyEntity party = partyRepository.findById(partyId).orElse(null);
        int partyPoints = party.getPartyPoints();
        List<LocationEntity> locations = locationRepository.findAll();

        for (LocationEntity location : locations) {
            double discountedPrice = (partyPoints >= location.getDiscountPointsRequired())
                    ? location.getPrice() * 0.85
                    : location.getPrice();
            location.setDiscountedPrice(discountedPrice);
        }

        if (minRating != null || maxPrice != null || maxPoints != null) {
            locationFilterCounter.increment(); // Increment metrică
        }

        if (minRating != null) {
            locations = locations.stream().filter(location -> location.getRating() >= minRating).toList();
        }
        if (maxPrice != null) {
            locations = locations.stream().filter(location -> location.getDiscountedPrice() <= maxPrice).toList();
        }
        if (maxPoints != null) {
            locations = locations.stream().filter(location -> location.getDiscountPointsRequired() <= maxPoints).toList();
        }

        return locations;
    }    

    public PartyEntity addLocationToParty(String partyId, String locationId) {
        PartyEntity party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Party not found"));
        if (party.getLocationId() != null && party.getLocationId().equals(locationId)) {
            return party;
        }
        locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        party.setLocationId(locationId);
        locationSetCounter.increment(); // Increment metrică

        return partyRepository.save(party);
    }
    

    public PartyEntity removeLocationFromParty(String partyId) {
        Optional<PartyEntity> partyOpt = partyRepository.findById(partyId);
        if (partyOpt.isPresent()) {
            PartyEntity party = partyOpt.get();
            party.setLocationId(null);
            locationRemovedCounter.increment(); // Increment metrică
            return partyRepository.save(party);
        }
        return null;
    }

    public PartyEntity removeFoodFromParty(String partyId, String foodId) {
        Optional<PartyEntity> party = partyRepository.findById(partyId);
        if (party.isEmpty()) throw new RuntimeException("Party not found");

        party.get().getFoodIds().remove(foodId);
        foodRemovedCounter.increment(); // Increment metrică

        return partyRepository.save(party.get());
    }


}