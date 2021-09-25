module concord_server {
	requires nitrite;
	requires static lombok;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires bcrypt;

	requires java.base;
	requires java.logging;
	requires java.net.http;

	requires concord_core;

	opens nl.andrewl.concord_server.config to com.fasterxml.jackson.databind;
}