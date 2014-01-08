package org.jboss.as.jbossws.jbpapp10927;
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class ExceptionWithGenericsHandlingIT
{
  private static final String name = "ejb";
  // ejb packed in test.jar is deployed by arquillian in the ear named test.ear
  private static final String ENDPOINT_URL = "http://" + getServerBindAddress() + ":" + getServerBindPort() + "/test-ejb/" + MyEJB3Bean.class.getSimpleName();
  private static final String WSDL_URL = ENDPOINT_URL + "?wsdl";

  static String getServerBindAddress() {
    String serverBindAddress = System.getProperty("jboss.bind.address");
    return serverBindAddress == null || serverBindAddress.isEmpty() ? "localhost" : serverBindAddress;
  }

  static String getServerBindPort() {
    String serverBindport = System.getProperty("jboss.bind.port");
    return serverBindport == null || serverBindport.isEmpty() ? "8080" : serverBindport;
  }

  @Deployment
  static JavaArchive createDeployment() throws Exception {

    JavaArchive archive = ShrinkWrap
        .create(JavaArchive.class, name + ".jar")
        .addClass(Pair.class)
        .addClass(PairException.class)
        .addClass(MyEJB3Bean.class)
        ;
    archive.as(ZipExporter.class).exportTo(new File("/tmp", archive.getName()), true);
    return archive;
  }
/**
 * Good WSDL schema should contain
 *
 *   <xs:complexType name="PairException">
 *     <xs:sequence>
 *       <xs:element name="age" nillable="true" type="xs:long"/>
 *       <xs:element maxOccurs="unbounded" minOccurs="0" name="flags" type="xs:boolean"/>
 *       <xs:element minOccurs="0" name="info" type="tns:pair"/>
 *     </xs:sequence>
 *   </xs:complexType>
 *
 * Bad WSDL schema
 *
 * <xs:complexType name="PairException">
 *   <xs:sequence>
 *     <xs:element maxOccurs="unbounded" minOccurs="0" name="flags" type="xs:boolean"/>
 *     <xs:element name="age" nillable="true" type="xs:long"/>
 *     <xs:element maxOccurs="unbounded" minOccurs="0" name="info" type="xs:int"/>
 *   </xs:sequence>
 * </xs:complexType>
 *
 */
  @Test
  public void testWsdlSchema () throws Exception
  {
    URL wsdlUrl = new URL(WSDL_URL);
    String wsdl = readFromUrlToString(wsdlUrl, "utf-8");
    String expected = "<xs:element minOccurs=\"0\" name=\"info\" type=\"tns:pair\" />";
    assertTrue("WSDL " + wsdl + " do not contain " + expected, wsdl.contains(expected));
  }

  @Test
  public void testSoapResponse() throws Exception
  {
    String request = "<?xml version='1.0'?>" +
        "<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/'>" +
        "  <env:Header/>" +
        "  <env:Body>" +
        "    <ns2:launcheException xmlns:ns2='http://jbpapp10927.jbossws.as.jboss.org/'>" +
        "    </ns2:launcheException>" +
        "  </env:Body>" +
        "</env:Envelope>";

    HttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(ENDPOINT_URL);
    post.setEntity(new StringEntity(request, ContentType.TEXT_XML));
    HttpResponse response = client.execute(post);
    String result = EntityUtils.toString(response.getEntity());
    assertTrue(result.contains("<first xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:int\">1</first>"));
  }


/**
 *
 * Good SOAP response:
 *
 * <detail>
 *   <ns1:PairException xmlns:ns1="http://jbpapp10927.jbossws.as.jboss.org/">
 *     <age xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/
 * XMLSchema-instance" xsi:type="xs:long">20</age>
 *     <flags xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/">false</flags>
 *     <flags xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/">false</flags>
 *     <flags xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/">true</flags>
 *     <info xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns2:pair">
 *       <first xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:int">1</first>
 *       <second xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:boolean">false</second>
 *     </info>
 *   </ns1:PairException>
 * </detail>
 *
 * Bad SOAP response:
 *
 * <detail>
 *   <ns1:PairException xmlns:ns1="http://jbpapp10927.jbossws.as.jboss.org/">
 *     <flags xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/">false</flags>
 *     <flags xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/">false</flags>
 *     <flags xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/">true</flags>
 *     <age xmlns:ns2="http://jbpapp10927.jbossws.as.jboss.org/" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:long">20</age>
 *   </ns1:PairException>
 * </detail>
 *
 */
  public static String readFromUrlToString(final URL url, String encoding) throws UnsupportedEncodingException, IOException
  {
     InputStream stream = null;
     InputStreamReader inputStream = null;
     URLConnection connection = null;
     try {
        connection = url.openConnection();
        stream = connection.getInputStream();
        inputStream = new InputStreamReader(stream, encoding);
        return IOUtils.toString(inputStream);
     } catch (ConnectException e) {
       throw new IllegalStateException("Can not read from " + url.toString() + " " + e.getMessage(), e);
     } catch (IOException e) {
       throw new IllegalStateException("Error reading from " + url.toString() + " " + e.getMessage(), e);
     } finally {
       IOUtils.close(connection); //has to be closed first, otherwise the connection is pooled and reused !!!!
       IOUtils.closeQuietly(inputStream);
       IOUtils.closeQuietly(stream);
     }
  }

}
