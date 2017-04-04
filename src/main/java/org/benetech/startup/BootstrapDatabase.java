package org.benetech.startup;

import javax.annotation.PostConstruct;

import org.opendatakit.common.web.CallingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BootstrapDatabase {

  @Autowired
  CallingContext callingContext;
  
  @PostConstruct
  public void init(){
      // start your monitoring in here
  }
}
