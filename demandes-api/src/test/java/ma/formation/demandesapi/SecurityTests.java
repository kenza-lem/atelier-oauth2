package ma.formation.demandesapi;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    MockMvc mvc;

    // Remplace le décodeur basé sur Keycloak : aucun appel réseau pendant les tests
    @MockitoBean
    JwtDecoder jwtDecoder;

    private static RequestPostProcessor user() {
        return jwt()
            .jwt(j -> j.subject("alice-id").claim("preferred_username", "alice"))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt()
            .jwt(j -> j.subject("bob-id").claim("preferred_username", "bob"))
            .authorities(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private long creerDemande() throws Exception {
        String json = mvc.perform(post("/api/demandes").with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"objet\":\"Attestation\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void sansTokenRetourne401() throws Exception {
        mvc.perform(get("/api/demandes"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalideRetourne401() throws Exception {
        when(jwtDecoder.decode(anyString()))
            .thenThrow(new BadJwtException("Signature invalide"));

        mvc.perform(get("/api/demandes")
                .header("Authorization", "Bearer token.invalide.xyz"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void userNePeutPasModifierStatut() throws Exception {
        long id = creerDemande();

        mvc.perform(patch("/api/admin/demandes/" + id).with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"VALIDEE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminPeutRefuserUneDemande() throws Exception {
        long id = creerDemande();

        mvc.perform(patch("/api/admin/demandes/" + id).with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"REFUSEE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.statut").value("REFUSEE"));
    }

    @Test
    void profilRenvoieSubEtUsername() throws Exception {
        mvc.perform(get("/api/profil").with(user()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("alice-id"))
            .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void profilSansPreferredUsernameNePlantePas() throws Exception {
        mvc.perform(get("/api/profil")
                .with(jwt().jwt(j -> j.subject("sans-username"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("sans-username"))
            .andExpect(jsonPath("$.username").value(nullValue()));
    }
}
