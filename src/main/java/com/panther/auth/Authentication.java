package com.panther.auth;

import java.util.TreeMap;

public interface Authentication {

	public TreeMap<String, String> headers();
}
