/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.samsungac.internal;

/**
 * The {@link SamsungACConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Rastislav Krist - Initial contribution
 */
public class SamsungACConfiguration {

    /**
     * AC hostname or IP
     */
    public String host;

    /**
     * AC MAC address
     */
    public String mac;

    /**
     * SSL certificate for client-side authentication, if needed (newer models)
     */
    public String certificate;

    /**
     * AC token
     */
    public String token;
}
