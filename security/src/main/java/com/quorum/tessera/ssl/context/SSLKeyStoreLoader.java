package com.quorum.tessera.ssl.context;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SSLKeyStoreLoader {

    private static final Pattern KEY_PATTERN = Pattern.compile(
        "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
            "([a-z0-9+/=\\r\\n]+)" +                       // Base64 text
            "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",            // Footer
        2);
    private static final Pattern CERT_PATTERN = Pattern.compile(
        "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
            "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
            "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
        2);
    private static final String KEYSTORE_TYPE="JKS";

    private static final Base64.Decoder decoder = Base64.getMimeDecoder();

    private SSLKeyStoreLoader(){

    }


    public static KeyManager[] fromJksKeyStore(Path keyStoreFile, String keyStorePassword) throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException {

        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

        try (final InputStream in = Files.newInputStream(keyStoreFile)) {
            keyStore.load(in, keyStorePassword.toCharArray());
        }

        final KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        return keyManagerFactory.getKeyManagers();

    }

    public static KeyManager[] fromPemKeyFile(Path key, Path certificate) throws IOException, GeneralSecurityException {

        final PKCS8EncodedKeySpec encodedKeySpec = getEncodedKeySpec(key);
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PrivateKey privateKey = keyFactory.generatePrivate(encodedKeySpec);

        List<X509Certificate> certificates = getCertificate(certificate);

        if (certificates.isEmpty()) {
            throw new CertificateException("NO CERTIFICATE FOUND IN FILE");
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("tessera", privateKey, "".toCharArray(), certificates.stream().toArray(Certificate[]::new));

        final KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "".toCharArray());

        return keyManagerFactory.getKeyManagers();
    }

    public static TrustManager[] fromJksTrustStore(Path trustStoreFile, String trustStorePassword) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final KeyStore trustStore = KeyStore.getInstance(KEYSTORE_TYPE);

        try (final InputStream in = Files.newInputStream(trustStoreFile)) {
            trustStore.load(in, trustStorePassword.toCharArray());
        }

        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        return trustManagerFactory.getTrustManagers();
    }

    public static TrustManager[] fromPemCertificatesFile(List<Path> trustedCertificates) throws GeneralSecurityException, IOException {
        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);

        List<X509Certificate> certificates = new ArrayList<>();

        for (Path path : trustedCertificates) {
            certificates.addAll(getCertificate(path));
        }

        for (X509Certificate certificate : certificates) {
            X500Principal principal = certificate.getSubjectX500Principal();
            trustStore.setCertificateEntry(principal.getName("RFC2253"), certificate);
        }

        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        return trustManagerFactory.getTrustManagers();
    }

    private static PKCS8EncodedKeySpec getEncodedKeySpec(Path keyFile) throws IOException, GeneralSecurityException {
        String keyFileContent = readFile(keyFile);
        Matcher matcher = KEY_PATTERN.matcher(keyFileContent);

        if (!matcher.find()) {
            throw new KeyStoreException("NO PRIVATE KEY IN FILE " + keyFile);
        }

        byte[] encodedKey = decoder.decode(matcher.group(1));

        return new PKCS8EncodedKeySpec(encodedKey);
    }

    private static List<X509Certificate> getCertificate(Path certificateFile) throws IOException, GeneralSecurityException {
        String contents = readFile(certificateFile);

        Matcher matcher = CERT_PATTERN.matcher(contents);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certificates = new ArrayList<>();

        int start = 0;
        while (matcher.find(start)) {
            byte[] buffer = decoder.decode(matcher.group(1));
            certificates.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(buffer)));
            start = matcher.end();
        }

        return certificates;
    }

    private static String readFile(Path file) throws IOException {

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            StringBuilder stringBuilder = new StringBuilder();
            CharBuffer buffer = CharBuffer.allocate(4096);

            while (reader.read(buffer) != -1) {
                buffer.flip();
                stringBuilder.append(buffer);
                buffer.clear();
            }
            return stringBuilder.toString();
        }
    }
}
