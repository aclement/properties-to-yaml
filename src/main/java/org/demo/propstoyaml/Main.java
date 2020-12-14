package org.demo.propstoyaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.demo.propstoyaml.ConversionStatus.ConversionMessage;
import org.demo.propstoyaml.PropertiesToYamlConverter.YamlConversionResult;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command()
public class Main implements Callable<Integer> {

	private static final int BUFFER_SIZE = 8192;

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main()).execute(args));
	}

	private @Option(names = { "-e", "--encoding" }) Charset charset = Charset.defaultCharset();
	private @Option(names = { "--output-encoding" }) Charset outCharset = Charset.defaultCharset();
	private @Option(names = { "-o", "--output" }) File output;
	private @Option(names = { "-a", "--append" }) boolean append;
	private @Parameters(index = "0", arity = "0..1") File input;

	@Override
	public Integer call() throws Exception {
		final Properties props = new Properties();
		try (Reader r = input()) {
			props.load(r);
		}
		final YamlConversionResult result = new PropertiesToYamlConverter().convert(props);
		if (result.getSeverity() == ConversionStatus.OK) {
			try (Writer w = output()) {
				w.write(result.getYaml());
			}
		} else {
			result.getStatus().getEntries().stream().map(ConversionMessage::getMessage).forEach(System.err::println);
		}
		return result.getSeverity();
	}

	private Reader input() throws FileNotFoundException {
		InputStream s;

		if (input == null) {
			s = System.in;
		} else {
			s = new FileInputStream(input);
		}
		return new BufferedReader(new InputStreamReader(s, charset), BUFFER_SIZE);
	}

	private Writer output() throws FileNotFoundException {
		final OutputStream s;
		if (output == null) {
			s = System.out;
		} else {
			s = new FileOutputStream(output, append);
		}
		return new BufferedWriter(new OutputStreamWriter(s, outCharset), BUFFER_SIZE);
	}
}
