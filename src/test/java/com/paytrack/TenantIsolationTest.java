package com.paytrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantIsolationTest {

    @LocalServerPort int port;

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;

    @Test
    void userOfTenantA_canLogin_and_sees_onlyOwnInvoices() throws Exception {
        String token = login("alice", "Passw0rd!");
        var res = rest.exchange(url("/api/v1/invoices"), HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        JsonNode arr = json.readTree(res.getBody());
        assertThat(arr).isNotEmpty();
        for (JsonNode inv : arr) {
            assertThat(inv.get("tenantId").asText())
                    .isEqualTo("11111111-1111-1111-1111-111111111111");
        }
    }

    @Test
    void userOfTenantA_cannotFetchInvoiceOfTenantB_byId() throws Exception {
        // A-Customer-1 = id 1, A-Customer-2 = id 2, B-Customer-1 = id 3
        String tokenA = login("alice", "Passw0rd!");
        var res = rest.exchange(url("/api/v1/invoices/3"), HttpMethod.GET,
                new HttpEntity<>(bearer(tokenA)), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() {
        var req = new HttpEntity<>(new LoginBody("alice", "wrong"));
        var res = rest.postForEntity(url("/api/v1/auth/login"), req, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String login(String user, String pass) throws Exception {
        var req = new HttpEntity<>(new LoginBody(user, pass));
        var res = rest.postForEntity(url("/api/v1/auth/login"), req, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody()).get("accessToken").asText();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    record LoginBody(String username, String password) {}
}