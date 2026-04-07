package com.circulo.auth0.service.impl;

import com.circulo.auth0.service.ApigeeProxyService;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

/**
 * Implementación del proxy hacia Apigee.
 * <p>
 * TODO: construir URL destino con {@code apigeeBaseUrl}; copiar método, query, body;
 * inyectar Authorization; streaming; timeouts; tamaño máximo.
 */
@Component(immediate = true, service = ApigeeProxyService.class)
public class ApigeeProxyServiceImpl implements ApigeeProxyService {

	@Override
	public void proxy(
			HttpServletRequest request, HttpServletResponse response,
			String pathRemainder)
		throws IOException {

		response.sendError(
			HttpServletResponse.SC_NOT_IMPLEMENTED,
			"TODO: proxy Apigee no implementado");
	}

}
