/*******************************************************************************
 * Copyright (c) 2017 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.demo.propstoyaml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.demo.propstoyaml.YamlPathSegment.AtIndex;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Helper class to convert (Spring Boot) .properties file content into
 * equivalent .yml file content.
 * 
 * @author Kris De Volder
 */
public class PropertiesToYamlConverter {

	private ConversionStatus status = new ConversionStatus();
	private String output;
	private static final Pattern COMMENT = Pattern.compile("(?m)^\\s*(\\#|\\!)");

	public PropertiesToYamlConverter() {
	}
	
	public YamlConversionResult convert(File f) {
		Properties p = new Properties();
		try {
			String content = new String(Files.readAllBytes(Paths.get(f.toURI())));
			if (hasComments(content)) {
				status.addWarning("The properties file has comments, which will be lost in the refactoring!");
			}
			p.load(new StringReader(content));
		} catch (IOException e) {
			status.addError("Problem loading file "+f+": "+e.getMessage());
		}
		return convert(p);
	}
	
	public YamlConversionResult convert(String properties) {
		Properties p = new Properties();
		try {
			p.load(new StringReader(properties));
		} catch (IOException e) {
			status.addError("Problem processing properties: "+e.getMessage());
		}
		return convert(p);
	}

	public YamlConversionResult convert(Properties p) {
		Map<String, Collection<String>> propertiesMap = new HashMap<>();
		for (Entry<Object, Object> e : p.entrySet()) {
			Set<String> s = new LinkedHashSet<>();
			s.add((String) e.getValue());
			propertiesMap.put((String) e.getKey(), s);
		}
		return convert(propertiesMap);
	}

	public YamlConversionResult convert(Map<String, Collection<String>> properties) {
		if (properties.isEmpty()) {
			output = "";
			return YamlConversionResult.EMPTY;
		}
		YamlBuilder root = new YamlBuilder(YamlPath.EMPTY);
		for (Entry<String, Collection<String>> e : properties.entrySet()) {
			for (String v : e.getValue()) {
				root.addProperty(YamlPath.fromProperty(e.getKey()), v);
			}
		}
		Object object = root.build();

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);

		Yaml yaml = new Yaml(options);
		this.output = yaml.dump(object);
		return new YamlConversionResult(status, output);
	}

	class YamlBuilder {
		final YamlPath path;
		final List<String> scalars = new ArrayList<>();
		final TreeMap<Integer, YamlBuilder> listItems = new TreeMap<>();
		final TreeMap<String, YamlBuilder> mapEntries = new TreeMap<>();

		public YamlBuilder(YamlPath path) {
			this.path = path;
		}

		void addProperty(YamlPath path, String value) {
			if (path.isEmpty()) {
				scalars.add(value);
			} else {
				YamlPathSegment segment = path.getSegment(0);
				YamlBuilder subBuilder;
				if (segment instanceof AtIndex) {
					subBuilder = getSubBuilder(listItems, segment, segment.toIndex());
				} else {
					subBuilder = getSubBuilder(mapEntries, segment, segment.toPropString());
				}
				subBuilder.addProperty(path.dropFirst(1), value);
			}
		}

		private <T> YamlBuilder getSubBuilder(TreeMap<T, YamlBuilder> subBuilders, YamlPathSegment segment, T key) {
			YamlBuilder existing = subBuilders.get(key);
			if (existing == null) {
				subBuilders.put(key, existing = new YamlBuilder(path.append(segment)));
			}
			return existing;
		}

		public Object build() {
			if (!scalars.isEmpty()) {
				if (listItems.isEmpty() && mapEntries.isEmpty()) {
					if (scalars.size() > 1) {
						status.addWarning("Multiple values " + scalars + " assigned to '" + path.toPropString()
								+ "'. Values will be merged into a yaml sequence node.");
						return scalars;
					} else {
						return scalars.get(0);
					}
				} else {
					if (!mapEntries.isEmpty()) {
						status.addError("Direct assignment '" + path.toPropString() + "=" + scalars.get(0)
								+ "' can not be combined " + "with sub-property assignment '" + path.toPropString()
								+ "." + mapEntries.keySet().iterator().next() + "...'. "
								+ "Direct assignment will be dropped!");
					} else {
						status.addError("Direct assignment '" + path.toPropString() + "=" + scalars.get(0)
								+ "' can not be combined " + "with sequence assignment '" + path.toPropString() + "["
								+ listItems.keySet().iterator().next() + "]...' "
								+ "Direct assignments will be dropped!");
					}
					scalars.clear();
				}
			}
			// Assert.isLegal(scalars.isEmpty());
			if (!listItems.isEmpty() && !mapEntries.isEmpty()) {
				status.addWarning("'" + path.toPropString()
						+ "' has some entries that look like list items and others that look like map entries. "
						+ "All these entries will be treated as map entries");
				for (Entry<Integer, YamlBuilder> listItem : listItems.entrySet()) {
					mapEntries.put(listItem.getKey().toString(), listItem.getValue());
				}
				listItems.clear();
			}
			if (!listItems.isEmpty()) {
				return listItems.values().stream().map(childBuilder -> childBuilder.build())
						.collect(Collectors.toList());
			} else {
				TreeMap<String, Object> map = new TreeMap<>();
				for (Entry<String, YamlBuilder> entry : mapEntries.entrySet()) {
					map.put(entry.getKey(), entry.getValue().build());
				}
				return map;
			}
		}
	}

	
    private boolean hasComments(String line) {
        return COMMENT.matcher(line).find();
    }

	static class YamlConversionResult {
		ConversionStatus status;
		String yaml;

		private static YamlConversionResult EMPTY = new YamlConversionResult(ConversionStatus.EMPTY, "");

		YamlConversionResult(ConversionStatus status, String output) {
			this.status = status;
			this.yaml = output;
		}

		public String getYaml() {
			return yaml;
		}

		public ConversionStatus getStatus() {
			return status;
		}

		public int getSeverity() {
			return status.getSeverity();
		}
	}

	public ConversionStatus getStatus() {
		return status;
	}

	public String getYaml() {
		return output;
	}

}