package com.googleapi;

import com.google.auth.oauth2.GoogleCredentials;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

import static io.restassured.RestAssured.given;

public class DriveApiTest {

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/test/resources/credentials.json"))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/drive.metadata.readonly"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    @Test
    public void listSharedFiles() throws IOException {
        String token = getAccessToken();
        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/files")
                .queryParam("pageSize", 10)
                .header("Authorization", "Bearer " + token)
                .when()
                .get();

        response.then().statusCode(200);
        System.out.println("📄 Shared Files: " + response.asPrettyString());
    }
}
