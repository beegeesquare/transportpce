/*
 * Copyright © 2021 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev200327.TcaNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcaListener710 {

    private static final Logger LOG = LoggerFactory.getLogger(TcaListener710.class);

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(TcaNotification.class, this::onTcaNotification)));
    }

    /**
     * Callback for tca-notification.
     * @param notification TcaNotification object
     */
    private void onTcaNotification(TcaNotification notification) {
        LOG.info("Notification {} received {}", TcaNotification.QNAME, notification);
    }

}