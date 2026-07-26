package empik.demo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import empik.demo.coupon.domain.Coupon;
import empik.demo.coupon.repository.CouponRepository;
import empik.demo.coupon.repository.CouponUsageRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CouponIntegrationTest {

    private static final String ERROR_TYPE_PREFIX = "https://demo.empik.com/errors/";

    private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(0);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    @DynamicPropertySource
    static void registerGeoIpBaseUrl(DynamicPropertyRegistry registry) {
        if (!WIRE_MOCK_SERVER.isRunning()) {
            WIRE_MOCK_SERVER.start();
        }
        registry.add("coupon.geoip.base-url", () -> "http://localhost:" + WIRE_MOCK_SERVER.port() + "/json");
    }

    @BeforeAll
    static void beforeAll() {
        if (!WIRE_MOCK_SERVER.isRunning()) {
            WIRE_MOCK_SERVER.start();
        }
    }

    @AfterAll
    static void afterAll() {
        if (WIRE_MOCK_SERVER.isRunning()) {
            WIRE_MOCK_SERVER.stop();
        }
    }

    @BeforeEach
    void setUp() {
        couponUsageRepository.deleteAll();
        couponRepository.deleteAll();
        WIRE_MOCK_SERVER.resetAll();
    }

    @Test
    void shouldCreateAndRedeemCouponForMatchingCountry() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/json/8.8.8.8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"countryCode\":\"PL\"}")));

        int createStatus = createCoupon("summer10", "PL", 3);
        assertThat(createStatus).isEqualTo(HttpStatus.CREATED.value());

        int redeemStatus = redeemCoupon("summer10", "user-1", "8.8.8.8");

        assertThat(redeemStatus).isEqualTo(HttpStatus.OK.value());
        Coupon savedCoupon = couponRepository.findById("SUMMER10").orElseThrow();
        assertThat(savedCoupon.getCurrentUses()).isEqualTo(1);
        assertThat(couponUsageRepository.count()).isEqualTo(1);
        WIRE_MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/json/8.8.8.8")));
    }

    @Test
    void shouldReturnBadRequestWhenGeoIpCountryDoesNotMatchCouponCountry() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/json/9.9.9.9"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"countryCode\":\"DE\"}")));

        int createStatus = createCoupon("winter10", "PL", 3);
        assertThat(createStatus).isEqualTo(HttpStatus.CREATED.value());

        HttpResponse<String> redeemResponse = redeemCouponResponse("winter10", "user-2", "9.9.9.9");

        assertThat(redeemResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertProblemDetail(redeemResponse.body(), "Country Mismatch", ERROR_TYPE_PREFIX + "country-mismatch");
        assertThat(redeemResponse.body()).contains("WINTER10");
        Coupon savedCoupon = couponRepository.findById("WINTER10").orElseThrow();
        assertThat(savedCoupon.getCurrentUses()).isEqualTo(0);
        assertThat(couponUsageRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldReturnNotFoundProblemDetailWhenCouponDoesNotExist() {
        HttpResponse<String> redeemResponse = redeemCouponResponse("missing", "user-404", "8.8.8.8");

        assertThat(redeemResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertProblemDetail(redeemResponse.body(), "Coupon Not Found", ERROR_TYPE_PREFIX + "coupon-not-found");
        assertThat(redeemResponse.body()).contains("MISSING");
    }

    @Test
    void shouldReturnConflictProblemDetailWhenCouponUsageLimitIsReached() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/json/8.8.8.8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"countryCode\":\"PL\"}")));

        assertThat(createCoupon("limit10", "PL", 1)).isEqualTo(HttpStatus.CREATED.value());
        HttpResponse<String> firstRedeem = redeemCouponResponse("limit10", "user-1", "8.8.8.8");
        HttpResponse<String> secondRedeem = redeemCouponResponse("limit10", "user-2", "8.8.8.8");

        assertThat(firstRedeem.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(secondRedeem.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertProblemDetail(secondRedeem.body(), "Coupon Conflict", ERROR_TYPE_PREFIX + "coupon-conflict");
        assertThat(secondRedeem.body()).contains("LIMIT10");
    }

    @Test
    void shouldReturnServiceUnavailableProblemDetailWhenGeoIpLookupFails() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/json/7.7.7.7"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"fail\"}")));

        assertThat(createCoupon("geoerr10", "PL", 2)).isEqualTo(HttpStatus.CREATED.value());
        HttpResponse<String> redeemResponse = redeemCouponResponse("geoerr10", "user-geo", "7.7.7.7");

        assertThat(redeemResponse.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertProblemDetail(redeemResponse.body(), "GeoIP Service Error", ERROR_TYPE_PREFIX + "geoip-unavailable");
        assertThat(redeemResponse.body()).contains("External GeoIP service is currently unavailable.");
    }

    @Test
    void shouldReturnValidationProblemDetailWhenCreateCouponRequestIsInvalid() {
        HttpResponse<String> createResponse = createCouponResponse("", "P", 0);

        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertProblemDetail(createResponse.body(), "Validation Error", ERROR_TYPE_PREFIX + "validation-error");
        assertThat(createResponse.body()).contains("Validation failed for one or more fields.");
        assertThat(createResponse.body()).contains("invalidFields", "code", "countryCode", "maxUses");
    }

    @Test
    void shouldAllowOnlyMaxUsesUnderConcurrentRedeemRequests() throws Exception {

        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/json/8.8.8.8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"countryCode\":\"PL\"}")));

        createCoupon("flash10", "PL", 10);

        int requestCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);

        List<CompletableFuture<Integer>> requests = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            String randomUserId = "user-" + UUID.randomUUID();
            requests.add(CompletableFuture.supplyAsync(
                    () -> redeemCoupon("flash10", randomUserId, "8.8.8.8"), 
                    executor
            ));
        }

       List<Integer> statusCodes = requests.stream()
                .map(f -> f.join())
                .toList();

        long successCount = statusCodes.stream().filter(s -> s == HttpStatus.OK.value()).count();
        long conflictCount = statusCodes.stream().filter(s -> s == HttpStatus.CONFLICT.value()).count();

        assertThat(successCount).isEqualTo(10); 
        assertThat(conflictCount).isEqualTo(40); 

        Coupon coupon = couponRepository.findById("FLASH10").orElseThrow();
        assertThat(coupon.getCurrentUses()).isEqualTo(10);
        assertThat(couponUsageRepository.count()).isEqualTo(10);
        
        executor.shutdown();
    }

    private int createCoupon(String code, String countryCode, int maxUses) {
        return createCouponResponse(code, countryCode, maxUses).statusCode();
    }

    private HttpResponse<String> createCouponResponse(String code, String countryCode, int maxUses) {
        String body = "{\"code\":\"" + escapeJson(code) + "\",\"countryCode\":\""
            + escapeJson(countryCode) + "\",\"maxUses\":" + maxUses + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/coupons"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return send(request);
    }

    private int redeemCoupon(String code, String userId, String ipAddress) {
        return redeemCouponResponse(code, userId, ipAddress).statusCode();
    }

    private HttpResponse<String> redeemCouponResponse(String code, String userId, String ipAddress) {
        String body = "{\"userId\":\"" + escapeJson(userId) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/coupons/" + code + "/redeem"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Forwarded-For", ipAddress)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void assertProblemDetail(String problemJson, String expectedTitle, String expectedType) {
        assertThat(JsonPath.<String>read(problemJson, "$.title")).isEqualTo(expectedTitle);
        assertThat(JsonPath.<String>read(problemJson, "$.type")).isEqualTo(expectedType);
        assertThat(JsonPath.<String>read(problemJson, "$.timestamp")).isNotNull();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}