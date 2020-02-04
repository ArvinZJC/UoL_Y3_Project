/*
 * @Description: a class containing methods for getting the info of the connected Wi-Fi
 * @Version: 1.8.0.20200131
 * @Author: Arvin Zhao
 * @Date: 2020-01-20 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-01-31 15:47:48
 */

package com.arvinzjc.xshielder;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

class WifiInfoUtils
{
    private static final String TAG = WifiInfoUtils.class.getSimpleName(); // the tag of the log from this class

    /**
     * The representation of a secured security type.
     */
    static final String SECURED_SECURITY_TYPE = "secured security type";

    /**
     * The representation of an unsecured security type.
     */
    static final String UNSECURED_SECURITY_TYPE = "unsecured security type";

    private static final int LOWER_FREQUENCY_24GHZ = 2412; // lower bound on the 2.4 GHz (802.11b/g/n) WLAN channels
    private static final int HIGHER_FREQUENCY_24GHZ = 2482; // upper bound on the 2.4 GHz (802.11b/g/n) WLAN channels
    private static final int LOWER_FREQUENCY_5GHZ = 4915; // lower bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels
    private static final int HIGHER_FREQUENCY_5GHZ = 5825; // upper bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels
    private static final int SIGNAL_LEVELS = 5; // the number of distinct wifi levels

    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    private DhcpInfo mDhcpInfo;
    private String mSsid;
    private Context mContext;

    WifiInfoUtils(@NonNull Context context) throws NetworkErrorException
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null)
        {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (networkCapabilities != null)
            {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                {
                    mContext = context;
                    mWifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    if (mWifiManager != null)
                    {
                        mWifiInfo = mWifiManager.getConnectionInfo();
                        mDhcpInfo = mWifiManager.getDhcpInfo();
                        mSsid = mWifiInfo.getSSID().replace("\"", "");
                    }
                    else
                        throw new NullPointerException("Null object instantiated from the class " + WifiManager.class.getSimpleName() + ".");
                }
                else
                    throw new NetworkErrorException("No connected Wi-Fi.");
            }
            else
                throw new NetworkErrorException("No connected network.");
        }
        else
            throw new NullPointerException("Null object instantiated from the class " + ConnectivityManager.class.getSimpleName() + ".");
    } // end constructor WifiInfoUtils

    /**
     * Get the SSID of the connected Wi-Fi.
     * @return the SSID of the connected Wi-Fi
     */
    String getSsid()
    {
        return mSsid;
    } // end method getSsid

    /**
     * Get the security type of the connected Wi-Fi and the representation indicating if the security type is secured.
     * Basically support WEP, WPA/WPA2/WPA3, WPA/WPA2/WPA3 802.1x EAP, 802.1x EAP Suite-B-192, and OWE security.
     * @return an array of the security type of the connected Wi-Fi as the first element and the representation indicating if the security type is secured as the second element
     */
    String[] getSecurity()
    {
        for (ScanResult scanResult : mWifiManager.getScanResults())
        {
            if (mSsid.equals(scanResult.SSID))
            {
                boolean hasWpaCode = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_wpa_code));
                boolean hasWpa2Code = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_wpa2_oldCode)) || scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_wpa2_newCode));
                boolean hasWpa3Code = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_wpa3_code));
                String owe = mContext.getString(R.string.wifi_info_security_owe); // terminology for Wi-Fi with OWE security
                String none = mContext.getString(R.string.wifi_info_security_none); // refer to Wi-Fi with no security

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_wep)))
                    return new String[]{mContext.getString(R.string.wifi_info_security_wep), UNSECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WEP security
                else if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_suiteB_code)))
                    return new String[]{mContext.getString(R.string.wifi_info_security_eap_suiteB), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with 802.1x EAP Suite-B-192 security
                else if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_owe_transition_code)))
                    return new String[]{owe + "/" + none, UNSECURED_SECURITY_TYPE};
                else if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_owe_code)))
                    return new String[]{owe, UNSECURED_SECURITY_TYPE};
                else if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_code)) || hasWpa3Code)
                {
                    if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_sae_code)))
                        return new String[]{mContext.getString(R.string.wifi_info_security_psk_sae), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA2/WPA3 Transition mode security
                    else if ((hasWpaCode && hasWpa2Code) || (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_unknownPsk_code))))
                        return new String[]{mContext.getString(R.string.wifi_info_security_wpa_wpa2_unknownPsk), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with both WPA/WPA2 security, or some unknown PSK types
                    else if (hasWpaCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_wpa), UNSECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA security
                    else if (hasWpa2Code)
                        return new String[]{mContext.getString(R.string.wifi_info_security_wpa2), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA2 security
                    else if (hasWpa3Code)
                        return new String[]{mContext.getString(R.string.wifi_info_security_wpa3), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA3 security
                    else
                    {
                        Log.w(TAG, "Received abnormal flag string: " + scanResult.capabilities);
                        return new String[]{mContext.getString(R.string.wifi_info_security_wpa_wpa2_unknownPsk), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with some other unknown PSK types
                    } // end nested if...else
                }
                else if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_code)))
                {
                    if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_wpa_code)))
                        return new String[]{mContext.getString(R.string.wifi_info_security_eap_wpa), UNSECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA 802.1x EAP security
                    else if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_wpa2_wpa3_code)))
                        return new String[]{mContext.getString(R.string.wifi_info_security_eap_wpa2_wpa3), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA2/WPA3 802.1x EAP security
                    else
                        return new String[]{mContext.getString(R.string.wifi_info_security_eap), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with 802.1x EAP security
                }
                else
                    return new String[]{none, UNSECURED_SECURITY_TYPE};
            } // end if
        } // end for

        Log.w(TAG, "Failed to get the security type. Some errors might occur.");
        return new String[]{mContext.getString(R.string.wifi_info_unknownResult), mContext.getString(R.string.wifi_info_unknownResult)};
    } // end method getSecurity

    /**
     * Get the frequency of the connected Wi-Fi.
     * @return the frequency of the connected Wi-Fi
     */
    String getFrequency()
    {
        int frequency = mWifiInfo.getFrequency();

        if (frequency >= LOWER_FREQUENCY_24GHZ && frequency <= HIGHER_FREQUENCY_24GHZ)
            return mContext.getString(R.string.wifi_info_frequency_24ghz);
        else if (frequency >= LOWER_FREQUENCY_5GHZ && frequency <= HIGHER_FREQUENCY_5GHZ)
            return mContext.getString(R.string.wifi_info_frequency_5ghz);
        else
        {
            Log.w(TAG, "Failed to get the frequency. Some errors might occur.");
            return mContext.getString(R.string.wifi_info_unknownResult);
        } // end nested if...else
    } // end method getFrequency

    /**
     * Get the signal strength of the connected Wi-Fi.
     * @return the signal strength of the connected Wi-Fi
     */
    String getSignalStrength()
    {
        switch (WifiManager.calculateSignalLevel(mWifiInfo.getRssi(), SIGNAL_LEVELS))
        {
            case 0:
            default:
                Log.w(TAG, "No signal strength. Some errors might occur.");
                return mContext.getString(R.string.wifi_info_signalStrength_none);

            case 1:
                return mContext.getString(R.string.wifi_info_signalStrength_poor);

            case 2:
                return mContext.getString(R.string.wifi_info_signalStrength_fair);

            case 3:
                return mContext.getString(R.string.wifi_info_signalStrength_good);

            case 4:
                return mContext.getString(R.string.wifi_info_signalStrength_excellent);
        } // end switch-case
    } // end method getSignalStrength

    /**
     * Get the link speed of the connected Wi-Fi.
     * @return the link speed of the connected Wi-Fi
     */
    String getLinkSpeed()
    {
        int linkSpeedValue = mWifiInfo.getLinkSpeed();

        if (linkSpeedValue < 0)
        {
            Log.w(TAG, "Failed to get the link speed. Some errors might occur.");
            return mContext.getString(R.string.wifi_info_unknownResult);
        }
        else
            return linkSpeedValue + " " + WifiInfo.LINK_SPEED_UNITS;
    } // end method getLinkSpeed

    /**
     * Get the Mac address of the connected Wi-Fi.
     * @return the Mac address of the connected Wi-Fi
     */
    String getMac()
    {
        String macAddress = mWifiInfo.getBSSID();
        return macAddress == null ? mContext.getString(R.string.wifi_info_unknownResult) : macAddress;
    } // getMac

    /**
     * Get the IPv4 address and subnet mask of the connected Wi-Fi.
     * @return an array of the IPv4 address as the first element and the subnet mask as the second element
     */
    String[] getIpAndSubnetMask()
    {
        int ipValue =  mWifiInfo.getIpAddress();
        String unknownResult = mContext.getString(R.string.wifi_info_unknownResult);

        if (ipValue != 0)
        {
            String ip = (ipValue & 0xFF) + "."
                    + ((ipValue >> 8) & 0xFF) + "."
                    + ((ipValue >> 16) & 0xFF) + "."
                    + ((ipValue >> 24) & 0xFF);

            try
            {
                int index;

                for (Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces(); networkInterfaceEnumeration.hasMoreElements();)
                {
                    index = 0;

                    for (Enumeration<InetAddress> inetAddressEnumeration = networkInterfaceEnumeration.nextElement().getInetAddresses(); inetAddressEnumeration.hasMoreElements();)
                    {
                        InetAddress inetAddress = inetAddressEnumeration.nextElement();

                        if (!inetAddress.isLoopbackAddress())
                        {
                            if (inetAddress.getHostAddress().contains(".") && !inetAddress.getHostAddress().contains(":"))
                            {
                                InterfaceAddress interfaceAddress = NetworkInterface.getByInetAddress(inetAddress).getInterfaceAddresses().get(index); // it should contain the IPv4 address of "wlan0" when index = 1

                                if (interfaceAddress.toString().contains(ip))
                                {
                                    int subnetMaskValue = 0xffffffff << (32 - interfaceAddress.getNetworkPrefixLength());
                                    return new String[]
                                            {
                                                    ip,
                                                    (((subnetMaskValue & 0xff000000) >> 24) & 0xff) + "."
                                                            + (((subnetMaskValue & 0x00ff0000) >> 16) & 0xff) + "."
                                                            + (((subnetMaskValue & 0x0000ff00) >> 8) & 0xff)  + "."
                                                            + ((subnetMaskValue & 0x000000ff) & 0xff)
                                            };
                                } // end if
                            } // end if

                            index++;
                        } // end if
                    } // end for
                } // end for
            }
            catch (SocketException e)
            {
                Log.e(TAG, "Exception occurred when the app tried to get the IPv4 submet mask.", e);
                return new String[]{ip, unknownResult};
            } // end try...catch
        } // end if

        Log.w(TAG, "Failed to get the IPv4 address. Hence, the IPv4 subnet mask cannot be got. Some errors might occur.");
        return new String[]{unknownResult, unknownResult};
    } // end method getIpAndSubnetMask

    /**
     * Get the IPv4 gateway of the connected Wi-Fi.
     * @return the IPv4 gateway of the connected Wi-Fi
     */
    String getGateway()
    {
        if (mDhcpInfo.gateway == 0)
        {
            Log.w(TAG, "Failed to get the IPv4 gateway. Some errors might occur.");
            return mContext.getString(R.string.wifi_info_unknownResult);
        }
        else
            return (mDhcpInfo.gateway & 0xFF) + "."
                    + ((mDhcpInfo.gateway >> 8) & 0xFF) + "."
                    + ((mDhcpInfo.gateway >> 16) & 0xFF) + "."
                    + ((mDhcpInfo.gateway >> 24) & 0xFF);
    } // end method getGateway

    /**
     * Get the IPv4 DNS server(s) of the connected Wi-Fi.
     * @return the IPv4 DNS server(s) of the connected Wi-Fi
     */
    String getDns()
    {
        String dnsServer1;

        if (mDhcpInfo.dns1 == 0)
        {
            Log.w(TAG, "Failed to get the IPv4 DNS server. Some errors might occur.");
            return mContext.getString(R.string.wifi_info_unknownResult);
        }
        else
            dnsServer1 = (mDhcpInfo.dns1 & 0xFF) + "."
                    + ((mDhcpInfo.dns1 >> 8) & 0xFF) + "."
                    + ((mDhcpInfo.dns1 >> 16) & 0xFF) + "."
                    + ((mDhcpInfo.dns1 >> 24) & 0xFF);

        if (mDhcpInfo.dns2 > 0)
        {
            String dnsServer2 = (mDhcpInfo.dns2 & 0xFF) + "."
                    + ((mDhcpInfo.dns2 >> 8) & 0xFF) + "."
                    + ((mDhcpInfo.dns2 >> 16) & 0xFF) + "."
                    + ((mDhcpInfo.dns2 >> 24) & 0xFF);
            return dnsServer1 + "\n" + dnsServer2;
        } // end if

        return dnsServer1;
    } // end method getDns
} // end class WifiInfoUtils