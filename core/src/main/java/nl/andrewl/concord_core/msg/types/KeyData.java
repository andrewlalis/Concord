package nl.andrewl.concord_core.msg.types;

import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This message is sent as the first message from both the server and the client
 * to establish an end-to-end encryption via a key exchange.
 */
@Getter
@NoArgsConstructor
public class KeyData implements Message {
	private byte[] iv;
	private byte[] salt;
	private byte[] publicKey;

	public KeyData(byte[] iv, byte[] salt, byte[] publicKey) {
		this.iv = iv;
		this.salt = salt;
		this.publicKey = publicKey;
	}

	@Override
	public int getByteCount() {
		return Integer.BYTES * 3 + iv.length + salt.length + publicKey.length;
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		o.writeInt(iv.length);
		o.write(iv);
		o.writeInt(salt.length);
		o.write(salt);
		o.writeInt(publicKey.length);
		o.write(publicKey);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		int ivLength = i.readInt();
		this.iv = i.readNBytes(ivLength);
		int saltLength = i.readInt();
		this.salt = i.readNBytes(saltLength);
		int publicKeyLength = i.readInt();
		this.publicKey = i.readNBytes(publicKeyLength);
	}
}
