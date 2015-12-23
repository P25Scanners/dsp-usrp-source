package org.anhonesteffort.usrp;

import org.junit.Test;

public class UsrpProviderTest {

  @Test
  public void testGetUsrp() {
    final UsrpProvider PROVIDER = new UsrpProvider();
    assert PROVIDER.get().isPresent();
  }

}
