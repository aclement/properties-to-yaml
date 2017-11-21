package org.demo.propstoyaml;

import java.util.Properties;

public class Test {
public static void main(String[] args) {
	
	Properties p = new Properties();
	p.setProperty("aaa", "bbb");
	p.setProperty("aaa", "bbb");
	System.out.println(p.stringPropertyNames());
}
}
