package com.googleapi;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class DriveApiTest {

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("src/test/resources/credentials.json"))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/drive.metadata.readonly"));
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        System.out.println("Access token: " + token.getTokenValue());
        System.out.println("Expires at: " + token.getExpirationTime());

        return credentials.getAccessToken().getTokenValue();
    }

    @Test
    public void listSharedFiles() throws IOException {
        String token = getAccessToken();

        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/files")
                .queryParam("pageSize", 10)
                .queryParam("fields", "files(id,name,mimeType)")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .when()
                .get();

        // ✅ Basic assertions
        response.then()
                .statusCode(200)
                .body("files", notNullValue());

        // ✅ Additional: Check structure of returned files
        List<Map<String, String>> files = response.jsonPath().getList("files");

        // Make sure there’s at least 1 file (optional)
        Assert.assertTrue(files.size() > 0, "Expected at least one file");

        // Validate that each file has required fields
        for (Map<String, String> file : files) {
            Assert.assertNotNull(file.get("id"), "File ID is missing");
            Assert.assertNotNull(file.get("name"), "File name is missing");
            Assert.assertNotNull(file.get("mimeType"), "File mimeType is missing");
            System.out.printf("✅ File: %s (%s)%n", file.get("name"), file.get("mimeType"));
        }
    }
    @Test
    public void getDriveAboutInfo() throws IOException {
        String token = getAccessToken();

        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/about")
                .queryParam("fields", "user,storageQuota,maxUploadSize")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .when()
                .get();

        // ✅ Basic response validation
        response.then()
                .statusCode(200)
                .body("user.displayName", notNullValue())
                .body("user.emailAddress", notNullValue())
                .body("storageQuota.limit", notNullValue())
                .body("storageQuota.usage", notNullValue());

        // ✅ Print for debug
        System.out.println("📊 Drive Info:");
        System.out.println(response.asPrettyString());
    }


}
