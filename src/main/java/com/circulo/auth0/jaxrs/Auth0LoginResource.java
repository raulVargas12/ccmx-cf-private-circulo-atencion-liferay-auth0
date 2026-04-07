package com.circulo.auth0.jaxrs;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.web.Auth0LoginRedirectHelper;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * {@code GET /o/auth/login} — inicia Authorization Code + PKCE y redirige a Auth.
 */
@Component(
	configurationPid = Auth0IntegrationConfiguration.PID,
	immediate = true,
	property = {
		JaxRsWhiteboardProperties.APPLICATION_SELECT + "=(osgi.jaxrs.name=Circulo.Auth0)",
		JaxRsWhiteboardProperties.RESOURCE + "=true"
	},
	service = Object.class
)
@Path("/login")
public class Auth0LoginResource {

	private static final Log _log = LogFactoryUtil.getLog(Auth0LoginResource.class);

	private volatile Auth0IntegrationConfiguration _configuration;

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_configuration = ConfigurableUtil.createConfigurable(
			Auth0IntegrationConfiguration.class, properties);
	}

	@GET
	@Produces(MediaType.WILDCARD)
	public Response login(
			@Context HttpServletRequest httpServletRequest,
			@Context HttpServletResponse httpServletResponse) {

		Auth0IntegrationConfiguration configuration = _configuration;

		if (configuration == null) {
			_log.error(
				"Auth0 login: configuración OSGi no cargada (PID " +
					Auth0IntegrationConfiguration.PID + ")");

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity(
					"Configuración Auth0 no disponible. Compruebe System Settings / OSGi.")
				.build();
		}

		try {
			URI location = Auth0LoginRedirectHelper.beginAuthorization(
				httpServletRequest, httpServletResponse, configuration);

			return Response.status(Response.Status.FOUND).location(location).build();
		}
		catch (IllegalStateException | IllegalArgumentException e) {
			_log.error("Auth0 login: " + e.getMessage(), e);

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity(e.getMessage())
				.build();
		}
	}

}
