/*
 * @Description: a class containing methods for getting the info of the connected Wi-Fi
 * @Version: 1.5.2.20200126
 * @Author: Arvin Zhao
 * @Date: 2020-01-20 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-01-26 15:47:48
 */

package com.arvinzjc.xshielder;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class WifiInfoUtils
{
    private static final String TAG = "WifiInfoUtils";

    /**
     * lower bound on the 2.4 GHz (802.11b/g/n) WLAN channels
     */
    private static final int LOWER_FREQUENCY_24GHZ = 2412;

    /**
     * upper bound on the 2.4 GHz (802.11b/g/n) WLAN channels
     */
    private static final int HIGHER_FREQUENCY_24GHZ = 2482;

    /**
     * lower bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels
     */
    private static final int LOWER_FREQUENCY_5GHZ = 4915;

    /**
     * upper bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels
     */
    private static final int HIGHER_FREQUENCY_5GHZ = 5825;

    /**
     * the number of distinct wifi levels
     */
    private static final int SIGNAL_LEVELS = 5;

    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private DhcpInfo dhcpInfo;
    private String ssid;
    private Activity activity;

    WifiInfoUtils(Activity callingActivity)
    {
        activity = callingActivity;
        wifiManager = (WifiManager)activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null)
        {
            wifiInfo = wifiManager.getConnectionInfo();
            dhcpInfo = wifiManager.getDhcpInfo();
            ssid = wifiInfo.getSSID().replace("\"", "");
        }
        else
            throw new NullPointerException("The object instantiated from the class WifiManager is null.");
    } // end constructor WifiInfoUtils

    /**
     * Get the SSID of the connected Wi-Fi.
     * @return the SSID of the connected Wi-Fi
     */
    String getSsid()
    {
        return ssid;
    } // end method getSsid

    /**
     * Get the security type of the connected Wi-Fi.
     * @return the security type of the connected Wi-Fi
     */
    String getSecurity()
    {
        for (ScanResult scanResult : wifiManager.getScanResults())
        {
            if (ssid.equals(scanResult.SSID))
            {
                boolean hasWpaCode = scanResult.capabilities.contains((activity.getString(R.string.wifi_info_security_wpa_code)));
                boolean hasWpa2Code = scanResult.capabilities.contains((activity.getString(R.string.wifi_info_security_wpa2_code)));
                boolean hasWpa3Code = scanResult.capabilities.contains((activity.getString(R.string.wifi_info_security_wpa3_code)));
                String owe = activity.getString(R.string.wifi_info_security_owe); // terminology for Wi-Fi with OWE security
                String none = activity.getString(R.string.wifi_info_security_none); // refer to Wi-Fi with no security

                if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_wep)))
                    return activity.getString(R.string.wifi_info_security_wep); // terminology for Wi-Fi with WEP security
                else if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_eap_suiteB_code)))
                    return activity.getString(R.string.wifi_info_security_eap_suiteB); // terminology for Wi-Fi with 802.1x EAP Suite-B-192 security
                else if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_owe_transition_code)))
                    return owe + "/" + none;
                else if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_owe_code)))
                    return owe;
                else if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_psk_code)) || hasWpa3Code)
                {
                    if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_psk_sae_code)))
                        return activity.getString(R.string.wifi_info_security_psk_sae); // terminology for Wi-Fi with WPA2/WPA3 Transition mode security
                    else if ((hasWpaCode && hasWpa2Code) || (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_unknownPsk_code))))
                        return activity.getString(R.string.wifi_info_security_wpa_wpa2_unknownPsk); // terminology for Wi-Fi with both WPA/WPA2 security, or some unknown PSK types
                    else if (hasWpaCode)
                        return activity.getString(R.string.wifi_info_security_wpa); // terminology for Wi-Fi with WPA security
                    else if (hasWpa2Code)
                        return activity.getString(R.string.wifi_info_security_wpa2); // terminology for Wi-Fi with WPA2 security
                    else if (hasWpa3Code)
                        return activity.getString(R.string.wifi_info_security_wpa3); // terminology for Wi-Fi with WPA3 security
                    else
                    {
                        Log.w(TAG, "Received abnormal flag string: " + scanResult.capabilities);
                        return activity.getString(R.string.wifi_info_security_wpa_wpa2_unknownPsk); // terminology for Wi-Fi with some other unknown PSK types
                    } // end nested if...else
                }
                else if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_eap_code)))
                {
                    if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_eap_wpa_code)))
                        return activity.getString(R.string.wifi_info_security_eap_wpa); // terminology for Wi-Fi with WPA 802.1x EAP security
                    else if (scanResult.capabilities.contains(activity.getString(R.string.wifi_info_security_eap_wpa2_wpa3_code)))
                        return activity.getString(R.string.wifi_info_security_eap_wpa2_wpa3); // terminology for Wi-Fi with WPA2/WPA3 802.1x EAP security
                    else
                        return activity.getString(R.string.wifi_info_security_eap); // terminology for Wi-Fi with 802.1x EAP security
                }
                else
                    return none;
            } // end if
        } // end for

        Log.w(TAG, "Failed to get the security type. Some errors may occur.");
        return activity.getString(R.string.wifi_info_unknownResult);
    } // end method getSecurity

    /**
     * Get the frequency of the connected Wi-Fi.
     * @return the frequency of the connected Wi-Fi
     */
    String getFrequency() // TODO
    {
        int frequency = wifiInfo.getFrequency();
        Log.d(TAG, String.valueOf(frequency));
        if (frequency >= LOWER_FREQUENCY_24GHZ && frequency <= HIGHER_FREQUENCY_24GHZ)
            return activity.getString(R.string.wifi_info_frequency_24ghz);
        else if (frequency >= LOWER_FREQUENCY_5GHZ && frequency <= HIGHER_FREQUENCY_5GHZ)
            return activity.getString(R.string.wifi_info_frequency_5ghz);
        else
        {
            Log.w(TAG, "Failed to get the frequency. Some errors may occur.");
            return activity.getString(R.string.wifi_info_unknownResult);
        } // end nested if...else
    } // end method getFrequency

    /**
     * Get the signal strength of the connected Wi-Fi.
     * @return the signal strength of the connected Wi-Fi
     */
    String getSignalStrength()
    {
        switch (WifiManager.calculateSignalLevel(wifiInfo.getRssi(), SIGNAL_LEVELS))
        {
            case 0:
            default:
                Log.w(TAG, "No signal strength. Some errors may occur.");
                return activity.getString(R.string.wifi_info_signalStrength_none);

            case 1:
                return activity.getString(R.string.wifi_info_signalStrength_poor);

            case 2:
                return activity.getString(R.string.wifi_info_signalStrength_fair);

            case 3:
                return activity.getString(R.string.wifi_info_signalStrength_good);

            case 4:
                return activity.getString(R.string.wifi_info_signalStrength_excellent);
        } // end switch-case
    } // end method getSignalStrength

    /**
     * Get the link speed of the connected Wi-Fi.
     * @return the link speed of the connected Wi-Fi
     */
    String getLinkSpeed()
    {
        int linkSpeedValue = wifiInfo.getLinkSpeed();

        if (linkSpeedValue < 0)
        {
            Log.w(TAG, "Failed to get the link speed. Some errors may occur.");
            return activity.getString(R.string.wifi_info_unknownResult);
        }
        else
            return linkSpeedValue + " " + WifiInfo.LINK_SPEED_UNITS;
    } // end method getLinkSpeed

    /**
     * Get the IPv4 address of the connected Wi-Fi.
     * @return the IPv4 address of the connected Wi-Fi
     */
    String getIp()
    {
        int ip =  wifiInfo.getIpAddress();

        if (ip == 0)
        {
            Log.w(TAG, "Failed to get the IPv4 address. Some errors may occur.");
            return activity.getString(R.string.wifi_info_unknownResult);
        }
        else
            return "" + (ip & 0xFF) + "."
                    + ((ip >> 8) & 0xFF) + "."
                    + ((ip >> 16) & 0xFF) + "."
                    + ((ip >> 24) & 0xFF);
    } // end method getIp

    /**
     * Get the IPv4 gateway of the connected Wi-Fi.
     * @return the IPv4 gateway of the connected Wi-Fi
     */
    String getGateway()
    {
        if (dhcpInfo.gateway == 0)
        {
            Log.w(TAG, "Failed to get the IPv4 gateway. Some errors may occur.");
            return activity.getString(R.string.wifi_info_unknownResult);
        }
        else
            return "" + (dhcpInfo.gateway & 0xFF) + "."
                    + ((dhcpInfo.gateway >> 8) & 0xFF) + "."
                    + ((dhcpInfo.gateway >> 16) & 0xFF) + "."
                    + ((dhcpInfo.gateway >> 24) & 0xFF);
    } // end method getGateway

    /**
     * Get the IPv4 subnet mask of the connected Wi-Fi.
     * @return the IPv4 subnet mask of the connected Wi-Fi
     */
    String getSubnetMask() // TODO
    {
        try
        {
            Process process = Runtime.getRuntime().exec("ifconfig wlan0");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String[] results;
            String[] key_value;

            do
            {
                String line = bufferedReader.readLine();

                if (line == null)
                    break;
                Log.d(TAG, line);
                if (line.trim().matches("inet addr:(\\d{1,3}\\.){3}\\d{1,3}( ){2}"
                            + "(Bcast:(\\d{1,3}\\.){3}\\d{1,3}( ){2})?"
                            + "Mask:(\\d{1,3}\\.){3}\\d{1,3}"))
                {
                    results = line.trim().split("( ){2}");

                    for (String result : results)
                    {
                        if (result.length() > 0)
                        {
                            key_value = result.split(":");

                            if (key_value[0].startsWith("Mask"))
                                return key_value[1];
                        } // end if
                    } // end for
                } // end if
            } while (true);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Exception occurs.", e);
        } // end try...catch

        Log.w(TAG, "Failed to get the IPv4 subnet mask. Some errors may occur.");
        return activity.getString(R.string.wifi_info_unknownResult);
    } // end method getSubnetMask

    /**
     * Get the IPv4 DNS server(s) of the connected Wi-Fi.
     * @return the IPv4 DNS server(s) of the connected Wi-Fi
     */
    String getDnsServer()
    {
        String dnsServer1;

        if (dhcpInfo.dns1 == 0)
        {
            Log.w(TAG, "Failed to get the IPv4 DNS server. Some errors may occur.");
            return activity.getString(R.string.wifi_info_unknownResult);
        }
        else
            dnsServer1 = (dhcpInfo.dns1 & 0xFF) + "."
                    + ((dhcpInfo.dns1 >> 8) & 0xFF) + "."
                    + ((dhcpInfo.dns1 >> 16) & 0xFF) + "."
                    + ((dhcpInfo.dns1 >> 24) & 0xFF);

        if (dhcpInfo.dns2 > 0)
        {
            String dnsServer2 = (dhcpInfo.dns2 & 0xFF) + "."
                    + ((dhcpInfo.dns2 >> 8) & 0xFF) + "."
                    + ((dhcpInfo.dns2 >> 16) & 0xFF) + "."
                    + ((dhcpInfo.dns2 >> 24) & 0xFF);
            return dnsServer1 + "\n" + dnsServer2;
        } // end if

        return dnsServer1;
    } // end method getDnsServer
} // end class WifiInfoUtils