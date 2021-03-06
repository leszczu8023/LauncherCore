/*
 * This file is part of Technic Launcher.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.utilslib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.mirror.MirrorStore;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class Utils {
    private static final Gson gson;
    private static final Logger logger = Logger.getLogger("net.technicpack.launcher.Main");

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    public static Gson getGson() {
        return gson;
    }

    public static Logger getLogger() {
        return logger;
    }

    /**
     * Establishes an HttpURLConnection from a URL, with the correct configuration to receive content from the given URL.
     *
     * @param url The URL to set up and receive content from
     * @return A valid HttpURLConnection
     * @throws IOException The openConnection() method throws an IOException and the calling method is responsible for handling it.
     */
    public static HttpURLConnection openHttpConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(false);
        System.setProperty("http.agent", TechnicConstants.getUserAgent());
        conn.setRequestProperty("User-Agent", TechnicConstants.getUserAgent());
        conn.setUseCaches(false);
        return conn;
    }

    /**
     * Opens an HTTP connection to a web URL and tests that the response is a valid 200-level code
     * and we can successfully open a stream to the content.
     *
     * @param urlLoc The HTTP URL indicating the location of the content.
     * @return True if the content can be accessed successfully, false otherwise.
     */
    public static boolean pingHttpURL(String urlLoc, MirrorStore mirrorStore) {
        InputStream stream = null;
        try {
            final URL url = mirrorStore.getFullUrl(urlLoc);
            HttpURLConnection conn = openHttpConnection(url);

            int responseCode = conn.getResponseCode();
            int responseFamily = responseCode / 100;

            if (responseFamily == 3) {
                String newUrl = conn.getHeaderField("Location");
                URL redirectUrl = null;
                try {
                    redirectUrl = new URL(newUrl);
                } catch (MalformedURLException ex) {
                    throw new DownloadException("Invalid Redirect URL: " + url, ex);
                }

                conn = openHttpConnection(redirectUrl);
                responseCode = conn.getResponseCode();
                responseFamily = responseCode/100;
            }

            if (responseFamily == 2) {
                stream = conn.getInputStream();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static boolean sendTracking(String category, String action, String label, String clientId) {
        String url = "https://www.google-analytics.com/collect";
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");

            String urlParameters = "v=1&tid=UA-30896795-3&cid=" + clientId + "&t=event&ec=" + category + "&ea=" + action + "&el=" + label;

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            Utils.getLogger().info("Analytics Response [" + category + "]: " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();


            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     *
     * Run a command on the local command line and return the program output.
     * THIS COMMAND IS BLOCKING!  Only run for short command line stuff, or I guess run on a thread.
     *
     * @param command List of args to run on the command line
     * @return The newline-separated program output
     */
    public static String getProcessOutput(String... command) {
        String out = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final StringBuilder response=new StringBuilder();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            response.append(line + "\n");
                        }
                    } catch (IOException ex) {
                        //Don't let other process' problems concern us
                    } finally {
                        IOUtils.closeQuietly(bufferedReader);
                    }
                }
            }).start();
            process.waitFor();


            if (response.toString().length() > 0) {
                out = response.toString().trim();
            }
        }
        catch (IOException e) {
            //Some kind of problem running java -version or getting output, just assume the version is bad
            return null;
        } catch (InterruptedException ex) {
            //Something booted us while we were waiting on java -version to complete, just assume
            //this version is bad
            return null;
        }
        return out;
    }
}
