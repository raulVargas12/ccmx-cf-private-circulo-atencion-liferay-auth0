package com.circulo.auth0.web;

import com.circulo.auth0.config.Auth0IntegrationConfiguration;
import com.circulo.auth0.constants.Auth0Constants;
import com.circulo.auth0.service.ApigeeProxyService;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Expone {@code /o/proxy/apigee/*} y delega en {@link ApigeeProxyService}.
 */
@Component(
	configurationPid = Auth0IntegrationConfiguration.PID,
	immediate = true,
	property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" +
			Auth0Constants.SERVLET_PATTERN_APIGEE_PROXY
	},
	service = Servlet.class
)
public class ApigeeProxyServlet extends AbstractAuth0ConfiguredServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	private ApigeeProxyService _apigeeProxyService;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		String pathInfo = request.getPathInfo();

		if (pathInfo == null) {
			pathInfo = "";
		}

		// pathInfo típico: "/recurso/..." relativo al mapping del servlet
		_apigeeProxyService.proxy(request, response, pathInfo);
	}

}
