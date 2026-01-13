package org.kicktipp.greenmailloadtest;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.concurrent.CountDownLatch;

public class MailSenderWorker implements Runnable {
    private final CountDownLatch readyThreadCounter;
    private final CountDownLatch callingThreadBlocker;
    private final CountDownLatch completedThreadCounter;
    private final JavaMailSender javaMailSender;
    private final int mailsPerThread;
    private static final Logger LOG = LoggerFactory.getLogger(MailSenderWorker.class);
    private final String mailBody;

    public MailSenderWorker(
            CountDownLatch readyThreadCounter,
            CountDownLatch callingThreadBlocker,
            CountDownLatch completedThreadCounter,
            JavaMailSender javaMailSender,
            int mailsPerThread) {

        this.readyThreadCounter = readyThreadCounter;
        this.callingThreadBlocker = callingThreadBlocker;
        this.completedThreadCounter = completedThreadCounter;
        this.javaMailSender = javaMailSender;
        this.mailsPerThread = mailsPerThread;
        this.mailBody = RandomString.getText(20000);
    }

    @Override
    public void run() {
        try {
            readyThreadCounter.countDown();
            callingThreadBlocker.await();
            for (int i = 0; i < mailsPerThread; i++) {
                send("hallo@example.org", "welt@example.org");
                if (i > 0 && i % 100 == 0) {
                    LOG.info("Mails sent: {}, {}", i, Thread.currentThread().getName());
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            long count = completedThreadCounter.getCount();
            completedThreadCounter.countDown();
            if (count % 10 == 0) {
                LOG.info(count + "");
            }
        }
    }

    public void send(String from, String to) throws MessagingException {
        var mimeMessage = javaMailSender.createMimeMessage();
        var mimeMessageHelper = new MimeMessageHelper(mimeMessage);
        mimeMessageHelper.setFrom(from);
        mimeMessageHelper.setTo(to);
        mimeMessageHelper.setSubject("Hello World!");
        mimeMessageHelper.setText(mailBody);
        javaMailSender.send(mimeMessageHelper.getMimeMessage());
//        LOG.info("Mail sent, {}", Thread.currentThread().getName());
    }

}
