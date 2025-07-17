package com.googleapi;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class DriveApiTest {

    private AccessToken getAccessToken() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("credentials.json");

        if (inputStream == null) {
            throw new FileNotFoundException("credentials.json not found in resources");
        }

        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(inputStream)
                .createScoped(List.of("https://www.googleapis.com/auth/drive"));

        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        System.out.println("Access token: " + token.getTokenValue());
        System.out.println("Expires at: " + token.getExpirationTime());

        return token;
    }

    @Test
    public void listSharedFiles() throws IOException {
        String token = getAccessToken().getTokenValue();

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
        String token = getAccessToken().getTokenValue();

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
    @Test
    public void createFolderInDrive() throws IOException {


        String token = getAccessToken().getTokenValue();

        // Folder metadata
        String folderMetadata = """
        {
          "name": "MyTestFolder",
          "mimeType": "application/vnd.google-apps.folder"
        }
        """;

        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/files")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(folderMetadata)
                .when()
                .post();

        // ✅ Validate response
        response.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("MyTestFolder"))
                .body("mimeType", equalTo("application/vnd.google-apps.folder"));

        // Print result
        System.out.println("📁 Folder created: " + response.jsonPath().getString("id"));
    }

    @Test
    public void updateFileNameInDrive() throws IOException {
        String token = getAccessToken().getTokenValue();

        String fileId = "1NsGRq0LrQ1ubWxNRxQGOUiPhkG9HgRkf"; // Replace with your test file ID

        String updatedName = "UpdatedTestFileName.txt";

        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/files/" + fileId)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .queryParam("fields", "id, name")
                .body("{\"name\": \"" + updatedName + "\"}")
                .when()
                .request("PATCH")
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("name", equalTo(updatedName))
                .extract().response();

        System.out.println("Updated File: " + response.asPrettyString());
    }
    @Test
    public void getFileMetadataById() throws IOException {
        String token = getAccessToken().getTokenValue();
        String fileId = "1NsGRq0LrQ1ubWxNRxQGOUiPhkG9HgRkf"; // Replace with your actual file ID

        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/files/" + fileId)
                .header("Authorization", "Bearer " + token)
                .queryParam("fields", "id, name, mimeType, size")
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("name", notNullValue())
                .body("mimeType", notNullValue())
                .extract().response();

        System.out.println("Metadata: " + response.asPrettyString());
    }

    @Test
    public void starFileInDrive() throws IOException {
        String token = getAccessToken().getTokenValue();
        String fileId = "1NsGRq0LrQ1ubWxNRxQGOUiPhkG9HgRkf"; // Replace with your actual file ID

        Response response = given()
                .baseUri("https://www.googleapis.com")
                .basePath("/drive/v3/files/" + fileId)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .queryParam("fields", "id, name, starred")
                .body("{\"starred\": true}")
                .when()
                .request("PATCH")
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("starred", equalTo(true))
                .extract().response();

        System.out.println("Starred File Response: " + response.asPrettyString());
    }


}
