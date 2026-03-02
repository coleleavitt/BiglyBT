/*
 * Created on Apr 30, 2004
 * Created by Alon Rohter
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package com.biglybt.core.peermanager.messaging.bittorrent;

import com.biglybt.core.networkmanager.RawMessage;
import com.biglybt.core.peermanager.messaging.Message;
import com.biglybt.core.peermanager.messaging.MessageException;
import com.biglybt.core.peermanager.utils.PeerClassifier;
import com.biglybt.core.util.ByteFormatter;
import com.biglybt.core.util.DirectByteBuffer;
import com.biglybt.core.util.DirectByteBufferPool;
import com.biglybt.core.util.RandomUtils;
import ghostfucker.spoof.PerfectSpoof;
import ghostfucker.spoof.client.PSClient;


/**
 * BitTorrent handshake message.
 */
public class BTHandshake implements BTMessage, RawMessage {
  // NOTE: Don't cache PSClient - use PerfectSpoof.getClient() dynamically
  public static final String PROTOCOL = "BitTorrent protocol";

  // Default reserve bytes (used when spoofing is NOT active)
  private static final byte[] DEFAULT_BT_RESERVED = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
  private static final byte[] DEFAULT_LT_RESERVED = new byte[]{0, 0, 0, 0, 0, (byte)16, 0, 0};
  private static final byte[] DEFAULT_AZ_RESERVED = new byte[]{(byte)128, 0, 0, 0, 0, (byte)19, 0, 0};

  // Mutable reserved bytes (can be updated when spoofing activates)
  private static byte[] BT_RESERVED = DEFAULT_BT_RESERVED.clone();
  private static byte[] LT_RESERVED = DEFAULT_LT_RESERVED.clone();
  private static byte[] AZ_RESERVED = DEFAULT_AZ_RESERVED.clone();

  public static final int BT_RESERVED_MODE = 0;
  public static final int LT_RESERVED_MODE = 1;
  public static final int AZ_RESERVED_MODE = 2;

  private static byte[][] RESERVED = new byte[][]{ BT_RESERVED, LT_RESERVED, AZ_RESERVED };

  /**
   * Update reserved bytes when spoofing is activated.
   * Called by PerfectSpoof.setActive() after client is loaded.
   */
  public static void updateReservedBytes() {
    PSClient client = PerfectSpoof.getClient();
    if (PerfectSpoof.isActive() && client != null) {
      byte[] spoofedReserved = client.getReservedBytes();
      if (spoofedReserved != null && spoofedReserved.length == 8) {
        System.out.println("[BTHandshake] Updating reserved bytes to spoofed values");
        BT_RESERVED = spoofedReserved.clone();
        LT_RESERVED = spoofedReserved.clone();
        AZ_RESERVED = spoofedReserved.clone();
        RESERVED = new byte[][]{ BT_RESERVED, LT_RESERVED, AZ_RESERVED };
      }
    } else {
      System.out.println("[BTHandshake] Resetting reserved bytes to defaults");
      BT_RESERVED = DEFAULT_BT_RESERVED.clone();
      LT_RESERVED = DEFAULT_LT_RESERVED.clone();
      AZ_RESERVED = DEFAULT_AZ_RESERVED.clone();
      RESERVED = new byte[][]{ BT_RESERVED, LT_RESERVED, AZ_RESERVED };
    }
    setFastExtensionEnabled(FAST_EXTENSION_ENABLED);
  }

  /**
   * Get the current AZ reserved bytes (used by PEPeerControlImpl).
   */
  public static byte[] getAzReserved() {
    return AZ_RESERVED;
  }

  public static void setMainlineDHTEnabled(boolean enabled) {
    if (!PerfectSpoof.isActive()) {
      if (enabled) {
        LT_RESERVED[7] = (byte)(LT_RESERVED[7] | 0x01);
        AZ_RESERVED[7] = (byte)(AZ_RESERVED[7] | 0x01);
      } else {
        LT_RESERVED[7] = (byte)(LT_RESERVED[7] & 0xFE);
        AZ_RESERVED[7] = (byte)(AZ_RESERVED[7] & 0xFE);
      }
    }
  }

  public static final boolean FAST_EXTENSION_ENABLED = true;

  public static void setFastExtensionEnabled(boolean enabled) {
    if (!PerfectSpoof.isActive()) {
      if (enabled) {
        LT_RESERVED[7] = (byte)(LT_RESERVED[7] | 0x04);
        AZ_RESERVED[7] = (byte)(AZ_RESERVED[7] | 0x04);
      } else {
        LT_RESERVED[7] = (byte)(LT_RESERVED[7] & 0xF3);
        AZ_RESERVED[7] = (byte)(AZ_RESERVED[7] & 0xF3);
      }
    }
  }

  static {
    // Initialize with defaults - will be updated when spoofing activates
    setFastExtensionEnabled(FAST_EXTENSION_ENABLED);
  }

  private DirectByteBuffer buffer = null;
  private String description = null;

  private final byte[] reserved_bytes;
  private final byte[] datahash_bytes;
  private byte[] peer_id_bytes;
  private final byte version;

  private static byte[] duplicate(byte[] b) {
	  byte[] r = new byte[b.length];
	  System.arraycopy(b, 0, r, 0, b.length);
	  return r;
  }

  /**
   * Used for outgoing handshake message.
   * @param data_hash
   * @param peer_id
   * @param set_reserve_bit
   */
  public BTHandshake( byte[] data_hash, byte[] peer_id, int reserved_mode, byte version ) {
    this( duplicate(RESERVED[reserved_mode]), data_hash, peer_id, version );
  }


  private BTHandshake( byte[] reserved, byte[] data_hash, byte[] peer_id, byte version ) {
    this.reserved_bytes = reserved;
    this.datahash_bytes = data_hash;
    this.peer_id_bytes = peer_id;
    this.version = version;
  }

  private void constructBuffer() {
    // [PEER-DEBUG] Log handshake buffer construction details
    try {
      StringBuilder hexPeerId = new StringBuilder();
      for (byte b : peer_id_bytes) { hexPeerId.append(String.format("%02x", b)); }
      String printablePeerId = new String(peer_id_bytes, 0, Math.min(8, peer_id_bytes.length)).replaceAll("[^\\x20-\\x7E]", ".");
      System.out.println("[PEER-DEBUG] BTHandshake.constructBuffer() - PerfectSpoof.isActive=" + PerfectSpoof.isActive);
      System.out.println("[PEER-DEBUG] BTHandshake.constructBuffer() - reserved_bytes=" + ByteFormatter.nicePrint(reserved_bytes, false));
      System.out.println("[PEER-DEBUG] BTHandshake.constructBuffer() - peer_id (printable)=" + printablePeerId + " (hex)=" + hexPeerId.toString());
    } catch (Exception e) { System.out.println("[PEER-DEBUG] BTHandshake.constructBuffer() - error printing debug: " + e.getMessage()); }
    buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_BT_HAND, 68 );
    buffer.put( DirectByteBuffer.SS_MSG, (byte)PROTOCOL.length() );
    buffer.put( DirectByteBuffer.SS_MSG, PROTOCOL.getBytes() );
    buffer.put( DirectByteBuffer.SS_MSG, reserved_bytes );
    buffer.put( DirectByteBuffer.SS_MSG, datahash_bytes );
    buffer.put( DirectByteBuffer.SS_MSG, peer_id_bytes );
    buffer.flip( DirectByteBuffer.SS_MSG );
  }

  public byte[] getReserved() {  return reserved_bytes;  }

  public byte[] getDataHash() {  return datahash_bytes;  }

  public byte[] getPeerId() {  return peer_id_bytes;  }

  public void setPeerId( byte[] id ){ peer_id_bytes = id; }

  // message
  @Override
  public String getID() {  return BTMessage.ID_BT_HANDSHAKE;  }
  @Override
  public byte[] getIDBytes() {  return BTMessage.ID_BT_HANDSHAKE_BYTES;  }

  @Override
  public String getFeatureID() {  return BTMessage.BT_FEATURE_ID;  }

  @Override
  public int getFeatureSubID() {  return BTMessage.SUBID_BT_HANDSHAKE;  }

  @Override
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }

  @Override
  public byte getVersion() { return version; }

	@Override
	public String getDescription() {
    if( description == null ) {
      description = BTMessage.ID_BT_HANDSHAKE + " of dataID: " +ByteFormatter.nicePrint( datahash_bytes, true ) + " peerID: " +PeerClassifier.getPrintablePeerID( peer_id_bytes );
    }

    return description;
  }


  @Override
  public DirectByteBuffer[] getData() {
    if( buffer == null ) {
      constructBuffer();
    }

    return new DirectByteBuffer[]{ buffer };
  }


  @Override
  public Message deserialize(DirectByteBuffer data, byte version ) throws MessageException {
    if( data == null ) {
      throw new MessageException( "[" +getID() + "] decode error: data == null" );
    }

    if( data.remaining( DirectByteBuffer.SS_MSG ) != 68 ) {
      throw new MessageException( "[" +getID() + "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] != 68" );
    }

    if( data.get( DirectByteBuffer.SS_MSG ) != (byte)PROTOCOL.length() ) {
      throw new MessageException( "[" +getID() + "] decode error: payload.get() != (byte)PROTOCOL.length()" );
    }

    byte[] header = new byte[ PROTOCOL.getBytes().length ];
    data.get( DirectByteBuffer.SS_MSG, header );

    if( !PROTOCOL.equals( new String( header ) ) ) {
      throw new MessageException( "[" +getID() + "] decode error: invalid protocol given: " + new String( header ) );
    }

    byte[] reserved = new byte[ 8 ];
    data.get( DirectByteBuffer.SS_MSG, reserved );

    byte[] infohash = new byte[ 20 ];
    data.get( DirectByteBuffer.SS_MSG, infohash );

    byte[] peerid = new byte[ 20 ];
    data.get( DirectByteBuffer.SS_MSG, peerid );

    data.returnToPool();

    if ( peerid[0] == (byte)0 && peerid[1] == (byte)0){
    	boolean ok = false;
    	for (int i=2;i<20;i++){
    		if ( peerid[i] != (byte)0){

    			ok = true;
    			break;
    		}
    	}
    	if ( !ok ){
    		byte[] x = ("-" + "#@" + "0000" + "-").getBytes();	// bad peer id decode

    		RandomUtils.nextBytes( peerid );

    		System.arraycopy(x, 0, peerid, 0, x.length );
    	}
    }
    return new BTHandshake( reserved, infohash, peerid, version );
  }



  // raw message
  @Override
  public DirectByteBuffer[] getRawData() {
    if( buffer == null ) {
      constructBuffer();
    }

    return new DirectByteBuffer[]{ buffer };
  }

  @Override
  public int getPriority() {  return RawMessage.PRIORITY_HIGH;  }

  @Override
  public boolean isNoDelay() {  return true;  }

  @Override
  public void
  setNoDelay(){}

  @Override
  public Message[] messagesToRemove() {  return null;  }

  @Override
  public void destroy() {
    if( buffer != null )  buffer.returnToPool();
  }

  @Override
  public Message getBaseMessage() {  return this;  }
}
