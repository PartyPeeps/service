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
import ro.unibuc.hello.data.TaskEntity;
import ro.unibuc.hello.repositories.TaskRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class TaskControllerIntegrationTest {

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
    private TaskRepository taskRepository;

    private TaskEntity task;

    @BeforeEach
    public void cleanUpAndAddTestData() {
        taskRepository.deleteAll();

        task = new TaskEntity("Test Task", "Test Description", 10, "party1", "user1");
        task = taskRepository.save(task);
    }

    @Test
    public void testCreateTask() throws Exception {
        TaskEntity newTask = new TaskEntity("New Task", "New Description", 15, "party2", "user2");
        ObjectMapper objectMapper = new ObjectMapper();
        
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Task"));
    }

    @Test
    public void testGetTaskById() throws Exception {
        mockMvc.perform(get("/tasks/" + task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()));
    }

    @Test
    public void testGetAllTasks() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(task.getId()));
    }

    @Test
    public void testGetTasksForParty() throws Exception {
        mockMvc.perform(get("/tasks/party/" + task.getPartyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partyId").value(task.getPartyId()));
    }

    @Test
    public void testGetTasksForUser() throws Exception {
        mockMvc.perform(get("/tasks/user/" + task.getAssignedUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignedUserId").value(task.getAssignedUserId()));
    }

    @Test
    public void testUpdateTask() throws Exception {
        task.setCompleted(true);
        ObjectMapper objectMapper = new ObjectMapper();
        
        mockMvc.perform(put("/tasks/" + task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    public void testDeleteTask() throws Exception {
        mockMvc.perform(delete("/tasks/" + task.getId()))
                .andExpect(status().isOk());
        
        Optional<TaskEntity> deletedTask = taskRepository.findById(task.getId());
        Assertions.assertTrue(deletedTask.isEmpty());
    }

    @Test
    public void testUpdateNonexistentTask() throws Exception {
        TaskEntity updatedTask = new TaskEntity("Updated Task", "Updated Description", 10, "party1", "user1");
        updatedTask.setId("999");
        ObjectMapper objectMapper = new ObjectMapper();
        
        mockMvc.perform(put("/tasks/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedTask)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }
}
