/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.identity.integration.test.scim2.rest.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.IOException;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.wso2.identity.integration.test.scim2.SCIM2BaseTestCase.GROUPS_ENDPOINT;
import static org.wso2.identity.integration.test.scim2.SCIM2BaseTestCase.PERMISSIONS_ENDPOINT;

public class SCIM2GroupTest extends SCIM2BaseTest {

    private static final Log log = LogFactory.getLog(SCIM2GroupTest.class);

    protected String END_POINT_URL;
    protected String GROUP_ID = null;
    private static final String SCIM_CONTENT_TYPE = "application/scim+json";


    @Factory(dataProvider = "restAPIUserConfigProvider")
    public SCIM2GroupTest(TestUserMode userMode) throws Exception {

        super.init(userMode);
        this.context = isServer;
        this.authenticatingUserName = context.getContextTenant().getTenantAdmin().getUserName();
        this.authenticatingCredential = context.getContextTenant().getTenantAdmin().getPassword();
        this.tenant = context.getContextTenant().getDomain();
    }

    @DataProvider(name = "restAPIUserConfigProvider")
    public static Object[][] restAPIUserConfigProvider() {

        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN},
                {TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.testInit(swaggerDefinition, tenant);
    }

    @AfterClass(alwaysRun = true)
    public void testFinish() {

        super.conclude();
    }

    @BeforeMethod(alwaysRun = true)
    public void testInit() {

        RestAssured.basePath = basePath;
    }

    @Test
    public void testGETGroupDetails() {

        END_POINT_URL = GROUPS_ENDPOINT;
        getResponseOfGet(END_POINT_URL, SCIM_CONTENT_TYPE)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .log().ifValidationFails();
    }

    @Test(dependsOnMethods = "testGETGroupDetails")
    public void testCreateGroup() throws IOException {

        String body = readResource("scim2-add-group.json");
        Response response = getResponseOfPost(GROUPS_ENDPOINT, body, SCIM_CONTENT_TYPE);
        response.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .header(HttpHeaders.LOCATION, notNullValue());
        String location =response.getHeader(HttpHeaders.LOCATION);
        this.GROUP_ID = location.split(GROUPS_ENDPOINT)[1];
        log.info("Group_id :" + GROUP_ID);
        Assert.assertNotNull(GROUP_ID, "The Group is not registered.");

    }

    @Test(dependsOnMethods = "testCreateGroup")
    public void putAndGetPermissionToGroup() throws IOException {

        String body = readResource("scim2-put-group-permissions.json");
        END_POINT_URL = GROUPS_ENDPOINT + this.GROUP_ID + PERMISSIONS_ENDPOINT;
        Response response = getResponseOfPut(END_POINT_URL, body, SCIM_CONTENT_TYPE);
        String responseString = response.then()
                                        .log().ifValidationFails()
                                        .assertThat()
                                        .statusCode(HttpStatus.SC_OK)
                                        .extract()
                                        .asString();
        Assert.assertNotNull(responseString);
        // PUT operation should return the same response as the requested.
        Assert.assertEquals(responseString, body, "The response for the PUT operation is incorrect.");

    }

    @Test(dependsOnMethods = "putAndGetPermissionToGroup")
    public void getPermissionsOfGroup() {

        END_POINT_URL = GROUPS_ENDPOINT + this.GROUP_ID + PERMISSIONS_ENDPOINT;
        getResponseOfGet(END_POINT_URL, SCIM_CONTENT_TYPE)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test(dependsOnMethods = "getPermissionsOfGroup")
    public void patchAndGetPermissionsToGroup() throws IOException {

        END_POINT_URL = GROUPS_ENDPOINT + this.GROUP_ID + PERMISSIONS_ENDPOINT;
        // Patch - add permissions.
        String body1 = readResource("scim2-patch-add-group-permissions.json");
        Response response1 = getResponseOfPatch(END_POINT_URL, body1, SCIM_CONTENT_TYPE);

        String responseString1 = response1.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .asString();
        Assert.assertNotNull(responseString1);
        Assert.assertTrue(responseString1.contains("permission/admin/configure/security/usermgt"));

        // Patch remove permissions
        String body2 = readResource("scim2-patch-remove-group-permissions.json");
        Response response2 = getResponseOfPatch(END_POINT_URL, body2, SCIM_CONTENT_TYPE);

        String responseString2 = response2.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .asString();
        Assert.assertNotNull(responseString2);
        Assert.assertFalse(responseString2.contains("permission/admin/configure/security/usermgt"));

    }

    @Test(dependsOnMethods = "patchAndGetPermissionsToGroup")
    public void getErrorGroupIdPermissions() throws IOException {

        String errorGroupId = "/a43fe003-d90d-43ca-ae38-d2332ecc0f36";
        END_POINT_URL = GROUPS_ENDPOINT + errorGroupId + PERMISSIONS_ENDPOINT;
        getResponseOfGet(END_POINT_URL, "application/scim+json")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND);

        getResponseOfPut(END_POINT_URL, readResource("scim2-put-group-permissions.json"), SCIM_CONTENT_TYPE)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND);

        getResponseOfPut(END_POINT_URL, readResource("scim2-patch-add-group-permissions.json"), SCIM_CONTENT_TYPE)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND);

    }


}
