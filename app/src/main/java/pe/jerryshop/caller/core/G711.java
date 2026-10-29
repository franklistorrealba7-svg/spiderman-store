package pe.jerryshop.caller.core;

/** Códecs telefónicos G.711 (μ-law = PCMU, A-law = PCMA) y utilidades de audio PCM 16 bits. */
public final class G711 {
    private G711() {}

    private static final int[] SEG_END = {0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF};
    private static final short[] ULAW_DEC = new short[256];
    private static final short[] ALAW_DEC = new short[256];
    private static final byte[] ULAW_TO_ALAW = new byte[256];

    static {
        for (int i = 0; i < 256; i++) {
            ULAW_DEC[i] = ulawDecode((byte) i);
            ALAW_DEC[i] = alawDecode((byte) i);
        }
        for (int i = 0; i < 256; i++) ULAW_TO_ALAW[i] = linearToAlaw(ULAW_DEC[i]);
    }

    private static short ulawDecode(byte b) {
        int u = ~b & 0xFF;
        int t = ((u & 0x0F) << 3) + 0x84;
        t <<= (u & 0x70) >> 4;
        return (short) ((u & 0x80) != 0 ? (0x84 - t) : (t - 0x84));
    }

    private static short alawDecode(byte b) {
        int a = (b ^ 0x55) & 0xFF;
        int t = (a & 0x0F) << 4;
        int seg = (a & 0x70) >> 4;
        if (seg == 0) t += 8;
        else if (seg == 1) t += 0x108;
        else { t += 0x108; t <<= seg - 1; }
        return (short) ((a & 0x80) != 0 ? t : -t);
    }

    public static byte linearToUlaw(int pcm) {
        final int BIAS = 0x84, CLIP = 32635;
        int sign = (pcm >> 8) & 0x80;
        if (sign != 0) pcm = -pcm;
        if (pcm > CLIP) pcm = CLIP;
        pcm += BIAS;
        int exp = 7;
        for (int mask = 0x4000; (pcm & mask) == 0 && exp > 0; mask >>= 1) exp--;
        int mant = (pcm >> (exp + 3)) & 0x0F;
        return (byte) ~(sign | (exp << 4) | mant);
    }

    public static byte linearToAlaw(int pcm) {
        int mask;
        if (pcm >= 0) mask = 0xD5;
        else { mask = 0x55; pcm = -pcm - 1; }
        if (pcm > 32767) pcm = 32767;
        int seg = 0;
        while (seg < 8 && pcm > SEG_END[seg]) seg++;
        if (seg >= 8) return (byte) (0x7F ^ mask);
        int aval = seg << 4;
        aval |= seg < 2 ? (pcm >> 4) & 0x0F : (pcm >> (seg + 3)) & 0x0F;
        return (byte) (aval ^ mask);
    }

    public static short ulawToLinear(byte b) { return ULAW_DEC[b & 0xFF]; }
    public static short alawToLinear(byte b) { return ALAW_DEC[b & 0xFF]; }

    /** μ-law -> A-law sin pasar por cálculos (tabla). */
    public static void ulawToAlaw(byte[] src, byte[] dst, int n) {
        for (int i = 0; i < n; i++) dst[i] = ULAW_TO_ALAW[src[i] & 0xFF];
    }

    /** Decodifica payload G.711 a PCM 16 bits. pt=0 PCMU, pt=8 PCMA. */
    public static short[] decode(byte[] data, int off, int len, int pt) {
        short[] out = new short[len];
        short[] tab = pt == 8 ? ALAW_DEC : ULAW_DEC;
        for (int i = 0; i < len; i++) out[i] = tab[data[off + i] & 0xFF];
        return out;
    }

    public static byte[] pcmToUlaw(short[] pcm) {
        byte[] out = new byte[pcm.length];
        for (int i = 0; i < pcm.length; i++) out[i] = linearToUlaw(pcm[i]);
        return out;
    }

    public static int rms(short[] pcm, int off, int len) {
        if (len <= 0) return 0;
        long acc = 0;
        for (int i = off; i < off + len; i++) acc += (long) pcm[i] * pcm[i];
        return (int) Math.sqrt((double) acc / len);
    }

    /** PCM 16 bits 8 kHz mono -> archivo WAV en memoria (para la transcripción). */
    public static byte[] wav(short[] pcm, int rate) {
        int dataLen = pcm.length * 2;
        byte[] b = new byte[44 + dataLen];
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes()).put("fmt ".getBytes())
          .putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate * 2)
          .putShort((short) 2).putShort((short) 16).put("data".getBytes()).putInt(dataLen);
        for (short s : pcm) bb.putShort(s);
        return b;
    }

    public static final byte ULAW_SILENCE = (byte) 0xFF;
}
