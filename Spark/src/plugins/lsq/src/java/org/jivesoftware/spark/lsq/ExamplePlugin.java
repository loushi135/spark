package org.jivesoftware.spark.lsq;

import org.jivesoftware.spark.plugin.Plugin;

public class ExamplePlugin implements Plugin{

	@Override
	public void initialize() {
		System.out.println("212");
		System.out.println("Welcome To Spark");
	}

	@Override
	public void shutdown() {
		
	}

	@Override
	public boolean canShutDown() {
		return true;
	}

	@Override
	public void uninstall() {
		
	}

}
