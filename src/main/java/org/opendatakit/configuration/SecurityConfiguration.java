/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.persistence.ServerPreferencesProperties;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.security.spring.UserDetailsServiceImpl;
import org.opendatakit.security.spring.UserDetailsServiceImpl.CredentialType;
import org.opendatakit.security.spring.UserDetailsServiceImpl.PasswordType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@Profile("default")
@EnableWebSecurity
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  DataConfiguration dataConfiguration;

  @Autowired
  UserServiceConfiguration userServiceConfiguration;


  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.exceptionHandling().authenticationEntryPoint(delegatingAuthenticationEntryPoint());
    
    // We have a choice here; stateless OR enable sessions and use CSRF.
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.csrf().disable();
    
    http.authorizeRequests().antMatchers("/**").authenticated().and()
    
        .addFilter(digestAuthenticationFilter());

    // http.authorizeRequests().antMatchers("/**").permitAll();
  }

  @Bean
  public DigestAuthenticationFilter digestAuthenticationFilter() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    DigestAuthenticationFilter digestAuthenticationFilter = new DigestAuthenticationFilter();
    digestAuthenticationFilter.setPasswordAlreadyEncoded(true);
    digestAuthenticationFilter.setAuthenticationEntryPoint(digestEntryPoint());
    digestAuthenticationFilter.setUserDetailsService(digestLoginService());
    // https://github.com/spring-projects/spring-security/issues/3310
    digestAuthenticationFilter.setCreateAuthenticatedToken(true);
    return digestAuthenticationFilter;
  }

  @Bean
  public DaoAuthenticationProvider digestAuthenticationProvider()
      throws ODKDatastoreException, PropertyVetoException {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(digestLoginService());
    return daoAuthenticationProvider;
  }


  @Bean
  public DigestAuthenticationEntryPoint digestEntryPoint() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    DigestAuthenticationEntryPoint entryPoint = new DigestAuthenticationEntryPoint();
    entryPoint.setRealmName(userServiceConfiguration.realm().getRealmString());
    entryPoint
        .setKey(ServerPreferencesProperties.getSiteKey(userServiceConfiguration.callingContext()));
    entryPoint.setNonceValiditySeconds(1800);
    return entryPoint;
  }


  @Bean
  public UserDetailsService digestLoginService()
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


}
