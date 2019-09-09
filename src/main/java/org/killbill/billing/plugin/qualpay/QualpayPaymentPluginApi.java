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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodInfoPlugin;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.plugin.qualpay.dao.QualpayDao;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.QualpayPaymentMethods;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.QualpayResponses;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.records.QualpayPaymentMethodsRecord;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.records.QualpayResponsesRecord;
import org.killbill.billing.plugin.util.KillBillMoney;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.customfield.CustomField;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.client.api.CustomerVaultApi;
import io.swagger.client.model.AddBillingCardRequest;
import io.swagger.client.model.AddCustomerRequest;
import io.swagger.client.model.BillingCard;
import io.swagger.client.model.CustomerResponse;
import io.swagger.client.model.DeleteBillingCardRequest;
import io.swagger.client.model.GetBillingCardsResponse;
import io.swagger.client.model.GetBillingResponse;
import qpPlatform.ApiClient;
import qpPlatform.ApiException;
import qpPlatform.Configuration;

public class QualpayPaymentPluginApi extends PluginPaymentPluginApi<QualpayResponsesRecord, QualpayResponses, QualpayPaymentMethodsRecord, QualpayPaymentMethods> {

    private static final Logger logger = LoggerFactory.getLogger(QualpayPaymentPluginApi.class);

    public static final String PROPERTY_OVERRIDDEN_TRANSACTION_STATUS = "overriddenTransactionStatus";

    private final QualpayConfigPropertiesConfigurationHandler qualpayConfigPropertiesConfigurationHandler;
    private final QualpayDao dao;

    public QualpayPaymentPluginApi(final QualpayConfigPropertiesConfigurationHandler qualpayConfigPropertiesConfigurationHandler,
                                   final OSGIKillbillAPI killbillAPI,
                                   final OSGIConfigPropertiesService configProperties,
                                   final OSGIKillbillLogService logService,
                                   final Clock clock,
                                   final QualpayDao dao) {
        super(killbillAPI, configProperties, logService, clock, dao);
        this.qualpayConfigPropertiesConfigurationHandler = qualpayConfigPropertiesConfigurationHandler;
        this.dao = dao;
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId,
                                                             final UUID kbPaymentId,
                                                             final Iterable<PluginProperty> properties,
                                                             final TenantContext context) throws PaymentPluginApiException {
        // It doesn't look like we can retrieve it from Qualpay?
        return super.getPaymentInfo(kbAccountId, kbPaymentId, properties, context);
    }

    @Override
    protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(final QualpayResponsesRecord record) {
        return QualpayPaymentTransactionInfoPlugin.build(record);
    }

    @Override
    protected PaymentMethodPlugin buildPaymentMethodPlugin(final QualpayPaymentMethodsRecord record) {
        return QualpayPaymentMethodPlugin.build(record);
    }

    @Override
    protected PaymentMethodInfoPlugin buildPaymentMethodInfoPlugin(final QualpayPaymentMethodsRecord record) {
        return new PluginPaymentMethodInfoPlugin(UUID.fromString(record.getKbAccountId()),
                                                 UUID.fromString(record.getKbPaymentMethodId()),
                                                 false,
                                                 record.getQualpayId());
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId,
                                 final UUID kbPaymentMethodId,
                                 final PaymentMethodPlugin paymentMethodProps,
                                 final boolean setDefault,
                                 final Iterable<PluginProperty> properties,
                                 final CallContext context) throws PaymentPluginApiException {
        final String qualpayCustomerIdMaybeNull = getCustomerIdNoException(kbAccountId, context);
        final Long merchantId = getMerchantId(context);

        // Sync Qualpay payment methods (source of truth)
        final ApiClient apiClient = buildApiClient(context);
        final CustomerVaultApi customerVaultApi = new CustomerVaultApi(apiClient);

        final AddBillingCardRequest billingCardsItem = new AddBillingCardRequest();

        try {
            if (qualpayCustomerIdMaybeNull == null) {
                // Create Vault and payment method
                final AddCustomerRequest addCustomerRequest = new AddCustomerRequest();
                addCustomerRequest.setAutoGenerateCustomerId(true);
                addCustomerRequest.addBillingCardsItem(billingCardsItem);
                customerVaultApi.addCustomer(addCustomerRequest);
            } else {
                // Add payment method to existing customer
                final CustomerResponse customerResponse = customerVaultApi.addBillingCard(qualpayCustomerIdMaybeNull, billingCardsItem);
                customerResponse.getData().getBillingCards().get(customerResponse.getData().getBillingCards().size() -1 );
            }
        } catch (final ApiException e) {
            throw new PaymentPluginApiException("Error connecting to Qualpay", e);
        }

        final Map<String, Object> additionalDataMap = PluginProperties.toMap(properties);
        final String qualpayId = paymentMethodProps.getExternalPaymentMethodId();//cardId
        if (paymentMethodProps.getExternalPaymentMethodId() == null) {
            throw new PaymentPluginApiException("USER", "PaymentMethodPlugin#getExternalPaymentMethodId must be passed");
        }

        final DateTime utcNow = clock.getUTCNow();
        try {
            dao.addPaymentMethod(kbAccountId, kbPaymentMethodId, additionalDataMap, qualpayId, utcNow, context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to add payment method", e);
        }
    }

    @Override
    protected String getPaymentMethodId(final QualpayPaymentMethodsRecord record) {
        return record.getKbPaymentMethodId();
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final QualpayPaymentMethodsRecord qualPayPaymentMethodsRecord;
        try {
            qualPayPaymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve payment method", e);
        }

        final DeleteBillingCardRequest deleteBillingCardRequest = new DeleteBillingCardRequest();
        deleteBillingCardRequest.setCardId(qualPayPaymentMethodsRecord.getQualpayId());
        deleteBillingCardRequest.setMerchantId(getMerchantId(context));

        final String qualpayCustomerId = getCustomerId(kbAccountId, context);

        final ApiClient apiClient = buildApiClient(context);
        final CustomerVaultApi customerVaultApi = new CustomerVaultApi(apiClient);
        try {
            // Delete the card in the Vault
            customerVaultApi.deleteBillingCard(qualpayCustomerId, deleteBillingCardRequest);
        } catch (final ApiException e) {
            throw new PaymentPluginApiException("Error connecting to Qualpay", e);
        }

        // Delete our local copy
        super.deletePaymentMethod(kbAccountId, kbPaymentMethodId, properties, context);
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // If refreshFromGateway isn't set, simply read our tables
        if (!refreshFromGateway) {
            return super.getPaymentMethods(kbAccountId, refreshFromGateway, properties, context);
        }

        // Retrieve our currently known payment methods
        final Map<String, QualpayPaymentMethodsRecord> existingPaymentMethodByQualpayId = new HashMap<String, QualpayPaymentMethodsRecord>();
        try {
            final List<QualpayPaymentMethodsRecord> existingQualpayPaymentMethodRecords = dao.getPaymentMethods(kbAccountId, context.getTenantId());
            for (final QualpayPaymentMethodsRecord existingQualpayPaymentMethodRecord : existingQualpayPaymentMethodRecords) {
                existingPaymentMethodByQualpayId.put(existingQualpayPaymentMethodRecord.getQualpayId(), existingQualpayPaymentMethodRecord);
            }
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve existing payment methods", e);
        }

        // To retrieve all payment methods in Qualpay, retrieve the Qualpay customer id (custom field on the account)
        final String qualpayCustomerId = getCustomerId(kbAccountId, context);

        // Sync Qualpay payment methods (source of truth)
        final ApiClient apiClient = buildApiClient(context);
        final CustomerVaultApi customerVaultApi = new CustomerVaultApi(apiClient);
        try {
            final GetBillingResponse billingResponse = customerVaultApi.getBillingCards(qualpayCustomerId, getMerchantId(context));
            final GetBillingCardsResponse billingCardsResponse = billingResponse.getData();
            syncPaymentMethods(kbAccountId, billingCardsResponse.getBillingCards(), existingPaymentMethodByQualpayId, context);
        } catch (final ApiException e) {
            throw new PaymentPluginApiException("Error connecting to Qualpay", e);
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException("Error creating payment method", e);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Error creating payment method", e);
        }

        for (final QualpayPaymentMethodsRecord qualpayPaymentMethodsRecord : existingPaymentMethodByQualpayId.values()) {
            logger.info("Deactivating local Qualpay payment method {} - not found in Qualpay", qualpayPaymentMethodsRecord.getQualpayId());
            super.deletePaymentMethod(kbAccountId, UUID.fromString(qualpayPaymentMethodsRecord.getKbPaymentMethodId()), properties, context);
        }

        // Refresh the state
        return super.getPaymentMethods(kbAccountId, false, properties, context);
    }

    private void syncPaymentMethods(final UUID kbAccountId,
                                    final Iterable<BillingCard> billingCards,
                                    final Map<String, QualpayPaymentMethodsRecord> existingPaymentMethodByQualpayId,
                                    final CallContext context) throws PaymentApiException, SQLException {
        for (final BillingCard billingCard : billingCards) {
            final Map<String, Object> additionalDataMap = QualpayPluginProperties.toAdditionalDataMap(billingCard);

            final QualpayPaymentMethodsRecord existingPaymentMethodRecord = existingPaymentMethodByQualpayId.remove(billingCard.getCardId());
            if (existingPaymentMethodRecord == null) {
                // We don't know about it yet, create it
                logger.info("Creating new local Qualpay payment method {}", billingCard.getCardId());
                final List<PluginProperty> properties = PluginProperties.buildPluginProperties(additionalDataMap);
                final PaymentMethodPlugin paymentMethodInfo = new QualpayPaymentMethodPlugin(null,
                                                                                             billingCard.getCardId(),
                                                                                             properties);
                killbillAPI.getPaymentApi().addPaymentMethod(getAccount(kbAccountId, context),
                                                             billingCard.getCardId(),
                                                             QualpayActivator.PLUGIN_NAME,
                                                             false,
                                                             paymentMethodInfo,
                                                             ImmutableList.<PluginProperty>of(),
                                                             context);
            } else {
                logger.info("Updating existing local Qualpay payment method {}", billingCard);
                dao.updatePaymentMethod(UUID.fromString(existingPaymentMethodRecord.getKbPaymentMethodId()),
                                        additionalDataMap,
                                        billingCard.getCardId(),
                                        clock.getUTCNow(),
                                        context.getTenantId());
            }
        }
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
        //return executeInitialTransaction(TransactionType.PURCHASE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new PaymentPluginApiException("INTERNAL", "#creditPayment not yet implemented, please contact support@killbill.io");
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @VisibleForTesting
    ApiClient buildApiClient(final TenantContext context) {
        final QualpayConfigProperties qualpayConfigProperties = qualpayConfigPropertiesConfigurationHandler.getConfigurable(context.getTenantId());

        final ApiClient apiClient = Configuration.getDefaultApiClient();
        apiClient.setUsername(qualpayConfigProperties.getApiKey());
        apiClient.setConnectTimeout(Integer.parseInt(qualpayConfigProperties.getConnectionTimeout()));
        apiClient.setReadTimeout(Integer.parseInt(qualpayConfigProperties.getReadTimeout()));
        apiClient.setUserAgent(qualpayConfigProperties.getApiKey());

        return apiClient;
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new PaymentPluginApiException("INTERNAL", "#buildFormDescriptor not yet implemented, please contact support@killbill.io");
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new PaymentPluginApiException("INTERNAL", "#processNotification not yet implemented, please contact support@killbill.io");
    }
    
    private Long getMerchantId(final TenantContext context) {
        final QualpayConfigProperties qualpayConfigProperties = qualpayConfigPropertiesConfigurationHandler.getConfigurable(context.getTenantId());
        return Long.valueOf(MoreObjects.firstNonNull(qualpayConfigProperties.getMerchantId(), "0"));
    }

    private String getCustomerId(final UUID kbAccountId, final CallContext context) throws PaymentPluginApiException {
        final String qualpayCustomerId = getCustomerIdNoException(kbAccountId, context);
        if (qualpayCustomerId == null) {
            throw new PaymentPluginApiException("INTERNAL", "Missing QUALPAY_CUSTOMER_ID custom field");
        }
        return qualpayCustomerId;
    }

    private String getCustomerIdNoException(final UUID kbAccountId, final CallContext context) {
        final List<CustomField> customFields = killbillAPI.getCustomFieldUserApi().getCustomFieldsForAccountType(kbAccountId, ObjectType.ACCOUNT, context);
        String qualpayCustomerId = null;
        for (final CustomField customField : customFields) {
            if ("QUALPAY_CUSTOMER_ID".equals(customField.getFieldName())) {
                qualpayCustomerId = customField.getFieldValue();
                break;
            }
        }
        return qualpayCustomerId;
    }

    private QualpayPaymentMethodsRecord getQualpayPaymentMethodsRecord(@Nullable final UUID kbPaymentMethodId, final TenantContext context) throws PaymentPluginApiException {
        QualpayPaymentMethodsRecord paymentMethodsRecord = null;

        if (kbPaymentMethodId != null) {
            try {
                paymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
            } catch (final SQLException e) {
                throw new PaymentPluginApiException("Failed to retrieve payment method", e);
            }
        }

        return MoreObjects.firstNonNull(paymentMethodsRecord, emptyRecord(kbPaymentMethodId));
    }

    private QualpayPaymentMethodsRecord emptyRecord(@Nullable final UUID kbPaymentMethodId) {
        final QualpayPaymentMethodsRecord record = new QualpayPaymentMethodsRecord();
        if (kbPaymentMethodId != null) {
            record.setKbPaymentMethodId(kbPaymentMethodId.toString());
        }
        return record;
    }

    private boolean shouldSkipQualpay(final Iterable<PluginProperty> properties) {
        return "true".equals(PluginProperties.findPluginPropertyValue("skipGw", properties)) || "true".equals(PluginProperties.findPluginPropertyValue("skip_gw", properties));
    }
}
