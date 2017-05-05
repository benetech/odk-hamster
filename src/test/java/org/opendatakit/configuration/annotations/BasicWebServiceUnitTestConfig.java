package org.opendatakit.configuration.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.benetech.boot.Application;
import org.opendatakit.configuration.TestDataConfiguration;
import org.opendatakit.configuration.TestBasicSecurityConfiguration;
import org.opendatakit.configuration.TestUserServiceConfiguration;
import org.opendatakit.configuration.TestWebServiceConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Annotations used for webservice integration tests.
 * 
 * Remember, you can only combine *Spring* annotations into meta-annotations like this. Annotations
 * don't normally inherit so don't go sticking all your annotations in here.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 */
@ContextConfiguration(classes = {TestDataConfiguration.class, TestUserServiceConfiguration.class,
    TestBasicSecurityConfiguration.class, TestWebServiceConfiguration.class, Application.class,},
    initializers = ConfigFileApplicationContextInitializer.class)
@ActiveProfiles({"integrationtest", "basicauth-integration"})
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Retention(RetentionPolicy.RUNTIME)
public @interface BasicWebServiceUnitTestConfig {

}
