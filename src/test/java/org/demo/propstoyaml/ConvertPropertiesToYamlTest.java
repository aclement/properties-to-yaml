package org.demo.propstoyaml;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.demo.propstoyaml.ConversionStatus.ConversionMessage;
import org.demo.propstoyaml.PropertiesToYamlConverter.YamlConversionResult;
import org.junit.Ignore;
import org.junit.Test;

public class ConvertPropertiesToYamlTest {
	
	@Test
	public void conversionWithListItems() throws Exception {
		doConversionTest(
				"some.thing[0].a=first-a\n" + 
				"some.thing[0].b=first-b\n" + 
				"some.thing[1].a=second-a\n" + 
				"some.thing[1].b=second-b\n"
				, // ==>
				"some:\n" + 
				"  thing:\n" + 
				"  - a: first-a\n" + 
				"    b: first-b\n" + 
				"  - a: second-a\n" + 
				"    b: second-b\n"
		);
	}

	@Test
	public void hasComments() throws Exception {
		do_hasComments_test("#comment");
		do_hasComments_test("!comment");
		do_hasComments_test("    \t!comment");
		String yaml = do_hasComments_test("    #!comment");
		assertYaml(yaml,
				"other:\n" +
				"  property: othervalue\n" +
				"some:\n" +
				"  property: somevalue\n"
		);
	}
	
	private void assertYaml(String yaml, String expected) {
		assertEquals(expected, yaml);
	}
	
	@Test public void almostHasComments() throws Exception {
		doConversionTest(
			"my.hello=Good morning!\n" + 
			"my.goodbye=See ya # later\n"
			, // ==>
			"my:\n" +
			"  goodbye: 'See ya # later'\n" +
			"  hello: Good morning!\n"
		);
	}

	
	@Test public void simpleConversion() throws Exception {
		doConversionTest(
				"some.thing=vvvv\n" + 
				"some.other.thing=blah\n" 
				, // ==>
				"some:\n" +
				"  other:\n" +
				"    thing: blah\n" +
				"  thing: vvvv\n"
		);
	}

	@Test public void emptyFileConversion() throws Exception {
		doConversionTest(
				""
				, // ==>
				""
		);
	}

	@Test
	public void unusualName() throws Exception {
		File input = createFile("no-extension", 
				"server.port: 6789"
		);
		YamlConversionResult result = new PropertiesToYamlConverter().convert(input);
		assertOkStatus(result.getStatus());
		assertEquals(
			"server:\n" +
			"  port: '6789'\n"
			, 
			result.getYaml()
		);
	}

	@Ignore
	@Test public void nonExistentInput() throws Exception {
//		IProject project = projects.createProject("nonExistentInput");
//		IFile input = project.getFile("doesnotexist.properties");
//		PropertiesToYamlConverter refactoring = new PropertiesToYamlConverter(input);
//		assertStatus(refactoring.checkInitialConditions(new NullProgressMonitor()),
//				ConversionStatus.FATAL, "is not accessible");
	}
	
	@Ignore
	@Test
	public void multipleAssignmentProblem() throws Exception {
		do_conversionTest(
				"some.property=something\n" +
				"some.property=something-else"
				, // ==>
				"some:\n" +
				"  property:\n" +
				"  - something\n" +
				"  - something-else\n"
 				, (status) -> {
					assertStatus(status, ConversionStatus.WARNING, "Multiple values [something, something-else] assigned to 'some.property'.");
				}
		);
	}
	
	@Test public void scalarAndMapConflict() throws Exception {
		do_conversionTest(
				"some.property=a-scalar\n" +
				"some.property.sub=sub-value"
				, 
				"some:\n" +
				"  property:\n" +
				"    sub: sub-value\n"
				, (status) -> {
					assertStatus(status, ConversionStatus.ERROR, "Direct assignment 'some.property=a-scalar' can not be combined with sub-property assignment 'some.property.sub...'");
				}
		);
	}

	@Test public void scalarAndSequenceConflict() throws Exception {
		do_conversionTest(
				"some.property=a-scalar\n" +
				"some.property[0]=zero\n" +
				"some.property[1]=one\n"
				, 
				"some:\n" +
				"  property:\n" +
				"  - zero\n" +
				"  - one\n"
				, (status) -> {
					assertStatus(status, ConversionStatus.ERROR, "Direct assignment 'some.property=a-scalar' can not be combined with sequence assignment 'some.property[0]...'");
				}
		);
	}

	@Test public void mapAndSequenceConflict() throws Exception {
		do_conversionTest(
				"some.property.abc=val1\n" +
				"some.property.def=val2\n" +
				"some.property[0]=zero\n" +
				"some.property[1]=one\n"
				, 
				"some:\n" +
				"  property:\n" +
				"    '0': zero\n" + 
				"    '1': one\n" + 
				"    abc: val1\n" + 
				"    def: val2\n"
				, (status) -> {
					assertStatus(status, ConversionStatus.WARNING, "'some.property' has some entries that look like list items and others that look like map entries");
				}
		);
	}
	
	@Test public void scalarAndMapAndSequenceConflict() throws Exception {
		do_conversionTest(
				"some.property=a-scalar\n" +
				"some.property.abc=val1\n" +
				"some.property.def=val2\n" +
				"some.property[0]=zero\n" +
				"some.property[1]=one\n"
				, 
				"some:\n" +
				"  property:\n" +
				"    '0': zero\n" + 
				"    '1': one\n" + 
				"    abc: val1\n" + 
				"    def: val2\n"
				, (status) -> {
					assertStatus(status, ConversionStatus.ERROR, "Direct assignment 'some.property=a-scalar' can not be combined with sub-property assignment 'some.property.abc...'. ");
					assertStatus(status, ConversionStatus.ERROR, "'some.property' has some entries that look like list items and others that look like map entries");
				}
		);
	}
	

	/* 
junit.framework.AssertionFailedError: Not found: Blah
 in 
Direct assignment 'some.property=a-scalar' can not be combined with sub-property assignment 'some.property.sub... Direct assignments will be dropped!
	at junit.framework.Assert.fail(Assert.java:57)
	at junit.framework.TestCase.fail(TestCase.java:227)
	at org.springsource.ide.eclipse.commons.tests.util.StsTestCase.assertContains(StsTestCase.java:145)
	at org.springframework.ide.eclipse.boot.properties.editor.test.PropertiesToYamlConverterTest.assertStatus(PropertiesToYamlConverterTest.java:242)
	at org.springframework.ide.eclipse.boot.properties.editor.test.PropertiesToYamlConverterTest.lambda$1(PropertiesToYamlConverterTest.java:168)
	at org.springframework.ide.eclipse.boot.properties.editor.test.PropertiesToYamlConverterTest.do_conversionTest(PropertiesToYamlConverterTest.java:187)
	at org.springframework.ide.eclipse.boot.properties.editor.test.PropertiesToYamlConverterTest.scalarAndMapConflict(PropertiesToYamlConverterTest.java:160)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:86)
	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:459)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:678)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:382)
	at org.eclipse.pde.internal.junit.runtime.RemotePluginTestRunner.main(RemotePluginTestRunner.java:66)
	at org.eclipse.pde.internal.junit.runtime.PlatformUITestHarness.lambda$0(PlatformUITestHarness.java:43)
	at org.eclipse.swt.widgets.RunnableLock.run(RunnableLock.java:37)
	at org.eclipse.swt.widgets.Synchronizer.runAsyncMessages(Synchronizer.java:182)
	at org.eclipse.swt.widgets.Display.runAsyncMessages(Display.java:4497)
	at org.eclipse.swt.widgets.Display.readAndDispatch(Display.java:4110)
	at org.eclipse.e4.ui.internal.workbench.swt.PartRenderingEngine$5.run(PartRenderingEngine.java:1155)
	at org.eclipse.core.databinding.observable.Realm.runWithDefault(Realm.java:336)
	at org.eclipse.e4.ui.internal.workbench.swt.PartRenderingEngine.run(PartRenderingEngine.java:1044)
	at org.eclipse.e4.ui.internal.workbench.E4Workbench.createAndRunUI(E4Workbench.java:153)
	at org.eclipse.ui.internal.Workbench.lambda$3(Workbench.java:680)
	at org.eclipse.core.databinding.observable.Realm.runWithDefault(Realm.java:336)
	at org.eclipse.ui.internal.Workbench.createAndRunWorkbench(Workbench.java:594)
	at org.eclipse.ui.PlatformUI.createAndRunWorkbench(PlatformUI.java:148)
	at org.eclipse.ui.internal.ide.application.IDEApplication.start(IDEApplication.java:151)
	at org.eclipse.pde.internal.junit.runtime.NonUIThreadTestApplication.runApp(NonUIThreadTestApplication.java:52)
	at org.eclipse.pde.internal.junit.runtime.UITestApplication.runApp(UITestApplication.java:43)
	at org.eclipse.pde.internal.junit.runtime.NonUIThreadTestApplication.start(NonUIThreadTestApplication.java:46)
	at org.eclipse.equinox.internal.app.EclipseAppHandle.run(EclipseAppHandle.java:196)
	at org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher.runApplication(EclipseAppLauncher.java:134)
	at org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher.start(EclipseAppLauncher.java:104)
	at org.eclipse.core.runtime.adaptor.EclipseStarter.run(EclipseStarter.java:388)
	at org.eclipse.core.runtime.adaptor.EclipseStarter.run(EclipseStarter.java:243)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.eclipse.equinox.launcher.Main.invokeFramework(Main.java:653)
	at org.eclipse.equinox.launcher.Main.basicRun(Main.java:590)
	at org.eclipse.equinox.launcher.Main.run(Main.java:1499)
	at org.eclipse.equinox.launcher.Main.main(Main.java:1472)


	 */
	private void doConversionTest(String input, String expectedOutput) throws Exception {
		do_conversionTest(input, expectedOutput, (status) -> {
			assertEquals(ConversionStatus.OK, status.getSeverity());
		});
	}

	private void do_conversionTest(String input, String expectedOutput, Checker<ConversionStatus> statusChecker) throws Exception {
		File propertiesFile = createFile("application.properties", input);
		assertTrue(propertiesFile.exists());
		
		YamlConversionResult result = new PropertiesToYamlConverter().convert(propertiesFile);
		statusChecker.check(result.getStatus());
		assertEquals(expectedOutput, result.getYaml());
	}

//	private void assertFile(IProject project, String path, String expectedContents) throws Exception {
//		IFile file = project.getFile(path);
//		assertTrue(file.getFullPath().toString(), file.exists());
//		assertEquals(expectedContents, IOUtil.toString(file.getContents()));
//	}

//	private void perform(PropertiesToYamlConverter refactoring) throws Exception {
//		Change change = refactoring.createChange(new NullProgressMonitor());
//		IWorkspace workspace = getWorkspace();
//		CompletableFuture<Void> result = new CompletableFuture<Void>();
//		Job job = new Job(refactoring.getName()) {
//			
//			@Override
//			protected IStatus run(IProgressMonitor monitor) {
//				try {
//					workspace.run(new PerformChangeOperation(change), monitor);
//					result.complete(null);
//				} catch (Throwable e) {
//					result.completeExceptionally(e);
//				}
//				return Status.OK_STATUS;
//			}
//		};
//		job.setRule(workspace.getRuleFactory().buildRule());
//		job.schedule();
//		result.get();
//	}

	private File createFile(String prefix, String content) {
		try {
			File file = File.createTempFile(prefix, null);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.append(content);
			bw.close();
			return file;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private String do_hasComments_test(String comment) throws Exception {
		File propsFile = createFile("application.properties",
				"some.property=somevalue\n"+ 
				comment +"\n" +
				"other.property=othervalue"
		);
		PropertiesToYamlConverter refactoring = new PropertiesToYamlConverter();
		YamlConversionResult result = refactoring.convert(propsFile);
		assertStatus(result.getStatus(), ConversionStatus.WARNING, "has comments, which will be lost");
		return result.getYaml();
	}

	private void assertOkStatus(ConversionStatus s) {
		assertEquals(ConversionStatus.OK, s.getSeverity());
	}

	private void assertStatus(ConversionStatus status, int expectedSeverity, String expectedMessageFragment) {
		assertEquals(expectedSeverity, status.getSeverity());
		StringBuilder allMessages = new StringBuilder();
		for (ConversionMessage entry : status.getEntries()) {
			allMessages.append(entry.getMessage());
			allMessages.append("\n-------------\n");
		}
		assertContains(expectedMessageFragment, allMessages.toString());
	}
	
	private void assertContains(String required, String inData) {
		assert inData.indexOf(required) != -1;
	}
	
	public interface Checker<T> {
		void check(T it) throws Exception;
	}
}
