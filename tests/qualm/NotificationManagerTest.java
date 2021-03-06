package qualm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import qualm.notification.BaseQualmNotifier;
import qualm.notification.CueChange;
import qualm.notification.EventMapActivation;
import qualm.notification.PatchChange;


public class NotificationManagerTest {

  private NotificationManager notificationMgr;

  @Before
  public void setUp() throws Exception {
    notificationMgr = new NotificationManager();
  }

  @Test
  public void addToAllLists() throws Exception {
    notificationMgr.addNotification(AllNotifications.class.getName());
    assertEquals(1, notificationMgr.getCueNotifiers().size());
    assertEquals(1, notificationMgr.getPatchNotifiers().size());
    assertEquals(1, notificationMgr.getMapNotifiers().size());
  }
  
  @Test
  public void removeFromAllLists() throws Exception {
    addToAllLists();
    notificationMgr.removeNotification(AllNotifications.class.getName());
    assertEquals(0, notificationMgr.getCueNotifiers().size());
    assertEquals(0, notificationMgr.getPatchNotifiers().size());
    assertEquals(0, notificationMgr.getMapNotifiers().size());
  }

  @Test
  public void addDirect() throws Exception {
    AllNotifications plugin = new AllNotifications();
    notificationMgr.addNotifier(plugin);
    assertEquals(1, notificationMgr.getCueNotifiers().size());
    assertEquals(plugin, notificationMgr.getCueNotifiers().iterator().next());
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void unknownClass() throws Exception {
    notificationMgr.addNotification("qualm.UnknownPlugin");
  }
  
  @Test
  public void handleCueChanges() throws Exception {
    CueChange ccn = mock(CueChange.class);
    notificationMgr.addNotifier(ccn);
    MasterController mc = mock(MasterController.class);
    
    notificationMgr.handleCueChanges(mc);
    verify(ccn).cueChange(mc);
  }
  
  @Test
  public void handlePatchChanges() throws Exception {
    PatchChange pcn = mock(PatchChange.class);
    notificationMgr.addNotifier(pcn);
    Patch p = new Patch("P1", 3);
    notificationMgr.handlePatchChanges(1, "K1", p);
    verify(pcn).patchChange(1, "K1", p);
  }
  
  @Test
  public void handleMapActivations() throws Exception {
    EventMapActivation emn = mock(EventMapActivation.class);
    notificationMgr.addNotifier(emn);
    
    MasterController mc = mock(MasterController.class);
    notificationMgr.handleMapActivations(mc);
    verify(emn).activeEventMapper(mc);
  }

  private static class AllNotifications extends BaseQualmNotifier
  implements CueChange, PatchChange, EventMapActivation {
    public AllNotifications() { }
    @Override public void cueChange(MasterController mc) { }
    @Override public void patchChange(int channel, String channelName, Patch patch) { }
    @Override public void activeEventMapper(MasterController mc) { }
  }
}
