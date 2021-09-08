package nl.andrewl.concord_server.util;

import java.util.UUID;

public interface IdProvider {
	UUID newId();
}
