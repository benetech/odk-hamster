package org.opendatakit.configuration.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opendatakit.configuration.TestDataConfiguration;
import org.opendatakit.configuration.TestUserServiceConfiguration;
import org.opendatakit.test.db.SetupTeardown;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * Annotations used for database tests. Includes a database setup and teardown (in a real, existing
 * database that must be set up.) 
 * 
 * Remember, you can only combine *Spring* annotations into
 * meta-annotations like this. Annotations don't normally inherit so don't go sticking all your
 * annotations in here..
 * 
 * @author Caden Howell <cadenh@benetech.org>
 */
@ContextConfiguration(classes = {TestDataConfiguration.class, TestUserServiceConfiguration.class},
    initializers = ConfigFileApplicationContextInitializer.class)
@ActiveProfiles("unittest")
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(listeners = {SetupTeardown.class,
    DependencyInjectionTestExecutionListener.class})
public @interface DBUnitTestConfig {

}
