package nl.andrewl.concord_core.msg;

import nl.andrewl.concord_core.msg.types.client_setup.KeyData;
import nl.andrewl.concord_core.util.Pair;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for handling the establishment of encrypted communication.
 */
public class Encryption {
	/**
	 * Upgrades the given input and output streams to a pair of cipher input and
	 * output streams. This upgrade follows the following steps:
	 * <ol>
	 *     <li>Generate an elliptic curve key pair, and send the public key to the output stream.</li>
	 *     <li>Read the public key that the other person has sent, from the input stream.</li>
	 *     <li>Compute a shared private key using the ECDH key exchange, with our private key and their public key.</li>
	 *     <li>Create the cipher streams from the shared private key.</li>
	 * </ol>
	 * @param in The unencrypted input stream.
	 * @param out The unencrypted output stream.
	 * @param serializer The message serializer that is used to read and write
	 *                   messages according to the standard Concord protocol.
	 * @return The pair of cipher streams, which can be used to send encrypted
	 * messages.
	 * @throws GeneralSecurityException If an error occurs while generating keys
	 * or preparing the cipher streams.
	 * @throws IOException If an error occurs while reading or writing data on
	 * the streams.
	 */
	public static Pair<CipherInputStream, CipherOutputStream> upgrade(
			InputStream in,
			OutputStream out,
			Serializer serializer
	) throws GeneralSecurityException, IOException {
		// Generate our own key pair.
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
		kpg.initialize(256);
		KeyPair keyPair = kpg.generateKeyPair();
		byte[] publicKey = keyPair.getPublic().getEncoded();
		var random = new SecureRandom();
		byte[] iv = new byte[16];
		random.nextBytes(iv);
		byte[] salt = new byte[8];
		random.nextBytes(salt);
		// Send our public key and related data to the client, unencrypted.
		serializer.writeMessage(new KeyData(iv, salt, publicKey), out);

		// Receive and decode client's unencrypted key data.
		KeyData clientKeyData = (KeyData) serializer.readMessage(in);
		PublicKey clientPublicKey = KeyFactory.getInstance("EC")
				.generatePublic(new X509EncodedKeySpec(clientKeyData.publicKey()));

		// Compute secret key from client's public key and our private key.
		KeyAgreement ka = KeyAgreement.getInstance("ECDH");
		ka.init(keyPair.getPrivate());
		ka.doPhase(clientPublicKey, true);
		byte[] secretKey = computeSecretKey(ka.generateSecret(), publicKey, clientKeyData.publicKey());

		// Initialize cipher streams.
		Cipher writeCipher = Cipher.getInstance("AES/CFB8/NoPadding");
		Cipher readCipher = Cipher.getInstance("AES/CFB8/NoPadding");
		Key cipherKey = new SecretKeySpec(secretKey, "AES");
		writeCipher.init(Cipher.ENCRYPT_MODE, cipherKey, new IvParameterSpec(iv));
		readCipher.init(Cipher.DECRYPT_MODE, cipherKey, new IvParameterSpec(clientKeyData.iv()));
		return new Pair<>(
				new CipherInputStream(in, readCipher),
				new CipherOutputStream(out, writeCipher)
		);
	}

	private static byte[] computeSecretKey(byte[] sharedSecret, byte[] pk1, byte[] pk2) throws NoSuchAlgorithmException {
		MessageDigest hash = MessageDigest.getInstance("SHA-256");
		hash.update(sharedSecret);
		List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(pk1), ByteBuffer.wrap(pk2));
		Collections.sort(keys);
		hash.update(keys.get(0));
		hash.update(keys.get(1));
		return hash.digest();
	}
}
