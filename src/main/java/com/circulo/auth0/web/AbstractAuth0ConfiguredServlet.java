package com.circulo.auth0.web;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;

import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;

/**
 * Base para servlets que consumen {@link Auth0IntegrationConfiguration}.
 */
abstract class AbstractAuth0ConfiguredServlet extends HttpServlet {

	private volatile Auth0IntegrationConfiguration _configuration;

	protected Auth0IntegrationConfiguration getConfiguration() {
		return _configuration;
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_configuration = ConfigurableUtil.createConfigurable(
			Auth0IntegrationConfiguration.class, properties);
	}

}
