package org.opendatakit.configuration;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.persistence.ServerPreferencesProperties;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.security.spring.DigestAuthenticationFilter;
import org.opendatakit.common.security.spring.UserDetailsServiceImpl;
import org.opendatakit.common.security.spring.UserDetailsServiceImpl.CredentialType;
import org.opendatakit.common.security.spring.UserDetailsServiceImpl.PasswordType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;

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
    http.authorizeRequests().antMatchers("/**").authenticated().and()
        .addFilter(digestAuthenticationFilter());
    
    //http.authorizeRequests().antMatchers("/**").permitAll();
  }

  @Bean
  public DigestAuthenticationFilter digestAuthenticationFilter() throws ODKEntityNotFoundException,
      ODKOverQuotaException, ODKDatastoreException, PropertyVetoException {
    DigestAuthenticationFilter digestAuthenticationFilter = new DigestAuthenticationFilter();
    digestAuthenticationFilter.setPasswordAlreadyEncoded(true);
    digestAuthenticationFilter.setAuthenticationEntryPoint(digestEntryPoint());
    digestAuthenticationFilter.setUserDetailsService(digestLoginService());
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
}
