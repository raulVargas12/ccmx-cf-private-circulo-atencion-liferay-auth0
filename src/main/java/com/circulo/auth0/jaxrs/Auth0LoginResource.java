package com.circulo.auth0.jaxrs;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.web.Auth0LoginRedirectHelper;

import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Component;

/**
 * {@code GET /o/auth/login} — inicia Authorization Code + PKCE y redirige a Auth.
 */
@Component(
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

	@org.osgi.service.component.annotations.Reference
	private ConfigurationProvider _configurationProvider;

	@GET
	@Produces(MediaType.WILDCARD)
	public Response login(
			@Context HttpServletRequest httpServletRequest,
			@Context HttpServletResponse httpServletResponse) {

		long companyId = PortalUtil.getCompanyId(httpServletRequest);
		Auth0IntegrationConfiguration configuration;
		try {
			configuration = _configurationProvider.getCompanyConfiguration(
				Auth0IntegrationConfiguration.class, companyId);
		}
		catch (ConfigurationException e) {
			_log.error(
				"Auth0 login: configuración OSGi no cargada para companyId " + companyId, e);

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity(
					"Configuración Auth0 no disponible. Compruebe Instance Settings.")
				.build();
		}

		try {
			URI location = Auth0LoginRedirectHelper.beginAuthorization(
				httpServletRequest, httpServletResponse, configuration);

			return Response.status(Response.Status.FOUND).location(location).build();
		}
		catch (IllegalStateException | IllegalArgumentException e) {
			_log.error("Auth0 login: error al preparar redirección de autorización", e);

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.TEXT_PLAIN + ";charset=UTF-8")
				.entity("No fue posible iniciar el proceso de autenticación.")
				.build();
		}
	}

}
