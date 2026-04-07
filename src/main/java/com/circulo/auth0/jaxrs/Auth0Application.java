package com.circulo.auth0.jaxrs;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.osgi.service.component.annotations.Component;

@Component(
    property = {
        JaxRsWhiteboardProperties.APPLICATION_BASE + "=/auth",
		JaxRsWhiteboardProperties.APPLICATION_NAME + "=Circulo.Auth0",
        "auth.verifier.guest.allowed=true",
        "liferay.access.control.disable=true"
    },
    service = Application.class
)
@ApplicationPath("/")
public class Auth0Application extends Application {
}