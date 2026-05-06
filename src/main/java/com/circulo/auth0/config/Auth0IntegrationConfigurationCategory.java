package com.circulo.auth0.config;

import com.liferay.configuration.admin.category.ConfigurationCategory;
import org.osgi.service.component.annotations.Component;

@Component(
	immediate = true,
	service = ConfigurationCategory.class
)
public class Auth0IntegrationConfigurationCategory implements ConfigurationCategory {

	@Override
	public String getCategoryIcon() {
		return "password-policies";
	}

	@Override
	public String getCategoryKey() {
		return "circulo-configuracion-auth0";
	}

	@Override
	public String getCategorySection() {
		return "circulo-autenticacion";
	}

}
