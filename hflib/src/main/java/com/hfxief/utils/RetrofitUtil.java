package com.hfxief.utils;

import android.content.Context;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * LongFor
 * com.hfxief.utils
 *
 * @Author: xie
 * @Time: 2017/5/15 10:11
 * @Description:
 */


public class RetrofitUtil {

    public static SSLSocketFactory getSSLSocketFactory(Context context, int[] certificates) {

        if (context == null) {
            throw new NullPointerException("context == null");
        }

        CertificateFactory certificateFactory;
        SSLContext sslContext = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            for (int i = 0; i < certificates.length; i++) {
                InputStream certificate = context.getResources().openRawResource(certificates[i]);
                keyStore.setCertificateEntry(String.valueOf(i), certificateFactory.generateCertificate(certificate));

                if (certificate != null) {
                    certificate.close();
                }
            }
            sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sslContext != null) {
            return sslContext.getSocketFactory();
        } else {
            return null;
        }
    }

    public static HostnameVerifier getHostnameVerifier(final String[] hostUrls) {

        return (String hostname, SSLSession session) -> {
            boolean ret = false;
            for (String host : hostUrls) {
                if (host.equalsIgnoreCase(hostname)) {
                    ret = true;
                }
//                else if (getHostHome(host).equalsIgnoreCase(hostname)) {
//                    ret = true;
//                }
            }
            return ret;
        };
    }

    private static String getHostHome(String home) {
        if (home.startsWith("https://")) {
            home = home.substring(8);
        }
        if (home.startsWith("http://")) {
            home = home.substring(7);
        }
        int index = home.indexOf(":");
        return home.substring(0, index);
    }

}
