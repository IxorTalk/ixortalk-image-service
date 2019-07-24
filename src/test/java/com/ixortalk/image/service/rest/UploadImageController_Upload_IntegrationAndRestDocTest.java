/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-present IxorTalk CVBA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ixortalk.image.service.rest;

import com.ixortalk.image.service.AbstractSpringIntegrationTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;

import static com.ixortalk.test.oauth2.OAuth2TestTokens.adminToken;
import static com.ixortalk.test.oauth2.OAuth2TestTokens.userToken;
import static com.jayway.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.http.HttpHeaders.LOCATION;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.MULTIPART_FORM_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;

public class UploadImageController_Upload_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {


    private static final String ORIGINAL_IMAGE_CONTENT_TYPE = IMAGE_PNG_VALUE;

    private static final String FILE_REQUEST_PART_NAME = "file";
    private static final String KEY_REQUEST_PART_NAME = "key";


    @Test
    public void success() throws IOException {
        String location =
                given()
                        .auth().preemptive().oauth2(adminToken().getValue())
                        .filter(
                                document("images/upload",
                                        preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParts(
                                                partWithName(FILE_REQUEST_PART_NAME).description("The (required) actual image file part."),
                                                partWithName(KEY_REQUEST_PART_NAME).description("The key to use for this image, the key is a directory path in which the image will be stored.  A UUID will be generated for the image under the specified path.")
                                        ),
                                        responseHeaders(headerWithName(LOCATION).description("Contains the actual path to the stored original image."))
                                )
                        )
                        .contentType(MULTIPART_FORM_DATA)
                        .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                        .multiPart(KEY_REQUEST_PART_NAME, TEST_KEY, TEXT_PLAIN_VALUE)
                        .post("/upload")
                        .then()
                        .statusCode(HTTP_CREATED)
                        .extract().header(LOCATION);


        ArgumentCaptor<InputStream> argumentCaptor = forClass(InputStream.class);
        verify(awsS3Template)
                .save(
                        eq(ixorTalkConfigProperties.getBucket()),
                        eq(location),
                        objectMetadataWithContentTypeAndLength(originalImageBytes.length, ORIGINAL_IMAGE_CONTENT_TYPE),
                        argumentCaptor.capture()
                );
        verifyNoMoreInteractions(awsS3Template);

        assertThat(toByteArray(argumentCaptor.getValue())).isEqualTo(originalImageBytes);
    }

    @Test
    public void fileMissing() {
        given()
                .auth().preemptive().oauth2(adminToken().getValue())
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(KEY_REQUEST_PART_NAME, TEST_KEY, TEXT_PLAIN_VALUE)
                .post("/upload")
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        verifyZeroInteractions(awsS3Template);
    }

    @Test
    public void keyMissing() {
        given()
                .auth().preemptive().oauth2(adminToken().getValue())
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/upload")
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        verifyZeroInteractions(awsS3Template);
    }

    @Test
    public void noAdminRights() {
        given()
                .auth().preemptive().oauth2(userToken().getValue())
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .multiPart(KEY_REQUEST_PART_NAME, TEST_KEY, TEXT_PLAIN_VALUE)
                .post("/upload")
                .then()
                .statusCode(HTTP_FORBIDDEN);
    }
}