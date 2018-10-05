package poc;

import akka.util.ByteString;

/**
 * Created by wisniewskim.
 */
public class AppConsts {
    
    public static final ByteString SOH = ByteString.fromArray(new byte[] {0x01});
    public static final ByteString EOH = ByteString.fromArray(new byte[] {0x0d, 0x0a});
}
