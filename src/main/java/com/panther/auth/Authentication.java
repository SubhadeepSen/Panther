package com.panther.auth;

import java.util.TreeMap;

@FunctionalInterface
public interface Authentication {

	public TreeMap<String, String> headers();
}
