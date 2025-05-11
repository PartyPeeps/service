package ro.unibuc.hello;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import ro.unibuc.hello.data.InformationEntity;
import ro.unibuc.hello.data.InformationRepository;

import ro.unibuc.hello.data.UserEntity;
import ro.unibuc.hello.repositories.UserRepository;
import ro.unibuc.hello.data.LocationEntity;
import ro.unibuc.hello.repositories.LocationRepository;
import ro.unibuc.hello.data.PartyEntity;
import ro.unibuc.hello.repositories.PartyRepository;
import ro.unibuc.hello.data.SongEntity;
import ro.unibuc.hello.repositories.SongRepository;
import ro.unibuc.hello.data.TaskEntity;
import ro.unibuc.hello.repositories.TaskRepository;
import ro.unibuc.hello.data.FoodEntity;
import ro.unibuc.hello.repositories.FoodRepository;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = {
        InformationRepository.class, UserRepository.class, LocationRepository.class, PartyRepository.class,
        SongRepository.class, TaskRepository.class, FoodRepository.class
})

public class PartyPlanningApp {

	@Autowired
	private InformationRepository informationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private FoodRepository foodRepository;

	public static void main(String[] args) {
		SpringApplication.run(PartyPlanningApp.class, args);
	}

	@PostConstruct
	public void runAfterObjectCreated() {
		informationRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        partyRepository.deleteAll();
        songRepository.deleteAll();
        taskRepository.deleteAll();
        foodRepository.deleteAll();
		
        UserEntity user1 = userRepository.save(new UserEntity("John Doe", "john@example.com", "securepassword"));
        UserEntity user2 = userRepository.save(new UserEntity("Paul Rudd", "paul@example.com", "securepassword1"));
        UserEntity user3 = userRepository.save(new UserEntity("Tony Stark", "tony@example.com", "securepassword2"));

        locationRepository.save(new LocationEntity("Club X", "123 Party Street", 700, 5, 200));
        locationRepository.save(new LocationEntity("Club Z", "456 Party Street", 500, 3.2, 150));

        foodRepository.save(new FoodEntity("Pizza", 20, 4.5, 30));
        foodRepository.save(new FoodEntity("Pasta", 30, 5, 50));
        foodRepository.save(new FoodEntity("Taco", 50, 3.8, 90));
        foodRepository.save(new FoodEntity("KFC", 40, 4.9, 70));

        // Creare și salvare petrecere 1
        PartyEntity party1 = new PartyEntity("Party1", "12.05.2025");
        party1.setPartyPoints(175);
        party1.setUserIds(new ArrayList<>());
        party1.getUserIds().add(user1.getId());
        party1.getUserIds().add(user2.getId()); // Adăugăm doi useri la Party1
        party1 = partyRepository.save(party1); 

        // Creare și salvare petrecere 2
        PartyEntity party2 = new PartyEntity("Party2", "10.08.2025");
        party2.setUserIds(new ArrayList<>());
        party2.getUserIds().add(user3.getId()); // Un user la Party2
        party2 = partyRepository.save(party2);
        
        // Creare și asignare task-uri
        TaskEntity task1 = new TaskEntity("Task 100 Points", "Un task important", 100, party1.getId(), user1.getId());
        task1.setCompleted(true);
        
        TaskEntity task2 = new TaskEntity("Task 200 Points", "Un task și mai important", 200, party1.getId(), user2.getId());
        task2.setCompleted(true);

        TaskEntity task3 = new TaskEntity("Task 300 Points", "Task pentru petrecerea 2", 300, party2.getId(), user3.getId());
        task3.setCompleted(true);

        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);

	}

}
