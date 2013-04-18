/*
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
package org.apache.access.binding.hive;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.access.binding.hive.authz.HiveAuthzBinding;
import org.apache.access.binding.hive.authz.HiveAuthzPrivileges;
import org.apache.access.binding.hive.authz.HiveAuthzPrivilegesMap;
import org.apache.access.binding.hive.conf.HiveAuthzConf;
import org.apache.access.binding.hive.conf.HiveAuthzConf.AuthzConfVars;
import org.apache.access.core.Authorizable;
import org.apache.access.core.Database;
import org.apache.access.core.Server;
import org.apache.access.core.Subject;
import org.apache.access.core.Table;
import org.apache.access.provider.file.PolicyFiles;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hive.ql.metadata.AuthorizationException;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.io.Files;

/**
 * Test for hive authz bindings
 * It uses the access.provider.file.ResourceAuthorizationProvider with the
 * resource test-authz-provider.ini
 */
public class TestHiveAuthzBindings {
  private static final String RESOURCE_PATH = "test-authz-provider.ini";
  // Servers
  private static final String SERVER1 = "server1";

  // Users
  private static final Subject ADMIN_SUBJECT = new Subject("admin1");
  private static final Subject MANAGER_SUBJECT = new Subject("manager1");
  private static final Subject ANALYST_SUBJECT = new Subject("analyst1");
  private static final Subject JUNIOR_ANALYST_SUBJECT = new Subject("junior_analyst1");

  // Databases
  private static final String CUSTOMER_DB = "customers";
  private static final String ANALYST_DB = "analyst";
  private static final String JUNIOR_ANALYST_DB = "junior_analyst";

  // Tables
  private static final String PURCHASES_TAB = "purchases";
  private static final String PAYMENT_TAB = "payments";

  // Entities
  private List<List<Authorizable>> inputTabHierarcyList = new ArrayList<List<Authorizable>>();
  private List<List<Authorizable>> outputTabHierarcyList = new ArrayList<List<Authorizable>>();
  private HiveAuthzConf authzConf =  new HiveAuthzConf();

  // Privileges
  private static final HiveAuthzPrivileges queryPrivileges =
      HiveAuthzPrivilegesMap.getHiveAuthzPrivileges(HiveOperation.QUERY);
  private static final HiveAuthzPrivileges createTabPrivileges =
      HiveAuthzPrivilegesMap.getHiveAuthzPrivileges(HiveOperation.CREATETABLE);
  private static final HiveAuthzPrivileges loadTabPrivileges =
      HiveAuthzPrivilegesMap.getHiveAuthzPrivileges(HiveOperation.LOAD);
  private static final HiveAuthzPrivileges createDbPrivileges =
      HiveAuthzPrivilegesMap.getHiveAuthzPrivileges(HiveOperation.CREATEDATABASE);
  private static final HiveAuthzPrivileges createFuncPrivileges =
      HiveAuthzPrivilegesMap.getHiveAuthzPrivileges(HiveOperation.CREATEFUNCTION);

  // auth bindings handler
  private HiveAuthzBinding testAuth = null;
  private File baseDir;

  @Before
  public void setUp() throws Exception {
    inputTabHierarcyList.clear();
    outputTabHierarcyList.clear();
    baseDir = Files.createTempDir();
    PolicyFiles.copyToDir(baseDir, RESOURCE_PATH);

    // create auth configuration
    authzConf.set(AuthzConfVars.AUTHZ_PROVIDER.getVar(),
        "org.apache.access.provider.file.LocalGroupResourceAuthorizationProvider");
    authzConf.set(AuthzConfVars.AUTHZ_PROVIDER_RESOURCE.getVar(),
        new File(baseDir, RESOURCE_PATH).getPath());
    authzConf.set(AuthzConfVars.AUTHZ_SERVER_NAME.getVar(), SERVER1);
    testAuth = new HiveAuthzBinding(authzConf);
  }

  @After
  public void teardown() {
    if(baseDir != null) {
      FileUtils.deleteQuietly(baseDir);
    }
  }

  /**
   * validate read permission for admin on customer:purchase
   */
  @Test
  public void TestValidateSelectPrivilegesForAdmin() throws Exception {
    inputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PURCHASES_TAB));
    testAuth.authorize(HiveOperation.QUERY, queryPrivileges, ADMIN_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate read permission for admin on customer:purchase
   */
  @Test
  public void TestValidateSelectPrivilegesForUsers() throws Exception {
    inputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PURCHASES_TAB));
    testAuth.authorize(HiveOperation.QUERY, queryPrivileges, ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate read permission for denied for junior analyst on customer:purchase
   */
  @Test(expected=AuthorizationException.class)
  public void TestValidateSelectPrivilegesRejectionForUsers() throws Exception {
    inputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PURCHASES_TAB));
    testAuth.authorize(HiveOperation.QUERY, queryPrivileges, JUNIOR_ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate create table permissions for admin in customer db
   */
  @Test
  public void TestValidateCreateTabPrivilegesForAdmin() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PAYMENT_TAB));
    testAuth.authorize(HiveOperation.CREATETABLE, createTabPrivileges, ADMIN_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate create table permissions for manager in junior_analyst sandbox db
   */
  @Test
  public void TestValidateCreateTabPrivilegesForUser() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, JUNIOR_ANALYST_DB, PAYMENT_TAB));
    testAuth.authorize(HiveOperation.CREATETABLE, createTabPrivileges, MANAGER_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate create table permissions denided to junior_analyst in customer db
   */
  @Test(expected=AuthorizationException.class)
  public void TestValidateCreateTabPrivilegesRejectionForUser() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, null));
    testAuth.authorize(HiveOperation.CREATETABLE, createTabPrivileges, JUNIOR_ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate create table permissions denided to junior_analyst in analyst sandbox db
   */
  @Test(expected=AuthorizationException.class)
  public void TestValidateCreateTabPrivilegesRejectionForUser2() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, ANALYST_DB, null));
    testAuth.authorize(HiveOperation.CREATETABLE, createTabPrivileges, JUNIOR_ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate load permissions for admin on customer:purchases
   */
  @Test
  public void TestValidateLoadTabPrivilegesForAdmin() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PURCHASES_TAB));
    testAuth.authorize(HiveOperation.LOAD, loadTabPrivileges, ADMIN_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate load table permissions on manager for customer:purchases
   */
  @Test
  public void TestValidateLoadTabPrivilegesForUser() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PURCHASES_TAB));
    testAuth.authorize(HiveOperation.LOAD, loadTabPrivileges, MANAGER_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);  }

  /**
   * validate load table permissions rejected for analyst on customer:purchases
   */
  @Test(expected=AuthorizationException.class)
  public void TestValidateLoadTabPrivilegesRejectionForUser() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, PURCHASES_TAB));
    testAuth.authorize(HiveOperation.LOAD, loadTabPrivileges, ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate create database permission for admin
   */
  @Test
  public void TestValidateCreateDbForAdmin() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, null));
    testAuth.authorize(HiveOperation.CREATEDATABASE, createDbPrivileges, ADMIN_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * validate create database permission for admin
   */
  @Test(expected=AuthorizationException.class)
  public void TestValidateCreateDbRejectionForUser() throws Exception {
    // Hive compiler doesn't capture Entities for DB operations
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, CUSTOMER_DB, null));
    testAuth.authorize(HiveOperation.CREATEDATABASE, createDbPrivileges, ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * Validate create function permission for admin (server level priviledge
   */
  @Test
  @Ignore // TODO fix functions
  public void TestValidateCreateFunctionForAdmin() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, null, null));
    testAuth.authorize(HiveOperation.CREATEFUNCTION, createFuncPrivileges, ADMIN_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  /**
   * Validate create function permission for admin (server level priviledge
   */
  @Test(expected=AuthorizationException.class)
  @Ignore // TODO fix functions
  public void TestValidateCreateFunctionRejectionForUser() throws Exception {
    outputTabHierarcyList.add(buildObjectHierarchy(SERVER1, null, null));
    testAuth.authorize(HiveOperation.CREATEFUNCTION, createFuncPrivileges, ANALYST_SUBJECT,
        inputTabHierarcyList, outputTabHierarcyList);
  }

  private List <Authorizable>  buildObjectHierarchy(String server, String db, String table) {
    List <Authorizable> authList = new ArrayList<Authorizable> ();
    authList.add(new Server(server));
    if (db != null) {
      authList.add(new Database(CUSTOMER_DB));
      if (table != null) {
        authList.add(new Table(PURCHASES_TAB));
      }
    }
    return authList;
  }

}