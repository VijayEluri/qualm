package qualm;

public abstract class AbstractQReceiver implements QReceiver {

  public QReceiver target;
  
  public QReceiver getTarget() { return target; }
  public void setTarget(QReceiver t) { this.target = t; }
  
}
