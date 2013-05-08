package qualm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import qualm.plugins.CueChangeNotification;

/**
 * Unit tests for {@link QualmREPL} 
 */
public class QualmREPLTest {
  
  @Mock MasterController controller;
  @Mock QController subController;
  StringWriter output;
  Writer input;
  QualmREPL repl;
  final static AtomicReference<String> lastCue = new AtomicReference<String>(null);
  final static AtomicInteger pluginCount = new AtomicInteger(0);
  
  @Before
  public void setUp() throws Exception {
    output = new StringWriter();
    PipedReader reader = new PipedReader();
    input = new PipedWriter(reader);
    repl = new QualmREPL(reader, output);
    setupController();
    pluginCount.set(0);
    lastCue.set(null);
  }

  private void setupController() throws Exception {
    controller = mock(MasterController.class);
    subController = mock(QController.class);
    when(controller.mainQC()).thenReturn(subController);
    repl.setMasterController(controller);
  }
  
  @Test
  public void confirmControllerSetup() throws Exception {
    verify(controller).setREPL(repl);
  }
  
  @Test
  public void emptyAdvances() throws Exception {
    repl.processLine("");
    repl.processLine(null);
    repl.processLine("]");  // oops; hit the wrong key and rolled to the enter key
    repl.processLine("\\"); // oops; hit the wrong key and rolled to the enter key
    verify(subController, times(4)).advancePatch();
  }

  @Test
  public void advanceCommand() throws Exception {
    repl.processLine("adv K1");
    verify(controller).advanceStream("K1");
  }

  @Test
  public void reverse() throws Exception {
    repl.processLine("rev qs2");
    verify(controller).reverseStream("qs2");
  }

  @Test
  public void dumpCommand() throws Exception {
    when(subController.getQData()).thenReturn(minimalData());
    repl.processLine("dump");
    assertNotNull(output.toString());
  }

  @Test
  public void showXmlCommand() throws Exception {
    when(subController.getQData()).thenReturn(minimalData());
    repl.processLine("showxml");
    assertNotNull(output.toString());
  }

  @Test
  public void resetCommand() throws Exception {
    repl.processLine("reset");
    verify(controller).gotoCue("0.0");
  }

  @Test
  public void showVersion() throws Exception {
    repl.processLine("version");
    assertEquals(Qualm.versionString() + "\n", output.toString());
  }
  
  @Test
  public void setMidiOutput() throws Exception {
    repl.processLine("showmidi");
    verify(controller).setDebugMIDI(true);
    repl.processLine("unshowmidi");
    verify(controller).setDebugMIDI(false);
  }

  @Test
  public void addUnknownPlugin() throws Exception {
    repl.processLine("plugin qualm.plugins.DoesNotExist");
    Assert.assertTrue(output.toString().startsWith("Unable to create or identify"));
  }
  
  @Test
  public void removeUnknownPlugin() throws Exception {
    repl.processLine("plugin remove qualm.plugins.DoesNotExist");
    Assert.assertTrue(output.toString().startsWith("Unable to find running"));
  }

  @Test
  public void basicPluginHandling() throws Exception {
    when(subController.getQData()).thenReturn(minimalData());

    String pluginName = "qualm.QualmREPLTest$MockChangePlugin";
    repl.processLine("plugin " + pluginName);
    assertEquals(1, pluginCount.get());
    
    repl.processLine("plugin list");
    Assert.assertTrue(output.toString().contains(pluginName));
    
    repl.processLine("plugin remove " + pluginName);
    assertEquals(0, pluginCount.get());
  }

  private QData minimalData() {
    return new QDataBuilder()
      .withTitle("Minimal")
      .addMidiChannel( 0, null, "Ch1")
      .addStream(new QStreamBuilder()
		 .addCue(new Cue.Builder()
			 .setCueNumber("1.1")
			 .build())
		 .build())
      .build();
  }
  
  @SuppressWarnings("unused") // called by name
  private static class MockChangePlugin implements CueChangeNotification {
    public MockChangePlugin() { }
    
    @Override public void initialize() { 
      pluginCount.incrementAndGet();
    }

    @Override public void shutdown() {
      pluginCount.decrementAndGet();
    }
    
    @Override
    public void cueChange(MasterController mc) {
      lastCue.set(mc.mainQC().getCurrentCue().getCueNumber());
    }
    
  }

}