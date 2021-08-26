package nl.andrewl.concord_catalog.servlet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class ServerMetaData implements Comparable<ServerMetaData> {
	private String name;
	private String description;
	private String host;
	private int port;

	@JsonIgnore
	private long lastUpdatedAt;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return this.host + ":" + this.port;
	}

	public long getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public void setLastUpdatedAt(long lastUpdatedAt) {
		this.lastUpdatedAt = lastUpdatedAt;
	}

	@Override
	public int compareTo(ServerMetaData o) {
		int result = this.name.compareTo(o.getName());
		if (result == 0) {
			result = this.getAddress().compareTo(o.getAddress());
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			ServerMetaData other = (ServerMetaData) o;
			return this.name.equals(other.getName()) && this.getAddress().equals(other.getAddress());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getAddress());
	}
}
