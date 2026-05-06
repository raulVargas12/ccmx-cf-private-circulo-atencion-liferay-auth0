package com.circulo.auth0.config;

import com.liferay.portal.kernel.settings.definition.ConfigurationBeanDeclaration;
import org.osgi.service.component.annotations.Component;

@Component(
	immediate = true,
	property = {
		"configuration.bean.declaration.class=com.circulo.auth0.config.Auth0IntegrationConfiguration"
	},
	service = ConfigurationBeanDeclaration.class
)
public class Auth0IntegrationConfigurationBeanDeclaration implements ConfigurationBeanDeclaration {

	@Override
	public Class<?> getConfigurationBeanClass() {
		return Auth0IntegrationConfiguration.class;
	}

}
