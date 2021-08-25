package nl.andrewl.concord_server.cli;

import nl.andrewl.concord_server.ConcordServer;

public interface ServerCliCommand {
	void handle(ConcordServer server, String[] args) throws Exception;
}
