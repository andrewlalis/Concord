module concord_catalog {
	requires com.fasterxml.jackson.databind;
	requires undertow.core;
	requires undertow.servlet;
	requires java.servlet;
	requires jdk.unsupported;

	exports nl.andrewl.concord_catalog.servlet to undertow.servlet;
	opens nl.andrewl.concord_catalog.servlet to com.fasterxml.jackson.databind;
}