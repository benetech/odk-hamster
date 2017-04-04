package org.opendatakit.configuration;

import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDataConfiguration.class,TestUserServiceConfiguration.class})
@ActiveProfiles("unittest")
public @interface UnitTestConfig {

}
