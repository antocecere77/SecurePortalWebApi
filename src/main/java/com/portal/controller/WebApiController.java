package com.portal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api2")
@CrossOrigin(origins="http://localhost:4200")
public class WebApiController {

    Logger logger = LoggerFactory.getLogger(WebApiController.class);


    @GetMapping(value = "/test")
    public String getGreetings() {
        logger.debug("Saluti, sono il tuo primo webservice controller PortalWebApi");
        return "Saluti, sono il tuo primo web services controller PortalWebApi";
    }

}
