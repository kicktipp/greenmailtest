package org.kicktipp.greenmailloadtest;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.management.HotSpotDiagnosticMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@SpringBootApplication
public class GreenmailLoadTestApplication implements CommandLineRunner {

    private static int numberOfThreads = 10;
    private static int mailsPerThread = 2;
    private static final Logger LOG = LoggerFactory.getLogger(GreenmailLoadTestApplication.class);

    public static void main(String[] args) {
        parseArgs(args);
        logMemory();
        SpringApplication.run(GreenmailLoadTestApplication.class, args).close();
    }

    private static void parseArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("numberOfThreads=")) {
                numberOfThreads = Integer.parseInt(arg.split("=")[1]);
            }
            if (arg.startsWith("mailsPerThread=")) {
                mailsPerThread = Integer.parseInt(arg.split("=")[1]);
            }
        }
        LOG.info("numberOfThreads: {}", numberOfThreads);
        LOG.info("mailsPerThread: {}", mailsPerThread);
    }

    private static void logMemory() {
        int mb = 1024 * 1024;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
        long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;

        LOG.info("Initial Memory (xms) : {} mb", xms);
        LOG.info("Max Memory (xmx) : {} mb", xmx);
        final HotSpotDiagnosticMXBean hsdiag = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (hsdiag != null) {
            var value = Long.parseLong(hsdiag.getVMOption("MaxDirectMemorySize").getValue()) / mb;
            LOG.info("MaxDirectMemorySize : {} mb", value);
        }
    }

    @Bean(destroyMethod = "stop")
    public GreenMail greenMail() throws IOException {
        var smtp = new ServerSetup(getPort(), null, ServerSetup.PROTOCOL_SMTP);
        var imap = new ServerSetup(getPort(), null, ServerSetup.PROTOCOL_IMAP);
        final var greenMail = new GreenMail(new ServerSetup[]{smtp, imap});
        greenMail.start();
        greenMail.getManagers().getUserManager().setAuthRequired(false);
//        greenMail.setUser("welt@example.org", "welt@example.org");
        greenMail.setUser("hallo@example.org", "welt@example.org");
        return greenMail;
    }

    private int getPort() throws IOException {
        var socket = new ServerSocket(0);
        var localPort = socket.getLocalPort();
        socket.close();
        return localPort;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        try {
            var javaMailSender = new JavaMailSenderImpl();
            javaMailSender.setHost("localhost");
            int port = greenMail().getSmtp().getPort();
            javaMailSender.setPort(port);
            return javaMailSender;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        LOG.info("EXECUTING : command line runner");
        var readyThreadCounter = new CountDownLatch(numberOfThreads);
        var callingThreadBlocker = new CountDownLatch(1);
        var completedThreadCounter = new CountDownLatch(numberOfThreads);
        final AtomicInteger i = new AtomicInteger(0);
        List<Thread> workers = Stream
                .generate(() -> {
                    var t = new Thread(new MailSenderWorker(readyThreadCounter, callingThreadBlocker, completedThreadCounter, javaMailSender(), mailsPerThread));
                    t.setName("Worker-" + i.getAndIncrement());
                    return t;
                })
                .limit(numberOfThreads)
                .toList();
        workers.forEach(Thread::start);
        readyThreadCounter.await();
        LOG.info("All threads ready");
        Thread.sleep(50);
        callingThreadBlocker.countDown();
        LOG.info("All threads started");
        completedThreadCounter.await();
        LOG.info("All threads finished");
        LOG.info("Mails received: {}", greenMail().getReceivedMessages().length);
    }
}
