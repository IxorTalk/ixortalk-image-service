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
package com.ixortalk.image.service.rest.exception;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.ixortalk.image.service.ImageServiceApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpStatus.*;

@ControllerAdvice(basePackageClasses = {ImageServiceApplication.class})
public class GenericExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericExceptionHandler.class);

    @ExceptionHandler(value = {IllegalArgumentException.class, MethodArgumentNotValidException.class, MissingServletRequestPartException.class})
    public ResponseEntity handleBadRequests(Exception e) {
        String errorUUID = logError(e);
        return new ResponseEntity("Invalid request - " + errorUUID, new HttpHeaders(), BAD_REQUEST);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity handleAccessDeniedException(Exception e) {
        String errorUUID = logError(e);
        return new ResponseEntity("Access denied - " + errorUUID, new HttpHeaders(), FORBIDDEN);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity handleException(Exception e) {
        String errorUUID = logError(e);
        return new ResponseEntity("Internal Server Error - " + errorUUID, new HttpHeaders(), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = AmazonS3Exception.class)
    public ResponseEntity handleAmazonS3Exception(AmazonS3Exception e) {
        String errorUUID = logError(e);
        return new ResponseEntity("Amazon S3 Error - " + errorUUID, new HttpHeaders(), HttpStatus.valueOf(e.getStatusCode()));
    }

    public static String logError(Exception e) {
        String errorUUID = randomUUID().toString();
        LOGGER.error("Invalid request - {}: {}", errorUUID, e.getMessage(), e);
        return errorUUID;
    }
}