package org.whispersystems.signalservice.internal.push;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;
import org.whispersystems.signalservice.internal.crypto.ProvisioningCipher;
import org.whispersystems.signalservice.internal.push.ProvisioningProtos.ProvisionMessage;
import org.whispersystems.signalservice.internal.push.ProvisioningProtos.ProvisioningUuid;
import org.whispersystems.signalservice.internal.websocket.WebSocketConnection;
import org.whispersystems.signalservice.internal.websocket.WebSocketProtos.WebSocketRequestMessage;

import com.google.protobuf.ByteString;

public class ProvisioningSocket {
  
  private WebSocketConnection connection;
  private boolean connected = false;

  public ProvisioningSocket(SignalServiceConfiguration signalServiceConfiguration, String userAgent) {
    // TODO uses first url, like in SignalServiceMessageReceiver
    // TODO should probably make this random, like in PushServiceSocket
    SignalServiceUrl[] serviceUrls = signalServiceConfiguration.getSignalServiceUrls();
    connection = new WebSocketConnection(serviceUrls[0].getUrl(), serviceUrls[0].getTrustStore(), userAgent, null);
  }
  
  public ProvisioningUuid getProvisioningUuid() throws TimeoutException, IOException {
    if(!connected) {
      connection.connect();
      connected = true;
    }
    ByteString bytes = readRequest();
    ProvisioningUuid msg = ProvisioningUuid.parseFrom(bytes);
    return msg;
  }
  
  public ProvisionMessage getProvisioningMessage(IdentityKeyPair tempIdentity) throws TimeoutException, IOException {
    if(!connected) {
      throw new IllegalStateException("No UUID requested yet!");
    }
    ByteString bytes = readRequest();
    connection.disconnect();
    connected = false;
    ProvisionMessage msg;
    try {
      msg = new ProvisioningCipher(null).decrypt(tempIdentity, bytes.toByteArray());
      return msg;
    } catch (InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }
  
  private ByteString readRequest() throws TimeoutException, IOException {
    WebSocketRequestMessage response = connection.readRequest(100000);
    ByteString bytes = response.getBody();
    return bytes;
  }

}
