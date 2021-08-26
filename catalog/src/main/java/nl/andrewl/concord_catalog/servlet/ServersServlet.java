package nl.andrewl.concord_catalog.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This servlet is the main HTTP endpoint for getting the list of servers and
 * also uploading one's own server data.
 */
public class ServersServlet extends HttpServlet {
	private static final SortedSet<ServerMetaData> servers = new TreeSet<>();
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	static {
		executorService.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			servers.removeIf(server -> server.getLastUpdatedAt() < now - (5 * 60000));
		}, 1, 1, TimeUnit.MINUTES);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		mapper.writeValue(resp.getWriter(), servers);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (servers.size() > 10000) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			mapper.writeValue(resp.getWriter(), Map.of("message", "Too many servers registered at this time."));
			return;
		}
		ServerMetaData data = mapper.readValue(req.getReader(), ServerMetaData.class);
		data.setHost(req.getRemoteHost());
		data.setLastUpdatedAt(System.currentTimeMillis());
		synchronized (servers) {
			servers.remove(data);
			servers.add(data);
		}
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		mapper.writeValue(resp.getWriter(), data);
	}
}
