/**
 *    Copyright 2013 jwm123
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.jwm123.loggly.reporter;

import org.apache.commons.lang3.StringUtils;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

/**
 * com.jwm123.loggly.reporter.ReportMailer
 *
 * @author jmcentire
 */
public class ReportMailer {
  public static final String DEFAULT_SUBJECT = "Loggly Report";
  private Configuration config;
  private String[] recipients;
  private byte[] reportContent;
  private String subject;
  private String fileName;

  public ReportMailer(Configuration config, String recipients[], String subject, String fileName, byte reportContent[]) {
    this.config = config;
    this.recipients = recipients;
    this.reportContent = reportContent;
    if(StringUtils.isNotBlank(subject)){
      this.subject = subject;
    } else {
      this.subject = DEFAULT_SUBJECT;
    }
    this.fileName = fileName;
  }

  public void send() throws MessagingException {
    if(StringUtils.isNotBlank(config.getMailServer()) && recipients.length > 0) {
      Properties props = new Properties();
      props.setProperty("mail.transport.protocol", "smtp");
      props.setProperty("mail.smtp.port", config.getMailPort().toString());
      props.setProperty("mail.smtp.host", config.getMailServer());
      if(StringUtils.isNotBlank(config.getMailUsername()) && StringUtils.isNotBlank(config.getMailPassword())) {
        props.setProperty("mail.smtp.user", config.getMailUsername());
        props.setProperty("mail.smtp.password", config.getMailPassword());
      }

      Session session = Session.getDefaultInstance(props);

      MimeMessage message = new MimeMessage(session);
      message.addFrom(new Address[] {new InternetAddress(config.getMailFrom())});
      message.setSubject(subject);
      for(String recipient : recipients) {
        message.addRecipient(RecipientType.TO, new InternetAddress(recipient));
      }

      MimeMultipart containingMultipart = new MimeMultipart("mixed");

      MimeMultipart messageMultipart = new MimeMultipart("alternative");
      containingMultipart.addBodyPart(newMultipartBodyPart(messageMultipart));

      messageMultipart.addBodyPart(newTextBodyPart(getText()));

      MimeMultipart htmlMultipart = new MimeMultipart("related");
      htmlMultipart.addBodyPart(newHtmlBodyPart(getHtml()));
      messageMultipart.addBodyPart(newMultipartBodyPart(htmlMultipart));

      containingMultipart.addBodyPart(addReportAttachment());

      message.setContent(containingMultipart);

      Transport.send(message);
    }
  }

  public MimeBodyPart addReportAttachment() throws MessagingException {
    MimeBodyPart mimeBodyPart = new MimeBodyPart();
    String mimeType = "application/vnd.ms-excel";
    if(fileName.endsWith(".xlsx")) {
      mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
    mimeBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(reportContent, mimeType)));
    mimeBodyPart.setDisposition("attachment");
    mimeBodyPart.setFileName(fileName);

    return mimeBodyPart;
  }

  public MimeBodyPart newMultipartBodyPart(Multipart multipart)
      throws MessagingException {
    MimeBodyPart mimeBodyPart = new MimeBodyPart();
    mimeBodyPart.setContent(multipart);
    return mimeBodyPart;
  }

  public MimeBodyPart newTextBodyPart(String text)
      throws MessagingException {
    MimeBodyPart mimeBodyPart = new MimeBodyPart();
    mimeBodyPart.setContent(text, "text/plain; charset=UTF-8");
    return mimeBodyPart;
  }

  public MimeBodyPart newHtmlBodyPart(String html)
      throws MessagingException {
    MimeBodyPart mimeBodyPart = new MimeBodyPart();
    mimeBodyPart.setContent(html, "text/html; charset=UTF-8");
    return mimeBodyPart;
  }

  private String getText() {
    return "Here is a Loggly report.";
  }

  private String getHtml() {
    return "<p>Here is a Loggly report.</p>";
  }

}
