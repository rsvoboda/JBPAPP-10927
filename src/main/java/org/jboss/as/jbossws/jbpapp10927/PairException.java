package org.jboss.as.jbossws.jbpapp10927;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.ws.WebFault;

@WebFault
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class PairException extends Exception {

  private static final long serialVersionUID = -1581844125108670118L;

  public PairException(String message) {
    super(message);
    init();
  }

  private long age = 20L;
  private Pair<Integer, Boolean> info = new Pair<Integer, Boolean>(1, false);
  private List<Boolean> flags = null;

  public PairException() {
    init();
  }

  private void init() {
    flags = new ArrayList<Boolean>();
    flags.add(Boolean.FALSE);
    flags.add(Boolean.FALSE);
    flags.add(Boolean.TRUE);
  }

  public long getAge() {
      return age;
  }

  public void setAge(long age) {
      this.age = age;
  }

  public Pair<Integer, Boolean> getInfo() {
      return info;
  }

  public void setInfo(Pair<Integer, Boolean> info) {
      this.info = info;
  }

  public List<Boolean> getFlags() {
    return this.flags;
  }

  public void setFlags(List<Boolean> flags) {
    this.flags = flags;
  }
}
