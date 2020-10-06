package uk.gov.hmcts.reform.rse.idam.simulator.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.GeneratePinRequest;
import uk.gov.hmcts.reform.idam.client.models.GeneratePinResponse;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.rse.idam.simulator.controllers.domain.IdamUserInfo;
import uk.gov.hmcts.reform.rse.idam.simulator.service.SimulatorService;
import uk.gov.hmcts.reform.rse.idam.simulator.service.memory.LiveMemoryService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.JUnitAssertionsShouldIncludeMessage",
    "PMD.JUnitTestsShouldIncludeAssert", "PMD.LawOfDemeter"})
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.idam.client"})
@SpringBootTest(classes = {IdamClient.class, IdamApi.class, IdamSimulatorController.class,
    LiveMemoryService.class, SimulatorService.class},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@PropertySource("classpath:application.yml")
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
public class IdamClientSpringBootTest {

    public static final String MYEMAIL_HMCTSTEST_NET = "myemail@hmctstest.net";
    public static final String THE_KID = "The Kid";
    public static final String BILLY = "Billy";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    int localServerPort;

    @Autowired
    SimulatorService simulatorService;

    @Autowired
    LiveMemoryService liveMemoryService;

    @Autowired
    IdamClient idamClient;

    String accessToken;

    /*
    Idam Methods to test
            idamClient.
            .searchUsers() // done
            .getUserInfo() // done
            .getUserDetails() //done
            .getUserByUserId() / Done
            .getAccessTokenResponse() // Done
            .generatePin()
            .exchangeCode()
            .authenticatePinUser()
            .authenticateUser()// Done
            .getAccessToken()// Done
    * */

    @Before
    public void setUp() {
        String userName = MYEMAIL_HMCTSTEST_NET;
        addUserToSimulator(BILLY, THE_KID, userName);
        accessToken = fetchAccessToken(MYEMAIL_HMCTSTEST_NET);
        assertNotNull(liveMemoryService);
    }

    @Test
    public void oauth2authenticateUserTest() throws Exception {

        String deprecatedToken = idamClient.authenticateUser(MYEMAIL_HMCTSTEST_NET, "somePassword");

        assertTrue(deprecatedToken.startsWith("Bearer "));
        assertTrue(deprecatedToken.length() > 575);
    }

    @Test
    public void openIdGetAccessTokenTest() {
        assertTrue(accessToken.startsWith("Bearer "));
        assertTrue(accessToken.length() > 575);
    }


    @Test
    public void canSearchUserTest() {
        List<UserDetails> returnedUser = idamClient.searchUsers(accessToken, "return Smith");

        assertEquals(1, returnedUser.size());
        assertEquals(Optional.of("Smith"), returnedUser.get(0).getSurname());
    }

    @Test
    public void fetchUserInfoTest() {
        UserInfo userInfo = idamClient.getUserInfo(accessToken);

        assertNotNull(userInfo);
        assertEquals(THE_KID, userInfo.getFamilyName());
    }

    @Test
    public void fetchUserDetailsTest() {
        UserDetails userDetails = idamClient.getUserDetails(accessToken);

        assertEquals(Optional.of(THE_KID), userDetails.getSurname());
    }

    @Test
    public void fetchUserByUserId() {
        IdamUserInfo idamUserInfo = addUserToSimulator("ForeName1", "SureName1", "AnotherEmail@hmcts.net");

        UserDetails userDetails = idamClient.getUserByUserId(accessToken, idamUserInfo.getUid());

        assertEquals(Optional.of("SureName1"), userDetails.getSurname());
        assertEquals("ForeName1", userDetails.getForename());
        assertEquals("AnotherEmail@hmcts.net", userDetails.getEmail());
        assertNotNull(userDetails.getId(), idamUserInfo.getUid());
    }

    @Test
    public void fetchAccessTokenByUserPassword() {
        TokenResponse tokenResponse = idamClient.getAccessTokenResponse(MYEMAIL_HMCTSTEST_NET, "somePassword");
        assertEquals(tokenResponse.expiresIn, "14400000");
        assertEquals(tokenResponse.scope, "openid profile roles");
        assertEquals(tokenResponse.tokenType, "Bearer");
        assertNotNull(tokenResponse.accessToken);
        assertTrue(tokenResponse.accessToken.length() > 400);
    }

    @Test
    public void generatePinTest() {
        GeneratePinRequest pinRequest = new GeneratePinRequest("pinRName");
        GeneratePinResponse generatePinResponse = idamClient.generatePin(pinRequest, accessToken);

        assertNotNull(generatePinResponse.getPin());
        assertNotNull(generatePinResponse.getUserId());
    }

    @Test
    public void accessTokenIsNotValidPrefixTest() {
        expect401Error(() -> {
            idamClient.getUserDetails("something wrong");
        });

        expect401Error(() -> {
            idamClient.searchUsers("wrong token", "a query");
        });

        expect401Error(() -> {
            idamClient.generatePin(new GeneratePinRequest("name"), "wrong stuff");
        });

        expect401Error(() -> {
            idamClient.getUserByUserId("Wrong token", "One id");
        });

        expect401Error(() -> {
            idamClient.getUserInfo("something wrong");
        });
    }

    private String fetchAccessToken(String email) {
        return idamClient.getAccessToken(email, "onePassword");
    }

    private void expect401Error(Executable apiCall) {
        FeignException exception = assertThrows(FeignException.class, apiCall);
        assertEquals(exception.status(), 401);
    }

    private IdamUserInfo addUserToSimulator(String forename, String surename, String email) {
        final String uri = "http://localhost:" + localServerPort + "/simulator/user";
        RestTemplate restTemplate = new RestTemplate();

        IdamUserInfo idamUserInfo = new IdamUserInfo();
        idamUserInfo.setEmail(email);
        idamUserInfo.setGivenName(forename);
        idamUserInfo.setFamilyName(surename);
        idamUserInfo.setRoles(Arrays.asList("role1", "role2"));

        String postForObject = restTemplate.postForObject(uri, idamUserInfo, String.class);

        String userUuid = extractUserUid(postForObject);

        idamUserInfo.setUid(userUuid);
        return idamUserInfo;
    }

    private String extractUserUid(String postForObject) {
        try {
            return objectMapper.readTree(postForObject).path("uuid").asText();
        } catch (JsonProcessingException jse) {
            throw new JsonTestException("impossible to extract uuid", jse);
        }
    }


}
