/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.qualpay;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.qualpay.dao.QualpayDao;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.records.QualpayPaymentMethodsRecord;

public class QualpayPaymentMethodPlugin extends PluginPaymentMethodPlugin {

    public static QualpayPaymentMethodPlugin build(final QualpayPaymentMethodsRecord QualpayPaymentMethodsRecord) {
        final Map additionalData = QualpayDao.fromAdditionalData(QualpayPaymentMethodsRecord.getAdditionalData());
        final String externalPaymentMethodId = (String) additionalData.get("id");

        return new QualpayPaymentMethodPlugin(UUID.fromString(QualpayPaymentMethodsRecord.getKbPaymentMethodId()),
                                              externalPaymentMethodId,
                                              PluginProperties.buildPluginProperties(additionalData));
    }

    public QualpayPaymentMethodPlugin(final UUID kbPaymentMethodId,
                                      final String externalPaymentMethodId,
                                      final List<PluginProperty> properties) {
        super(kbPaymentMethodId,
              externalPaymentMethodId,
              false,
              properties);
    }
}
