package com.babbaj.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;

/**
 * Created by Babbaj on 1/12/2018.
 */
public class Utils {

    public static final HttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final String AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";

    public static byte[] downloadFile(String url) {
        HttpGet get = new HttpGet(url);
        get.addHeader("User-Agent", AGENT);
        try (InputStream data = HTTP_CLIENT.execute(get).getEntity().getContent()) {
            return IOUtils.toByteArray(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
