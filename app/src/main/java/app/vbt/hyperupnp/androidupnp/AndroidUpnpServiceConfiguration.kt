/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package app.vbt.hyperupnp.androidupnp

import android.os.Build
import app.vbt.hyperupnp.upnp.cling.DefaultUpnpServiceConfiguration
import app.vbt.hyperupnp.upnp.cling.binding.xml.DeviceDescriptorBinder
import app.vbt.hyperupnp.upnp.cling.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl
import app.vbt.hyperupnp.upnp.cling.binding.xml.ServiceDescriptorBinder
import app.vbt.hyperupnp.upnp.cling.binding.xml.UDA10ServiceDescriptorBinderSAXImpl
import app.vbt.hyperupnp.upnp.cling.model.Namespace
import app.vbt.hyperupnp.upnp.cling.model.ServerClientTokens
import app.vbt.hyperupnp.upnp.cling.transport.impl.RecoveringGENAEventProcessorImpl
import app.vbt.hyperupnp.upnp.cling.transport.impl.RecoveringSOAPActionProcessorImpl
import app.vbt.hyperupnp.upnp.cling.transport.impl.StreamClientConfigurationImpl
import app.vbt.hyperupnp.upnp.cling.transport.impl.StreamClientImpl
import app.vbt.hyperupnp.upnp.cling.transport.impl.StreamServerConfigurationImpl
import app.vbt.hyperupnp.upnp.cling.transport.impl.StreamServerImpl
import app.vbt.hyperupnp.upnp.cling.transport.spi.*

/**
 * Configuration settings for deployment on Android.
 *
 *
 * This configuration uses the JDK HttpURLConnection based [StreamClientImpl] for
 * outgoing HTTP (descriptor retrieval and SOAP control). The stream server is a
 * no-op: this app is a browse-only control point and never subscribes to GENA
 * events, so no inbound HTTP listener is required.
 *
 *
 *
 * The kxml2 implementation of `org.xmlpull` is available on Android, therefore
 * this configuration uses [RecoveringUDA10DeviceDescriptorBinderImpl],
 * [RecoveringSOAPActionProcessorImpl], and [RecoveringGENAEventProcessorImpl].
 *
 *
 *
 * This configuration utilizes [UDA10ServiceDescriptorBinderSAXImpl], the system property
 * `org.xml.sax.driver` is set to  `org.xmlpull.v1.sax2.Driver`.
 *
 *
 *
 * To preserve battery, the [app.vbt.hyperupnp.upnp.cling.registry.Registry] will only
 * be maintained every 3 seconds.
 *
 *
 * @author Christian Bauer
 * @author Var Bhat
 */
class AndroidUpnpServiceConfiguration @JvmOverloads constructor(streamListenPort: Int = 0) :
    DefaultUpnpServiceConfiguration(streamListenPort, false) {
    override fun createNetworkAddressFactory(streamListenPort: Int): NetworkAddressFactory {
        return AndroidNetworkAddressFactory(streamListenPort)
    }

    override fun createNamespace(): Namespace {
        return Namespace("/upnp")
    }

    override fun createStreamClient(): StreamClient<*> {
        return StreamClientImpl(
            object : StreamClientConfigurationImpl(
                syncProtocolExecutorService
            ) {
                override fun getUserAgentValue(majorVersion: Int, minorVersion: Int): String {
                    // TODO: UPNP VIOLATION: Synology NAS requires User-Agent to contain
                    // "Android" to return DLNA protocolInfo required to stream to Samsung TV
                    // see: http://two-play.com/forums/viewtopic.php?f=6&t=81
                    val tokens = ServerClientTokens(majorVersion, minorVersion)
                    tokens.osName = "Android"
                    tokens.osVersion = Build.VERSION.RELEASE
                    return tokens.toString()
                }
            }
        )
    }

    override fun createStreamServer(networkAddressFactory: NetworkAddressFactory): StreamServer<*> {
        // No-op server: browse-only control point, no GENA subscriptions or local devices
        return StreamServerImpl(
            StreamServerConfigurationImpl(networkAddressFactory.streamListenPort)
        )
    }

    override fun createDeviceDescriptorBinderUDA10(): DeviceDescriptorBinder =
        RecoveringUDA10DeviceDescriptorBinderImpl()

    override fun createServiceDescriptorBinderUDA10(): ServiceDescriptorBinder =
        UDA10ServiceDescriptorBinderSAXImpl()

    override fun createSOAPActionProcessor(): SOAPActionProcessor =
        RecoveringSOAPActionProcessorImpl()

    override fun createGENAEventProcessor(): GENAEventProcessor = RecoveringGENAEventProcessorImpl()

    // Preserve battery on Android, only run every 3 seconds
    override fun getRegistryMaintenanceIntervalMillis(): Int = 3000

    init {
        // This should be the default on Android 2.1 but it's not set by default
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver")
    }
}