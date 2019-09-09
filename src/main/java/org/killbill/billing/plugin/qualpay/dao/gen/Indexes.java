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

/*
 * This file is generated by jOOQ.
*/
package org.killbill.billing.plugin.qualpay.dao.gen;


import javax.annotation.Generated;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.AbstractKeys;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.QualpayPaymentMethods;
import org.killbill.billing.plugin.qualpay.dao.gen.tables.QualpayResponses;


/**
 * A class modelling indexes of tables of the <code>killbill</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index QUALPAY_PAYMENT_METHODS_PRIMARY = Indexes0.QUALPAY_PAYMENT_METHODS_PRIMARY;
    public static final Index QUALPAY_PAYMENT_METHODS_QUALPAY_PAYMENT_METHODS_KB_PAYMENT_ID = Indexes0.QUALPAY_PAYMENT_METHODS_QUALPAY_PAYMENT_METHODS_KB_PAYMENT_ID;
    public static final Index QUALPAY_PAYMENT_METHODS_QUALPAY_PAYMENT_METHODS_QUALPAY_ID = Indexes0.QUALPAY_PAYMENT_METHODS_QUALPAY_PAYMENT_METHODS_QUALPAY_ID;
    public static final Index QUALPAY_PAYMENT_METHODS_RECORD_ID = Indexes0.QUALPAY_PAYMENT_METHODS_RECORD_ID;
    public static final Index QUALPAY_RESPONSES_PRIMARY = Indexes0.QUALPAY_RESPONSES_PRIMARY;
    public static final Index QUALPAY_RESPONSES_QUALPAY_RESPONSES_KB_PAYMENT_ID = Indexes0.QUALPAY_RESPONSES_QUALPAY_RESPONSES_KB_PAYMENT_ID;
    public static final Index QUALPAY_RESPONSES_QUALPAY_RESPONSES_KB_PAYMENT_TRANSACTION_ID = Indexes0.QUALPAY_RESPONSES_QUALPAY_RESPONSES_KB_PAYMENT_TRANSACTION_ID;
    public static final Index QUALPAY_RESPONSES_QUALPAY_RESPONSES_QUALPAY_ID = Indexes0.QUALPAY_RESPONSES_QUALPAY_RESPONSES_QUALPAY_ID;
    public static final Index QUALPAY_RESPONSES_RECORD_ID = Indexes0.QUALPAY_RESPONSES_RECORD_ID;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 extends AbstractKeys {
        public static Index QUALPAY_PAYMENT_METHODS_PRIMARY = createIndex("PRIMARY", QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS, new OrderField[] { QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS.RECORD_ID }, true);
        public static Index QUALPAY_PAYMENT_METHODS_QUALPAY_PAYMENT_METHODS_KB_PAYMENT_ID = createIndex("qualpay_payment_methods_kb_payment_id", QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS, new OrderField[] { QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID }, true);
        public static Index QUALPAY_PAYMENT_METHODS_QUALPAY_PAYMENT_METHODS_QUALPAY_ID = createIndex("qualpay_payment_methods_qualpay_id", QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS, new OrderField[] { QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS.QUALPAY_ID }, false);
        public static Index QUALPAY_PAYMENT_METHODS_RECORD_ID = createIndex("record_id", QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS, new OrderField[] { QualpayPaymentMethods.QUALPAY_PAYMENT_METHODS.RECORD_ID }, true);
        public static Index QUALPAY_RESPONSES_PRIMARY = createIndex("PRIMARY", QualpayResponses.QUALPAY_RESPONSES, new OrderField[] { QualpayResponses.QUALPAY_RESPONSES.RECORD_ID }, true);
        public static Index QUALPAY_RESPONSES_QUALPAY_RESPONSES_KB_PAYMENT_ID = createIndex("qualpay_responses_kb_payment_id", QualpayResponses.QUALPAY_RESPONSES, new OrderField[] { QualpayResponses.QUALPAY_RESPONSES.KB_PAYMENT_ID }, false);
        public static Index QUALPAY_RESPONSES_QUALPAY_RESPONSES_KB_PAYMENT_TRANSACTION_ID = createIndex("qualpay_responses_kb_payment_transaction_id", QualpayResponses.QUALPAY_RESPONSES, new OrderField[] { QualpayResponses.QUALPAY_RESPONSES.KB_PAYMENT_TRANSACTION_ID }, false);
        public static Index QUALPAY_RESPONSES_QUALPAY_RESPONSES_QUALPAY_ID = createIndex("qualpay_responses_qualpay_id", QualpayResponses.QUALPAY_RESPONSES, new OrderField[] { QualpayResponses.QUALPAY_RESPONSES.QUALPAY_ID }, false);
        public static Index QUALPAY_RESPONSES_RECORD_ID = createIndex("record_id", QualpayResponses.QUALPAY_RESPONSES, new OrderField[] { QualpayResponses.QUALPAY_RESPONSES.RECORD_ID }, true);
    }
}
