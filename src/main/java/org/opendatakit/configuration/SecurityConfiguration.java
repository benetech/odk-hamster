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
import java.util.LinkedHashMap;
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
import org.springframework.context.annotation.ComponentScan;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@Profile("default")
@EnableWebSecurity
@ComponentScan(basePackages = {"org.opendatakit"})
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static Log logger = LogFactory.getLog(SecurityConfiguration.class);


  @Autowired
  DataConfiguration dataConfiguration;

  @Autowired
  UserServiceConfiguration userServiceConfiguration;


  @Override
  protected void configure(HttpSecurity http) throws Exception {
    logger.info("Setting up authentication.");
    http.exceptionHandling().authenticationEntryPoint(delegatingAuthenticationEntryPoint());

    // We have a choice here; stateless OR enable sessions and use CSRF.
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.csrf().disable();

    http.authorizeRequests().antMatchers("/").permitAll();
    http.authorizeRequests().antMatchers("/healthcheck").permitAll();
    http.authorizeRequests().antMatchers("/swagger.json").permitAll();
    http.authorizeRequests().antMatchers("/favicon.ico").permitAll();
    http.authorizeRequests().antMatchers("/index.html").permitAll();
    http.authorizeRequests().antMatchers("/swagger/**").permitAll();
    http.authorizeRequests().antMatchers("/images/**").permitAll();
    http.authorizeRequests().antMatchers("/odktables/**").hasRole("SYNCHRONIZE_TABLES");
    http.authorizeRequests().antMatchers("/users/list").hasRole("USER"); // Backwards compatible
                                                                         // with aggregate
    http.authorizeRequests().antMatchers("/roles/granted").hasRole("USER"); // Backwards compatible
                                                                            // with aggregate
    http.authorizeRequests().antMatchers("/admin/**").hasRole("SITE_ACCESS_ADMIN");

    // This is where we are currently enabling a fallback to Basic Authentication.
    // We may wish to remove this, as it is not very secure. On the other hand, we're not requiring
    // anyone to use it.
    http.authorizeRequests().antMatchers("/**").authenticated().and()
        .addFilterBefore(basicAuthenticationFilter(), AnonymousAuthenticationFilter.class)
        .addFilterAt(anonymousFilter(), AnonymousAuthenticationFilter.class)
        .addFilter(digestAuthenticationFilter());

  }

  @Bean
  public AnonymousAuthenticationFilter anonymousFilter() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    String siteKey =
        ServerPreferencesPropertiesTable.getSiteKey(userServiceConfiguration.callingContext());
    List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority("USER_IS_ANONYMOUS"));

    return new AnonymousAuthenticationFilter(siteKey, "anonymousUser", authorities);

  }

  @Bean
  public AnonymousAuthenticationProvider anonymousProvider()
      throws ODKDatastoreException, PropertyVetoException {
    AnonymousAuthenticationProvider anonymousProvider = new AnonymousAuthenticationProvider(
        ServerPreferencesPropertiesTable.getSiteKey(userServiceConfiguration.callingContext()));
    return anonymousProvider;
  }

  @Bean
  public DigestAuthenticationFilter digestAuthenticationFilter() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    DigestAuthenticationFilter digestAuthenticationFilter = new DigestAuthenticationFilter();
    digestAuthenticationFilter.setPasswordAlreadyEncoded(true);
    digestAuthenticationFilter.setAuthenticationEntryPoint(digestEntryPoint());
    digestAuthenticationFilter.setUserDetailsService(digestAndBasicLoginService());
    // https://github.com/spring-projects/spring-security/issues/3310
    digestAuthenticationFilter.setCreateAuthenticatedToken(true);
    return digestAuthenticationFilter;
  }

  @Bean
  public DaoAuthenticationProvider digestAuthenticationProvider()
      throws ODKDatastoreException, PropertyVetoException {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(digestAndBasicLoginService());

    return daoAuthenticationProvider;
  }


  @Bean
  public DigestAuthenticationEntryPoint digestEntryPoint() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    DigestAuthenticationEntryPoint entryPoint = new DigestAuthenticationEntryPoint();
    entryPoint.setRealmName(userServiceConfiguration.realm().getRealmString());
    entryPoint.setKey(
        ServerPreferencesPropertiesTable.getSiteKey(userServiceConfiguration.callingContext()));
    entryPoint.setNonceValiditySeconds(1800);
    return entryPoint;
  }


  @Bean
  public UserDetailsService digestAndBasicLoginService()
      throws ODKDatastoreException, PropertyVetoException {
    UserDetailsServiceImpl userDetailsServiceImpl = new UserDetailsServiceImpl();
    userDetailsServiceImpl.setCredentialType(CredentialType.Username);
    userDetailsServiceImpl.setPasswordType(PasswordType.DigestAuth);
    userDetailsServiceImpl.setDatastore(dataConfiguration.datastore());
    userDetailsServiceImpl.setUserService(userServiceConfiguration.userService());

    List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority("AUTH_LOCAL"));
    userDetailsServiceImpl.setAuthorities(authorities);
    return userDetailsServiceImpl;
  }

  @Bean
  DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint()
      throws ODKEntityNotFoundException, ODKOverQuotaException, ODKDatastoreException,
      PropertyVetoException {
    LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints =
        new LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>();
    entryPoints.put(new RequestHeaderRequestMatcher("X-OpenRosa-Version", "1.0"),
        digestEntryPoint());
    entryPoints.put(new RequestHeaderRequestMatcher("X-OpenDataKit-Version", "2.0"),
        digestEntryPoint());
    DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint =
        new DelegatingAuthenticationEntryPoint(entryPoints);
    delegatingAuthenticationEntryPoint.setDefaultEntryPoint(digestEntryPoint());
    return delegatingAuthenticationEntryPoint;
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
  public MessageDigestPasswordEncoder basicUsingDigestPasswordEncoder() {
    BasicUsingDigestPasswordEncoder passwordEncoder = new BasicUsingDigestPasswordEncoder();
    passwordEncoder.setRealmName(userServiceConfiguration.realm().getRealmString());
    return passwordEncoder;
  }

  @Bean
  public DaoAuthenticationProvider basicAuthenticationProvider()
      throws ODKDatastoreException, PropertyVetoException {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(digestAndBasicLoginService());
    daoAuthenticationProvider.setPasswordEncoder(basicUsingDigestPasswordEncoder());
    daoAuthenticationProvider.setSaltSource(new BasicUsingDigestSaltSource());

    return daoAuthenticationProvider;
  }


  @Bean
  public BasicAuthenticationEntryPoint basicEntryPoint() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(userServiceConfiguration.realm().getRealmString());
    return entryPoint;
  }


}
