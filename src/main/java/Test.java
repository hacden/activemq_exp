import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class Test {
    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        ConnectionFactory connectionFactory = new
                ActiveMQConnectionFactory("tcp://"+ip+":61616");
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue("tempQueue");

        MessageProducer producer = session.createProducer(destination);
        Message message = session.createObjectMessage("123");
        producer.send(message);

        connection.close();

    }
}