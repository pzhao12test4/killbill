/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

package com.ning.billing.util;

import javax.inject.Inject;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.apache.shiro.util.ThreadContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.ning.billing.GuicyKillbillTestSuiteNoDB;
import com.ning.billing.bus.api.PersistentBus;
import com.ning.billing.security.Permission;
import com.ning.billing.security.api.SecurityApi;
import com.ning.billing.util.api.AuditUserApi;
import com.ning.billing.util.audit.dao.AuditDao;
import com.ning.billing.util.cache.CacheControllerDispatcher;
import com.ning.billing.util.callcontext.InternalCallContextFactory;
import com.ning.billing.util.dao.NonEntityDao;
import com.ning.billing.util.glue.TestUtilModuleNoDB;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class UtilTestSuiteNoDB extends GuicyKillbillTestSuiteNoDB {

    @Inject
    protected PersistentBus eventBus;
    @Inject
    protected CacheControllerDispatcher controlCacheDispatcher;
    @Inject
    protected NonEntityDao nonEntityDao;
    @Inject
    protected InternalCallContextFactory internalCallContextFactory;
    @Inject
    protected CacheControllerDispatcher cacheControllerDispatcher;
    @Inject
    protected AuditDao auditDao;
    @Inject
    protected AuditUserApi auditUserApi;
    @Inject
    protected SecurityApi securityApi;

    @BeforeClass(groups = "fast")
    public void beforeClass() throws Exception {
        final Injector g = Guice.createInjector(Stage.PRODUCTION, new TestUtilModuleNoDB(configSource));
        g.injectMembers(this);
    }

    @BeforeMethod(groups = "fast")
    public void beforeMethod() throws Exception {
        eventBus.start();
    }

    @AfterMethod(groups = "fast")
    public void afterMethod() throws Exception {
        eventBus.stop();
    }

    // Security helpers

    protected void login(final String username) {
        logout();

        final AuthenticationToken token = new UsernamePasswordToken(username, "password");
        final Subject currentUser = SecurityUtils.getSubject();
        currentUser.login(token);
    }

    protected void logout() {
        final Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isAuthenticated()) {
            currentUser.logout();
        }
    }

    protected void configureShiro() {
        final Ini config = new Ini();
        config.addSection("users");
        config.getSection("users").put("pierre", "password, creditor");
        config.getSection("users").put("stephane", "password, refunder");
        config.addSection("roles");
        config.getSection("roles").put("creditor", Permission.INVOICE_CAN_CREDIT.toString() + "," + Permission.INVOICE_CAN_ITEM_ADJUST.toString());
        config.getSection("roles").put("refunder", Permission.PAYMENT_CAN_REFUND.toString());

        // Reset the security manager
        ThreadContext.unbindSecurityManager();

        final Factory<SecurityManager> factory = new IniSecurityManagerFactory(config);
        final SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);
    }
}
