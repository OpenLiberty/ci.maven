package io.openliberty.tools.maven.server.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openliberty.tools.maven.server.Dependency;


public class Key {
	private String keyid;
	private String keyurl;
	
	public void setKeyid(String keyid) {
		this.keyid = keyid;
	}
	
	public void setKeyurl(String keyurl) {
		this.keyurl = keyurl;
	}
	
	public String getKeyid() {
		return keyid;
	}
	
	public String getKeyurl() {
		return keyurl;
	}
}


	
