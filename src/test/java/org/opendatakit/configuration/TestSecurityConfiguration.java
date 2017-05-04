/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.configuration;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.persistence.table.ServerPreferencesPropertiesTable;
import org.opendatakit.security.spring.BasicAuthenticationFilter;
import org.opendatakit.security.spring.BasicUsingDigestPasswordEncoder;
import org.opendatakit.security.spring.BasicUsingDigestSaltSource;
import org.opendatakit.security.spring.UserDetailsServiceImpl;
import org.opendatakit.security.spring.UserDetailsServiceImpl.CredentialType;
import org.opendatakit.security.spring.UserDetailsServiceImpl.PasswordType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

@Configuration
@Profile("integrationtest")
@EnableWebSecurity
public class TestSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static Log logger = LogFactory.getLog(SecurityConfiguration.class);


  @Autowired
  TestDataConfiguration testDataConfiguration;

  @Autowired
  TestUserServiceConfiguration testUserServiceConfiguration;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    logger.info("Setting up authentication.");

    // We have a choice here; stateless OR enable sessions and use CSRF.
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.csrf().disable();


    http.authorizeRequests().antMatchers("/*").permitAll();

    http.authorizeRequests().antMatchers("/**").authenticated().and()
        .addFilterBefore(basicAuthenticationFilter(), AnonymousAuthenticationFilter.class);

  }

  @Bean
  public BasicAuthenticationFilter basicAuthenticationFilter() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();
    providers.add(anonymousProvider());
    providers.add(basicAuthenticationProvider());

    ProviderManager providerManager = new ProviderManager(providers);
    BasicAuthenticationFilter basicAuthenticationFilter =
        new BasicAuthenticationFilter(providerManager, basicEntryPoint());
    return basicAuthenticationFilter;
  }

  @Bean
  public AnonymousAuthenticationProvider anonymousProvider()
      throws ODKDatastoreException, PropertyVetoException {
    AnonymousAuthenticationProvider anonymousProvider = new AnonymousAuthenticationProvider(
        ServerPreferencesPropertiesTable.getSiteKey(testUserServiceConfiguration.callingContext()));

    return anonymousProvider;
  }

  @Bean
  public MessageDigestPasswordEncoder passwordEncoder() {
    BasicUsingDigestPasswordEncoder passwordEncoder = new BasicUsingDigestPasswordEncoder();
    passwordEncoder.setRealmName(testUserServiceConfiguration.realm().getRealmString());
    return passwordEncoder;
  }
  
  @Bean
  public DaoAuthenticationProvider basicAuthenticationProvider()
      throws ODKDatastoreException, PropertyVetoException {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(basicLoginService());
    daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
    daoAuthenticationProvider.setSaltSource(new BasicUsingDigestSaltSource());
    return daoAuthenticationProvider;
  }


  @Bean
  public BasicAuthenticationEntryPoint basicEntryPoint() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(testUserServiceConfiguration.realm().getRealmString());
    return entryPoint;
  }


  @Bean
  public UserDetailsService basicLoginService()
      throws ODKDatastoreException, PropertyVetoException {
    UserDetailsServiceImpl userDetailsServiceImpl = new UserDetailsServiceImpl();
    userDetailsServiceImpl.setCredentialType(CredentialType.Username);
    userDetailsServiceImpl.setPasswordType(PasswordType.BasicAuth);
    userDetailsServiceImpl.setDatastore(testDataConfiguration.datastore());
    userDetailsServiceImpl.setUserService(testUserServiceConfiguration.userService());

    List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority("AUTH_LOCAL"));
    userDetailsServiceImpl.setAuthorities(authorities);
    return userDetailsServiceImpl;
  }


}
