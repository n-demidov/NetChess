package edu.demidov.netchess.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptAlgorithm {

    public static final String SHA512_ALGORITHM = "SHA-512";
    private final static Logger log = LoggerFactory.getLogger(EncryptAlgorithm.class);
    private static EncryptAlgorithm instance;

    private EncryptAlgorithm() {
    }

    public static synchronized EncryptAlgorithm getInstance() {
        if (instance == null) instance = new EncryptAlgorithm();
        return instance;
    }

    /**
     * Возвращает хеш по строке
     *
     * @param algorithm
     * @param str
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String getHashCodeFromString(final String algorithm, final String str)
            throws NoSuchAlgorithmException {
        log.trace("getHashCodeFromString algorithm={}", algorithm);
        final MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(str.getBytes());
        final byte byteData[] = md.digest();

        // convert the byte to hex format
        final StringBuffer hashCodeBuffer = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            hashCodeBuffer.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return hashCodeBuffer.toString();
    }

}
