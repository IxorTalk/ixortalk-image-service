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
package com.ixortalk.image.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ixortalk.aws.s3.library.config.AwsS3Template;
import com.ixortalk.image.service.config.IxorTalkConfigProperties;
import com.ixortalk.test.oauth2.OAuth2EmbeddedTestServer;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import io.findify.s3mock.S3Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.restdocs.restassured.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import static com.jayway.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static com.jayway.restassured.config.RestAssuredConfig.config;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.restassured.operation.preprocess.RestAssuredPreprocessors.modifyUris;

@SpringBootTest(classes = {ImageServiceApplication.class, OAuth2EmbeddedTestServer.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@RunWith(SpringRunner.class)
public abstract class AbstractSpringIntegrationTest {

    protected static final String HOST_IXORTALK_COM = "www.ixortalk.com";

    @LocalServerPort
    protected int port;

    @Value("${server.context-path}")
    protected String contextPath;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Inject
    private ObjectMapper testObjectMapper;

    @Inject
    protected IxorTalkConfigProperties ixorTalkConfigProperties;

    @MockBean
    protected AwsS3Template awsS3Template;

    public S3Mock s3Mock;
    public int s3MockPort = 10901;
    public AmazonS3 s3Client;

    @Before
    public final void setupRestAssuredAndOrganizationMocking() {
        RestAssured.port = port;
        RestAssured.basePath = contextPath;
        RestAssured.config = config().objectMapperConfig(objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> testObjectMapper));
        RestAssured.requestSpecification =
                new RequestSpecBuilder()
                        .addFilter(documentationConfiguration(this.restDocumentation))
                        .addHeader(X_FORWARDED_PROTO_HEADER, HTTPS_SCHEME)
                        .addHeader(X_FORWARDED_HOST_HEADER, HOST_IXORTALK_COM)
                        .addHeader(X_FORWARDED_PORT_HEADER, "")
                        .build();
    }

    @Before
    public void setupS3Mock() {
        s3Mock = new S3Mock.Builder().withPort(s3MockPort).withInMemoryBackend().build();
        s3Mock.start();
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration("http://localhost:"+s3MockPort, "us-west-2");
        s3Client = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpoint)
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();

        s3Client.createBucket(ixorTalkConfigProperties.getBucket());
    }

    @After
    public void after() {
        s3Mock.stop();
    }

    protected static UriModifyingOperationPreprocessor staticUris() {
        return modifyUris().scheme(HTTPS_SCHEME).host(HOST_IXORTALK_COM).removePort();
    }

    protected OperationPreprocessor removeBinaryContent() {
        return new ContentModifyingOperationPreprocessor((originalContent, contentType) -> "<theBinaryContent>".getBytes());
    }

    protected static HeaderDescriptor describeAuthorizationTokenHeader() {
        return headerWithName("Authorization").description("The bearer token needed to authorize this request.");
    }

    protected static ObjectMetadata objectMetadataWithContentTypeAndLength(int contentLength, String contentType) {
        return argThat(objectMetadata -> objectMetadata.getContentLength() == contentLength && objectMetadata.getContentType().equals(contentType));
    }
}
