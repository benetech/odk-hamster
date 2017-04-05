package org.opendatakit.configuration;

import java.lang.annotation.Retention;

import org.junit.runner.RunWith;
import org.opendatakit.common.persistence.SetupTeardown;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.lang.annotation.RetentionPolicy;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDataConfiguration.class,UserServiceConfiguration.class})
@ActiveProfiles("unittest")
@TestExecutionListeners(listeners = {SetupTeardown.class}, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@Retention(RetentionPolicy.RUNTIME) 
public @interface DBUnitTestConfig {

}
