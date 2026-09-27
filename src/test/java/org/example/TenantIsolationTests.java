package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UserRepository userRepository;

    private record Account(String token, long userId, long organizationId) {
    }

    private Account register() throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Map<String, String> body = Map.of(
                "email", id + "@example.com",
                "password", "correct-horse-battery",
                "firstName", "Test",
                "lastName", "User",
                "organizationName", "Org " + id,
                "organizationSlug", "org-" + id);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return new Account(node.get("token").asText(), node.get("userId").asLong(), node.get("organizationId").asLong());
    }

    private MockHttpServletRequestBuilder as(Account account, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + account.token());
    }

    private String jsonBody(Map<String, ?> body) throws Exception {
        return json.writeValueAsString(body);
    }

    private long createProject(Account account) throws Exception {
        MvcResult result = mvc.perform(as(account, post("/api/organizations/{id}/projects", account.organizationId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("name", "Project", "description", "d"))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTask(Account account, long projectId) throws Exception {
        MvcResult result = mvc.perform(as(account, post("/api/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "Task", "projectId", projectId))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void passwordHashesAreNeverReturned() throws Exception {
        Account a = register();
        long project = createProject(a);
        createTask(a, project);

        mvc.perform(as(a, get("/api/tasks")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("$2a$"))));
        mvc.perform(as(a, get("/api/organizations/{id}/projects", a.organizationId())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void taskListsAreScopedToTheCallersOrganization() throws Exception {
        Account a = register();
        Account b = register();
        createTask(a, createProject(a));

        mvc.perform(as(a, get("/api/tasks"))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as(b, get("/api/tasks"))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherOrganizationsProjectsCannotBeReadOrCreated() throws Exception {
        Account a = register();
        Account b = register();
        createProject(a);

        mvc.perform(as(b, get("/api/organizations/{id}/projects", a.organizationId())))
                .andExpect(status().isNotFound());
        mvc.perform(as(b, post("/api/organizations/{id}/projects", a.organizationId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("name", "Planted"))))
                .andExpect(status().isNotFound());
        mvc.perform(as(a, get("/api/organizations/{id}/projects", a.organizationId())))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void anotherOrganizationsProjectsCannotBeChangedOrDeleted() throws Exception {
        Account a = register();
        Account b = register();
        long project = createProject(a);

        mvc.perform(as(b, put("/api/projects/{id}", project))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("name", "Hijacked"))))
                .andExpect(status().isNotFound());
        mvc.perform(as(b, delete("/api/projects/{id}", project))).andExpect(status().isNotFound());
        mvc.perform(as(a, get("/api/projects")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Project"));
    }

    @Test
    void tasksCannotBeCreatedInAnotherOrganizationsProject() throws Exception {
        Account a = register();
        Account b = register();
        long project = createProject(a);

        mvc.perform(as(b, post("/api/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "Planted", "projectId", project))))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherOrganizationsTasksCannotBeChangedOrDeleted() throws Exception {
        Account a = register();
        Account b = register();
        long task = createTask(a, createProject(a));

        mvc.perform(as(b, put("/api/tasks/{id}", task))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "Hijacked"))))
                .andExpect(status().isNotFound());
        mvc.perform(as(b, delete("/api/tasks/{id}", task))).andExpect(status().isNotFound());
        mvc.perform(as(a, get("/api/tasks")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Task"));
    }

    @Test
    void tasksCannotBeAssignedToAUserFromAnotherOrganization() throws Exception {
        Account a = register();
        Account b = register();
        long project = createProject(a);

        mvc.perform(as(a, post("/api/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "T", "projectId", project, "assignedToId", b.userId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assignee must belong to your organization"));
    }

    @Test
    void partialTaskUpdateOnlyChangesTheSentFields() throws Exception {
        Account a = register();
        long task = createTask(a, createProject(a));

        mvc.perform(as(a, put("/api/tasks/{id}", task))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.title").value("Task"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void invalidEnumValuesAreRejectedWithA400() throws Exception {
        Account a = register();
        long project = createProject(a);

        mvc.perform(as(a, post("/api/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "T", "projectId", project, "status", "NOPE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status"));
    }

    @Test
    void unauthenticatedAndMalformedTokenRequestsGet401() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organizationsCannotBeListedOrCreatedThroughTheApi() throws Exception {
        Account a = register();

        mvc.perform(post("/api/organizations/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("name", "X", "slug", "x"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(a, get("/api/organizations"))).andExpect(status().isNotFound());
    }

    @Test
    void badLoginReturns401WithAMessage() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("email", "nobody@example.com", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void registrationRejectsWeakPasswordsAndDuplicateEmails() throws Exception {
        Account a = register();

        Map<String, String> weak = Map.of("email", "weak@example.com", "password", "short", "firstName", "T",
                "lastName", "U", "organizationName", "Weak", "organizationSlug", "weak-org");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(jsonBody(weak)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("password")));

        String email = userRepository.findById(a.userId()).orElseThrow().getEmail();
        Map<String, String> duplicate = Map.of("email", email, "password", "correct-horse-battery", "firstName", "T",
                "lastName", "U", "organizationName", "Dup", "organizationSlug", "dup-" + UUID.randomUUID().toString().substring(0, 6));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(jsonBody(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void viewersAreReadOnly() throws Exception {
        Account a = register();
        long project = createProject(a);
        long task = createTask(a, project);

        User user = userRepository.findById(a.userId()).orElseThrow();
        user.setRole(User.Role.VIEWER);
        userRepository.save(user);

        mvc.perform(as(a, get("/api/tasks"))).andExpect(status().isOk());
        mvc.perform(as(a, post("/api/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "T", "projectId", project))))
                .andExpect(status().isForbidden());
        mvc.perform(as(a, delete("/api/tasks/{id}", task))).andExpect(status().isForbidden());
        mvc.perform(as(a, delete("/api/projects/{id}", project))).andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminsCanDeleteProjects() throws Exception {
        Account a = register();
        long project = createProject(a);

        User user = userRepository.findById(a.userId()).orElseThrow();
        user.setRole(User.Role.MEMBER);
        userRepository.save(user);

        mvc.perform(as(a, delete("/api/projects/{id}", project))).andExpect(status().isForbidden());
        mvc.perform(as(a, post("/api/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("title", "Members can write", "projectId", project))))
                .andExpect(status().isOk());
    }
}
