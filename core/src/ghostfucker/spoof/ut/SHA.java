package ghostfucker.spoof.ut;

import java.nio.ByteBuffer;

/**
 * Custom SHA-1 implementation used for uTorrent peer ID verification bytes.
 * This is a standalone implementation that does not rely on java.security.MessageDigest.
 */
public final class SHA {

	private int h0;
	private int h1;
	private int h2;
	private int h3;
	private int h4;
	private ByteBuffer finalBuffer = ByteBuffer.allocate(64);
	private ByteBuffer saveBuffer;
	private int s0;
	private int s1;
	private int s2;
	private int s3;
	private int s4;
	private long length;
	private long saveLength;
	private byte[] cacheBlock;
	private static final int cacheSize = 4096;

	public SHA() {
		this.finalBuffer.position(0);
		this.finalBuffer.limit(64);
		this.saveBuffer = ByteBuffer.allocate(64);
		this.saveBuffer.position(0);
		this.saveBuffer.limit(64);
		this.reset();
	}

	private void transform(byte[] ar, int offset) {
		int w0 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w1 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w2 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w3 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w4 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w5 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w6 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w7 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w8 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w9 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w10 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w11 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w12 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w13 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w14 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset++] & 255;
		int w15 = (ar[offset++] & 255) << 24 | (ar[offset++] & 255) << 16 | (ar[offset++] & 255) << 8 | ar[offset] & 255;

		int a = this.h0;
		int b = this.h1;
		int c = this.h2;
		int d = this.h3;
		int e = this.h4;

		// Rounds 0-15
		e += (a << 5 | a >>> 27) + w0 + (b & c | ~b & d) + 1518500249; b = b << 30 | b >>> 2;
		d += (e << 5 | e >>> 27) + w1 + (a & b | ~a & c) + 1518500249; a = a << 30 | a >>> 2;
		c += (d << 5 | d >>> 27) + w2 + (e & a | ~e & b) + 1518500249; e = e << 30 | e >>> 2;
		b += (c << 5 | c >>> 27) + w3 + (d & e | ~d & a) + 1518500249; d = d << 30 | d >>> 2;
		a += (b << 5 | b >>> 27) + w4 + (c & d | ~c & e) + 1518500249; c = c << 30 | c >>> 2;
		e += (a << 5 | a >>> 27) + w5 + (b & c | ~b & d) + 1518500249; b = b << 30 | b >>> 2;
		d += (e << 5 | e >>> 27) + w6 + (a & b | ~a & c) + 1518500249; a = a << 30 | a >>> 2;
		c += (d << 5 | d >>> 27) + w7 + (e & a | ~e & b) + 1518500249; e = e << 30 | e >>> 2;
		b += (c << 5 | c >>> 27) + w8 + (d & e | ~d & a) + 1518500249; d = d << 30 | d >>> 2;
		a += (b << 5 | b >>> 27) + w9 + (c & d | ~c & e) + 1518500249; c = c << 30 | c >>> 2;
		e += (a << 5 | a >>> 27) + w10 + (b & c | ~b & d) + 1518500249; b = b << 30 | b >>> 2;
		d += (e << 5 | e >>> 27) + w11 + (a & b | ~a & c) + 1518500249; a = a << 30 | a >>> 2;
		c += (d << 5 | d >>> 27) + w12 + (e & a | ~e & b) + 1518500249; e = e << 30 | e >>> 2;
		b += (c << 5 | c >>> 27) + w13 + (d & e | ~d & a) + 1518500249; d = d << 30 | d >>> 2;
		a += (b << 5 | b >>> 27) + w14 + (c & d | ~c & e) + 1518500249; c = c << 30 | c >>> 2;
		e += (a << 5 | a >>> 27) + w15 + (b & c | ~b & d) + 1518500249; b = b << 30 | b >>> 2;

		// Rounds 16-19
		w0 = w13 ^ w8 ^ w2 ^ w0; w0 = w0 << 1 | w0 >>> 31;
		d += (e << 5 | e >>> 27) + w0 + (a & b | ~a & c) + 1518500249; a = a << 30 | a >>> 2;
		w1 = w14 ^ w9 ^ w3 ^ w1; w1 = w1 << 1 | w1 >>> 31;
		c += (d << 5 | d >>> 27) + w1 + (e & a | ~e & b) + 1518500249; e = e << 30 | e >>> 2;
		w2 = w15 ^ w10 ^ w4 ^ w2; w2 = w2 << 1 | w2 >>> 31;
		b += (c << 5 | c >>> 27) + w2 + (d & e | ~d & a) + 1518500249; d = d << 30 | d >>> 2;
		w3 = w0 ^ w11 ^ w5 ^ w3; w3 = w3 << 1 | w3 >>> 31;
		a += (b << 5 | b >>> 27) + w3 + (c & d | ~c & e) + 1518500249; c = c << 30 | c >>> 2;

		// Rounds 20-39 (XOR function)
		w4 = w1 ^ w12 ^ w6 ^ w4; w4 = w4 << 1 | w4 >>> 31;
		e += (a << 5 | a >>> 27) + w4 + (b ^ c ^ d) + 1859775393; b = b << 30 | b >>> 2;
		w5 = w2 ^ w13 ^ w7 ^ w5; w5 = w5 << 1 | w5 >>> 31;
		d += (e << 5 | e >>> 27) + w5 + (a ^ b ^ c) + 1859775393; a = a << 30 | a >>> 2;
		w6 = w3 ^ w14 ^ w8 ^ w6; w6 = w6 << 1 | w6 >>> 31;
		c += (d << 5 | d >>> 27) + w6 + (e ^ a ^ b) + 1859775393; e = e << 30 | e >>> 2;
		w7 = w4 ^ w15 ^ w9 ^ w7; w7 = w7 << 1 | w7 >>> 31;
		b += (c << 5 | c >>> 27) + w7 + (d ^ e ^ a) + 1859775393; d = d << 30 | d >>> 2;
		w8 = w5 ^ w0 ^ w10 ^ w8; w8 = w8 << 1 | w8 >>> 31;
		a += (b << 5 | b >>> 27) + w8 + (c ^ d ^ e) + 1859775393; c = c << 30 | c >>> 2;
		w9 = w6 ^ w1 ^ w11 ^ w9; w9 = w9 << 1 | w9 >>> 31;
		e += (a << 5 | a >>> 27) + w9 + (b ^ c ^ d) + 1859775393; b = b << 30 | b >>> 2;
		w10 = w7 ^ w2 ^ w12 ^ w10; w10 = w10 << 1 | w10 >>> 31;
		d += (e << 5 | e >>> 27) + w10 + (a ^ b ^ c) + 1859775393; a = a << 30 | a >>> 2;
		w11 = w8 ^ w3 ^ w13 ^ w11; w11 = w11 << 1 | w11 >>> 31;
		c += (d << 5 | d >>> 27) + w11 + (e ^ a ^ b) + 1859775393; e = e << 30 | e >>> 2;
		w12 = w9 ^ w4 ^ w14 ^ w12; w12 = w12 << 1 | w12 >>> 31;
		b += (c << 5 | c >>> 27) + w12 + (d ^ e ^ a) + 1859775393; d = d << 30 | d >>> 2;
		w13 = w10 ^ w5 ^ w15 ^ w13; w13 = w13 << 1 | w13 >>> 31;
		a += (b << 5 | b >>> 27) + w13 + (c ^ d ^ e) + 1859775393; c = c << 30 | c >>> 2;
		w14 = w11 ^ w6 ^ w0 ^ w14; w14 = w14 << 1 | w14 >>> 31;
		e += (a << 5 | a >>> 27) + w14 + (b ^ c ^ d) + 1859775393; b = b << 30 | b >>> 2;
		w15 = w12 ^ w7 ^ w1 ^ w15; w15 = w15 << 1 | w15 >>> 31;
		d += (e << 5 | e >>> 27) + w15 + (a ^ b ^ c) + 1859775393; a = a << 30 | a >>> 2;
		w0 = w13 ^ w8 ^ w2 ^ w0; w0 = w0 << 1 | w0 >>> 31;
		c += (d << 5 | d >>> 27) + w0 + (e ^ a ^ b) + 1859775393; e = e << 30 | e >>> 2;
		w1 = w14 ^ w9 ^ w3 ^ w1; w1 = w1 << 1 | w1 >>> 31;
		b += (c << 5 | c >>> 27) + w1 + (d ^ e ^ a) + 1859775393; d = d << 30 | d >>> 2;
		w2 = w15 ^ w10 ^ w4 ^ w2; w2 = w2 << 1 | w2 >>> 31;
		a += (b << 5 | b >>> 27) + w2 + (c ^ d ^ e) + 1859775393; c = c << 30 | c >>> 2;
		w3 = w0 ^ w11 ^ w5 ^ w3; w3 = w3 << 1 | w3 >>> 31;
		e += (a << 5 | a >>> 27) + w3 + (b ^ c ^ d) + 1859775393; b = b << 30 | b >>> 2;
		w4 = w1 ^ w12 ^ w6 ^ w4; w4 = w4 << 1 | w4 >>> 31;
		d += (e << 5 | e >>> 27) + w4 + (a ^ b ^ c) + 1859775393; a = a << 30 | a >>> 2;
		w5 = w2 ^ w13 ^ w7 ^ w5; w5 = w5 << 1 | w5 >>> 31;
		c += (d << 5 | d >>> 27) + w5 + (e ^ a ^ b) + 1859775393; e = e << 30 | e >>> 2;
		w6 = w3 ^ w14 ^ w8 ^ w6; w6 = w6 << 1 | w6 >>> 31;
		b += (c << 5 | c >>> 27) + w6 + (d ^ e ^ a) + 1859775393; d = d << 30 | d >>> 2;
		w7 = w4 ^ w15 ^ w9 ^ w7; w7 = w7 << 1 | w7 >>> 31;
		a += (b << 5 | b >>> 27) + w7 + (c ^ d ^ e) + 1859775393; c = c << 30 | c >>> 2;

		// Rounds 40-59 (majority function)
		w8 = w5 ^ w0 ^ w10 ^ w8; w8 = w8 << 1 | w8 >>> 31;
		e += (a << 5 | a >>> 27) + w8 + (b & c | b & d | c & d) + -1894007588; b = b << 30 | b >>> 2;
		w9 = w6 ^ w1 ^ w11 ^ w9; w9 = w9 << 1 | w9 >>> 31;
		d += (e << 5 | e >>> 27) + w9 + (a & b | a & c | b & c) + -1894007588; a = a << 30 | a >>> 2;
		w10 = w7 ^ w2 ^ w12 ^ w10; w10 = w10 << 1 | w10 >>> 31;
		c += (d << 5 | d >>> 27) + w10 + (e & a | e & b | a & b) + -1894007588; e = e << 30 | e >>> 2;
		w11 = w8 ^ w3 ^ w13 ^ w11; w11 = w11 << 1 | w11 >>> 31;
		b += (c << 5 | c >>> 27) + w11 + (d & e | d & a | e & a) + -1894007588; d = d << 30 | d >>> 2;
		w12 = w9 ^ w4 ^ w14 ^ w12; w12 = w12 << 1 | w12 >>> 31;
		a += (b << 5 | b >>> 27) + w12 + (c & d | c & e | d & e) + -1894007588; c = c << 30 | c >>> 2;
		w13 = w10 ^ w5 ^ w15 ^ w13; w13 = w13 << 1 | w13 >>> 31;
		e += (a << 5 | a >>> 27) + w13 + (b & c | b & d | c & d) + -1894007588; b = b << 30 | b >>> 2;
		w14 = w11 ^ w6 ^ w0 ^ w14; w14 = w14 << 1 | w14 >>> 31;
		d += (e << 5 | e >>> 27) + w14 + (a & b | a & c | b & c) + -1894007588; a = a << 30 | a >>> 2;
		w15 = w12 ^ w7 ^ w1 ^ w15; w15 = w15 << 1 | w15 >>> 31;
		c += (d << 5 | d >>> 27) + w15 + (e & a | e & b | a & b) + -1894007588; e = e << 30 | e >>> 2;
		w0 = w13 ^ w8 ^ w2 ^ w0; w0 = w0 << 1 | w0 >>> 31;
		b += (c << 5 | c >>> 27) + w0 + (d & e | d & a | e & a) + -1894007588; d = d << 30 | d >>> 2;
		w1 = w14 ^ w9 ^ w3 ^ w1; w1 = w1 << 1 | w1 >>> 31;
		a += (b << 5 | b >>> 27) + w1 + (c & d | c & e | d & e) + -1894007588; c = c << 30 | c >>> 2;
		w2 = w15 ^ w10 ^ w4 ^ w2; w2 = w2 << 1 | w2 >>> 31;
		e += (a << 5 | a >>> 27) + w2 + (b & c | b & d | c & d) + -1894007588; b = b << 30 | b >>> 2;
		w3 = w0 ^ w11 ^ w5 ^ w3; w3 = w3 << 1 | w3 >>> 31;
		d += (e << 5 | e >>> 27) + w3 + (a & b | a & c | b & c) + -1894007588; a = a << 30 | a >>> 2;
		w4 = w1 ^ w12 ^ w6 ^ w4; w4 = w4 << 1 | w4 >>> 31;
		c += (d << 5 | d >>> 27) + w4 + (e & a | e & b | a & b) + -1894007588; e = e << 30 | e >>> 2;
		w5 = w2 ^ w13 ^ w7 ^ w5; w5 = w5 << 1 | w5 >>> 31;
		b += (c << 5 | c >>> 27) + w5 + (d & e | d & a | e & a) + -1894007588; d = d << 30 | d >>> 2;
		w6 = w3 ^ w14 ^ w8 ^ w6; w6 = w6 << 1 | w6 >>> 31;
		a += (b << 5 | b >>> 27) + w6 + (c & d | c & e | d & e) + -1894007588; c = c << 30 | c >>> 2;
		w7 = w4 ^ w15 ^ w9 ^ w7; w7 = w7 << 1 | w7 >>> 31;
		e += (a << 5 | a >>> 27) + w7 + (b & c | b & d | c & d) + -1894007588; b = b << 30 | b >>> 2;
		w8 = w5 ^ w0 ^ w10 ^ w8; w8 = w8 << 1 | w8 >>> 31;
		d += (e << 5 | e >>> 27) + w8 + (a & b | a & c | b & c) + -1894007588; a = a << 30 | a >>> 2;
		w9 = w6 ^ w1 ^ w11 ^ w9; w9 = w9 << 1 | w9 >>> 31;
		c += (d << 5 | d >>> 27) + w9 + (e & a | e & b | a & b) + -1894007588; e = e << 30 | e >>> 2;
		w10 = w7 ^ w2 ^ w12 ^ w10; w10 = w10 << 1 | w10 >>> 31;
		b += (c << 5 | c >>> 27) + w10 + (d & e | d & a | e & a) + -1894007588; d = d << 30 | d >>> 2;
		w11 = w8 ^ w3 ^ w13 ^ w11; w11 = w11 << 1 | w11 >>> 31;
		a += (b << 5 | b >>> 27) + w11 + (c & d | c & e | d & e) + -1894007588; c = c << 30 | c >>> 2;

		// Rounds 60-79 (XOR function again)
		w12 = w9 ^ w4 ^ w14 ^ w12; w12 = w12 << 1 | w12 >>> 31;
		e += (a << 5 | a >>> 27) + w12 + (b ^ c ^ d) + -899497514; b = b << 30 | b >>> 2;
		w13 = w10 ^ w5 ^ w15 ^ w13; w13 = w13 << 1 | w13 >>> 31;
		d += (e << 5 | e >>> 27) + w13 + (a ^ b ^ c) + -899497514; a = a << 30 | a >>> 2;
		w14 = w11 ^ w6 ^ w0 ^ w14; w14 = w14 << 1 | w14 >>> 31;
		c += (d << 5 | d >>> 27) + w14 + (e ^ a ^ b) + -899497514; e = e << 30 | e >>> 2;
		w15 = w12 ^ w7 ^ w1 ^ w15; w15 = w15 << 1 | w15 >>> 31;
		b += (c << 5 | c >>> 27) + w15 + (d ^ e ^ a) + -899497514; d = d << 30 | d >>> 2;
		w0 = w13 ^ w8 ^ w2 ^ w0; w0 = w0 << 1 | w0 >>> 31;
		a += (b << 5 | b >>> 27) + w0 + (c ^ d ^ e) + -899497514; c = c << 30 | c >>> 2;
		w1 = w14 ^ w9 ^ w3 ^ w1; w1 = w1 << 1 | w1 >>> 31;
		e += (a << 5 | a >>> 27) + w1 + (b ^ c ^ d) + -899497514; b = b << 30 | b >>> 2;
		w2 = w15 ^ w10 ^ w4 ^ w2; w2 = w2 << 1 | w2 >>> 31;
		d += (e << 5 | e >>> 27) + w2 + (a ^ b ^ c) + -899497514; a = a << 30 | a >>> 2;
		w3 = w0 ^ w11 ^ w5 ^ w3; w3 = w3 << 1 | w3 >>> 31;
		c += (d << 5 | d >>> 27) + w3 + (e ^ a ^ b) + -899497514; e = e << 30 | e >>> 2;
		w4 = w1 ^ w12 ^ w6 ^ w4; w4 = w4 << 1 | w4 >>> 31;
		b += (c << 5 | c >>> 27) + w4 + (d ^ e ^ a) + -899497514; d = d << 30 | d >>> 2;
		w5 = w2 ^ w13 ^ w7 ^ w5; w5 = w5 << 1 | w5 >>> 31;
		a += (b << 5 | b >>> 27) + w5 + (c ^ d ^ e) + -899497514; c = c << 30 | c >>> 2;
		w6 = w3 ^ w14 ^ w8 ^ w6; w6 = w6 << 1 | w6 >>> 31;
		e += (a << 5 | a >>> 27) + w6 + (b ^ c ^ d) + -899497514; b = b << 30 | b >>> 2;
		w7 = w4 ^ w15 ^ w9 ^ w7; w7 = w7 << 1 | w7 >>> 31;
		d += (e << 5 | e >>> 27) + w7 + (a ^ b ^ c) + -899497514; a = a << 30 | a >>> 2;
		w8 = w5 ^ w0 ^ w10 ^ w8; w8 = w8 << 1 | w8 >>> 31;
		c += (d << 5 | d >>> 27) + w8 + (e ^ a ^ b) + -899497514; e = e << 30 | e >>> 2;
		w9 = w6 ^ w1 ^ w11 ^ w9; w9 = w9 << 1 | w9 >>> 31;
		b += (c << 5 | c >>> 27) + w9 + (d ^ e ^ a) + -899497514; d = d << 30 | d >>> 2;
		w10 = w7 ^ w2 ^ w12 ^ w10; w10 = w10 << 1 | w10 >>> 31;
		a += (b << 5 | b >>> 27) + w10 + (c ^ d ^ e) + -899497514; c = c << 30 | c >>> 2;
		w11 = w8 ^ w3 ^ w13 ^ w11; w11 = w11 << 1 | w11 >>> 31;
		e += (a << 5 | a >>> 27) + w11 + (b ^ c ^ d) + -899497514; b = b << 30 | b >>> 2;
		w12 = w9 ^ w4 ^ w14 ^ w12; w12 = w12 << 1 | w12 >>> 31;
		d += (e << 5 | e >>> 27) + w12 + (a ^ b ^ c) + -899497514; a = a << 30 | a >>> 2;
		w13 = w10 ^ w5 ^ w15 ^ w13; w13 = w13 << 1 | w13 >>> 31;
		c += (d << 5 | d >>> 27) + w13 + (e ^ a ^ b) + -899497514; e = e << 30 | e >>> 2;
		w14 = w11 ^ w6 ^ w0 ^ w14; w14 = w14 << 1 | w14 >>> 31;
		b += (c << 5 | c >>> 27) + w14 + (d ^ e ^ a) + -899497514; d = d << 30 | d >>> 2;
		w15 = w12 ^ w7 ^ w1 ^ w15; w15 = w15 << 1 | w15 >>> 31;
		a += (b << 5 | b >>> 27) + w15 + (c ^ d ^ e) + -899497514; c = c << 30 | c >>> 2;

		this.h0 += a;
		this.h1 += b;
		this.h2 += c;
		this.h3 += d;
		this.h4 += e;
	}

	private void completeFinalBuffer(ByteBuffer buffer) {
		if (this.finalBuffer.position() != 0) {
			while (buffer.remaining() > 0 && this.finalBuffer.remaining() > 0) {
				this.finalBuffer.put(buffer.get());
			}

			if (this.finalBuffer.remaining() == 0) {
				this.transform(this.finalBuffer.array(), 0);
				this.finalBuffer.rewind();
			}
		}
	}

	public void reset() {
		this.h0 = 1732584193;
		this.h1 = -271733879;
		this.h2 = -1732584194;
		this.h3 = 271733878;
		this.h4 = -1009589776;
		this.length = 0L;
		this.finalBuffer.clear();
	}

	public void update(ByteBuffer buffer) {
		int position = buffer.position();
		this.length += (long) buffer.remaining();
		this.completeFinalBuffer(buffer);
		if (buffer.hasArray() && !buffer.isDirect()) {
			int endPos = buffer.position() + buffer.remaining() - buffer.remaining() % 64;
			int internalEndPos = endPos + buffer.arrayOffset();

			for (int i = buffer.arrayOffset() + buffer.position(); i < internalEndPos; i += 64) {
				this.transform(buffer.array(), i);
			}

			buffer.position(endPos);
		} else {
			if (this.cacheBlock == null) {
				this.cacheBlock = new byte[4096];
			}

			while (buffer.remaining() >= 64) {
				int toProcess = Math.min(buffer.remaining() - buffer.remaining() % 64, 4096);
				buffer.get(this.cacheBlock, 0, toProcess);

				for (int i = 0; i < toProcess; i += 64) {
					this.transform(this.cacheBlock, i);
				}
			}
		}

		if (buffer.remaining() != 0) {
			this.finalBuffer.put(buffer);
		}

		buffer.position(position);
	}

	public byte[] digest() {
		byte[] result = new byte[20];
		this.finalBuffer.put((byte) -128);
		if (this.finalBuffer.remaining() < 8) {
			while (this.finalBuffer.remaining() > 0) {
				this.finalBuffer.put((byte) 0);
			}

			this.finalBuffer.position(0);
			this.transform(this.finalBuffer.array(), 0);
			this.finalBuffer.position(0);
		}

		while (this.finalBuffer.remaining() > 8) {
			this.finalBuffer.put((byte) 0);
		}

		this.finalBuffer.putLong(this.length << 3);
		this.finalBuffer.position(0);
		this.transform(this.finalBuffer.array(), 0);
		this.finalBuffer.position(0);
		this.finalBuffer.putInt(this.h0);
		this.finalBuffer.putInt(this.h1);
		this.finalBuffer.putInt(this.h2);
		this.finalBuffer.putInt(this.h3);
		this.finalBuffer.putInt(this.h4);
		this.finalBuffer.rewind();
		this.finalBuffer.get(result, 0, 20);
		return result;
	}

	public byte[] digest(byte[] buffer) {
		this.update(ByteBuffer.wrap(buffer));
		return this.digest();
	}

	public void saveState() {
		this.s0 = this.h0;
		this.s1 = this.h1;
		this.s2 = this.h2;
		this.s3 = this.h3;
		this.s4 = this.h4;
		this.saveLength = this.length;
		int position = this.finalBuffer.position();
		this.finalBuffer.rewind();
		this.finalBuffer.limit(position);
		this.saveBuffer.clear();
		this.saveBuffer.put(this.finalBuffer);
		this.saveBuffer.flip();
		this.finalBuffer.limit(64);
		this.finalBuffer.position(position);
	}

	public void restoreState() {
		this.h0 = this.s0;
		this.h1 = this.s1;
		this.h2 = this.s2;
		this.h3 = this.s3;
		this.h4 = this.s4;
		this.length = this.saveLength;
		this.finalBuffer.clear();
		this.finalBuffer.put(this.saveBuffer);
	}
}
