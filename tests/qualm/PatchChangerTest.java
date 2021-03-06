package qualm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class PatchChangerTest {

  ChangeDelegate mockDelegate;

  @Before
  public void setup() {
    mockDelegate = mock(ChangeDelegate.class);
    PatchChanger.installDelegateForChannel(0, mockDelegate, "MockDelegate");
  }

  @Test
  public void confirmDelegateInstalled() {
    assertEquals("MockDelegate", PatchChanger.getRequestedDeviceForChannel(0));
  }

  @Test
  public void confirmPatchChangeMessagesSent() {
    ProgramChangeEvent pce = new ProgramChangeEvent( 0, null, null ); // don't care about Cue or Patch
    PatchChanger.patchChange(pce, null);
    verify(mockDelegate).patchChange( pce, null );
  }

  @Test
  public void confirmNoteWindowMessagesSent() {
    NoteWindowChangeEvent nwce = new NoteWindowChangeEvent( 0, null, 
							    new Integer(30), 
							    new Integer(50) ); 
    PatchChanger.noteWindowChange(nwce, null);
    verify(mockDelegate).noteWindowChange( nwce, null );
  }

  @Test(expected=RuntimeException.class)
  public void exceptionForUnknownChannel() {
    ProgramChangeEvent pce = new ProgramChangeEvent( 1, null, null ); // don't care about Cue or Patch
    PatchChanger.patchChange(pce, null);
  }
  
  @Test
  public void partialDelegateNamesHandled() {
    PatchChanger.addPatchChanger(1, "Alesis 8.1");
    assertEquals(qualm.delegates.AlesisDelegate.class, PatchChanger.delegateForChannel(1).getClass());
  }
  
  @Test(expected=RuntimeException.class)
  public void unknownDelegate() {
    PatchChanger.addPatchChanger(1, "Foobar Baz");
    assertNull(PatchChanger.delegateForChannel(1));
  }
}
