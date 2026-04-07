package com.circulo.auth0.jaxrs;

/**
 * Propiedades del <i>JAX-RS Whiteboard</i> (OSGi R7). Literales para evitar dependencia de un
 * artefacto Maven con coordenadas inestables entre 7.3 / 7.4.
 *
 * @see <a href="https://docs.osgi.org/specification/#service.jaxrs">OSGi JAX-RS Whiteboard</a>
 */
public final class JaxRsWhiteboardProperties {

	private JaxRsWhiteboardProperties() {
	}

	public static final String APPLICATION_BASE = "osgi.jaxrs.application.base";

	public static final String APPLICATION_NAME = "osgi.jaxrs.name";

	public static final String APPLICATION_SELECT = "osgi.jaxrs.application.select";

	public static final String RESOURCE = "osgi.jaxrs.resource";

}
