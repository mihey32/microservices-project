package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.request.ChangeBlockStatusRequest;
import com.ecommerce.auth_service.dto.request.ChangeRoleRequest;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(username = AdminControllerTest.ADMIN_EMAIL, roles = "ADMIN")
@Transactional
public class AdminControllerTest extends AbstractIntegrationTest {
    static final String ADMIN_EMAIL = "admin@test.local";
    static final String USER_EMAIL = "user@test.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AccountService accountService;

    private WebTestClient client;
    private AccountDto adminAccount;
    private AccountDto userAccount;

    @BeforeEach
    void setUp() {
        client = MockMvcWebTestClient.bindTo(mockMvc).build();
        Set<String> keys = redisTemplate.keys("rl:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);

        adminAccount = accountService.register(ADMIN_EMAIL, "qwerty1234");
        accountService.markEmailVerified(adminAccount.id());
        accountService.changeRole(adminAccount.id(), Role.ROLE_ADMIN);
        adminAccount = accountService.findAccountById(adminAccount.id()).orElseThrow();

        userAccount = accountService.register(USER_EMAIL, "qwerty1234");
        accountService.markEmailVerified(userAccount.id());
        userAccount = accountService.findAccountById(userAccount.id()).orElseThrow();
    }

    @Test
    void changeRole_asAdmin_returns200AndUpdatesRole() {
        client.patch().uri("/admin/accounts/{id}/role", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeRoleRequest(Role.ROLE_MANAGER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.role").isEqualTo("ROLE_MANAGER");

        assertThat(accountService.findAccountById(userAccount.id()).orElseThrow().role())
                .isEqualTo(Role.ROLE_MANAGER);
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "USER")
    void changeRole_withoutAdminRole_returns403() {
        client.patch().uri("/admin/accounts/{id}/role", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeRoleRequest(Role.ROLE_ADMIN))
                .exchange()
                .expectStatus().isForbidden();

        assertThat(accountService.findAccountById(userAccount.id()).orElseThrow().role())
                .isEqualTo(Role.ROLE_USER);
    }

    @Test
    @WithAnonymousUser
    void changeRole_withoutAuth_returns401() {
        client.patch().uri("/admin/accounts/{id}/role", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeRoleRequest(Role.ROLE_MANAGER))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void changeRole_selfDemotion_returns409() {
        client.patch().uri("/admin/accounts/{id}/role", adminAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeRoleRequest(Role.ROLE_USER))
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(accountService.findAccountById(adminAccount.id()).orElseThrow().role())
                .isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void changeRole_targetNotFound_returns404() {
        client.patch().uri("/admin/accounts/{id}/role", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeRoleRequest(Role.ROLE_MANAGER))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void changeRole_nullRole_returns400() {
        client.patch().uri("/admin/accounts/{id}/role", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeRoleRequest(null))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void changeBlockStatus_asAdmin_returns200AndUpdatesStatus() {
        client.patch().uri("/admin/accounts/{id}/block", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeBlockStatusRequest(true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.blocked").isEqualTo(true);

        assertThat(accountService.findAccountById(userAccount.id()).orElseThrow().blocked())
                .isTrue();
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "USER")
    void changeBlockStatus_withoutAdminRole_returns403() {
        client.patch().uri("/admin/accounts/{id}/block", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeBlockStatusRequest(true))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void changeBlockStatus_selfBlock_returns409() {
        client.patch().uri("/admin/accounts/{id}/block", adminAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeBlockStatusRequest(true))
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(accountService.findAccountById(adminAccount.id()).orElseThrow().blocked())
                .isFalse();
    }

    @Test
    void changeBlockStatus_targetNotFound_returns404() {
        client.patch().uri("/admin/accounts/{id}/block", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeBlockStatusRequest(true))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void changeBlockStatus_nullBlocked_returns400() {
        client.patch().uri("/admin/accounts/{id}/block", userAccount.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangeBlockStatusRequest(null))
                .exchange()
                .expectStatus().isBadRequest();
    }
}