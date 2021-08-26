package nl.andrewl.concord_catalog;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import nl.andrewl.concord_catalog.servlet.ServersServlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class CatalogServer {
	private static final String SETTINGS_FILE = "concord-catalog.properties";

	public static void main(String[] args) throws ServletException, IOException {
		var props = loadProperties();
		startServer(Integer.parseInt(props.getProperty("port")));
	}

	/**
	 * Starts the Undertow HTTP servlet container.
	 * @param port The port to bind to.
	 * @throws ServletException If the server could not be started.
	 */
	private static void startServer(int port) throws ServletException {
		System.out.println("Starting server on port " + port + ".");
		DeploymentInfo servletBuilder = Servlets.deployment()
				.setClassLoader(CatalogServer.class.getClassLoader())
				.setContextPath("/")
				.setDeploymentName("Concord Catalog")
				.addServlets(
						Servlets.servlet("ServersServlet", ServersServlet.class)
								.addMapping("/servers")
				);
		DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
		manager.deploy();
		HttpHandler servletHandler = manager.start();
		Undertow server = Undertow.builder()
				.addHttpListener(port, "0.0.0.0")
				.setHandler(servletHandler)
				.build();
		server.start();
	}

	/**
	 * Loads properties from all necessary locations.
	 * @return The properties that were loaded.
	 * @throws IOException If an error occurs while reading properties.
	 */
	private static Properties loadProperties() throws IOException {
		Properties props = new Properties();
		props.load(CatalogServer.class.getResourceAsStream("/nl/andrewl/concord_catalog/defaults.properties"));
		Path settingsPath = Path.of(SETTINGS_FILE);
		if (Files.exists(settingsPath)) {
			props.load(Files.newBufferedReader(settingsPath));
		} else {
			System.out.println("Using built-in default settings. Create a concord-catalog.properties file to configure.");
		}
		return props;
	}
}
