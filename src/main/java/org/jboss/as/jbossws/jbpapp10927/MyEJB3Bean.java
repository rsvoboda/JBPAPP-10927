package org.jboss.as.jbossws.jbpapp10927;

import javax.ejb.Stateless;
import javax.jws.WebService;

@Stateless
@WebService
public class MyEJB3Bean {

    public void launcheException() throws PairException {
        throw new PairException();
    }

    public Pair<Integer, String> getPair() {
        return new Pair<Integer, String>(22, "Geneva");
    }
}
