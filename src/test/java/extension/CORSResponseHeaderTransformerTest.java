package extension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import io.restassured.RestAssured;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class CORSResponseHeaderTransformerTest {

    @ClassRule
    public static WireMockRule wireMockClassRule = new WireMockRule(wireMockConfig().dynamicPort().extensions(
            CORSResponseHeaderTransformer.class));

    @Rule
    public WireMockRule wireMockRule = wireMockClassRule;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost:" + wireMockRule.port();
        RestAssured.urlEncodingEnabled = false;

        /*
         * to test that static headers for the CORS fix are added to the "Access-Control-Allow-Headers" as well as
         * custom request headers
         */
        stubFor(options(urlEqualTo("/api/v1/corsTest")).withHeader("CORSHeader1", containing("CORVal1"))
                .withHeader("CORSHeader2", containing("CORVal2")).withHeader("CORSHeader3", containing("CORVal3"))
                .willReturn(aResponse().withTransformers("CORSResponseHeaderTransformer")));

        /*
         * to test that, by default, an Accept header will always be added to "Access-Control-Allow-Headers" header
         */
        stubFor(options(urlEqualTo("/api/v1/acceptTest")).willReturn(
                aResponse().withTransformers("CORSResponseHeaderTransformer")));

        /*
         * to test that, the Content-Type header will be added to "Access-Control-Allow-Headers" header
         */
        stubFor(options(urlEqualTo("/api/v1/contentTypeTest")).withHeader("Content-Type",
                containing("application/json")).willReturn(
                aResponse().withTransformers("CORSResponseHeaderTransformer")));

        /*
         * to test that, if a Content-Type header isn't passed in by default (in GET requests via Swagger) then it still
         * gets added to "Access-Control-Allow-Headers" header
         */
        stubFor(options(urlEqualTo("/api/v2/contentTypeTest")).willReturn(
                aResponse().withTransformers("CORSResponseHeaderTransformer")));

        stubFor(options(urlEqualTo("/api/v2/pensionSchemeMembers"))
                .withHeader("Accept", containing("application/json"))
                .withHeader("Authorization", containing("authorizedUser"))
                .withHeader("Requesting-System", containing("SMEportal"))
                .withHeader("correlation-ID", containing("a1e4b301-351f-43aa-9bea-f5b62063829d"))
                .willReturn(aResponse().withTransformers("CORSResponseHeaderTransformer")));
    }

    @Test
    public void getCorsFixStaticHeaders() {
        given().header("CORSHeader1", "CORVal1").header("CORSHeader2", "CORVal2").header("CORSHeader3", "CORVal3")
                .when()
                .options("/api/v1/corsTest")
                .then()
                .statusCode(200)
                // static headers required to fix CORS issue
                .header("Access-Control-Allow-Origin", Matchers.containsString("*"))
                .header("Access-Control-Allow-Methods", Matchers.containsString("*"))
                .header("X-Content-Type-Options", Matchers.containsString("nosniff"))
                .header("x-frame-options", Matchers.containsString("DENY"))
                .header("x-xss-protection", Matchers.containsString("1; mode=block"));
    }

    @Test
    public void getCorsFixRequestHeaders() {
        given().header("CORSHeader1", "CORVal1").header("CORSHeader2", "CORVal2").header("CORSHeader3", "CORVal3")
                .when().options("/api/v1/corsTest").then()
                .statusCode(200)
                // dynamic header containing supplied request headers
                .header("Access-Control-Allow-Headers", Matchers.containsString("CORSHeader1"))
                .header("Access-Control-Allow-Headers", Matchers.containsString("CORSHeader2"))
                .header("Access-Control-Allow-Headers", Matchers.containsString("CORSHeader3"));
    }

    @Test
    public void getAcceptHeader() {
        given().when().options("/api/v1/acceptTest").then().statusCode(200)
        // Accept header is returned by default
                .header("Access-Control-Allow-Headers", Matchers.containsString("Accept"));
    }

    @Test
    public void getContentTypeHeader() {
        given().header("Content-Type", "application/json").when().options("/api/v1/contentTypeTest").then()
                .statusCode(200)
                // Accept header is returned by default
                .header("Content-Type", Matchers.containsString("application/json"))
                .header("Access-Control-Allow-Headers", Matchers.containsString("Content-Type"));
    }

    @Test
    public void getDefaultContentTypeHeader() {
        given().when().options("/api/v2/contentTypeTest").then().statusCode(200)
                // Accept header is returned by default
                .header("Content-Type", Matchers.containsString("application/json"))
                .header("Access-Control-Allow-Headers", Matchers.containsString("Content-Type"));
    }
}