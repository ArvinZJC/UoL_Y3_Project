/*
 * @Description: a class containing methods for getting the info of the connected Wi-Fi
 * @Version: 1.9.2.20200209
 * @Author: Arvin Zhao
 * @Date: 2020-01-20 13:59:45
 * @Last Editors: Arvin Zhao
 * @LastEditTime : 2020-02-09 15:47:48
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

import com.apkfuns.logutils.LogUtils;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

class WifiUtils
{
    private static final String UNKNOWN_SSID = "<unknown ssid>";
    private static final String ABNORMAL_BSSID_1 = "02:00:00:00:00:00";
    private static final String ABNORMAL_BSSID_2 = "02:00:00:00:01:00";

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
    private static final int SIGNAL_LEVELS = 4; // the number of distinct Wi-Fi levels

    private NetworkCapabilities mNetworkCapabilities;
    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    private DhcpInfo mDhcpInfo;
    private String mUnknownResult, mSsid;
    private boolean mHasQuotesAroundSsid;
    private Context mContext;

    WifiUtils(@NonNull Context context) throws NetworkErrorException
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null)
        {
            mNetworkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (mNetworkCapabilities != null)
            {
                if (mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                {
                    mContext = context;
                    mWifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    if (mWifiManager != null)
                    {
                        mWifiInfo = mWifiManager.getConnectionInfo();
                        mDhcpInfo = mWifiManager.getDhcpInfo();
                        mUnknownResult = mContext.getString(R.string.unknownInfo);

                        String ssid = mWifiInfo.getSSID();
                        int ssidLength = ssid.length();

                        mHasQuotesAroundSsid = ssidLength > 1 && ssid.charAt(0) == '"' && ssid.charAt(ssidLength - 1) == '"';

                        if (mHasQuotesAroundSsid)
                            mSsid = ssid.substring(1, ssidLength - 1);
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
    } // end constructor WifiUtils

    /**
     * Get the service set identifier (SSID) of the connected Wi-Fi.
     * @return the SSID of the connected Wi-Fi
     */
    String getSsid()
    {
        if (!mHasQuotesAroundSsid && mSsid.equals(UNKNOWN_SSID))
        {
            LogUtils.w("Received abnormal SSID (" + UNKNOWN_SSID + "). Something wrong with the network or the caller has insufficient permissions.");
            return mUnknownResult;
        } // end if

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
                LogUtils.i("Security: " + scanResult.capabilities);

                String owe = mContext.getString(R.string.wifi_info_security_owe); // terminology for Wi-Fi with OWE security
                String none = mContext.getString(R.string.wifi_info_security_none); // refer to Wi-Fi with no security

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_wep)))
                    return new String[]{mContext.getString(R.string.wifi_info_security_wep), UNSECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WEP security

                boolean hasWpaPskCode = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_wpa_code));
                boolean hasWpa2PskCode = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_wpa2_oldCode)) || scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_wpa2_newCode));
                boolean hasWpa3PskCode = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_wpa3_code));

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_code)) || hasWpa3PskCode)
                {
                    if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_sae_code)))
                        return new String[]{mContext.getString(R.string.wifi_info_security_psk_sae), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA2/WPA3 Transition mode security

                    if ((hasWpaPskCode && hasWpa2PskCode) || (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_psk_unknown_code))))
                        return new String[]{mContext.getString(R.string.wifi_info_security_psk_wpa_wpa2_unknown), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with both WPA/WPA2 security, or some unknown PSK types

                    if (hasWpaPskCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_psk_wpa), UNSECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA security

                    if (hasWpa2PskCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_psk_wpa2), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA2 security

                    if (hasWpa3PskCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_psk_wpa3), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA3 security

                    LogUtils.w("Unknown security type. Received abnormal flag string.");
                    return new String[]{mContext.getString(R.string.wifi_info_security_psk_wpa_wpa2_unknown), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with some other unknown PSK types
                } // end if

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_suiteB_code)))
                    return new String[]{mContext.getString(R.string.wifi_info_security_eap_suiteB), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with 802.1x EAP Suite-B-192 security

                boolean hasWpaEapCode = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_wpa_code));
                boolean hasWpa2Wpa3EapCode = scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_wpa2_wpa3_oldCode)) || scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_wpa2_wpa3_newCode));

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_eap_code)))
                {
                    if (hasWpaEapCode && hasWpa2Wpa3EapCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_eap), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with 802.1x EAP security

                    if (hasWpa2Wpa3EapCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_eap_wpa2_wpa3), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA2/WPA3 802.1x EAP security

                    if (hasWpaEapCode)
                        return new String[]{mContext.getString(R.string.wifi_info_security_eap_wpa), UNSECURED_SECURITY_TYPE}; // terminology for Wi-Fi with WPA 802.1x EAP security

                    LogUtils.w("Unknown security type. Received abnormal flag string.");
                    return new String[]{mContext.getString(R.string.wifi_info_security_eap), SECURED_SECURITY_TYPE}; // terminology for Wi-Fi with some unknown EAP types
                } // end if

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_owe_transition_code)))
                    return new String[]{owe + "/" + none, UNSECURED_SECURITY_TYPE};

                if (scanResult.capabilities.contains(mContext.getString(R.string.wifi_info_security_owe_code)))
                    return new String[]{owe, UNSECURED_SECURITY_TYPE};

                return new String[]{none, UNSECURED_SECURITY_TYPE};
            } // end if
        } // end for

        LogUtils.w("Failed to get the security type. Some errors might occur.");
        return new String[]{mUnknownResult, mUnknownResult};
    } // end method getSecurity

    /**
     * Categorise the frequency of the connected Wi-Fi.
     * @return the frequency category of the connected Wi-Fi
     */
    String categoriseFrequency()
    {
        int frequency = mWifiInfo.getFrequency();
        LogUtils.i("Frequency: " + frequency + " " + WifiInfo.FREQUENCY_UNITS);

        if (frequency >= LOWER_FREQUENCY_24GHZ && frequency <= HIGHER_FREQUENCY_24GHZ)
            return mContext.getString(R.string.wifi_info_frequency_24ghz);
        else if (frequency >= LOWER_FREQUENCY_5GHZ && frequency <= HIGHER_FREQUENCY_5GHZ)
            return mContext.getString(R.string.wifi_info_frequency_5ghz);
        else
        {
            LogUtils.w("Failed to categorise the frequency. Some errors might occur.");
            return mUnknownResult;
        } // end nested if...else
    } // end method categoriseFrequency

    /**
     * Get the signal strength of the connected Wi-Fi.
     * @return the signal strength of the connected Wi-Fi
     */
    String getSignalStrength()
    {
        int rssi = mWifiInfo.getRssi();
        LogUtils.i("Signal strength: " + rssi + " dBm");

        switch (WifiManager.calculateSignalLevel(rssi, SIGNAL_LEVELS))
        {
            case 0:
            default:
                return mContext.getString(R.string.wifi_info_signalStrength_poor);

            case 1:
                return mContext.getString(R.string.wifi_info_signalStrength_fair);

            case 2:
                return mContext.getString(R.string.wifi_info_signalStrength_good);

            case 3:
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
            LogUtils.w("Received abnormal link speed (" + linkSpeedValue + "). Some errors might occur.");
            return mUnknownResult;
        }
        else
            return linkSpeedValue + " " + WifiInfo.LINK_SPEED_UNITS;
    } // end method getLinkSpeed

    /**
     * Get the basic service set identifier (BSSID, also seen as the Wi-Fi Mac address) of the connected Wi-Fi.
     * @return the BSSID of the connected Wi-Fi
     */
    String getBssid()
    {
        String bssid = mWifiInfo.getBSSID();

        if (bssid == null)
        {
            LogUtils.w("Received abnormal BSSID (null String object). Something wrong with the network.");
            return mUnknownResult;
        } // end if

        if (bssid.equals(ABNORMAL_BSSID_1))
        {
            LogUtils.w("Received abnormal BSSID (" + ABNORMAL_BSSID_1 + "). The caller has insufficient permissions.");
            return mUnknownResult;
        } // end if

        if (bssid.equals(ABNORMAL_BSSID_2))
        {
            LogUtils.w("Received abnormal BSSID (" + ABNORMAL_BSSID_2 + "). Some errors might occur.");
            return mUnknownResult;
        } // end if

        return bssid;
    } // getBssid

    /**
     * Get the IPv4 address and subnet mask of the connected Wi-Fi.
     * @return an array of the IPv4 address as the first element and the subnet mask as the second element
     */
    String[] getIpAndSubnetMask()
    {
        int ipValue =  mWifiInfo.getIpAddress();

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
                LogUtils.e("Exception occurred when the app tried to get the IPv4 submet mask.");
                LogUtils.e(e);
                return new String[]{ip, mUnknownResult};
            } // end try...catch
        } // end if

        LogUtils.w("Received abnormal IPv4 address value (" + ipValue + "). Hence, the IPv4 subnet mask cannot be got. Some errors might occur.");
        return new String[]{mUnknownResult, mUnknownResult};
    } // end method getIpAndSubnetMask

    /**
     * Get the IPv4 gateway of the connected Wi-Fi.
     * @return the IPv4 gateway of the connected Wi-Fi
     */
    String getGateway()
    {
        if (mDhcpInfo.gateway == 0)
        {
            LogUtils.w("Received abnormal IPv4 gateway value (" + mDhcpInfo.gateway + "). Some errors might occur.");
            return mUnknownResult;
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
            LogUtils.w("Received abnormal IPv4 DNS server value (" + mDhcpInfo.dns1 + "). Some errors might occur.");
            return mUnknownResult;
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

    /**
     * Validate if the connectivity on the connected Wi-Fi is validated so that the user can access the Internet.
     * @return true if the connectivity is validated; otherwise, false
     */
    boolean isValidatedConnectivity()
    {
        return mNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    } // end method isValidatedConnectivity

    /**
     * The method is treated as the one which can validate if there is DNS spoofing.
     * Note: the method should never be called in the main thread.
     * @return true if DNS is secured; otherwise, false
     */
    boolean isSecuredDns()
    {
        try
        {
            InetAddress[] inetAddressArray = InetAddress.getAllByName("github.com");

            LogUtils.i("DNS security:");
            LogUtils.i(inetAddressArray);
            return true;
        }
        catch (UnknownHostException e)
        {
            LogUtils.e("Exception occurred when the app checked DNS security.\n" + e.getMessage());
            return false;
        } // end try...catch
    } // end method isSecuredDns

    /**
     * The method is treated as the one which can validate if there is SSL spoofing.
     * @return true if SSL is secured; otherwise, false
     */
    boolean isSecuredSsl()
    {
        try
        {
            new URL("https://github.com").openConnection();
            return true;
        }
        catch (Exception e)
        {
            LogUtils.e("Exception occurred when the app checked SSL security.");
            LogUtils.e(e);
            return false;
        } // end try...catch
    } // end method isSecuredSsl
} // end class WifiUtils