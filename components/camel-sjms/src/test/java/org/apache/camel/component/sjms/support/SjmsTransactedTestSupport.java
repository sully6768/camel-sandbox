/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms.support;

import javax.transaction.TransactionManager;


import com.atomikos.icatch.jta.UserTransactionManager;

public abstract class SjmsTransactedTestSupport extends
        SjmsConnectionTestSupport {

    private TransactionManager transactionManager;

    @Override
    public void setup() throws Exception {
        super.setup();
        if (transactionManager == null) {
            createTransactionManager();
        }
    }
    
    @Override
    public void teardown() throws Exception {
        if (transactionManager == null) {
            ((UserTransactionManager) transactionManager).close();
            transactionManager = null;
        }
        super.teardown();
    }

    /**
     * Sets the TransactionManager value of transactionManager for this instance
     * of SjmsConnectionTestSupport.
     * 
     * @param transactionManager
     *            Sets TransactionManager, default is TODO add default
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Gets the TransactionManager value of transactionManager for this instance
     * of SjmsConnectionTestSupport.
     * 
     * @return the transactionManager
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Gets the TransactionManager value of transactionManager for this instance
     * of SjmsConnectionTestSupport.
     * 
     * @return the transactionManager
     * @throws Exception 
     */
    protected void createTransactionManager() throws Exception {
        transactionManager = new UserTransactionManager();
        ((UserTransactionManager) transactionManager).setForceShutdown(false);
        ((UserTransactionManager) transactionManager).setTransactionTimeout(300);
        ((UserTransactionManager) transactionManager).init();

    }

}
